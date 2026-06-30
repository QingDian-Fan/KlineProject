package com.common.kline.entity

/**
 * KDJ指标(随机指标)接口
 * Created on 2018/8/20 16:08
 */
interface IKDJ {

    /** K值 */
    val k: Float

    /** D值 */
    val d: Float

    /** J值 */
    val j: Float
}
