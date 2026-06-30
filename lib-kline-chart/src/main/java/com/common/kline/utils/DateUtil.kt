package com.common.kline.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 时间工具类
 * Created by tifezh on 2016/4/27.
 */
object DateUtil {
    @JvmField
    val longTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    @JvmField
    val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    @JvmField
    val DateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
}
