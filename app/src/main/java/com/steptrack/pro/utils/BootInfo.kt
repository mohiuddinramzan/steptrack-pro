package com.steptrack.pro.utils

import android.content.Context
import android.provider.Settings

object BootInfo {

    /** Number of times the phone has booted, or null if this device does not expose it. */
    fun readBootCount(context: Context): Int? =
        try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        } catch (e: Settings.SettingNotFoundException) {
            null
        } catch (e: SecurityException) {
            null
        }
}
