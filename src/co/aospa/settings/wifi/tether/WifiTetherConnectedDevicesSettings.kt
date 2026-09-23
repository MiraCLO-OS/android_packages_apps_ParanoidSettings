/*
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.settings.wifi.tether

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment

/** Shows the devices connected to the Wi-Fi hotspot. */
class WifiTetherConnectedDevicesSettings : DashboardFragment() {
    override fun getPreferenceScreenResId() = R.xml.wifi_tether_connected_devices

    override fun getLogTag() = TAG

    override fun getMetricsCategory() = SettingsEnums.WIFI_TETHER_SETTINGS

    private companion object {
        const val TAG = "WifiTetherConnectedDevices"
    }
}
