package com.example.khoanhtim

import com.example.khoanhtim.R

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Man hinh thiet lap: huong dan cap quyen theo dung thu tu app goc —
 * 1) ve de len ung dung khac, 2) Google la tro ly mac dinh,
 * 3) an thong bao he thong (tuy chon), 4) pin/tu khoi chay —
 * kem cong tac bat/tat thanh cu chi va nut thu kich hoat.
 */
class MainActivity : Activity() {

    private lateinit var switchOverlay: Switch
    private lateinit var tvServiceState: TextView
    private lateinit var statusOverlay: TextView
    private lateinit var statusAssistant: TextView
    private lateinit var statusNotif: TextView
    private lateinit var statusBattery: TextView
    private var syncingSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchOverlay = findViewById(R.id.switchOverlay)
        tvServiceState = findViewById(R.id.tvServiceState)
        statusOverlay = findViewById(R.id.statusOverlay)
        statusAssistant = findViewById(R.id.statusAssistant)
        statusNotif = findViewById(R.id.statusNotif)
        statusBattery = findViewById(R.id.statusBattery)

        switchOverlay.setOnCheckedChangeListener { _, checked ->
            if (syncingSwitch) return@setOnCheckedChangeListener
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    toast("Cần bật quyền “Hiển thị trên ứng dụng khác” trước (bước 1)")
                    openOverlaySettings()
                    refresh()
                    return@setOnCheckedChangeListener
                }
                prefs().edit().putBoolean(BootReceiver.KEY_OVERLAY_ON, true).apply()
                try {
                    OverlayService.start(this)
                } catch (e: Exception) {
                    toast("Chưa bật được thanh cử chỉ: ${e.message}")
                }
            } else {
                prefs().edit().putBoolean(BootReceiver.KEY_OVERLAY_ON, false).apply()
                OverlayService.stop(this)
            }
            refresh()
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener { openOverlaySettings() }
        findViewById<Button>(R.id.btnAssistant).setOnClickListener { openAssistantSettings() }
        findViewById<Button>(R.id.btnNotif).setOnClickListener { openNotifListenerSettings() }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { openBatterySettings() }
        findViewById<Button>(R.id.btnOem).setOnClickListener { openOemAutostart() }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val ok = Trigger.fire(this)
            if (!ok) toast("Chưa gọi được trợ lý — kiểm tra bước 2 (Google làm trợ lý mặc định)")
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun prefs() = getSharedPreferences(BootReceiver.PREFS, MODE_PRIVATE)

    private fun refresh() {
        // Buoc 1: quyen ve de
        val hasOverlay = Settings.canDrawOverlays(this)
        setStatus(statusOverlay, hasOverlay, "Đã bật ✓", "Chưa bật")

        // Buoc 2: tro ly mac dinh co phai Google
        val assistant = Settings.Secure.getString(contentResolver, "assistant") ?: ""
        val googleAssistant = assistant.contains("com.google.android.googlequicksearchbox")
        setStatus(
            statusAssistant, googleAssistant, "Google ✓",
            if (assistant.isBlank()) "Chưa đặt" else "Đang là app khác"
        )

        // Buoc 3: quyen doc thong bao
        val listeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: ""
        val hasListener = listeners.contains(packageName)
        setStatus(statusNotif, hasListener, "Đã bật ✓", "Chưa bật")

        // Buoc 4: pin
        val pm = getSystemService(PowerManager::class.java)
        val noBatteryLimit = pm?.isIgnoringBatteryOptimizations(packageName) == true
        setStatus(statusBattery, noBatteryLimit, "Đã bỏ giới hạn ✓", "Đang bị giới hạn")

        // Cong tac thanh cu chi theo trang thai dich vu that
        val wantOn = prefs().getBoolean(BootReceiver.KEY_OVERLAY_ON, false)
        syncingSwitch = true
        switchOverlay.isChecked = OverlayService.running || (wantOn && hasOverlay)
        syncingSwitch = false
        tvServiceState.text = if (OverlayService.running) {
            "Đang chạy — giữ thanh điều hướng của máy để tìm"
        } else {
            "Đang tắt"
        }
    }

    private fun setStatus(tv: TextView, ok: Boolean, okText: String, noText: String) {
        tv.text = if (ok) okText else noText
        tv.setTextColor(if (ok) 0xFF81C784.toInt() else 0xFFFFB74D.toInt())
    }

    private fun openOverlaySettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openAssistantSettings() {
        val candidates = listOf(
            Intent("android.settings.VOICE_INPUT_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (i in candidates) {
            try {
                startActivity(i)
                toast("Chọn “Ứng dụng trợ lý số” → Google")
                return
            } catch (e: Exception) {
                // Thu trang cai dat tiep theo
            }
        }
    }

    private fun openNotifListenerSettings() {
        try {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openBatterySettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    /** Trang Tu khoi chay rieng cua tung hang (chi mo duoc tren may cua hang do). */
    private fun openOemAutostart() {
        val candidates = listOf(
            // Xiaomi / Redmi / POCO
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            ),
            // Vivo / iQOO
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            ),
            // OPPO / Realme
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            ),
            // Huawei / Honor
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            // Samsung
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
        for (cn in candidates) {
            try {
                startActivity(Intent().setComponent(cn))
                return
            } catch (e: Exception) {
                // Khong phai may cua hang nay: thu tiep
            }
        }
        toast("Không tìm thấy trang tự khởi chạy của hãng này — vào Cài đặt → Ứng dụng → Khoanh Tìm để bật thủ công")
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
