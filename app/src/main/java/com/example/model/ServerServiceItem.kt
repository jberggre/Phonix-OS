package com.example.model

/**
 * A server package the user can install into the Alpine container.
 *
 * Nothing here is "running" until the guest says so: [isInstalled] and [isRunning] are filled
 * in from a real probe (`apk info -e`, `pidof`) over the live container, never assumed. The
 * Alpine packages are the real package names, so the install button does what it says.
 */
data class ServerServiceItem(
    val id: String,
    val name: String,
    val category: String,
    val port: Int,
    val protocol: String,
    /** Alpine package that provides this service. */
    val apkPackage: String,
    /** Name of the daemon process, used to tell "installed" from "actually running". */
    val processName: String,
    val description: String,
    /** Where the service keeps its files inside the guest. */
    val internalPath: String,
    val isInstalled: Boolean = false,
    val isRunning: Boolean = false,
    /**
     * Set when a module manifest declares this service, so the Hub shows the command that will
     * actually run. Null for the built-in catalog, whose hints are unchanged.
     */
    val declaredLaunch: String? = null
) {
    /** Exact command to start it, shown to the user instead of silently flipping a switch. */
    val startHint: String
        get() = declaredLaunch ?: when (id) {
            "nginx_web" -> "nginx"
            "openssh" -> "ssh-keygen -A && /usr/sbin/sshd"
            else -> apkPackage
        }

    fun endpoint(ipAddress: String): String =
        if (protocol == "HTTP" || protocol == "HTTPS") "http://$ipAddress:$port" else "$ipAddress:$port"
}

object DefaultServerCatalog {

    fun getDefaultServices(): List<ServerServiceItem> = listOf(
        ServerServiceItem(
            id = "nginx_web",
            name = "Nginx Web Server",
            category = "Web",
            port = 8080,
            protocol = "HTTP",
            apkPackage = "nginx",
            processName = "nginx",
            description = "Static site and reverse proxy. Serves /var/www/html from the guest.",
            internalPath = "/var/www/html"
        ),
        ServerServiceItem(
            id = "openssh",
            name = "OpenSSH Server",
            category = "Remote Access",
            port = 2222,
            protocol = "SSH",
            apkPackage = "openssh",
            processName = "sshd",
            description = "Encrypted shell from your laptop. Needs a key generated inside the guest first.",
            internalPath = "/etc/ssh"
        ),
        // SMB (445) and DNS (53) are not listed here on purpose: both need a privileged port,
        // and Android's kernel refuses to let this app's UID bind anything below 1024 even
        // inside the container. Adding the package is easy; making it reachable is impossible
        // without root. The Privacy Hub explains the Pi-hole path that actually works.
    )
}
