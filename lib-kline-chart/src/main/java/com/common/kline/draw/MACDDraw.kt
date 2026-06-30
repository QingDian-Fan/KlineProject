package com.common.kline.draw

import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.common.kline.BaseKLineChartView
import com.common.kline.R
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.IMACD
import com.common.kline.formatter.ValueFormatter

/**
 * macd实现类
 * Created by tifezh on 2016/6/19.
 */
class MACDDraw(view: BaseKLineChartView) : IChartDraw<IMACD> {

    private val mRedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDIFPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDEAPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mMACDPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** macd 中柱子的宽度 */
    private var mMACDWidth = 0f

    init {
        val context = view.context
        mRedPaint.color = ContextCompat.getColor(context, R.color.chart_red)
        mGreenPaint.color = ContextCompat.getColor(context, R.color.chart_green)
    }

    override fun drawTranslated(
        lastPoint: IMACD?,
        curPoint: IMACD,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        drawMACD(canvas, view, curX, curPoint.macd)
        view.drawChildLine(canvas, mDIFPaint, lastX, last.dea, curX, curPoint.dea)
        view.drawChildLine(canvas, mDEAPaint, lastX, last.dif, curX, curPoint.dif)
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IMACD
        var px = x
        var text = "MACD(12,26,9)  "
        canvas.drawText(text, px, y, view.textPaint)
        px += view.textPaint.measureText(text)
        text = "MACD:" + view.formatValue(point.macd) + "  "
        canvas.drawText(text, px, y, mMACDPaint)
        px += mMACDPaint.measureText(text)
        text = "DIF:" + view.formatValue(point.dif) + "  "
        canvas.drawText(text, px, y, mDEAPaint)
        px += mDIFPaint.measureText(text)
        text = "DEA:" + view.formatValue(point.dea)
        canvas.drawText(text, px, y, mDIFPaint)
    }

    override fun getMaxValue(point: IMACD): Float =
        maxOf(point.macd, maxOf(point.dea, point.dif))

    override fun getMinValue(point: IMACD): Float =
        minOf(point.macd, minOf(point.dea, point.dif))

    override fun getValueFormatter(): IValueFormatter = ValueFormatter()

    /** 画macd */
    private fun drawMACD(canvas: Canvas, view: BaseKLineChartView, x: Float, macd: Float) {
        val macdY = view.getChildY(macd)
        val r = mMACDWidth / 2
        val zeroY = view.getChildY(0f)
        if (macd > 0) {
            //               left   top    right  bottom
            canvas.drawRect(x - r, macdY, x + r, zeroY, mRedPaint)
        } else {
            canvas.drawRect(x - r, zeroY, x + r, macdY, mGreenPaint)
        }
    }

    /** 设置DIF颜色 */
    fun setDIFColor(color: Int) {
        mDIFPaint.color = color
    }

    /** 设置DEA颜色 */
    fun setDEAColor(color: Int) {
        mDEAPaint.color = color
    }

    /** 设置MACD颜色 */
    fun setMACDColor(color: Int) {
        mMACDPaint.color = color
    }

    /** 设置MACD的宽度 */
    fun setMACDWidth(MACDWidth: Float) {
        mMACDWidth = MACDWidth
    }

    /** 设置曲线宽度 */
    fun setLineWidth(width: Float) {
        mDEAPaint.strokeWidth = width
        mDIFPaint.strokeWidth = width
        mMACDPaint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        mDEAPaint.textSize = textSize
        mDIFPaint.textSize = textSize
        mMACDPaint.textSize = textSize
    }
}
