package com.example.khoanhtim

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Hoat dong trong suot, khong giao dien: nhan lenh -> cho mot nhip
 * (de bang thong bao sap xuong / hieu ung chuyen xong) -> ban lenh
 * kich hoat Circle to Search -> dong ngay.
 */
class TriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val delay = intent.getLongExtra(EXTRA_DELAY, 250L)
        Handler(Looper.getMainLooper()).postDelayed({
            when (Trigger.fire(this)) {
                Trigger.RESULT_FAILED -> Toast.makeText(
                    this,
                    "Chưa gọi được. Kiểm tra đã đặt Google làm trợ lý mặc định chưa (bước 2).",
                    Toast.LENGTH_LONG
                ).show()
                Trigger.RESULT_ASSIST_FALLBACK -> Toast.makeText(
                    this,
                    "Chỉ mở được trợ lý thường — máy chưa cho vào thẳng Circle to Search.",
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }, delay)
    }

    companion object {
        const val EXTRA_DELAY = "delay_ms"
    }
}
