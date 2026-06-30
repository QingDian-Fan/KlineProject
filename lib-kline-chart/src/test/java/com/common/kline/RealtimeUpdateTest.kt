package com.common.kline

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 实时行情更新模式的单元测试：演示并验证「同一周期内更新最后一根 / 跨周期新增一根」。
 *
 * [onTick] 即接入方在收到实时推送时应执行的逻辑，可直接参考。
 */
class RealtimeUpdateTest {

    private val period = TestDataFactory.PERIOD_MILLIS

    /**
     * 模拟接入方收到一个实时 tick 时的处理逻辑。
     *
     * @param tickTime 行情时间戳（毫秒）
     * @param price    最新成交价
     * @param volume   本次成交量增量
     */
    private fun onTick(adapter: KLineChartAdapter, tickTime: Long, price: Float, volume: Float) {
        val barTime = tickTime - tickTime % period   // 对齐到所属周期的起始时间
        val last = adapter.getLastData()
        if (last != null && last.barTime == barTime) {
            // 同一根：原地更新 OHLCV
            last.Close = price
            last.High = maxOf(last.High, price)
            last.Low = minOf(last.Low, price)
            last.Volume += volume
            DataHelper.calculate(adapter.getDatas())   // 重算指标
            adapter.updateLast(last)                    // 刷新最后一根（条数不变，视图不跳动）
        } else {
            // 新的一根
            val bar = TestDataFactory.bar(barTime, price, price, price, price, volume)
            adapter.addFooterData(listOf(bar))
            DataHelper.calculate(adapter.getDatas())
            adapter.notifyDataSetChanged()
        }
    }

    @Test
    fun sameBar_updatesInPlace() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(mutableListOf(TestDataFactory.bar(0L, 10f, 10f, 10f, 10f, 0f)))

        // 同一分钟内（time ∈ [0, 60000)）多次 tick
        onTick(adapter, 1_000L, 11f, 5f)
        onTick(adapter, 30_000L, 9f, 5f)
        onTick(adapter, 59_000L, 12f, 5f)

        assertEquals("仍应只有一根", 1, adapter.getCount())
        val bar = adapter.getLastData()!!
        assertEquals(12f, bar.Close, 1e-4f)   // 最新价
        assertEquals(12f, bar.High, 1e-4f)    // max(10,11,9,12)
        assertEquals(9f, bar.Low, 1e-4f)      // min(10,11,9,12)
        assertEquals(15f, bar.Volume, 1e-4f)  // 0+5+5+5
    }

    @Test
    fun crossPeriod_appendsNewBar() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(mutableListOf(TestDataFactory.bar(0L, 10f, 10f, 10f, 10f, 0f)))

        // 跨入下一分钟（time = 60000 → barTime = 60000）
        onTick(adapter, 60_000L, 11f, 100f)

        assertEquals(2, adapter.getCount())
        assertEquals(60_000L, adapter.getLastData()!!.barTime)
        assertEquals(11f, adapter.getLastData()!!.Close, 1e-4f)
    }

    @Test
    fun highFrequency_60TicksInOneMinute_staysOneBar() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(mutableListOf(TestDataFactory.bar(0L, 100f, 100f, 100f, 100f, 0f)))

        // 一分钟内 60 次 tick（每秒一次）
        repeat(60) { i ->
            onTick(adapter, i * 1_000L, 100f + (i % 10), 10f)
        }

        assertEquals("60 次同周期 tick 后仍是一根", 1, adapter.getCount())
        assertEquals(600f, adapter.getLastData()!!.Volume, 1e-4f) // 60 * 10
    }

    @Test
    fun threeMinutes_producesThreeBars() {
        val adapter = KLineChartAdapter()
        // 第 0、1、2 分钟各推送若干 tick
        for (minute in 0L until 3L) {
            repeat(10) { i ->
                onTick(adapter, minute * period + i * 1_000L, 100f + minute, 1f)
            }
        }
        assertEquals(3, adapter.getCount())
        val datas = adapter.getDatas()
        assertEquals(0L, datas[0].barTime)
        assertEquals(period, datas[1].barTime)
        assertEquals(2 * period, datas[2].barTime)
    }
}