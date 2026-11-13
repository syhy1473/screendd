package com.screendd

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * 控制中心磁贴：点一下在「最大锁屏时长（10 分钟）」和「上次的锁屏时长」之间切换。
 *
 * 注意：磁贴由 SystemUI 按需绑定，应用进程平时可以是死的，不需要常驻后台。
 */
class ScreenTimeoutTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!TimeoutRepository.canWrite(applicationContext)) {
            toast("请先打开 screendd 授予「修改系统设置」权限")
            return
        }
        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    private fun toggle() {
        val ctx = applicationContext
        if (TimeoutRepository.isAtMax(ctx)) {
            val ok = TimeoutRepository.disable(ctx)
            if (ok) {
                toast("已恢复上次锁屏时长（${TimeoutRepository.formatMs(TimeoutRepository.savedTimeout(ctx))}）")
            } else {
                toast("恢复失败，请检查权限")
            }
        } else {
            val ok = TimeoutRepository.enable(ctx)
            if (ok) {
                toast("已设置到最大锁屏时长")
            } else {
                toast("设置失败，请检查权限")
            }
        }
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val ctx = applicationContext
        tile.state = if (TimeoutRepository.isAtMax(ctx)) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_label)
        // 副标题直接显示当前系统值，点击后立刻刷新，作为 Toast 被 ROM 屏蔽时的兜底反馈
        tile.subtitle = "当前自动锁屏时长：${TimeoutRepository.formatMs(TimeoutRepository.currentTimeout(ctx))}"
        tile.updateTile()
    }

    private fun toast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
