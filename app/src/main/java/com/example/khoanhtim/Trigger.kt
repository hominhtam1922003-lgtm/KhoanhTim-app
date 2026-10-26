package com.example.khoanhtim

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Kich hoat Circle to Search.
 *
 * DUONG CHINH (dung co che cua MiCTS, giong cach may Samsung/Pixel
 * goi khi giu thanh dieu huong): goi thang dich vu he thong
 * IVoiceInteractionManagerService.showSessionFromSession voi
 * flags = 7 (ASSIST + SCREENSHOT) va goi du lieu co khoa
 * "omni.entry_point". He thong se trao phien tim kiem cho TRO LY
 * MAC DINH — vi vay bat buoc dat Google lam tro ly mac dinh, va
 * Google se mo dung Circle to Search thay vi bang tro ly thuong.
 * Day la API an cua Android nen can HiddenApiBypass de goi.
 *
 * DUONG DU PHONG: intent ACTION_ASSIST nhu cac ban dau (chi mo
 * duoc tro ly thuong tren hau het may).
 */
object Trigger {

    const val RESULT_FAILED = 0
    const val RESULT_ASSIST_FALLBACK = 1
    const val RESULT_OMNIENT = 2

    private const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"

    /** @return RESULT_OMNIENT / RESULT_ASSIST_FALLBACK / RESULT_FAILED */
    fun fire(ctx: Context): Int {
        if (fireOmnient()) {
            vibrate(ctx)
            return RESULT_OMNIENT
        }
        val attempts = listOf(
            Intent(Intent.ACTION_ASSIST).setPackage(GOOGLE_PKG),
            Intent(Intent.ACTION_ASSIST),
            Intent("android.intent.action.VOICE_COMMAND").setPackage(GOOGLE_PKG),
            Intent("android.intent.action.VOICE_COMMAND")
        )
        for (raw in attempts) {
            try {
                val i = Intent(raw).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
                vibrate(ctx)
                return RESULT_ASSIST_FALLBACK
            } catch (e: Exception) {
                // Intent nay khong duoc nhan: thu cach tiep theo
            }
        }
        return RESULT_FAILED
    }

    private fun fireOmnient(): Boolean {
        return try {
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", 1)
            }
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "voiceinteraction") as? IBinder
                ?: return false
            val vims = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService\$Stub"
            ).getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)
                ?: return false
            val iVimsClass = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService"
            )
            val result = if (Build.VERSION.SDK_INT >= 34) {
                HiddenApiBypass.invoke(
                    iVimsClass, vims, "showSessionFromSession",
                    null, bundle, 7, "hyperOS_home"
                )
            } else {
                HiddenApiBypass.invoke(
                    iVimsClass, vims, "showSessionFromSession",
                    null, bundle, 7
                )
            }
            result as? Boolean ?: false
        } catch (e: Throwable) {
            false
        }
    }

    private fun vibrate(ctx: Context) {
        try {
            val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vib?.vibrate(
                VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (e: Exception) {
            // Rung chi la phu tro
        }
    }
}
