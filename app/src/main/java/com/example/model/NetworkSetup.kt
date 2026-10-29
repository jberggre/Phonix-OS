package com.example.model

/**
 * How the machine reaches the outside world.
 *
 * Every value here comes from Android's own [android.net.ConnectivityManager]; nothing is
 * inferred from the app's wishful thinking. A unit on a USB-C hub with Ethernet never needs to
 * touch Wi-Fi at all, which is why wired is a first-class case rather than a footnote.
 */
enum class NetworkTransport {
    WIFI,
    ETHERNET,
    CELLULAR,
    NONE,
    OTHER;

    val label: String
        get() = when (this) {
            WIFI -> "Wi-Fi"
            ETHERNET -> "Wired"
            CELLULAR -> "Mobile data"
            NONE -> "Not connected"
            OTHER -> "Network"
        }
}

data class NetworkLinkState(
    val online: Boolean = false,
    val transport: NetworkTransport = NetworkTransport.NONE,
    /** SSID on Wi-Fi. Null when Android withholds it or on a wired link. */
    val networkName: String? = null,
    val localIpv4: String? = null
) {
    /**
     * One sentence for the UI. `online` is only true when Android reports the network as
     * validated, so this never claims a working internet connection that has not been proven
     * by the platform.
     */
    val summary: String
        get() = when {
            transport == NetworkTransport.NONE ->
                "No network connection. Phonix cannot reach the internet until one is set up."
            online && networkName != null ->
                "${transport.label} connected to $networkName. Android has verified internet access."
            online ->
                "${transport.label} connected. Android has verified internet access."
            networkName != null ->
                "${transport.label} connected to $networkName, but the internet is not reachable yet."
            else ->
                "${transport.label} connected, but the internet is not reachable yet."
        }
}
