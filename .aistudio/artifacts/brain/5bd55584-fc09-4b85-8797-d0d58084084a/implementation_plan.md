# Phonix OS: Samsung DeX-Style Native App Overlay & First-Run Setup Engine

> **Status (2026-10-29): historical implementation plan, not current documentation.**
> This is the original AI Studio plan for the OOBE and the desktop shell. Several items below
> were aspirational and are **not** in the shipping app — notably Docker, GPU clock control, a
> streaming-emulator path, and some Windows-11-specific OOBE details. Do not treat it as a
> feature list or a test plan.
>
> Current sources of truth:
> - [docs/VERIFICATION.md](../../../../docs/VERIFICATION.md) — measured 1.1 state, bugs, matrix and work queue
> - [docs/ROADMAP.md](../../../../docs/ROADMAP.md) — platform, AI and update research
> - [docs/PROVISIONING.md](../../../../docs/PROVISIONING.md) — build, signing, install and bench traps

Transforming salvaged smartphone motherboards in recycled-plastic Mini PC shells into a desktop experience. This revised plan introduces a complete **Windows 11 Out-of-Box Experience (OOBE) Setup Wizard** that runs on first launch (and can be reset anytime for a new user/buyer). It guides users through zero-waste app selection to preserve phone flash memory, aggressive mobile hardware debloating, desktop personalization, and seamless integration with the host phone's genuine installed apps, Google Play Store, and Linux tools.

---

## Hardware Context & The Refurbished Mini PC Lifecycle

> [!IMPORTANT]
> **The Refurbisher & New User Experience**:
> * **Hardware Base**: Repurposed smartphone motherboards with broken/unusable screens housed inside custom recycled-plastic Mini PC chassis plugged into desktop monitors.
> * **"Fresh Laptop" First-Run Experience**: Just like powering on a new Windows laptop or Mac Mini, a user (or customer receiving a refurbished unit) is greeted with an interactive setup wizard walking them step-by-step through their device configuration.
> * **Zero-Waste App Curation (Preserving Internal Storage)**: Phones often have limited storage (32GB–64GB). The wizard asks which app categories to configure and which to skip, avoiding bloat before the desktop even loads.
> * **Prepare for New User / Factory Reset**: Refurbishers or previous owners can click "Reset for New User" in the Start Menu, clearing local desktop customizations and presenting the clean OOBE setup wizard for the next user.

---

## User Decisions & Clarifications

* **First-Run OOBE Setup Wizard**:
  * Runs automatically on first launch, or when triggered via "Reset for New User".
  * **Step 1: Device Profile & Identity** (Name your Mini PC, choose use-case preset: Everyday Desktop, Homelab Server, Gaming & Emulation, Minimalist).
  * **Step 2: Zero-Waste App Curator** (Select which apps to enable/download and which to skip, with a live flash memory calculator).
  * **Step 3: Mini PC Debloater** (Disable unnecessary phone radios, SMS, and lockscreen services).
  * **Step 4: Desktop Personalization** (Theme, taskbar alignment, icon size/shape).
  * **Step 5: Instant Desktop Provisioning**.
* **App Store & Future Downloads**:
  * Dedicated **"Microsoft Store"-styled "App Store"** on desktop and Start Menu launching Google Play Store (`com.android.vending`), F-Droid, or local APK installer.
  * Real-time package monitoring automatically detects and displays newly installed apps in the Start Menu.
* **Native App Launching**: Native phone apps (Browser, Files, Photos, Camera, Settings) launch in new computer-like windows using Android `ActivityOptions` launch bounds and task flags, mirroring Samsung DeX.
* **App Discovery & Desktop Pinning**:
  * **Start Menu "All Apps"**: Windows 11 Start Menu with a toggleable "All apps" view querying `PackageManager` to list all installed phone apps alphabetically with real app icons.
  * **Desktop Shortcut Picker**: Right-click desktop menu / "+ Add Shortcut" button opens an installed app catalog with live search to pin any native phone app to the desktop canvas.
* **Phonix Specialized Tools**: Linux Distro Store, Server Hub, Phonix Terminal, and System Monitor remain as floating desktop windows with minimize, maximize, and close controls alongside phone apps.

---

## 1. Technical Architecture & Data Model Changes

### 1.1 First-Run OOBE Setup Engine (`OobeState`)
Create a comprehensive setup wizard state model:
```kotlin
data class OobeSetupConfig(
    val deviceName: String = "Phonix-MiniPC",
    val useCaseProfile: MiniPcProfile = MiniPcProfile.EVERYDAY_DESKTOP,
    val selectedAppCategories: Set<AppCategory> = setOf(
        AppCategory.WEB_BROWSER,
        AppCategory.FILE_MANAGER,
        AppCategory.APP_STORE
    ),
    val enableAggressiveDebloat: Boolean = true,
    val themePreset: DesktopThemePreset = DesktopThemePreset.WINDOWS_11_DARK,
    val taskbarCentered: Boolean = true
)

enum class MiniPcProfile(val title: String, val description: String) {
    EVERYDAY_DESKTOP("Everyday Desktop", "Optimized for browsing, documents, and media playback"),
    HOMELAB_SERVER("Homelab & Server", "Headless services, SSH/FTP, Linux containers, and minimal UI"),
    GAMING_EMULATION("Gaming & Emulation", "Max GPU clock governor, 3D drivers, and gamepad support"),
    MINIMAL_LEAN("Ultra Lean", "Strictly essential utilities to preserve max flash storage and RAM")
}

enum class AppCategory(val title: String, val description: String, val estStorageMb: Int, val isDefault: Boolean) {
    WEB_BROWSER("Web Browser (Chrome)", "Fast internet browsing with native hardware acceleration", 0, true),
    FILE_MANAGER("File Explorer", "Organize external storage, USB drives, and downloads", 0, true),
    APP_STORE("App Store (Google Play / F-Droid)", "Download and update Android apps on demand", 0, true),
    MEDIA_GALLERY("Photos & Media Player", "View images, videos, and music", 0, false),
    LINUX_TERMINAL("Phonix Linux Terminal", "Command line environment with package management", 25, true),
    LINUX_DISTROS("Linux Distro Hub", "1-click user-space container distros (Debian, Ubuntu, Arch)", 150, false),
    SERVER_CENTER("Server Management Hub", "Run Web, FTP, and Docker servers on your Mini PC", 40, false),
    HARDWARE_MONITOR("System & Cooling Monitor", "Live CPU/GPU thermals and AC power governor", 15, true)
}
```

### 1.2 Out-of-Box Preference Persistence
* Store `isOobeCompleted: Boolean` in SharedPreferences.
* Provide `resetOobeForNewUser(context: Context)` which clears desktop configurations, unpins optional apps, resets theme, and launches the OOBE Setup Wizard.

### 1.3 Dynamic Package Monitoring & App Store
* `AppStoreHelper` managing Google Play Store (`com.android.vending`), F-Droid, and APK installation intents.
* `BroadcastReceiver` detecting `Intent.ACTION_PACKAGE_ADDED` to refresh the Start Menu and desktop shortcut lists without requiring a reboot.

---

## 2. Proposed Component Changes

### 2.1 `OobeWizardScreen.kt` (New Full-Screen First-Run Wizard)
Create a polished, modern Windows 11 setup wizard:
* **Welcome Screen**: Clean branding, "Let's set up your Phonix Mini PC", audio/visual cue.
* **Name & Profile**: Set custom Mini PC hostname, pick use-case preset.
* **Zero-Waste Storage Curator**:
  * Visual storage bar: "Estimated App Footprint: [X MB] / Flash Saved: [Y GB]".
  * Checkboxes for each category with clear explanations so users only install what they need.
* **Hardware Debloater Step**:
  * One-click option to freeze telephony and background cellular daemons for the screenless Mini PC motherboard.
* **Personalize Desktop**:
  * Theme selector (Dark Fluent, Cyber Phoenix, Clean Slate) and Taskbar alignment preview.
* **Finish & Launch Desktop**:
  * Plays a smooth initialization animation and transitions into the customized Phonix Desktop.

### 2.2 `PhonixViewModel.kt`
* Add `oobeConfig: StateFlow<OobeSetupConfig>`.
* Add `completeOobeSetup(context: Context)`.
* Add `resetForNewUser(context: Context)` to return to OOBE anytime.
* Populate desktop shortcuts based on the user's specific selections in the wizard.
* Add `installedApps: StateFlow<List<InstalledAppInfo>>` and DeX-style launch helper.

### 2.3 `Windows11StartMenu.kt`
* Add "All apps ->" view listing phone applications with real icons.
* Add prominent "App Store" pinned tile.
* In the Power / Profile flyout at the bottom, add **"Prepare for New User (OOBE Reset)"** alongside Sleep, Restart, and Shut Down.

### 2.4 `PhonixDesktopScreen.kt` & Desktop App Picker Dialog
* Render native phone app icons with custom Windows shapes.
* Right-click desktop menu includes:
  * "Add App to Desktop" picker with instant search across all installed apps.
  * "Open App Store (Get more apps)".
  * "Personalize Desktop".
  * "Mini PC Setup & Debloat".

---

## 3. Verification Plan

### Automated Verification
* `compile_applet`: Verify that the new `OobeWizardScreen.kt`, models, viewmodel methods, and intent helpers compile cleanly with zero errors.
* `gradle :app:testDebugUnitTest`: Verify unit tests for OOBE configuration transitions, storage calculation, and shortcut generation.

### Manual Verification in Streaming Emulator
1. **First-Run Experience**:
   * Launch app -> observe the Windows 11 OOBE Setup Wizard.
   * Step through Device Profile -> Zero-Waste Storage Curator -> Hardware Debloat -> Desktop Style.
   * Uncheck optional components (e.g. Server Hub or Linux Distros) -> verify storage calculation decreases.
   * Finish setup -> verify the desktop opens with *only* the chosen shortcuts.
2. **Reset for New User**:
   * Open Start Menu -> click Power / Options -> select "Prepare for New User (OOBE Reset)".
   * Verify app cleanly returns to the welcome setup screen.
3. **App Store & Discovery**:
   * Click "App Store" -> opens Google Play Store or store intent.
   * Open Start Menu -> "All apps ->" -> verify alphabetical installed phone app list.
