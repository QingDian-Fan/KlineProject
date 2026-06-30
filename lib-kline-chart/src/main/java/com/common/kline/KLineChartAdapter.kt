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
