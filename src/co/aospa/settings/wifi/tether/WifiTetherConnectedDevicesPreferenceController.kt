/*
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.settings.wifi.tether

import android.content.Context
import android.net.TetheredClient
import android.net.TetheringManager
import android.net.TetheringManager.TETHERING_WIFI
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.wifi.WifiUtils.Companion.getWifiTetherSummaryForConnectedDevices
import java.net.Inet4Address

/**
 * Lists the clients of the Wi-Fi hotspot on the connected devices screen, and counts them in the
 * summary of the row that opens it.
 *
 * The clients come from the hotspot's DHCP leases, so the list is best-effort: one on a static IP
 * has no address, and one that just left lingers until its lease expires.
 */
class WifiTetherConnectedDevicesPreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey),
    DefaultLifecycleObserver,
    TetheringManager.TetheringEventCallback {
    private val tetheringManager by lazy { mContext.getSystemService(TetheringManager::class.java) }

    private var preference: Preference? = null
    private var connectedClients = emptyList<TetheredClient>()

    override fun getAvailabilityStatus(): Int =
        if (tetheringManager == null) UNSUPPORTED_ON_DEVICE else AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
        update()
    }

    override fun onStart(owner: LifecycleOwner) {
        tetheringManager?.registerTetheringEventCallback(mContext.mainExecutor, this)
    }

    override fun onStop(owner: LifecycleOwner) {
        tetheringManager?.unregisterTetheringEventCallback(this)
    }

    override fun onClientsChanged(clients: Collection<TetheredClient>) {
        connectedClients = clients.filter { it.tetheringType == TETHERING_WIFI }
        update()
    }

    private fun update() {
        when (val preference = preference) {
            null -> return
            is PreferenceScreen -> preference.listClients()
            else -> preference.summary = clientCountSummary()
        }
    }

    /** The count as the opening row's summary, worded like the rest of the hotspot screens. */
    private fun clientCountSummary(): String =
        if (connectedClients.isEmpty()) {
            mContext.getString(R.string.wifi_tether_connected_devices_empty)
        } else {
            getWifiTetherSummaryForConnectedDevices(mContext, connectedClients.size)
        }

    private fun PreferenceScreen.listClients() {
        removeAll()
        if (connectedClients.isEmpty()) {
            addPreference(
                Preference(mContext).apply {
                    key = KEY_NO_DEVICES
                    title = mContext.getString(R.string.wifi_tether_connected_devices_empty)
                    isSelectable = false
                }
            )
            return
        }
        for (client in connectedClients) {
            val macAddress = client.macAddress.toString()
            val ipAddress = client.ipAddress()
            addPreference(
                Preference(mContext).apply {
                    key = macAddress
                    title =
                        client.hostname()
                            ?: mContext.getString(
                                R.string.wifi_tether_connected_device_unknown_name
                            )
                    summary =
                        ipAddress?.let {
                            mContext.getString(
                                R.string.wifi_tether_connected_device_summary,
                                it,
                                macAddress,
                            )
                        } ?: macAddress
                    isSelectable = false
                }
            )
        }
    }

    /** The name the client asked for over DHCP, or null when it sent none. */
    private fun TetheredClient.hostname(): String? =
        addresses.firstNotNullOfOrNull { it.hostname?.takeIf(String::isNotEmpty) }

    /** The client's IPv4 address, falling back to whatever address is known. */
    private fun TetheredClient.ipAddress(): String? {
        val inetAddresses = addresses.map { it.address.address }
        return (inetAddresses.firstOrNull { it is Inet4Address } ?: inetAddresses.firstOrNull())
            ?.hostAddress
    }

    private companion object {
        const val KEY_NO_DEVICES = "wifi_tether_no_connected_devices"
    }
}
