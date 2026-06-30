package com.common.kline.draw

import android.graphics.Canvas
import android.graphics.Paint
import com.common.kline.BaseKLineChartView
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.IWR
import com.common.kline.formatter.ValueFormatter

/**
 * WR实现类
 * Created by tifezh on 2016/6/19.
 */
class WRDraw(view: BaseKLineChartView) : IChartDraw<IWR> {

    private val mRPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun drawTranslated(
        lastPoint: IWR?,
        curPoint: IWR,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        if (last.r != -10f) {
            view.drawChildLine(canvas, mRPaint, lastX, last.r, curX, curPoint.r)
        }
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IWR
        var px = x
        if (point.r != -10f) {
            val text = "WR(14):"
            canvas.drawText(text, px, y, view.textPaint)
            px += view.textPaint.measureText(text)
            canvas.drawText(view.formatValue(point.r) + " ", px, y, mRPaint)
        }
    }

    override fun getMaxValue(point: IWR): Float = point.r

    override fun getMinValue(point: IWR): Float = point.r

    override fun getValueFormatter(): IValueFormatter = ValueFormatter()

    /** 设置%R颜色 */
    fun setRColor(color: Int) {
        mRPaint.color = color
    }

    /** 设置曲线宽度 */
    fun setLineWidth(width: Float) {
        mRPaint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        mRPaint.textSize = textSize
    }
}
