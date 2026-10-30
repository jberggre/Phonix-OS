package com.example.util

import android.content.Context
import com.example.model.DeviceChangeChoices
import com.example.model.DeviceUseMode

/**
 * Stores user consent for optional host-device behavior. New installs default to no automatic
 * host changes. Existing installs are prompted once after upgrade; until the new choices are
 * saved, optional behavior remains disabled rather than inheriting old implicit consent.
 */
object DeviceSetupPreferences {
    private const val PREFS = "phonix_os_prefs"
    private const val KEY_SETUP_SAVED = "device_setup_choices_saved"
    private const val KEY_USE_MODE = "device_use_mode"
    private const val KEY_WALLPAPER = "device_apply_system_wallpaper"
    private const val KEY_LINUX_ON_LAUNCH = "device_start_linux_on_launch"
    private const val KEY_LINUX_BACKGROUND = "device_keep_linux_in_background"
    private const val KEY_LINUX_AT_BOOT = "device_start_linux_at_boot"
    private const val KEY_DISPLAY_WATCHER = "device_watch_external_displays"
    private const val KEY_IMMERSIVE_EXTERNAL = "device_immersive_external_display"
    private const val KEY_PRIVATE_DNS = "device_allow_system_private_dns"
    private const val KEY_OWNER_NETWORK = "device_allow_owner_network_management"
    private const val KEY_DEBLOAT_COMMANDS = "device_allow_debloat_commands"
    private const val KEY_FACTORY_RESET = "device_allow_factory_reset"

    fun save(context: Context, choices: DeviceChangeChoices) {
        prefs(context).edit()
            .putBoolean(KEY_SETUP_SAVED, true)
            .putString(KEY_USE_MODE, choices.useMode.name)
            .putBoolean(KEY_WALLPAPER, choices.applySystemWallpaper)
            .putBoolean(KEY_LINUX_ON_LAUNCH, choices.startLinuxOnLaunch)
            .putBoolean(KEY_LINUX_BACKGROUND, choices.keepLinuxInBackground)
            .putBoolean(KEY_LINUX_AT_BOOT, choices.startLinuxAtBoot)
            .putBoolean(KEY_DISPLAY_WATCHER, choices.watchExternalDisplays)
            .putBoolean(KEY_IMMERSIVE_EXTERNAL, choices.immersiveExternalDisplay)
            .putBoolean(KEY_PRIVATE_DNS, choices.allowSystemPrivateDns)
            .putBoolean(KEY_OWNER_NETWORK, choices.allowDeviceOwnerNetworkManagement)
            .putBoolean(KEY_DEBLOAT_COMMANDS, choices.allowDebloatCommands)
            .putBoolean(KEY_FACTORY_RESET, choices.allowFactoryReset)
            .apply()
    }

    fun choices(context: Context): DeviceChangeChoices {
        val p = prefs(context)
        // Do not infer consent from the old setup-complete flag. An app upgrade must ask
        // before continuing the previous implicit device changes.
        val savedMode = runCatching { DeviceUseMode.valueOf(p.getString(KEY_USE_MODE, null) ?: "") }
            .getOrDefault(DeviceUseMode.DAILY_DRIVER)
        return DeviceChangeChoices(
            useMode = savedMode,
            applySystemWallpaper = p.getBoolean(KEY_WALLPAPER, false),
            startLinuxOnLaunch = p.getBoolean(KEY_LINUX_ON_LAUNCH, false),
            keepLinuxInBackground = p.getBoolean(KEY_LINUX_BACKGROUND, false),
            startLinuxAtBoot = p.getBoolean(KEY_LINUX_AT_BOOT, false),
            watchExternalDisplays = p.getBoolean(KEY_DISPLAY_WATCHER, false),
            immersiveExternalDisplay = p.getBoolean(KEY_IMMERSIVE_EXTERNAL, false),
            allowSystemPrivateDns = p.getBoolean(KEY_PRIVATE_DNS, false),
            allowDeviceOwnerNetworkManagement = p.getBoolean(KEY_OWNER_NETWORK, false),
            allowDebloatCommands = p.getBoolean(KEY_DEBLOAT_COMMANDS, false),
            allowFactoryReset = p.getBoolean(KEY_FACTORY_RESET, false)
        )
    }

    fun shouldApplySystemWallpaper(context: Context): Boolean = choices(context).applySystemWallpaper
    fun shouldStartLinuxOnLaunch(context: Context): Boolean = choices(context).startLinuxOnLaunch
    fun shouldKeepLinuxInBackground(context: Context): Boolean = choices(context).keepLinuxInBackground
    fun shouldStartLinuxAtBoot(context: Context): Boolean {
        val choices = choices(context)
        return choices.startLinuxAtBoot && choices.keepLinuxInBackground
    }
    fun shouldWatchExternalDisplays(context: Context): Boolean = choices(context).watchExternalDisplays
    fun shouldUseImmersiveExternalDisplay(context: Context): Boolean = choices(context).immersiveExternalDisplay
    fun shouldWaitForWorkspace(context: Context): Boolean =
        android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    fun mayApplySystemPrivateDns(context: Context): Boolean = choices(context).allowSystemPrivateDns
    fun mayManageDeviceOwnerNetwork(context: Context): Boolean = choices(context).allowDeviceOwnerNetworkManagement
    fun hasSavedChoices(context: Context): Boolean = prefs(context).getBoolean(KEY_SETUP_SAVED, false)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
