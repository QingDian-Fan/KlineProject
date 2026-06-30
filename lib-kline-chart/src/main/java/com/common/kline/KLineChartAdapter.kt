package com.common.kline

/**
 * 数据适配器
 * Created by tifezh on 2016/6/18.
 */
class KLineChartAdapter : BaseKLineChartAdapter() {

    private val datas = mutableListOf<KLineEntity>()

    override fun getCount(): Int = datas.size

    override fun getItem(position: Int): Any = datas[position]

    override fun getDate(position: Int): String = datas[position].Date ?: ""

    /** 获取最后一条数据，没有数据时返回 null（用于实时行情更新最后一根） */
    fun getLastData(): KLineEntity? = datas.lastOrNull()

    /**
     * 只读数据列表，便于对完整数据集调用 [DataHelper.calculate] 重算指标。
     * 不要直接增删该列表，请使用 [addFooterData] / [addHeaderData] / [updateLast] 等方法。
     */
    fun getDatas(): List<KLineEntity> = datas

    /**
     * 更新最后一根数据并刷新（用于实时行情：同一周期内的多次 tick）。
     * 列表为空时等价于追加一条。该操作不改变数据条数，因此不会引起图表横向跳动。
     */
    fun updateLast(data: KLineEntity) {
        if (datas.isEmpty()) {
            datas.add(data)
        } else {
            datas[datas.size - 1] = data
        }
        notifyDataSetChanged()
    }

    /** 向头部添加数据 */
    fun addHeaderData(data: List<KLineEntity>?) {
        if (!data.isNullOrEmpty()) {
            datas.addAll(0, data)
        }
    }

    /** 向尾部添加数据 */
    fun addFooterData(data: List<KLineEntity>?) {
        if (!data.isNullOrEmpty()) {
            datas.addAll(data)
        }
    }

    /**
     * 改变某个点的值
     *
     * @param position 索引值
     */
    fun changeItem(position: Int, data: KLineEntity) {
        datas[position] = data
        notifyDataSetChanged()
    }

    /** 数据清除 */
    fun clearData() {
        datas.clear()
        notifyDataSetChanged()
    }
}
