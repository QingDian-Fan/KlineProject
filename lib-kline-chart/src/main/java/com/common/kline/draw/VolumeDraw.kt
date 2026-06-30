package com.common.kline.draw

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.common.kline.BaseKLineChartView
import com.common.kline.R
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.IVolume
import com.common.kline.formatter.BigValueFormatter
import com.common.kline.utils.ViewUtil

/**
 * Created by hjm on 2017/11/14 17:49.
 */
class VolumeDraw(view: BaseKLineChartView) : IChartDraw<IVolume> {

    private val mRedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ma5Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ma10Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillarWidth: Int

    init {
        val context = view.context
        mRedPaint.color = ContextCompat.getColor(context, R.color.chart_red)
        mGreenPaint.color = ContextCompat.getColor(context, R.color.chart_green)
        pillarWidth = ViewUtil.Dp2Px(context, 4f)
    }

    override fun drawTranslated(
        lastPoint: IVolume?,
        curPoint: IVolume,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        drawHistogram(canvas, curPoint, last, curX, view, position)
        if (last.MA5Volume != 0f) {
            view.drawVolLine(canvas, ma5Paint, lastX, last.MA5Volume, curX, curPoint.MA5Volume)
        }
        if (last.MA10Volume != 0f) {
            view.drawVolLine(canvas, ma10Paint, lastX, last.MA10Volume, curX, curPoint.MA10Volume)
        }
    }

    private fun drawHistogram(
        canvas: Canvas,
        curPoint: IVolume,
        lastPoint: IVolume,
        curX: Float,
        view: BaseKLineChartView,
        position: Int
    ) {
        val r = (pillarWidth / 2).toFloat()
        val top = view.getVolY(curPoint.volume)
        val bottom = view.volRect.bottom.toFloat()
        if (curPoint.closePrice >= curPoint.openPrice) { // 涨
            canvas.drawRect(curX - r, top, curX + r, bottom, mRedPaint)
        } else {
            canvas.drawRect(curX - r, top, curX + r, bottom, mGreenPaint)
        }
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IVolume
        var px = x
        var text = "VOL:" + getValueFormatter().format(point.volume) + "  "
        canvas.drawText(text, px, y, view.textPaint)
        px += view.textPaint.measureText(text)
        text = "MA5:" + getValueFormatter().format(point.MA5Volume) + "  "
        canvas.drawText(text, px, y, ma5Paint)
        px += ma5Paint.measureText(text)
        text = "MA10:" + getValueFormatter().format(point.MA10Volume)
        canvas.drawText(text, px, y, ma10Paint)
    }

    override fun getMaxValue(point: IVolume): Float =
        maxOf(point.volume, maxOf(point.MA5Volume, point.MA10Volume))

    override fun getMinValue(point: IVolume): Float =
        minOf(point.volume, minOf(point.MA5Volume, point.MA10Volume))

    override fun getValueFormatter(): IValueFormatter = BigValueFormatter()

    /** 设置 MA5 线的颜色 */
    fun setMa5Color(color: Int) {
        ma5Paint.color = color
    }

    /** 设置 MA10 线的颜色 */
    fun setMa10Color(color: Int) {
        ma10Paint.color = color
    }

    fun setLineWidth(width: Float) {
        ma5Paint.strokeWidth = width
        ma10Paint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        ma5Paint.textSize = textSize
        ma10Paint.textSize = textSize
    }
}
