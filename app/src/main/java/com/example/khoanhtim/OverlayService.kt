package com.example.khoanhtim

import com.example.khoanhtim.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Dich vu giu mot VUNG CHAM VO HINH nam de DUNG TREN thanh dieu
 * huong mac dinh cua may (dung vi tri + chieu cao thanh cu chi goc).
 * Nguoi dung cu GIU TREN THANH CUA MAY nhu binh thuong ~nua giay la
 * ban lenh Circle to Search; khong co thanh la nao xuat hien.
 * Bam nhanh khong lam gi; keo lech nhieu (vuot Home/man gan day)
 * thi huy giu — vuot dieu huong cua he thong van hoat dong rieng.
 */
class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var strip: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var longPressFired = false

    private val longPressRunnable = Runnable {
        longPressFired = true
        Trigger.fire(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        startForegroundCompat()
        showStrip()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(longPressRunnable)
        strip?.let {
            try {
                wm?.removeView(it)
            } catch (e: Exception) {
                // Da bi go boi he thong
            }
        }
        strip = null
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val channelId = "khoanhtim_overlay"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Thanh cử chỉ Khoanh Tìm",
                NotificationManager.IMPORTANCE_MIN
            )
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Khoanh Tìm đang chạy")
            .setContentText("Giữ thanh điều hướng của máy để tìm bằng vòng tròn")
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun showStrip() {
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = manager
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // Vung cham HOAN TOAN trong suot: khong ve gi ca, chi nam nghe
        // thao tac giu tren thanh dieu huong cua may
        val container = FrameLayout(this).apply {
            setBackgroundColor(0x00000000)
            setOnTouchListener { _, ev -> onStripTouch(ev) }
        }

        // Chieu cao dung bang thanh dieu huong goc cua may
        val navH = try {
            val resId = resources.getIdentifier(
                "navigation_bar_height", "dimen", "android"
            )
            if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        } catch (e: Exception) {
            0
        }
        val stripHeight = maxOf(navH, dp(28))

        val params = WindowManager.LayoutParams(
            dp(170),
            stripHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }
        try {
            manager.addView(container, params)
            strip = container
        } catch (e: Exception) {
            // Chua co quyen ve de len ung dung khac: dung dich vu,
            // man hinh chinh se huong dan cap quyen
            stopSelf()
        }
    }

    private fun onStripTouch(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                longPressFired = false
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (dx * dx + dy * dy > MOVE_CANCEL_PX * MOVE_CANCEL_PX) {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
            }
        }
        // Tra true de nhan du chuoi cham (DOWN..UP); vung thanh rat nho
        // o sat canh duoi nen khong anh huong thao tac thuong ngay
        return true
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val LONG_PRESS_MS = 480L
        private const val MOVE_CANCEL_PX = 36f

        @Volatile
        var running: Boolean = false
            private set

        fun start(ctx: Context) {
            val i = Intent(ctx, OverlayService::class.java)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }
    }
}
