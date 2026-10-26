package com.example.khoanhtim

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Ban lenh kich hoat Circle to Search qua duong Voice Interaction cua
 * he thong (cung co che cac app kieu MiCTS dung o che do khong root):
 * he thong se trao lenh cho TRO LY MAC DINH — vi vay bat buoc dat
 * Google lam tro ly mac dinh thi moi ra dung Circle to Search /
 * Gemini nhu mong muon. Thu lan luot cac intent tu cu the den tong
 * quat, cach nao he thong nhan thi dung cach do.
 */
object Trigger {

    private const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"

    /** true = da ban duoc 1 lenh (he thong co the mo tro ly). */
    fun fire(ctx: Context): Boolean {
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
                return true
            } catch (e: Exception) {
                // Intent nay khong duoc nhan: thu cach tiep theo
            }
        }
        return false
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
