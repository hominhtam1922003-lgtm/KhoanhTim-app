package com.example.khoanhtim

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Nut bat nhanh trong bang Cai dat nhanh: cham = kich hoat Circle to
 * Search (cho bang thong bao sap xuong truoc roi moi ban lenh).
 * Giu lau = mo app de cai dat.
 */
class TriggerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { t ->
            t.state = Tile.STATE_ACTIVE
            t.icon = Icon.createWithResource(this, R.drawable.ic_tile)
            t.label = "Khoanh Tìm"
            t.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val i = Intent(this, TriggerActivity::class.java).apply {
            putExtra(TriggerActivity.EXTRA_DELAY, 450L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        @Suppress("DEPRECATION")
        startActivityAndCollapse(i)
    }
}
