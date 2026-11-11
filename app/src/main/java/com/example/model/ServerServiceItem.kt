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
    /** Exact command to start it, shown to the user and executable with 1 click. */
    val startHint: String
        get() = declaredLaunch ?: when (id) {
            "nginx_web" -> "nginx"
            "openssh" -> "ssh-keygen -A && /usr/sbin/sshd -p 2222"
            "samba_smb" -> "phonix-smb start"
            "dnsmasq_adblock" -> "phonix-adblock start"
            else -> apkPackage
        }

    fun endpoint(ipAddress: String): String = when (protocol) {
        "HTTP", "HTTPS" -> "http://$ipAddress:$port"
        "SMB" -> "smb://$ipAddress:$port/phonix"
        else -> "$ipAddress:$port"
    }
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
            description = "Encrypted shell from your laptop on port 2222. Generates host keys inside the guest on first start.",
            internalPath = "/etc/ssh"
        ),
        ServerServiceItem(
            id = "samba_smb",
            name = "Samba SMB File Server",
            category = "File Sharing",
            port = 4445,
            protocol = "SMB",
            apkPackage = "samba",
            processName = "smbd",
            description = "Real Samba (smbd) network file share at /root/shared bound to unprivileged port 4445 (smb://<IP>:4445/phonix) via phonix-smb.",
            internalPath = "/root/shared"
        ),
        ServerServiceItem(
            id = "dnsmasq_adblock",
            name = "Adblock DNS Sinkhole (Pi-hole Engine)",
            category = "DNS & Adblock",
            port = 5353,
            protocol = "DNS",
            apkPackage = "dnsmasq",
            processName = "dnsmasq",
            description = "Pi-hole's underlying dnsmasq sinkhole engine on unprivileged port 5353 with StevenBlack hosts blocklist support via phonix-adblock.",
            internalPath = "/etc/phonix-adblock.hosts"
        )
    )
}
