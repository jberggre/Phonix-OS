package com.example.model

data class ServerServiceItem(
    val id: String,
    val name: String,
    val category: String, // Web, Remote Access, Storage, Network, Automation
    val port: Int,
    val protocol: String,
    val isRunning: Boolean,
    val description: String,
    val internalPath: String,
    val autoStartOnBoot: Boolean = true
) {
    fun getLocalEndpoint(ipAddress: String): String {
        return if (protocol == "HTTP" || protocol == "HTTPS") {
            "http://$ipAddress:$port"
        } else {
            "$ipAddress:$port"
        }
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
            isRunning = true,
            description = "High-performance web server serving static websites, PHP, and proxying web apps.",
            internalPath = "/var/www/html",
            autoStartOnBoot = true
        ),
        ServerServiceItem(
            id = "openssh",
            name = "OpenSSH Server",
            category = "Remote Access",
            port = 2222,
            protocol = "SSH",
            isRunning = true,
            description = "Secure encrypted terminal access. SSH directly into this phone from your laptop using: ssh phonix@IP -p 2222",
            internalPath = "/etc/ssh",
            autoStartOnBoot = true
        ),
        ServerServiceItem(
            id = "samba_nas",
            name = "Samba Windows File Share (NAS)",
            category = "Storage",
            port = 4455,
            protocol = "SMB",
            isRunning = true,
            description = "Turn phone internal storage or MicroSD card into network drive visible in Windows File Explorer.",
            internalPath = "/home/phonix/shared",
            autoStartOnBoot = true
        ),
        ServerServiceItem(
            id = "pihole_dns",
            name = "Pi-hole / DNS Ad-Blocker",
            category = "Network",
            port = 8053,
            protocol = "DNS / HTTP",
            isRunning = false,
            description = "Network-wide advertisement and tracking blocker. Route your home Wi-Fi through this old phone.",
            internalPath = "/etc/pihole",
            autoStartOnBoot = false
        ),
        ServerServiceItem(
            id = "node_api",
            name = "Node.js REST Service",
            category = "Dev",
            port = 3000,
            protocol = "HTTP",
            isRunning = false,
            description = "Full Node.js v20 runtime hosting backend APIs or lightweight Express servers.",
            internalPath = "/home/phonix/app",
            autoStartOnBoot = false
        ),
        ServerServiceItem(
            id = "nextcloud",
            name = "Nextcloud Personal Cloud",
            category = "Storage",
            port = 8443,
            protocol = "HTTPS",
            isRunning = false,
            description = "Self-hosted alternative to Google Drive / Dropbox running on your phone.",
            internalPath = "/var/www/nextcloud",
            autoStartOnBoot = false
        ),
        ServerServiceItem(
            id = "python_flask",
            name = "Python 3 Micro-service",
            category = "Dev",
            port = 5000,
            protocol = "HTTP",
            isRunning = true,
            description = "Fast Python server for web hooks, home automation, and sensor monitoring.",
            internalPath = "/home/phonix/server.py",
            autoStartOnBoot = true
        )
    )
}
