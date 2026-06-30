package com.common.kline.formatter

import com.common.kline.base.IValueFormatter

/**
 * Value格式化类
 * Created by tifezh on 2016/6/21.
 */
class ValueFormatter : IValueFormatter {
    override fun format(value: Float): String = String.format("%.2f", value)
}
