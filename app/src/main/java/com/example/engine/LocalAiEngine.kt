package com.example.engine

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * The real local model behind the AI window.
 *
 * There is no cloud call here. [MODEL_URL] is fetched once over HTTPS, checked against the
 * published SHA-256, and stored in the Alpine guest's /root/models. Inference is a
 * `llama-server` process bound to 127.0.0.1:8081 inside the proot guest, and the app talks to
 * it over the same loopback HTTP path VS Code already uses.
 *
 * The rules this class keeps:
 *  - "Ready" is only reported after `/health` answers 200, never after the launch command.
 *  - The model is not bundled in the APK and is not loaded at boot; the customer opts in.
 *  - A failed download leaves no file that could be mistaken for a usable model.
 *  - Every chat is a stream with a caller-supplied token sink, so the UI can be stopped.
 */
class LocalAiEngine(private val context: Context) {

    data class ChatTurn(val isUser: Boolean, val text: String)

    data class ChatResult(
        val text: String,
        val tokenCount: Int,
        val seconds: Double,
        val tokensPerSecond: Float?,
        /** False when tokenCount is really the number of SSE chunks, not model tokens. */
        val exactTokenCount: Boolean
    )

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.SECONDS)
        .build()

    private val chatClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    /** A stalled 1 GB download must fail, not hold the setup coroutine forever. */
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.MINUTES)
        .build()

    @Volatile
    private var activeChat: Call? = null

    val modelFile: File
        get() = File(LinuxRuntime.container(context).rootfs, "$MODEL_DIR_IN_GUEST/$MODEL_FILE_NAME")

    private val partialFile: File
        get() = File(modelFile.parentFile, "$MODEL_FILE_NAME.part")

    private val pidFile: File
        get() = File(LinuxRuntime.container(context).rootfs, "root/models/llama-server.pid")

    private val serverLog: File
        get() = File(LinuxRuntime.container(context).rootfs, "root/models/llama-server.log")

    fun hasModel(): Boolean = modelFile.isFile && modelFile.length() == MODEL_BYTES

    fun modelBytes(): Long = if (modelFile.isFile) modelFile.length() else 0L

    fun freeBytes(): Long {
        val dir = modelFile.parentFile ?: context.filesDir
        dir.mkdirs()
        return runCatching { StatFs(dir.absolutePath).availableBytes }.getOrDefault(0L)
    }

    fun cancelChat() {
        activeChat?.cancel()
    }

    /** Starts the guest if it is not already alive. Blocking shell boot stays off the main thread. */
    suspend fun ensureContainer(): Boolean = withContext(Dispatchers.IO) {
        if (LinuxRuntime.isRunning) return@withContext true
        runCatching { LinuxRuntime.start(context) }.isSuccess && LinuxRuntime.isRunning
    }

    suspend fun isServerPackageInstalled(): Boolean = withContext(Dispatchers.IO) {
        if (!LinuxRuntime.isRunning) return@withContext false
        val output = LinuxRuntime.run("command -v llama-server 2>/dev/null", 10_000)
        output?.lineSequence()?.any { it.contains("llama-server") } == true
    }

    suspend fun isServerHealthy(): Boolean = withContext(Dispatchers.IO) {
        try {
            probeClient.newCall(Request.Builder().url("$BASE_URL/health").build()).execute().use {
                it.isSuccessful
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Installs Alpine's llama-server package, enabling the community repository if the image
     * only has main. The package is what the current ROADMAP notes had wrong: `llama.cpp`
     * provides the CLI, while `llama-server` is the package with the HTTP endpoint.
     */
    suspend fun ensureServerPackage(onLog: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        if (isServerPackageInstalled()) return@withContext Result.success(Unit)
        if (!ensureContainer()) {
            return@withContext Result.failure(
                IllegalStateException("The Alpine guest did not start, so llama-server cannot be installed.")
            )
        }
        onLog("Installing llama-server in Alpine (community repository)...")
        val addRepo = "if ! grep -q '/community' /etc/apk/repositories 2>/dev/null; then " +
            "echo \"https://dl-cdn.alpinelinux.org/alpine/v\$(cut -d. -f1,2 /etc/alpine-release)/community\" " +
            ">> /etc/apk/repositories; fi"
        val command = "$addRepo; apk add --no-cache llama-server 2>&1"
        val output = LinuxRuntime.run(command, timeoutMs = 600_000)
        if (output == null) {
            return@withContext Result.failure(
                IllegalStateException("The guest did not finish installing llama-server within 10 minutes.")
            )
        }
        output.lineSequence().filter { it.isNotBlank() }.toList().takeLast(8).forEach(onLog)
        if (!isServerPackageInstalled()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "llama-server is still missing after apk add. Last output: " +
                        output.lineSequence().filter { it.isNotBlank() }.toList().takeLast(3).joinToString(" | ")
                )
            )
        }
        onLog("llama-server installed.")
        Result.success(Unit)
    }

    /**
     * Downloads the GGUF into the guest and verifies both length and SHA-256. The partial file
     * is removed on failure or cancellation, so a half download can never be started as a model.
     */
    suspend fun downloadModel(
        onProgress: (downloaded: Long, total: Long) -> Unit,
        onLog: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (hasModel()) return@withContext Result.success(modelFile)

        val dir = modelFile.parentFile ?: return@withContext Result.failure(
            IllegalStateException("The guest model directory is unavailable.")
        )
        if (!dir.exists() && !dir.mkdirs()) {
            return@withContext Result.failure(
                IllegalStateException("Could not create ${dir.absolutePath}.")
            )
        }
        if (modelFile.exists() && !hasModel()) modelFile.delete()
        val free = freeBytes()
        val required = MODEL_BYTES + 200L * 1024 * 1024
        if (free in 1 until required) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Not enough free space: ${formatMb(free)} MB available, " +
                        "${formatMb(required)} MB needed for the model and working room."
                )
            )
        }

        partialFile.delete()
        var completed = false
        try {
            onLog("Downloading $MODEL_DISPLAY_NAME (${formatMb(MODEL_BYTES)} MB)...")
            val request = Request.Builder()
                .url(MODEL_URL)
                .header("User-Agent", "Phonix-OS/1.3 (offline model setup)")
                .build()
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Model download failed: HTTP ${response.code}.")
                    )
                }
                val body = response.body
                    ?: return@withContext Result.failure(
                        IllegalStateException("Model download returned an empty body.")
                    )
                val total = body.contentLength().takeIf { it > 0 } ?: MODEL_BYTES
                if (total != MODEL_BYTES) {
                    onLog("Warning: server reported $total bytes; the pinned model is $MODEL_BYTES.")
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                var lastReport = 0L
                onProgress(0L, MODEL_BYTES)
                FileOutputStream(partialFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 250L) {
                                onProgress(downloaded, MODEL_BYTES)
                                lastReport = now
                            }
                        }
                    }
                    output.fd.sync()
                }
                onProgress(downloaded, MODEL_BYTES)
                if (downloaded != MODEL_BYTES) {
                    partialFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Download stopped at ${formatMb(downloaded)} of ${formatMb(MODEL_BYTES)} MB."
                        )
                    )
                }
                val actualHash = digest.digest().toHex()
                if (!actualHash.equals(MODEL_SHA256, ignoreCase = true)) {
                    partialFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException(
                            "The model failed its SHA-256 check (got ${actualHash.take(12)}..., " +
                                "expected ${MODEL_SHA256.take(12)}...). Nothing was installed."
                        )
                    )
                }
                if (!partialFile.renameTo(modelFile)) {
                    partialFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException("The verified model could not be moved into place.")
                    )
                }
                completed = true
                onLog("Model verified: ${formatMb(MODEL_BYTES)} MB, SHA-256 matches.")
                Result.success(modelFile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(
                IllegalStateException("Model download failed: ${e.message ?: e.javaClass.simpleName}", e)
            )
        } finally {
            if (!completed && partialFile.exists()) partialFile.delete()
        }
    }

    /** Launches llama-server, then waits for a real 200 from /health. */
    suspend fun startServer(onLog: (String) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        if (!ensureContainer()) {
            return@withContext Result.failure(
                IllegalStateException("The Alpine guest is not running, so the model cannot start.")
            )
        }
        if (!hasModel()) {
            return@withContext Result.failure(
                IllegalStateException("The model file is missing or incomplete. Run setup again.")
            )
        }
        if (isServerHealthy()) {
            onLog("llama-server is already answering on 127.0.0.1:$PORT.")
            return@withContext Result.success(Unit)
        }
        stopServerNow()
        serverLog.parentFile?.mkdirs()
        serverLog.delete()
        onLog("Starting llama-server on 127.0.0.1:$PORT (CPU, 4 threads)...")
        val command = "cd /root/models && nohup llama-server -m $MODEL_FILE_NAME " +
            "--host 127.0.0.1 --port $PORT -c 4096 -t 4 > $SERVER_LOG_IN_GUEST 2>&1 & " +
            "echo \$! > $PID_FILE_IN_GUEST; sleep 1; cat $PID_FILE_IN_GUEST"
        val pid = LinuxRuntime.run(command, timeoutMs = 20_000)
        if (pid.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("The guest did not launch llama-server. ${serverLogTail()}")
            )
        }
        onLog("llama-server pid ${pid.trim().lineSequence().lastOrNull { it.isNotBlank() } ?: pid.trim()}")

        val deadline = System.currentTimeMillis() + 120_000L
        while (System.currentTimeMillis() < deadline) {
            currentCoroutineContext().ensureActive()
            if (!LinuxRuntime.isRunning) {
                return@withContext Result.failure(
                    IllegalStateException("The Alpine guest stopped while loading the model.")
                )
            }
            if (isServerHealthy()) {
                onLog("Health check passed. The model is ready.")
                return@withContext Result.success(Unit)
            }
            delay(700L)
        }
        Result.failure(
            IllegalStateException(
                "llama-server did not answer /health within 2 minutes. ${serverLogTail()}"
            )
        )
    }

    suspend fun stopServer() = withContext(Dispatchers.IO) {
        stopServerNow()
    }

    /** Synchronous variant for onCleared(), where suspending is not possible. */
    fun stopServerNow() {
        cancelChat()
        if (!LinuxRuntime.isRunning) {
            pidFile.delete()
            return
        }
        LinuxRuntime.send(
            "if [ -f $PID_FILE_IN_GUEST ]; then kill \$(cat $PID_FILE_IN_GUEST) 2>/dev/null || true; " +
                "rm -f $PID_FILE_IN_GUEST; fi"
        )
    }

    fun serverLogTail(maxLines: Int = 6): String {
        val lines = runCatching { serverLog.readLines() }.getOrNull()
            ?.filter { it.isNotBlank() }
            ?.takeLast(maxLines)
            .orEmpty()
        return if (lines.isEmpty()) "No server log was written." else lines.joinToString(" | ")
    }

    /**
     * Streams `/v1/chat/completions`. Tokens are delivered as they arrive; the caller owns the
     * UI buffer. Cancellation closes the socket through [activeChat].
     */
    suspend fun streamChat(
        systemPrompt: String,
        referenceNotes: String?,
        history: List<ChatTurn>,
        onToken: (String) -> Unit
    ): Result<ChatResult> = withContext(Dispatchers.IO) {
        if (!isServerHealthy()) {
            return@withContext Result.failure(
                IllegalStateException("The local model is not listening on 127.0.0.1:$PORT.")
            )
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        if (!referenceNotes.isNullOrBlank()) {
            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "Reference notes from the device's offline knowledge base. Use them when " +
                            "they answer the question; do not mention this message or invent sources.\n\n" +
                            referenceNotes
                    )
            )
        }
        history.takeLast(8).forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", if (turn.isUser) "user" else "assistant")
                    .put("content", turn.text.take(4000))
            )
        }
        val payload = JSONObject()
            .put("model", MODEL_FILE_NAME)
            .put("messages", messages)
            .put("stream", true)
            .put("temperature", 0.4)
            .put("top_p", 0.9)
            .put("max_tokens", 512)
            .put("stream_options", JSONObject().put("include_usage", true))

        val request = Request.Builder()
            .url("$BASE_URL/v1/chat/completions")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val call = chatClient.newCall(request)
        activeChat = call
        val completionJob = currentCoroutineContext()[Job]
        completionJob?.invokeOnCompletion { call.cancel() }

        val started = System.currentTimeMillis()
        val text = StringBuilder()
        var chunks = 0
        var usageTokens = 0
        var timingTokens = 0
        var timingTps: Float? = null
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val error = response.body?.string()?.take(400).orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException(
                            "The model returned HTTP ${response.code}${if (error.isBlank()) "." else ": $error"}"
                        )
                    )
                }
                val source = response.body?.source()
                    ?: return@withContext Result.failure(
                        IllegalStateException("The model returned an empty response.")
                    )
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val event = runCatching { JSONObject(data) }.getOrNull() ?: continue
                    event.optJSONObject("error")?.let { error ->
                        return@withContext Result.failure(
                            IllegalStateException("Model error: ${error.optString("message", data)}")
                        )
                    }
                    event.optJSONObject("usage")?.let { usage ->
                        usageTokens = usage.optInt("completion_tokens", usageTokens)
                    }
                    event.optJSONObject("timings")?.let { timings ->
                        timingTokens = timings.optInt("predicted_n", timingTokens)
                        val tps = timings.optDouble("predicted_per_second", 0.0)
                        if (tps > 0.0) timingTps = tps.toFloat()
                    }
                    val deltaObject = event.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                    // The role delta has "content": null. JSONObject.optString turns that into
                    // the literal string "null", which used to appear in the first answer.
                    val delta = if (deltaObject == null || deltaObject.isNull("content")) {
                        ""
                    } else {
                        deltaObject.optString("content", "")
                    }
                    if (delta.isNotEmpty()) {
                        chunks++
                        text.append(delta)
                        onToken(delta)
                    }
                }
            }
            if (text.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("The model returned no text. Check Terminal and the server log.")
                )
            }
            val seconds = ((System.currentTimeMillis() - started) / 1000.0).coerceAtLeast(0.001)
            val exact = timingTps != null || usageTokens > 0 || timingTokens > 0
            val tokens = timingTokens.takeIf { it > 0 }
                ?: usageTokens.takeIf { it > 0 }
                ?: chunks
            val tps = timingTps ?: (tokens / seconds).toFloat()
            Result.success(
                ChatResult(
                    text = text.toString(),
                    tokenCount = tokens,
                    seconds = seconds,
                    tokensPerSecond = tps,
                    exactTokenCount = exact
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(
                IllegalStateException("Local model request failed: ${e.message ?: e.javaClass.simpleName}", e)
            )
        } finally {
            if (activeChat === call) activeChat = null
        }
    }

    private fun ByteArray.toHex(): String {
        val chars = "0123456789abcdef".toCharArray()
        val out = CharArray(size * 2)
        for (i in indices) {
            val value = this[i].toInt() and 0xff
            out[i * 2] = chars[value ushr 4]
            out[i * 2 + 1] = chars[value and 0x0f]
        }
        return String(out)
    }

    private fun formatMb(bytes: Long): Long =
        (bytes.toDouble() / (1024.0 * 1024.0)).roundToLong().coerceAtLeast(0L)

    companion object {
        const val MODEL_DISPLAY_NAME = "Qwen2.5 1.5B Instruct (Q4_K_M)"
        const val MODEL_FILE_NAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        const val MODEL_URL =
            "https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/" +
                "Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true"
        const val MODEL_BYTES = 986_048_768L
        const val MODEL_SHA256 = "1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370"
        const val PORT = 8081
        const val BASE_URL = "http://127.0.0.1:$PORT"

        private const val MODEL_DIR_IN_GUEST = "root/models"
        private const val PID_FILE_IN_GUEST = "/root/models/llama-server.pid"
        private const val SERVER_LOG_IN_GUEST = "/root/models/llama-server.log"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
