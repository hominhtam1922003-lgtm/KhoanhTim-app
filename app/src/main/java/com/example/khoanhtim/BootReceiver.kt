package com.example.khoanhtim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Khoi dong lai may: bat lai thanh cu chi neu truoc do dang bat. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_OVERLAY_ON, false)) {
            try {
                OverlayService.start(context)
            } catch (e: Exception) {
                // Thieu quyen ve de: cho nguoi dung mo app cap lai
            }
        }
    }

    companion object {
        const val PREFS = "khoanhtim_prefs"
        const val KEY_OVERLAY_ON = "overlay_on"
    }
}
