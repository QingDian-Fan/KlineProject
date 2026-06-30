package com.common.kline

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 测试数据生成工具：生成 1 分钟周期的 K 线测试数据。
 */
object TestDataFactory {

    const val PERIOD_MILLIS = 60_000L

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    /**
     * 生成 [count] 条 1 分钟 K 线（随机游走），起始周期时间从 [startTime] 起逐根 +1 分钟。
     *
     * @param seed 固定随机种子，保证测试可重现
     */
    fun generate(
        count: Int,
        startPrice: Float = 100f,
        startTime: Long = 0L,
        seed: Long = 42L
    ): MutableList<KLineEntity> {
        val random = Random(seed)
        val list = ArrayList<KLineEntity>(count)
        var prevClose = startPrice
        for (i in 0 until count) {
            val open = prevClose
            val close = (open + (random.nextFloat() - 0.5f) * 2f).coerceAtLeast(1f)
            val high = maxOf(open, close) + random.nextFloat()
            val low = (minOf(open, close) - random.nextFloat()).coerceAtLeast(0.5f)
            val volume = 1000f + random.nextInt(0, 9000)
            list.add(bar(startTime + i * PERIOD_MILLIS, open, high, low, close, volume))
            prevClose = close
        }
        return list
    }

    /** 构造单根 K 线 */
    fun bar(
        barTime: Long,
        open: Float,
        high: Float,
        low: Float,
        close: Float,
        volume: Float
    ): KLineEntity = KLineEntity().apply {
        this.barTime = barTime
        Date = dateFormat.format(Date(barTime))
        Open = open
        High = high
        Low = low
        Close = close
        Volume = volume
    }
}