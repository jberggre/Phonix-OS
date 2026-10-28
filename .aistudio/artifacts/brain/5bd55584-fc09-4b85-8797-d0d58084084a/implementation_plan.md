# Phonix OS: Hardware-Accelerated Mini PC & Server Suite for Repurposed Phones

Giving older flagship smartphones (Samsung Galaxy S8, Note 8, S9, S10, and newer) a high-performance second life as dedicated, actively-cooled Mini PCs, 24/7 home servers, and retro gaming stations.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Name Change**: Retiring the "Lindows" name to avoid trademark issues with the historical 2002–2004 Lindows/Linspire case. We propose **"Phonix OS"** (a fusion of *Phone*, *Linux*, and the mythical *Phoenix* rising from the ashes of outdated hardware).
> 
> **Hardware Cooling Support**: Introducing an **Active Cooling Governor** mode that detects when custom heatsinks/fans are fitted, unlocking sustained 2.45 GHz clock speeds without thermal throttling.
> 
> **Direct Linux Boot Appliance**: Booting straight into the desktop environment or headless server shell with Android system bars completely hidden.

* **Confirmed Decision**: Rebrand app label, metadata, and UI tokens from "Lindows" to **Phonix OS**.
* **Confirmed Decision**: Add dedicated **Cooling & Sustained Clocks Module** for motherboard-modded devices.
* **Confirmed Decision**: Add **Gaming & x86 Emulation Hub** (Box64, Wine, RetroArch, Steam Link).

---

## 1. Overview & Core Concept

**Phonix OS** converts deprecated Android phones into functional desktop workstations and 24/7 low-power home servers. 

```
┌────────────────────────────────────────────────────────┐
│                   Phonix OS Engine                     │
├───────────────────────────┬────────────────────────────┤
│   Mini PC Desktop Mode    │   24/7 Server Suite Mode   │
│ (XFCE / IceWM / Box64 /   │ (Nginx / SSH / Samba NAS / │
│  DeX HDMI 1080p Monitor)  │  Pi-hole DNS / Nextcloud)  │
├───────────────────────────┴────────────────────────────┤
│           Active Cooling & Thermal Guardian            │
│  (Sustained Clocks / Overclock / Modded Heatsink Mode) │
├────────────────────────────────────────────────────────┤
│           Hardware Acceleration Pipeline               │
│ (VirGL GLES 3.1 / Qualcomm Turnip Vulkan / Mali DRI3)  │
├────────────────────────────────────────────────────────┤
│          Virtual Storage & Memory Partition            │
│   (Sparse EXT4 Container / zRAM Memory Paging 2GB)     │
└────────────────────────────────────────────────────────┘
```

* **What It Does**: Provisions a virtual EXT4 storage container, configures zRAM memory paging (vital for 4GB/6GB RAM phones like Galaxy S8/Note 8), initializes 3D GPU hardware acceleration (VirGL/Turnip/Mali), and boots directly into a customizable desktop or headless server.
* **Target Audience**: Makers, tech enthusiasts, homelab hobbyists, and anyone looking to eliminate electronic waste by repurposing old phones into home servers, NAS units, or mini retro consoles.
* **Key Value**: $0 hardware cost, built-in battery UPS backup, sub-5W power consumption, and up to 60% sustained performance boost with custom motherboard cooling.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Direct Appliance Boot**: When the phone powers on, Android's navigation and status bars are hidden in immersive mode. The phone displays the Phonix bootloader splash and lands immediately into the Phonix Desktop Environment.
2. **Motherboard Cooling Profile Selector**:
   * *Stock Glass Phone*: Caps thermal limit to 40°C to protect passive batteries.
   * *Active Modded Cooling (Heatsink + 5V Fan)*: Unlocks maximum sustained 2.45 GHz CPU and 710 MHz GPU frequencies indefinitely.
3. **Gaming & Emulation Suite**:
   * *Retro Console Hub*: 1-click launch for NES, SNES, PS1, GBA, N64.
   * *x86 Translation Layer*: Box64 + Wine configuration for classic PC titles.
   * *Remote Streaming*: Sub-5ms Moonlight & Steam Link client for living room gaming.
4. **Server Operations**:
   * 1-click toggles for Nginx, SSH (`ssh phonix@IP -p 2222`), Samba network drive (`\\IP\shared`), and Pi-hole DNS ad-blocking.

### Visual Identity & Theme
* **Name**: Phonix OS
* **Palette**:
  * Dominant Background: Deep technical obsidian (`#070B14`) and midnight navy (`#0F172A`)
  * Electric Accent: Phoenix Ember Amber (`#F59E0B`), Cyan Pulse (`#06B6D4`), and Emerald Server Online (`#10B981`)
  * Desktop Taskbar: Classic Windows/Phonix aero dark taskbar with system tray, start button, and live CPU/temperature indicators.
* **Typography**: JetBrains Mono for system terminals and kernel telemetry; Inter/Roboto for desktop windows and setup forms.

---

## 3. Key Product Decisions & Trade-Offs

* **Rebranding to Phonix OS**:
  * *Chosen Approach*: Replace all occurrences of "Lindows" with "Phonix OS" across strings, metadata, icon drawables, viewmodels, and desktop assets.
  * *Why*: Protects against trademark liabilities and creates a unique, powerful identity focused on hardware rebirth.
* **Active Cooling Hardware Mode**:
  * *Chosen Approach*: Provide a dedicated toggle for users who add copper heatsinks or cooling fans. When active, it disables software thermal throttling and commands the CPU governor to maintain `performance` mode.
  * *Why*: Older phone SoCs are capable of desktop-class tasks, but normally throttle down to 40% speed due to enclosed glass bodies. Custom cooling unlocks their full potential.
* **Zero-Wipe Virtual EXT4 Partitioning**:
  * *Chosen Approach*: Store the Linux rootfs inside a sparse `.img` container with `barrier=0` wear-leveling rather than repartitioning raw UFS/eMMC chips.
  * *Why*: 100% safe—never bricks recovery, bootloaders, or Android phone functionality.

---

## 4. Technical Architecture & Implementation Steps

```
[Phone Boot / Power-On]
         │
         ▼
[BootReceiver / MainActivity (Home Launcher Intent)]
         │
         ├────────────────────────────────────────┐
         │ (Direct Boot Enabled)                  │ (Dual-Boot Menu)
         ▼                                        ▼
[Phonix Kernel Boot Splash]              [GRUB Dual-Boot Prompt]
  - Verifies EXT4 Sparse Container         - Option 1: Phonix OS (Default)
  - Allocates 2GB zRAM Swap                - Option 2: Android Phone OS
  - Binds VirGL / Mali DRI3 Context
         │
         ▼
[Phonix Desktop Environment (Immersive Kiosk Mode)]
  - Movable, Resizable Floating Windows (Terminal, Servers, Htop, File Manager)
  - Active Cooling Governor (Thermal Monitor & Clock Lock)
  - Gaming & x86 Emulation Suite (Box64 / Wine / RetroArch)
  - 24/7 WakeLock Background Service (Keeps ports alive with screen off)
```

### Proposed Action Items
1. **Rebrand Codebase**:
   * Update `metadata.json`, `strings.xml`, `settings.gradle.kts` to **Phonix OS** / **Phonix**.
   * Update all UI titles, terminal prompts (`phonix@minipc:~$`), and Start Menu logos.
   * Update adaptive launcher icons with a sleek phoenix/microchip insignia.
2. **Implement Active Cooling & Thermal Governor Screen**:
   * Hardware thermal sensor reading, heatsink profile toggle, and sustained CPU governor tuning.
3. **Implement Gaming & x86 Emulation Module**:
   * Add Box64/Wine runtime presets, RetroArch emulator manager, and Moonlight game streaming shortcuts.
4. **Compile & Verify**:
   * Validate clean compilation with `compile_applet`.
