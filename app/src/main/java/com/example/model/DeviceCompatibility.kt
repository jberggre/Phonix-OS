package com.example.model

/** Current fold posture as reported for the activity window by AndroidX WindowManager. */
enum class FoldPosture {
    FLAT,
    HALF_OPENED,
    UNKNOWN
}

enum class DisplayRoute {
    SAMSUNG_DEX_WORKSPACE,
    SAMSUNG_EXTERNAL_NO_WORKSPACE,
    SAMSUNG_NO_EXTERNAL,
    PIXEL_EXTERNAL_DISPLAY_UNVERIFIED,
    EXTERNAL_DISPLAY_UNVERIFIED,
    PHONE_ONLY
}

enum class DeviceFormFactor {
    FOLDABLE_UNFOLDED,
    FOLDABLE_HALF_OPENED,
    LARGE_WINDOW,
    PHONE
}

data class DeviceCapabilityReport(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val apiLevel: Int,
    val appTargetSdk: Int,
    val route: DisplayRoute,
    val formFactor: DeviceFormFactor,
    val externalDisplayCount: Int,
    val hasDexWorkspace: Boolean,
    val isDeviceOwner: Boolean,
    val canWriteSecureSettings: Boolean,
    val recommendedCheck: String
)

enum class DeviceUseMode {
    DAILY_DRIVER,
    CYBERDECK
}

/** Inputs and preferences for optional setup actions that affect host-device behavior. */
data class DeviceChangeChoices(
    val useMode: DeviceUseMode = DeviceUseMode.DAILY_DRIVER,
    val applySystemWallpaper: Boolean = false,
    val startLinuxOnLaunch: Boolean = false,
    val keepLinuxInBackground: Boolean = false,
    val startLinuxAtBoot: Boolean = false,
    val watchExternalDisplays: Boolean = false,
    val immersiveExternalDisplay: Boolean = false,
    val allowSystemPrivateDns: Boolean = false,
    val allowDeviceOwnerNetworkManagement: Boolean = false,
    val allowDebloatCommands: Boolean = false,
    val allowFactoryReset: Boolean = false
)

/** Pure route classifier; an attached display is not proof of a particular link protocol. */
object DeviceCompatibilityClassifier {
    /**
     * A foldable's second panel is not an external monitor.
     *
     * Measured on a Pixel 9 Pro Fold (Android 17): both panels report as ordinary displays, the
     * cover one as "Outer Display", display id 3, and neither carries FLAG_PRESENTATION. A
     * heuristic that only excluded the default display therefore claimed a monitor was attached
     * and launched the desktop onto the cover screen while the phone was folded.
     *
     * FLAG_PRESENTATION is the public flag the framework itself filters its presentation display
     * category on, so an external HDMI/DP output carries it while an internal panel does not.
     */
    fun isPresentationOutput(hasPresentationFlag: Boolean, isOn: Boolean): Boolean =
        isOn && hasPresentationFlag

    fun displayRoute(
        manufacturer: String,
        hasDexWorkspace: Boolean,
        hasExternalDisplay: Boolean
    ): DisplayRoute = when {
        manufacturer.equals("samsung", ignoreCase = true) && hasDexWorkspace ->
            DisplayRoute.SAMSUNG_DEX_WORKSPACE
        manufacturer.equals("samsung", ignoreCase = true) && hasExternalDisplay ->
            DisplayRoute.SAMSUNG_EXTERNAL_NO_WORKSPACE
        manufacturer.equals("samsung", ignoreCase = true) ->
            DisplayRoute.SAMSUNG_NO_EXTERNAL
        manufacturer.equals("Google", ignoreCase = true) && hasExternalDisplay ->
            DisplayRoute.PIXEL_EXTERNAL_DISPLAY_UNVERIFIED
        hasExternalDisplay -> DisplayRoute.EXTERNAL_DISPLAY_UNVERIFIED
        else -> DisplayRoute.PHONE_ONLY
    }

    fun formFactor(foldPosture: FoldPosture, windowWidthDp: Int): DeviceFormFactor = when {
        foldPosture == FoldPosture.FLAT -> DeviceFormFactor.FOLDABLE_UNFOLDED
        foldPosture == FoldPosture.HALF_OPENED -> DeviceFormFactor.FOLDABLE_HALF_OPENED
        windowWidthDp >= 600 -> DeviceFormFactor.LARGE_WINDOW
        else -> DeviceFormFactor.PHONE
    }

    fun recommendation(route: DisplayRoute, manufacturer: String, apiLevel: Int): String = when (route) {
        DisplayRoute.SAMSUNG_DEX_WORKSPACE ->
            "DeX workspace detected. Run the real DeX test: window placement, input, resize, dock/undock."
        DisplayRoute.SAMSUNG_EXTERNAL_NO_WORKSPACE ->
            "External display detected on Samsung, but no DeX workspace is visible yet. Wait for DeX to start; do not treat the raw HDMI output as the DeX desktop."
        DisplayRoute.SAMSUNG_NO_EXTERNAL ->
            "Samsung device. Connect a DeX-capable USB-C display/dock, then run the DeX test."
        DisplayRoute.PIXEL_EXTERNAL_DISPLAY_UNVERIFIED -> if (apiLevel >= 36) {
            "External display detected on Pixel. USB-C DP Alt Mode is a candidate, not independently proven by Android APIs. Select Desktop if offered, then verify separate desktop vs mirroring. This Pixel route remains unverified until tested on-device."
        } else {
            "External display detected on Pixel. This Android version may mirror only; verify the actual display mode."
        }
        DisplayRoute.EXTERNAL_DISPLAY_UNVERIFIED ->
            "External display detected. Confirm whether Android provides a separate desktop or only mirroring."
        DisplayRoute.PHONE_ONLY -> if (manufacturer.equals("Google", ignoreCase = true)) {
            "No external display is connected. Android cannot confirm DisplayPort Alt Mode before a compatible adapter/display is attached; Pixel 9 series can be tested with a USB-C DP Alt Mode adapter."
        } else {
            "Phone-only test. Connect an external display to test the vendor's desktop mode."
        }
    }
}
