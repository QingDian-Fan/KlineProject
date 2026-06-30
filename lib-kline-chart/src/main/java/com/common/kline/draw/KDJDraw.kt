package com.common.kline.draw

import android.graphics.Canvas
import android.graphics.Paint
import com.common.kline.BaseKLineChartView
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.IKDJ
import com.common.kline.formatter.ValueFormatter

/**
 * KDJ实现类
 * Created by tifezh on 2016/6/19.
 */
class KDJDraw(view: BaseKLineChartView) : IChartDraw<IKDJ> {

    private val mKPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mJPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun drawTranslated(
        lastPoint: IKDJ?,
        curPoint: IKDJ,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        if (last.k != 0f) {
            view.drawChildLine(canvas, mKPaint, lastX, last.k, curX, curPoint.k)
        }
        if (last.d != 0f) {
            view.drawChildLine(canvas, mDPaint, lastX, last.d, curX, curPoint.d)
        }
        if (last.j != 0f) {
            view.drawChildLine(canvas, mJPaint, lastX, last.j, curX, curPoint.j)
        }
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IKDJ
        var px = x
        if (point.k != 0f) {
            var text = "KDJ(14,1,3)  "
            canvas.drawText(text, px, y, view.textPaint)
            px += view.textPaint.measureText(text)
            text = "K:" + view.formatValue(point.k) + " "
            canvas.drawText(text, px, y, mKPaint)
            px += mKPaint.measureText(text)
            if (point.d != 0f) {
                text = "D:" + view.formatValue(point.d) + " "
                canvas.drawText(text, px, y, mDPaint)
                px += mDPaint.measureText(text)
                text = "J:" + view.formatValue(point.j) + " "
                canvas.drawText(text, px, y, mJPaint)
            }
        }
    }

    override fun getMaxValue(point: IKDJ): Float = maxOf(point.k, maxOf(point.d, point.j))

    override fun getMinValue(point: IKDJ): Float = minOf(point.k, minOf(point.d, point.j))

    override fun getValueFormatter(): IValueFormatter = ValueFormatter()

    /** 设置K颜色 */
    fun setKColor(color: Int) {
        mKPaint.color = color
    }

    /** 设置D颜色 */
    fun setDColor(color: Int) {
        mDPaint.color = color
    }

    /** 设置J颜色 */
    fun setJColor(color: Int) {
        mJPaint.color = color
    }

    /** 设置曲线宽度 */
    fun setLineWidth(width: Float) {
        mKPaint.strokeWidth = width
        mDPaint.strokeWidth = width
        mJPaint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        mKPaint.textSize = textSize
        mDPaint.textSize = textSize
        mJPaint.textSize = textSize
    }
}
