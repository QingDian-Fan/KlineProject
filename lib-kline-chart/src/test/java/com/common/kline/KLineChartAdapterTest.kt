package com.common.kline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [KLineChartAdapter] 数据操作的单元测试。
 * （依赖 android.database.DataSetObservable，已在 build.gradle 开启 unitTests.returnDefaultValues）
 */
class KLineChartAdapterTest {

    @Test
    fun addFooterData_appends() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(TestDataFactory.generate(5))
        assertEquals(5, adapter.getCount())
    }

    @Test
    fun addHeaderData_prepends() {
        val adapter = KLineChartAdapter()
        val older = TestDataFactory.generate(3, startTime = 0L)
        val newer = TestDataFactory.generate(2, startTime = 3 * TestDataFactory.PERIOD_MILLIS)
        adapter.addFooterData(newer)
        adapter.addHeaderData(older)
        assertEquals(5, adapter.getCount())
        // 头部第一条应为较旧数据的第一条
        assertSame(older.first(), adapter.getItem(0))
    }

    @Test
    fun getLastData_returnsLastOrNull() {
        val adapter = KLineChartAdapter()
        assertNull(adapter.getLastData())
        val list = TestDataFactory.generate(4)
        adapter.addFooterData(list)
        assertSame(list.last(), adapter.getLastData())
    }

    @Test
    fun updateLast_replacesWithoutChangingCount() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(TestDataFactory.generate(3))
        val countBefore = adapter.getCount()
        val replaced = TestDataFactory.bar(999L, 1f, 2f, 0.5f, 1.5f, 100f)
        adapter.updateLast(replaced)
        assertEquals(countBefore, adapter.getCount())
        assertSame(replaced, adapter.getLastData())
    }

    @Test
    fun updateLast_onEmpty_appends() {
        val adapter = KLineChartAdapter()
        adapter.updateLast(TestDataFactory.bar(1L, 1f, 1f, 1f, 1f, 1f))
        assertEquals(1, adapter.getCount())
    }

    @Test
    fun clearData_empties() {
        val adapter = KLineChartAdapter()
        adapter.addFooterData(TestDataFactory.generate(3))
        adapter.clearData()
        assertEquals(0, adapter.getCount())
    }
}