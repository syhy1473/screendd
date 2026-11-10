package com.screendd

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/**
 * 读写系统自动锁屏时长（Settings.System.SCREEN_OFF_TIMEOUT）。
 *
 * 设计要点：
 * - 开启时把「当前值」存进 SharedPreferences，再写入最大值；
 * - 关闭时只有「当前值仍是最大值」才写回保存值，避免覆盖用户手动改的值；
 * - 磁贴/界面状态一律以系统真实值为准，应用自己的标记只做辅助。
 */
object TimeoutRepository {

    /** OriginOS 6 的最大自动锁屏时长：10 分钟 */
    const val MAX_TIMEOUT_MS = 600_000

    /** 日常兜底值：1 分钟（保存值丢失时用它） */
    const val DEFAULT_TIMEOUT_MS = 60_000

    private const val PREFS = "screendd_prefs"
    private const val KEY_SAVED = "saved_timeout"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun canWrite(ctx: Context): Boolean = Settings.System.canWrite(ctx)

    fun currentTimeout(ctx: Context): Int =
        Settings.System.getInt(
            ctx.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            DEFAULT_TIMEOUT_MS
        )

    fun isAtMax(ctx: Context): Boolean = currentTimeout(ctx) == MAX_TIMEOUT_MS

    fun savedTimeout(ctx: Context): Int =
        prefs(ctx).getInt(KEY_SAVED, DEFAULT_TIMEOUT_MS)

    fun isEnabledFlag(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, false)

    /** 开启：保存当前值 → 写入最大值。返回是否成功。 */
    fun enable(ctx: Context): Boolean {
        if (!canWrite(ctx)) return false
        val cur = currentTimeout(ctx)
        prefs(ctx).edit()
            .putInt(KEY_SAVED, cur)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        return Settings.System.putInt(
            ctx.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            MAX_TIMEOUT_MS
        )
    }

    /** 关闭：若当前仍是最大值，写回保存值；否则只把标记置为关闭。 */
    fun disable(ctx: Context): Boolean {
        if (!canWrite(ctx)) return false
        val ok = if (isAtMax(ctx)) {
            Settings.System.putInt(
                ctx.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                savedTimeout(ctx)
            )
        } else {
            true
        }
        prefs(ctx).edit().putBoolean(KEY_ENABLED, false).apply()
        return ok
    }

    /** 兜底：直接恢复日常值（1 分钟）。 */
    fun restoreDefault(ctx: Context): Boolean {
        if (!canWrite(ctx)) return false
        val ok = Settings.System.putInt(
            ctx.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            DEFAULT_TIMEOUT_MS
        )
        prefs(ctx).edit().putBoolean(KEY_ENABLED, false).apply()
        return ok
    }

    fun formatMs(ms: Int): String = when {
        ms >= 60_000 && ms % 60_000 == 0 -> "${ms / 60_000} 分钟"
        ms >= 1_000 && ms % 1_000 == 0 -> "${ms / 1_000} 秒"
        else -> "${ms} 毫秒"
    }
}
