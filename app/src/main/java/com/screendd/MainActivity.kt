package com.screendd

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var toggleButton: Button
    private lateinit var grantButton: Button
    private lateinit var restoreButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        detailText = findViewById(R.id.detailText)
        toggleButton = findViewById(R.id.toggleButton)
        grantButton = findViewById(R.id.grantButton)
        restoreButton = findViewById(R.id.restoreButton)

        toggleButton.setOnClickListener { toggle() }
        grantButton.setOnClickListener { openWriteSettings() }
        restoreButton.setOnClickListener { restoreDefault() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val canWrite = TimeoutRepository.canWrite(this)
        grantButton.visibility = if (canWrite) View.GONE else View.VISIBLE

        val atMax = TimeoutRepository.isAtMax(this)
        val cur = TimeoutRepository.currentTimeout(this)

        statusText.text = "当前自动锁屏时长：${TimeoutRepository.formatMs(cur)}"
        detailText.text = if (atMax) {
            "已开启最大锁屏时长（${TimeoutRepository.formatMs(TimeoutRepository.MAX_TIMEOUT_MS)}），点下方按钮恢复上次值"
        } else {
            "点下方按钮切换到 ${TimeoutRepository.formatMs(TimeoutRepository.MAX_TIMEOUT_MS)} 锁屏"
        }
        toggleButton.text = if (atMax) "恢复上次锁屏时长" else "切换到最大锁屏时长"
        toggleButton.isEnabled = canWrite
        restoreButton.isEnabled = canWrite
    }

    private fun toggle() {
        if (!TimeoutRepository.canWrite(this)) {
            openWriteSettings()
            return
        }
        val atMax = TimeoutRepository.isAtMax(this)
        val ok = if (atMax) {
            TimeoutRepository.disable(this)
        } else {
            TimeoutRepository.enable(this)
        }
        val msg = if (ok) {
            if (atMax) {
                "已恢复上次锁屏时长（${TimeoutRepository.formatMs(TimeoutRepository.savedTimeout(this))}）"
            } else {
                "已设置到最大锁屏时长"
            }
        } else {
            "切换失败，请检查「修改系统设置」权限"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        render()
    }

    private fun restoreDefault() {
        if (!TimeoutRepository.canWrite(this)) {
            openWriteSettings()
            return
        }
        val ok = TimeoutRepository.restoreDefault(this)
        Toast.makeText(
            this,
            if (ok) "已恢复默认（${TimeoutRepository.formatMs(TimeoutRepository.DEFAULT_TIMEOUT_MS)}）" else "恢复失败",
            Toast.LENGTH_SHORT
        ).show()
        render()
    }

    private fun openWriteSettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "无法打开系统设置页，请手动到「设置 → 应用 → 特殊权限」中授权", Toast.LENGTH_LONG).show()
            }
        }
    }
}
