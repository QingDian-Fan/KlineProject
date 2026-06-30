package com.common.kline

import com.common.kline.entity.IKLine

/**
 * K线实体
 * Created by tifezh on 2016/5/16.
 *
 * 注意：OHLCV 字段名 (Date/Open/High/Low/Close/Volume) 必须保持不变，
 * demo 通过 Gson 按字段名反序列化 assets/ibm.json。
 */
class KLineEntity : IKLine {

    var Date: String? = null
    var Open = 0f
    var High = 0f
    var Low = 0f
    var Close = 0f

    // Volume 用 @JvmField 暴露为字段（供 Gson 映射），避免与接口 volume 生成的 getVolume() 冲突
    @JvmField
    var Volume = 0f

    override var MA5Price = 0f
    override var MA10Price = 0f
    override var MA20Price = 0f
    override var MA30Price = 0f
    override var MA60Price = 0f

    override var dea = 0f
    override var dif = 0f
    override var macd = 0f

    override var k = 0f
    override var d = 0f
    override var j = 0f

    override var r = 0f
    override var rsi = 0f

    override var up = 0f
    override var mb = 0f
    override var dn = 0f

    override var MA5Volume = 0f
    override var MA10Volume = 0f

    override val openPrice: Float get() = Open
    override val highPrice: Float get() = High
    override val lowPrice: Float get() = Low
    override val closePrice: Float get() = Close
    override val volume: Float get() = Volume
}
