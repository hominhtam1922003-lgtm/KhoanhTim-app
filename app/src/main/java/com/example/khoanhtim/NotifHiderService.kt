package com.example.khoanhtim

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Tu an thong bao he thong "Ung dung dang hien thi tren ung dung
 * khac" — thong bao dai han Android tu hien khi bat quyen ve de len
 * ung dung khac (chinh la quyen thanh cu chi can). Chi tac dong dung
 * thong bao do cua he thong (goi "android"), khong dung thong bao
 * cua bat ky app nao khac.
 */
class NotifHiderService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != "android") return
        val extras = sbn.notification?.extras ?: return
        val text = buildString {
            append(extras.getCharSequence("android.title") ?: "")
            append(' ')
            append(extras.getCharSequence("android.text") ?: "")
            append(' ')
            append(extras.getCharSequence("android.bigText") ?: "")
        }.lowercase()
        val isOverlayNotice = OVERLAY_HINTS.any { text.contains(it) }
        if (isOverlayNotice) {
            try {
                // Tam an 1 ngay, lan hien ke tiep lai duoc an tiep
                snoozeNotification(sbn.key, SNOOZE_MS)
            } catch (e: Exception) {
                try {
                    cancelNotification(sbn.key)
                } catch (e2: Exception) {
                    // He thong khong cho: bo qua
                }
            }
        }
    }

    companion object {
        private const val SNOOZE_MS = 24L * 60 * 60 * 1000
        private val OVERLAY_HINTS = listOf(
            "displaying over other apps",
            "hiển thị trên ứng dụng khác",
            "hien thi tren ung dung khac",
            "draw over other apps",
            "đang hiển thị trên",
            "is displaying over"
        )
    }
}
