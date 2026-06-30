package com.common.kline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 指标计算 [DataHelper] 的单元测试（纯 JVM 逻辑）。
 */
class DataHelperTest {

    private fun closeSeq(vararg closes: Float): MutableList<KLineEntity> {
        val list = ArrayList<KLineEntity>(closes.size)
        for ((i, c) in closes.withIndex()) {
            list.add(TestDataFactory.bar(i * 60_000L, c, c, c, c, 100f))
        }
        return list
    }

    @Test
    fun ma5_isCorrect() {
        val list = closeSeq(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)
        DataHelper.calculate(list)
        // 不足 5 根时为 0
        assertEquals(0f, list[3].MA5Price, 1e-4f)
        // index4: (1+2+3+4+5)/5 = 3
        assertEquals(3f, list[4].MA5Price, 1e-4f)
        // index9: (6+7+8+9+10)/5 = 8
        assertEquals(8f, list[9].MA5Price, 1e-4f)
    }

    @Test
    fun ma10_isCorrect() {
        val closes = FloatArray(12) { (it + 1).toFloat() } // 1..12
        val list = closeSeq(*closes)
        DataHelper.calculate(list)
        // index9: 收盘价 1..10 的均值 = 5.5
        assertEquals(5.5f, list[9].MA10Price, 1e-4f)
        // index11: 收盘价 3..12 的均值 = 7.5
        assertEquals(7.5f, list[11].MA10Price, 1e-4f)
    }

    @Test
    fun volumeMa5_isCorrect() {
        val list = ArrayList<KLineEntity>()
        for (i in 1..6) {
            list.add(TestDataFactory.bar(i * 60_000L, 1f, 1f, 1f, 1f, i * 100f))
        }
        DataHelper.calculate(list)
        // index4: (100+200+300+400+500)/5 = 300
        assertEquals(300f, list[4].MA5Volume, 1e-4f)
    }

    @Test
    fun generatedData_indicatorsAreFilled() {
        val list = TestDataFactory.generate(100)
        DataHelper.calculate(list)
        // MA60 在第 60 根之后有值
        assertTrue("MA60 应已填充", list[60].MA60Price > 0f)
        // 各指标均为有效数值（非 NaN）
        val p = list[80]
        assertFalse(p.macd.isNaN())
        assertFalse(p.rsi.isNaN())
        assertFalse(p.k.isNaN())
        assertFalse(p.up.isNaN())
    }
}