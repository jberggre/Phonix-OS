package com.example.util

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log
import com.example.model.NetworkLinkState
import com.example.model.NetworkTransport
import com.example.receiver.PhonixDeviceAdminReceiver
import java.net.Inet4Address

/**
 * Reading and setting up the network without a phone screen.
 *
 * Why this is not built on `WifiNetworkSpecifier`: that API creates an app-scoped connection
 * that is torn down when the requesting process dies, and the system asks the user to approve
 * it on screen. On an appliance that reboots unattended and runs a foreground service for days,
 * that would mean the machine loses its network every time the app is killed — and the
 * approval dialog is not guaranteed to land on the DeX monitor.
 *
 * What Phonix does instead, because it is the device owner (a DPC):
 *  - `WifiManager.addNetwork` writes a real, persistent Android network. Android 10's
 *    "apps targeting Q cannot configure Wi-Fi" restriction explicitly exempts DPCs, and the
 *    configuration survives reboots because it belongs to the platform, not to this process.
 *  - When the unit is not the device owner, or the platform refuses, the UI falls back to
 *    opening Android's own Wi-Fi settings, which always works and also lists nearby networks
 *    (scan results need location, which is why the in-app form asks for the name instead).
 */
object NetworkOnboarding {

    private const val TAG = "PhonixNetwork"

    sealed interface ConnectResult {
        /** The platform accepted and saved the network; it still has to associate and validate. */
        data class Configured(val netId: Int) : ConnectResult

        /** Nothing was written; [reason] is shown to the customer verbatim. */
        data class Rejected(val reason: String) : ConnectResult
    }

    fun currentState(context: Context): NetworkLinkState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkLinkState()
        val network = cm.activeNetwork ?: return NetworkLinkState()
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkLinkState()
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            else -> NetworkTransport.OTHER
        }
        // VALIDATED is the platform's own "captive portal check passed" signal. Without it the
        // link may be up and still unable to reach anything, so it does not count as online.
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val ipv4 = try {
            cm.getLinkProperties(network)?.linkAddresses
                ?.firstOrNull { it.address is Inet4Address }
                ?.address?.hostAddress
        } catch (_: Throwable) {
            null
        }

        return NetworkLinkState(
            online = online,
            transport = transport,
            networkName = if (transport == NetworkTransport.WIFI) wifiSsid(context) else null,
            localIpv4 = ipv4
        )
    }

    fun isDeviceOwner(context: Context): Boolean = try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.isDeviceOwnerApp(context.packageName) == true
    } catch (_: Throwable) {
        false
    }

    /**
     * A DPC can grant its own runtime permissions. Only ACCESS_FINE_LOCATION is needed, and only
     * so Android will reveal the connected SSID to us; Phonix does not scan for networks and
     * does not turn location services on. Silent when the unit is not device owner.
     */
    fun allowSsidReadback(context: Context) {
        if (!isDeviceOwner(context)) return
        try {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, PhonixDeviceAdminReceiver::class.java)
            val granted = dpm.setPermissionGrantState(
                admin,
                context.packageName,
                Manifest.permission.ACCESS_FINE_LOCATION,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
            )
            Log.i(TAG, "device owner granted ACCESS_FINE_LOCATION for SSID readback: $granted")
        } catch (t: Throwable) {
            Log.w(TAG, "could not grant SSID permission", t)
        }
    }

    /**
     * Saves a normal Android Wi-Fi network and asks the platform to join it.
     *
     * The password is handed straight to WifiManager and never written anywhere by Phonix.
     * Returns [ConnectResult.Rejected] with a reason the UI can show instead of pretending.
     */
    fun connectWifi(context: Context, ssid: String, passphrase: String): ConnectResult {
        val app = context.applicationContext
        val wifi = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ConnectResult.Rejected("Android reports no Wi-Fi adapter on this unit.")
        val name = ssid.trim()
        if (name.isEmpty()) return ConnectResult.Rejected("Enter the Wi-Fi network name.")

        if (!isDeviceOwner(context)) {
            return ConnectResult.Rejected(
                "This unit is not provisioned as a Phonix device owner, so Android will not let " +
                    "the app save a network. Use \"Open Android Wi-Fi settings\" below."
            )
        }

        val configuration = WifiConfiguration().apply {
            SSID = quote(name)
            if (passphrase.isEmpty()) {
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            } else {
                preSharedKey = quote(passphrase)
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK)
            }
        }

        val netId = try {
            @Suppress("DEPRECATION")
            wifi.addNetwork(configuration)
        } catch (t: Throwable) {
            Log.w(TAG, "addNetwork failed", t)
            return ConnectResult.Rejected(
                "Android refused to save the network (${t.javaClass.simpleName}). " +
                    "Use \"Open Android Wi-Fi settings\" below."
            )
        }

        if (netId < 0) {
            return ConnectResult.Rejected(
                "Android refused to save that network. Check the name and password, or use " +
                    "\"Open Android Wi-Fi settings\" below."
            )
        }

        @Suppress("DEPRECATION")
        val enabled = wifi.enableNetwork(netId, true)
        @Suppress("DEPRECATION")
        wifi.reconnect()
        Log.i(TAG, "saved network $netId (ssid=$name) enableNetwork=$enabled")
        return ConnectResult.Configured(netId)
    }

    /** Android's own picker: the escape hatch that works regardless of DPC state. */
    fun openWifiSettings(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Calls [onChange] whenever the default network appears, disappears or changes quality. */
    fun watch(context: Context, onChange: () -> Unit): AutoCloseable {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return AutoCloseable {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onChange()
            override fun onLost(network: Network) = onChange()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = onChange()
        }
        return try {
            cm.registerDefaultNetworkCallback(callback)
            AutoCloseable {
                try {
                    cm.unregisterNetworkCallback(callback)
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "could not watch connectivity", t)
            AutoCloseable {}
        }
    }

    private fun wifiSsid(context: Context): String? = try {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val raw = wifi?.connectionInfo?.ssid
        val clean = raw?.trim('"')
        if (clean.isNullOrBlank() || clean.equals("<unknown ssid>", ignoreCase = true)) null else clean
    } catch (_: Throwable) {
        null
    }

    /**
     * WifiConfiguration takes the SSID and passphrase as quoted strings. Both can contain a
     * quote or a backslash, which would otherwise produce a broken configuration on the
     * platform side, so they are escaped rather than concatenated raw.
     */
    private fun quote(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
