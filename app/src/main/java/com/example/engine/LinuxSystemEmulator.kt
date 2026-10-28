package com.example.engine

import com.example.model.GpuDriverType
import com.example.model.HardwareAccelerationConfig
import com.example.model.PartitionConfig
import com.example.model.ServerServiceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
) {
    enum class LineType {
        INPUT, OUTPUT, SUCCESS, ERROR, ACCENT, SYSTEM
    }
}

class LinuxSystemEmulator {
    var currentDirectory: String = "/home/phonix"
        private set

    private val virtualFileSystem: MutableMap<String, List<String>> = mutableMapOf(
        "/" to listOf("bin", "boot", "dev", "etc", "home", "lib", "mnt", "proc", "root", "sdcard", "sys", "tmp", "usr", "var"),
        "/home" to listOf("phonix"),
        "/home/phonix" to listOf("Desktop", "Documents", "Downloads", "Games", "server.py", "shared", "app", "README.txt"),
        "/home/phonix/Desktop" to listOf("Terminal.desktop", "ServerManager.desktop", "Games.desktop", "CoolingGovernor.desktop", "FileExplorer.desktop"),
        "/home/phonix/Games" to listOf("Fallout_NV.sh", "HalfLife_2.sh", "SuperTuxKart.desktop", "RetroArch.desktop"),
        "/home/phonix/Documents" to listOf("server_notes.txt", "nginx.conf", "cooling_profile.conf"),
        "/var/www/html" to listOf("index.html", "style.css", "api.php"),
        "/etc" to listOf("os-release", "fstab", "hosts", "resolv.conf", "nginx", "ssh", "pihole")
    )

    private val fileContents: MutableMap<String, String> = mutableMapOf(
        "/home/phonix/README.txt" to """
            ===========================================================
             Welcome to Phonix OS - Second Life for Android Hardware!
            ===========================================================
            Your phone is now operating as a dedicated, hardware-accelerated
            Mini PC & 24/7 server appliance.
            
            Useful commands:
              neofetch      : Display system specs & Phoenix ASCII logo
              sensors       : Check motherboard SoC & heatsink thermals
              box64         : Inspect x86 translation engine for PC games
              htop          : Real-time CPU core load & process tree
              df -h         : Inspect storage partition breakdown
              free -m       : Inspect physical RAM & zRAM swap usage
              systemctl     : View and manage background server daemons
              glxinfo       : Check VirGL / OpenGL ES 3.1 3D GPU context
        """.trimIndent(),
        "/etc/os-release" to """
            NAME="Phonix OS"
            VERSION="12.4 (Bookworm)"
            ID=phonix
            ID_LIKE=debian
            PRETTY_NAME="Phonix OS 12 (Hardware-Accelerated Linux on Mobile Silicon)"
            VERSION_CODENAME=bookworm
            HOME_URL="https://github.com/phonix-os"
        """.trimIndent(),
        "/home/phonix/server.py" to """
            # Python 3 Micro-service on Phonix OS
            from http.server import HTTPServer, SimpleHTTPRequestHandler
            PORT = 5000
            print(f"Phonix server active on port {PORT}")
            httpd = HTTPServer(('0.0.0.0', PORT), SimpleHTTPRequestHandler)
            httpd.serve_forever()
        """.trimIndent()
    )

    fun executeCommand(
        cmdRaw: String,
        ipAddress: String,
        partitionConfig: PartitionConfig,
        hwConfig: HardwareAccelerationConfig,
        services: List<ServerServiceItem>,
        uptimeSeconds: Long
    ): List<TerminalLine> {
        val trimmed = cmdRaw.trim()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.split("\\s+".toRegex())
        val command = tokens[0].lowercase(Locale.ROOT)
        val args = tokens.drop(1)

        val output = mutableListOf<TerminalLine>()
        output.add(TerminalLine("phonix@phonix-minipc:$currentDirectory$ $trimmed", TerminalLine.LineType.INPUT))

        when (command) {
            "clear" -> {
                return output
            }
            "help" -> {
                output.add(TerminalLine("Phonix OS Built-in Command Reference:", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("  neofetch       - Hardware specs, OS release & Phoenix logo"))
                output.add(TerminalLine("  sensors        - Motherboard SoC, heatsink & battery thermals"))
                output.add(TerminalLine("  box64          - x86_64 to ARM64 binary translation runner"))
                output.add(TerminalLine("  distros        - View available ARM64 Linux rootfs catalog"))
                output.add(TerminalLine("  htop, top      - Process monitor and CPU core utilization"))
                output.add(TerminalLine("  free -m        - Memory, buffers, cache & zRAM swap usage"))
                output.add(TerminalLine("  df -h          - Partition and EXT4 virtual storage breakdown"))
                output.add(TerminalLine("  ip a, ifconfig - Network interfaces & local Wi-Fi IP"))
                output.add(TerminalLine("  glxinfo        - Hardware acceleration & GPU driver status"))
                output.add(TerminalLine("  virgl_test     - Test 3D rendering pipeline"))
                output.add(TerminalLine("  ls, cd, cat    - Navigate virtual file system"))
                output.add(TerminalLine("  systemctl      - Check and manage background server daemons"))
                output.add(TerminalLine("  apt update     - Update package repository index"))
                output.add(TerminalLine("  uptime         - Phone 24/7 server uptime"))
                output.add(TerminalLine("  clear          - Clear terminal display"))
                output.add(TerminalLine("  reboot         - Reboot Linux OS container"))
                output.add(TerminalLine("  shutdown       - Shutdown Linux container"))
            }
            "distros" -> {
                output.add(TerminalLine("Phonix OS Linux Distribution Catalog (8 ARM64 Rootfs):", TerminalLine.LineType.ACCENT))
                val distros = com.example.model.DistroCatalog.distros
                for (d in distros) {
                    val status = if (d.isInstalled) "[INSTALLED]" else "[AVAILABLE]"
                    val lineType = if (d.isInstalled) TerminalLine.LineType.SUCCESS else TerminalLine.LineType.OUTPUT
                    output.add(TerminalLine(String.format(Locale.ROOT, "  %-24s %-12s %-10s (%d MB) - %s", d.name, status, d.base, d.downloadSizeMb, d.desktopEnvironment), lineType))
                }
                output.add(TerminalLine("Use Distro Store window or 'apt install <distro-id>' to download.", TerminalLine.LineType.SYSTEM))
            }
            "sensors" -> {
                output.add(TerminalLine("soc_thermal-virtual-0", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("Adapter: Virtual device", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("CPU Core 0-3:   +33.2°C  (crit = +85.0°C)", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("CPU Core 4-7:   +34.8°C  (crit = +85.0°C) [Sustained 2.45 GHz]", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("GPU Mali/Adreno:+32.4°C  (crit = +90.0°C)", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("Motherboard HS: +31.0°C  [Active 5V Fan @ 3200 RPM active]", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("Battery Cell:   +30.5°C  (Safe for 24/7 wall operation)", TerminalLine.LineType.OUTPUT))
            }
            "box64" -> {
                output.add(TerminalLine("Box64 v0.2.8 Linux ARM64 with Dynarec", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("Dynarec for ARM64: ON (Fast JIT recompilation)", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("Hardware Float: VFPv4/NEON active", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("Wine compatibility: Ready for x86 Windows DirectX/OpenGL games", TerminalLine.LineType.SUCCESS))
            }
            "uname" -> {
                if (args.contains("-a") || args.isEmpty()) {
                    output.add(TerminalLine("Linux phonix-server 5.15.148-phonix-arm64 #1 SMP PREEMPT aarch64 GNU/Linux", TerminalLine.LineType.OUTPUT))
                } else if (args.contains("-r")) {
                    output.add(TerminalLine("5.15.148-phonix-arm64", TerminalLine.LineType.OUTPUT))
                } else {
                    output.add(TerminalLine("Linux", TerminalLine.LineType.OUTPUT))
                }
            }
            "uptime" -> {
                val hours = uptimeSeconds / 3600
                val mins = (uptimeSeconds % 3600) / 60
                val secs = uptimeSeconds % 60
                output.add(TerminalLine(" $hours:$mins:$secs up $hours hrs, 1 user, load average: 0.18, 0.14, 0.10", TerminalLine.LineType.OUTPUT))
            }
            "neofetch" -> {
                output.addAll(getNeofetchOutput(hwConfig, partitionConfig, ipAddress, uptimeSeconds))
            }
            "free" -> {
                output.add(TerminalLine("               total        used        free      shared  buff/cache   available", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("Mem:            3840        1080        2180          48         580        2660", TerminalLine.LineType.OUTPUT))
                val swapMb = (partitionConfig.allocatedSwapGb * 1024).toInt()
                val swapUsed = (swapMb * 0.12f).toInt()
                val swapFree = swapMb - swapUsed
                output.add(TerminalLine("Swap (zRAM):    $swapMb         $swapUsed        $swapFree", TerminalLine.LineType.OUTPUT))
            }
            "df" -> {
                output.add(TerminalLine("Filesystem      Size  Used Avail Use% Mounted on", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("/dev/rootfs     ${partitionConfig.allocatedLinuxGb.toInt()}G  ${partitionConfig.linuxUsedStorageGb.toInt()}G   ${(partitionConfig.allocatedLinuxGb - partitionConfig.linuxUsedStorageGb).toInt()}G  42% /", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("tmpfs           1.8G   12M  1.8G   1% /dev/shm", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("/dev/swapimg    ${partitionConfig.allocatedSwapGb.toInt()}G  180M  1.8G  10% [SWAP]", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("/data/sdcard    ${partitionConfig.totalDeviceStorageGb.toInt()}G   ${partitionConfig.androidSystemUsedGb.toInt()}G  ${partitionConfig.freeStorageGb.toInt()}G  38% /sdcard", TerminalLine.LineType.OUTPUT))
            }
            "ip", "ifconfig" -> {
                output.add(TerminalLine("1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("    inet 127.0.0.1/8 scope host lo", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("2: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 state UP", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("    inet $ipAddress/24 brd 192.168.1.255 scope global wlan0", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("    Phonix LAN Endpoint: http://$ipAddress:8080", TerminalLine.LineType.SUCCESS))
            }
            "glxinfo" -> {
                output.add(TerminalLine("OpenGL vendor string: Mesa/VirGL Project", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("OpenGL renderer string: ${hwConfig.gpuDriver.rendererName}", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("OpenGL version string: 4.3 (Compatibility Profile) Mesa 24.1.0", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("Direct rendering: Yes (Hardware Acceleration DRI3 active)", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("Accelerated: Yes (VirGL GLES context mapped to phone GPU)", TerminalLine.LineType.SUCCESS))
            }
            "virgl_test" -> {
                output.add(TerminalLine("[virgl_test] Probing VirGL 3D pipe through /dev/dri/renderD128...", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("[virgl_test] Handshake established with host GLES 3.1 context", TerminalLine.LineType.SUCCESS))
                output.add(TerminalLine("[virgl_test] Rendering 60 frames: avg 60.0 FPS. Hardware Acceleration verified!", TerminalLine.LineType.SUCCESS))
            }
            "htop", "top" -> {
                output.add(TerminalLine("  CPU[|||||||||||||||                  21.4%]   Tasks: 44, 1 thr; 1 running", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("  Mem[|||||||||||||||||||||        1080/3840MB]   Swp[||             240/2048MB]", TerminalLine.LineType.ACCENT))
                output.add(TerminalLine("  PID USER      PRI  NI  VIRT   RES   SHR S CPU% MEM%   TIME+  Command", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("  101 root       20   0  145M   32M   18M S  2.8  0.8  1:12.40 nginx: master", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("  102 phonix     20   0  380M   84M   42M S  7.2  2.1  2:44.18 xfce4-panel", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("  105 phonix     20   0  210M   45M   22M S  1.5  1.1  0:18.90 virgl_server", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("  108 root       20   0  112M   18M   12M S  0.4  0.4  0:05.12 sshd", TerminalLine.LineType.OUTPUT))
                output.add(TerminalLine("  112 phonix     20   0  520M  110M   65M S  8.4  2.8  0:32.10 python3 server.py", TerminalLine.LineType.OUTPUT))
            }
            "systemctl", "service" -> {
                output.add(TerminalLine("UNIT                        LOAD   ACTIVE SUB     DESCRIPTION", TerminalLine.LineType.ACCENT))
                for (s in services) {
                    val statusStr = if (s.isRunning) "active (running)" else "inactive (dead)"
                    val type = if (s.isRunning) TerminalLine.LineType.SUCCESS else TerminalLine.LineType.ERROR
                    output.add(TerminalLine(String.format(Locale.ROOT, "%-26s loaded %-15s %s on port :%d", s.id + ".service", statusStr, s.name, s.port), type))
                }
            }
            "ls" -> {
                val contents = virtualFileSystem[currentDirectory] ?: listOf("README.txt")
                output.add(TerminalLine(contents.joinToString("   "), TerminalLine.LineType.ACCENT))
            }
            "pwd" -> {
                output.add(TerminalLine(currentDirectory, TerminalLine.LineType.OUTPUT))
            }
            "cd" -> {
                val target = args.firstOrNull() ?: "/home/phonix"
                if (target == ".." || target == "../") {
                    val parent = currentDirectory.substringBeforeLast('/', "")
                    currentDirectory = if (parent.isEmpty()) "/" else parent
                } else if (target.startsWith("/")) {
                    if (virtualFileSystem.containsKey(target)) {
                        currentDirectory = target
                    } else {
                        output.add(TerminalLine("bash: cd: $target: No such file or directory", TerminalLine.LineType.ERROR))
                    }
                } else {
                    val next = if (currentDirectory == "/") "/$target" else "$currentDirectory/$target"
                    if (virtualFileSystem.containsKey(next)) {
                        currentDirectory = next
                    } else {
                        output.add(TerminalLine("bash: cd: $target: No such file or directory", TerminalLine.LineType.ERROR))
                    }
                }
            }
            "cat" -> {
                val targetFile = args.firstOrNull()
                if (targetFile == null) {
                    output.add(TerminalLine("Usage: cat <filename>", TerminalLine.LineType.ERROR))
                } else {
                    val fullPath = if (targetFile.startsWith("/")) targetFile else "$currentDirectory/$targetFile"
                    val content = fileContents[fullPath]
                    if (content != null) {
                        content.lines().forEach { output.add(TerminalLine(it, TerminalLine.LineType.OUTPUT)) }
                    } else {
                        output.add(TerminalLine("cat: $targetFile: No such file or directory", TerminalLine.LineType.ERROR))
                    }
                }
            }
            "apt" -> {
                if (args.contains("update")) {
                    output.add(TerminalLine("Hit:1 http://deb.debian.org/debian bookworm InRelease", TerminalLine.LineType.OUTPUT))
                    output.add(TerminalLine("Hit:2 http://archive.phonix-os.org/arm64 main InRelease", TerminalLine.LineType.OUTPUT))
                    output.add(TerminalLine("Reading package lists... Done. All packages up to date.", TerminalLine.LineType.SUCCESS))
                } else if (args.contains("install")) {
                    val pkg = args.drop(1).joinToString(" ")
                    output.add(TerminalLine("Setting up $pkg (latest ARM64 build)... Done.", TerminalLine.LineType.SUCCESS))
                } else {
                    output.add(TerminalLine("Usage: apt update | apt install <package>", TerminalLine.LineType.OUTPUT))
                }
            }
            "whoami" -> {
                output.add(TerminalLine("phonix", TerminalLine.LineType.OUTPUT))
            }
            else -> {
                output.add(TerminalLine("bash: $command: command not found. Type 'help' for available commands.", TerminalLine.LineType.ERROR))
            }
        }

        return output
    }

    private fun getNeofetchOutput(
        hwConfig: HardwareAccelerationConfig,
        partition: PartitionConfig,
        ipAddress: String,
        uptimeSeconds: Long
    ): List<TerminalLine> {
        val lines = mutableListOf<TerminalLine>()
        val hours = uptimeSeconds / 3600
        val mins = (uptimeSeconds % 3600) / 60

        val art = listOf(
            "         /\\            phonix@phonix-minipc",
            "        /  \\           --------------------",
            "       / /\\ \\          OS: Phonix OS 12 (ARM64)",
            "      / /  \\ \\         Kernel: 5.15.148-phonix-arm64",
            "     / / /\\ \\ \\        Uptime: ${hours}h ${mins}m (24/7 Appliance Mode)",
            "    < < (  ) > >       Packages: 1,024 (dpkg, box64, wine)",
            "     \\ \\ \\/ / /        Shell: bash 5.2.15",
            "      \\ \\  / /         DE: Phonix Aero Desktop (XFCE4 / IceWM)",
            "       \\ \\/ /          Cooling: Active Motherboard Heatsink Mode",
            "        \\  /           CPU: ARMv8 Octa-Core @ Sustained 2.45 GHz",
            "         \\/            GPU: ${hwConfig.gpuDriver.rendererName}",
            "                       Memory: 1080MB / 3840MB (zRAM active)",
            "                       Storage: ${partition.allocatedLinuxGb.toInt()}G ext4 (${partition.linuxUsedStorageGb.toInt()}G used)",
            "                       Local IP: $ipAddress (Port 8080/2222)"
        )

        for (l in art) {
            lines.add(TerminalLine(l, TerminalLine.LineType.OUTPUT))
        }
        return lines
    }
}
