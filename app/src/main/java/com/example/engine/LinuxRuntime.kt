package com.example.engine

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

/**
 * Process-wide owner of the Alpine container.
 *
 * The desktop is just one window onto this: [com.example.service.LinuxServerService] starts the
 * runtime at boot and keeps the process alive with a foreground notification, so closing the
 * desktop does not kill a running server or a long package install. Activities attach an
 * [OutputListener] and receive a replay of recent output.
 */
object LinuxRuntime {

    fun interface OutputListener {
        fun onLine(text: String, systemMessage: Boolean)
    }

    enum class State { STOPPED, INSTALLING, RUNNING }

    private val listeners = CopyOnWriteArrayList<OutputListener>()
    private val recentLines = ArrayDeque<String>()
    private val recentLock = Any()

    @Volatile
    private var container: LinuxContainer? = null

    @Volatile
    private var session: LinuxShellSession? = null

    @Volatile
    var state: State = State.STOPPED
        private set

    /**
     * Bumped on every boot and shutdown. Anything that set up state inside the guest (the X
     * server, a package install) can compare generations to notice its work was thrown away.
     */
    @Volatile
    var generation: Int = 0
        private set

    val isRunning: Boolean
        get() = session != null

    fun container(context: Context): LinuxContainer =
        container ?: LinuxContainer(context.applicationContext).also { container = it }

    fun recentLines(): List<String> = synchronized(recentLock) { recentLines.toList() }

    fun addListener(listener: OutputListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: OutputListener) {
        listeners.remove(listener)
    }

    /**
     * Blocking: unpacks the rootfs on first call and boots a shell. Safe to call repeatedly;
     * concurrent callers are serialised by the method lock.
     */
    @Synchronized
    fun start(context: Context) {
        if (session != null) return
        val guest = container(context)

        if (!guest.isInstalled) {
            state = State.INSTALLING
            emit("Unpacking Alpine Linux root filesystem (first run, ~4 MB)...", true)
            guest.install()
            emit("Root filesystem ready.", true)
        }

        val started = guest.startShell(
            onLine = { line -> emit(line, false) },
            onExit = { code ->
                session = null
                state = State.STOPPED
                generation++
                emit("[container] shell exited (code $code) — press Reboot to start it again", true)
            }
        )
        session = started
        state = State.RUNNING
        generation++
        emit("Alpine Linux ${guest.version()} (aarch64) — real shell, proot, no root needed", true)
    }

    /**
     * Runs one command in the live shell and returns its output.
     *
     * This is how the UI asks the guest for facts instead of inventing them: package probes,
     * disk usage and the storage benchmark all read real output. Returns null when the shell
     * is not running or does not answer within [timeoutMs].
     */
    suspend fun run(command: String, timeoutMs: Long = 15_000): String? {
        if (!isRunning) return null
        val marker = "PHONIX_END_" + System.nanoTime()
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val buffer = StringBuilder()
                lateinit var listener: OutputListener
                listener = OutputListener { line, systemMessage ->
                    if (systemMessage) return@OutputListener
                    if (line.contains(marker)) {
                        removeListener(listener)
                        if (continuation.isActive) continuation.resume(buffer.toString())
                    } else {
                        buffer.appendLine(line)
                    }
                }
                addListener(listener)
                continuation.invokeOnCancellation { removeListener(listener) }
                if (!send("$command; echo $marker")) {
                    removeListener(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    /** Sends one command line. Returns false when there is no live shell. */
    fun send(command: String): Boolean {
        val live = session ?: return false
        return try {
            live.send(command)
            true
        } catch (e: Exception) {
            emit("[container] shell is gone (${e.message})", true)
            false
        }
    }

    fun stop() {
        session?.close()
        session = null
        state = State.STOPPED
        generation++
        emit("[container] stopped", true)
    }

    private fun emit(line: String, systemMessage: Boolean) {
        synchronized(recentLock) {
            if (recentLines.size >= 300) recentLines.removeFirst()
            recentLines.addLast(line)
        }
        for (listener in listeners) {
            try {
                listener.onLine(line, systemMessage)
            } catch (_: Throwable) {
                // A dead UI must never take the container down.
            }
        }
    }
}
