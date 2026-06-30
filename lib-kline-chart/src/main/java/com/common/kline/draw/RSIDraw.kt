package com.common.kline.draw

import android.graphics.Canvas
import android.graphics.Paint
import com.common.kline.BaseKLineChartView
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.IRSI
import com.common.kline.formatter.ValueFormatter

/**
 * RSI实现类
 * Created by tifezh on 2016/6/19.
 */
class RSIDraw(view: BaseKLineChartView) : IChartDraw<IRSI> {

    private val mRSI1Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRSI2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRSI3Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun drawTranslated(
        lastPoint: IRSI?,
        curPoint: IRSI,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        if (last.rsi != 0f) {
            view.drawChildLine(canvas, mRSI1Paint, lastX, last.rsi, curX, curPoint.rsi)
        }
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IRSI
        var px = x
        if (point.rsi != 0f) {
            val text = "RSI(14)  "
            canvas.drawText(text, px, y, view.textPaint)
            px += view.textPaint.measureText(text)
            canvas.drawText(view.formatValue(point.rsi), px, y, mRSI1Paint)
        }
    }

    override fun getMaxValue(point: IRSI): Float = point.rsi

    override fun getMinValue(point: IRSI): Float = point.rsi

    override fun getValueFormatter(): IValueFormatter = ValueFormatter()

    fun setRSI1Color(color: Int) {
        mRSI1Paint.color = color
    }

    fun setRSI2Color(color: Int) {
        mRSI2Paint.color = color
    }

    fun setRSI3Color(color: Int) {
        mRSI3Paint.color = color
    }

    /** 设置曲线宽度 */
    fun setLineWidth(width: Float) {
        mRSI1Paint.strokeWidth = width
        mRSI2Paint.strokeWidth = width
        mRSI3Paint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        mRSI2Paint.textSize = textSize
        mRSI3Paint.textSize = textSize
        mRSI1Paint.textSize = textSize
    }
}
