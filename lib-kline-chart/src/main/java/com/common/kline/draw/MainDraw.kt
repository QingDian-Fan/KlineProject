package com.common.kline.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.common.kline.BaseKLineChartView
import com.common.kline.KLineChartView
import com.common.kline.R
import com.common.kline.base.IChartDraw
import com.common.kline.base.IValueFormatter
import com.common.kline.entity.ICandle
import com.common.kline.entity.IKLine
import com.common.kline.formatter.ValueFormatter
import com.common.kline.utils.ViewUtil

/**
 * 主图的实现类
 * Created by tifezh on 2016/6/14.
 */
class MainDraw(view: BaseKLineChartView) : IChartDraw<ICandle> {

    private var mCandleWidth = 0f
    private var mCandleLineWidth = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ma5Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ma10Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ma30Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mSelectorTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectorBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mContext: Context = view.context

    private var mCandleSolid = true

    // 是否分时
    var isLine = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    kChartView.setCandleWidth(kChartView.dp2px(7f).toFloat())
                } else {
                    kChartView.setCandleWidth(kChartView.dp2px(6f).toFloat())
                }
            }
        }

    var status = Status.MA
    private val kChartView: KLineChartView = view as KLineChartView

    init {
        mRedPaint.color = ContextCompat.getColor(mContext, R.color.chart_red)
        mGreenPaint.color = ContextCompat.getColor(mContext, R.color.chart_green)
        mLinePaint.color = ContextCompat.getColor(mContext, R.color.chart_line)
        paint.color = ContextCompat.getColor(mContext, R.color.chart_line_background)
    }

    override fun drawTranslated(
        lastPoint: ICandle?,
        curPoint: ICandle,
        lastX: Float,
        curX: Float,
        canvas: Canvas,
        view: BaseKLineChartView,
        position: Int
    ) {
        val last = lastPoint!!
        if (isLine) {
            view.drawMainLine(canvas, mLinePaint, lastX, last.closePrice, curX, curPoint.closePrice)
            view.drawMainMinuteLine(canvas, paint, lastX, last.closePrice, curX, curPoint.closePrice)
            if (status == Status.MA) {
                // 画ma60
                if (last.MA60Price != 0f) {
                    view.drawMainLine(canvas, ma10Paint, lastX, last.MA60Price, curX, curPoint.MA60Price)
                }
            } else if (status == Status.BOLL) {
                // 画boll
                if (last.mb != 0f) {
                    view.drawMainLine(canvas, ma10Paint, lastX, last.mb, curX, curPoint.mb)
                }
            }
        } else {
            drawCandle(view, canvas, curX, curPoint.highPrice, curPoint.lowPrice, curPoint.openPrice, curPoint.closePrice)
            if (status == Status.MA) {
                // 画ma5
                if (last.MA5Price != 0f) {
                    view.drawMainLine(canvas, ma5Paint, lastX, last.MA5Price, curX, curPoint.MA5Price)
                }
                // 画ma10
                if (last.MA10Price != 0f) {
                    view.drawMainLine(canvas, ma10Paint, lastX, last.MA10Price, curX, curPoint.MA10Price)
                }
                // 画ma30
                if (last.MA30Price != 0f) {
                    view.drawMainLine(canvas, ma30Paint, lastX, last.MA30Price, curX, curPoint.MA30Price)
                }
            } else if (status == Status.BOLL) {
                // 画boll
                if (last.up != 0f) {
                    view.drawMainLine(canvas, ma5Paint, lastX, last.up, curX, curPoint.up)
                }
                if (last.mb != 0f) {
                    view.drawMainLine(canvas, ma10Paint, lastX, last.mb, curX, curPoint.mb)
                }
                if (last.dn != 0f) {
                    view.drawMainLine(canvas, ma30Paint, lastX, last.dn, curX, curPoint.dn)
                }
            }
        }
    }

    override fun drawText(canvas: Canvas, view: BaseKLineChartView, position: Int, x: Float, y: Float) {
        val point = view.getItem(position) as IKLine
        var px = x
        val py = y - 5
        if (isLine) {
            if (status == Status.MA) {
                if (point.MA60Price != 0f) {
                    val text = "MA60:" + view.formatValue(point.MA60Price) + "  "
                    canvas.drawText(text, px, py, ma10Paint)
                }
            } else if (status == Status.BOLL) {
                if (point.mb != 0f) {
                    val text = "BOLL:" + view.formatValue(point.mb) + "  "
                    canvas.drawText(text, px, py, ma10Paint)
                }
            }
        } else {
            if (status == Status.MA) {
                var text: String
                if (point.MA5Price != 0f) {
                    text = "MA5:" + view.formatValue(point.MA5Price) + "  "
                    canvas.drawText(text, px, py, ma5Paint)
                    px += ma5Paint.measureText(text)
                }
                if (point.MA10Price != 0f) {
                    text = "MA10:" + view.formatValue(point.MA10Price) + "  "
                    canvas.drawText(text, px, py, ma10Paint)
                    px += ma10Paint.measureText(text)
                }
                if (point.MA20Price != 0f) {
                    text = "MA30:" + view.formatValue(point.MA30Price)
                    canvas.drawText(text, px, py, ma30Paint)
                }
            } else if (status == Status.BOLL) {
                if (point.mb != 0f) {
                    var text = "BOLL:" + view.formatValue(point.mb) + "  "
                    canvas.drawText(text, px, py, ma10Paint)
                    px += ma5Paint.measureText(text)
                    text = "UB:" + view.formatValue(point.up) + "  "
                    canvas.drawText(text, px, py, ma5Paint)
                    px += ma10Paint.measureText(text)
                    text = "LB:" + view.formatValue(point.dn)
                    canvas.drawText(text, px, py, ma30Paint)
                }
            }
        }
        if (view.isLongPress) {
            drawSelector(view, canvas)
        }
    }

    override fun getMaxValue(point: ICandle): Float {
        return if (status == Status.BOLL) {
            when {
                point.up.isNaN() -> if (point.mb == 0f) point.highPrice else point.mb
                point.up == 0f -> point.highPrice
                else -> point.up
            }
        } else {
            maxOf(point.highPrice, point.MA30Price)
        }
    }

    override fun getMinValue(point: ICandle): Float {
        return if (status == Status.BOLL) {
            if (point.dn == 0f) point.lowPrice else point.dn
        } else {
            if (point.MA30Price == 0f) point.lowPrice else minOf(point.MA30Price, point.lowPrice)
        }
    }

    override fun getValueFormatter(): IValueFormatter = ValueFormatter()

    /**
     * 画Candle
     *
     * @param x     x轴坐标
     * @param high  最高价
     * @param low   最低价
     * @param open  开盘价
     * @param close 收盘价
     */
    private fun drawCandle(
        view: BaseKLineChartView,
        canvas: Canvas,
        x: Float,
        high: Float,
        low: Float,
        open: Float,
        close: Float
    ) {
        val highY = view.getMainY(high)
        val lowY = view.getMainY(low)
        val openY = view.getMainY(open)
        val closeY = view.getMainY(close)
        val r = mCandleWidth / 2
        val lineR = mCandleLineWidth / 2
        if (openY > closeY) {
            // 实心
            if (mCandleSolid) {
                canvas.drawRect(x - r, closeY, x + r, openY, mRedPaint)
                canvas.drawRect(x - lineR, highY, x + lineR, lowY, mRedPaint)
            } else {
                mRedPaint.strokeWidth = mCandleLineWidth
                canvas.drawLine(x, highY, x, closeY, mRedPaint)
                canvas.drawLine(x, openY, x, lowY, mRedPaint)
                canvas.drawLine(x - r + lineR, openY, x - r + lineR, closeY, mRedPaint)
                canvas.drawLine(x + r - lineR, openY, x + r - lineR, closeY, mRedPaint)
                mRedPaint.strokeWidth = mCandleLineWidth * view.scaleX
                canvas.drawLine(x - r, openY, x + r, openY, mRedPaint)
                canvas.drawLine(x - r, closeY, x + r, closeY, mRedPaint)
            }
        } else if (openY < closeY) {
            canvas.drawRect(x - r, openY, x + r, closeY, mGreenPaint)
            canvas.drawRect(x - lineR, highY, x + lineR, lowY, mGreenPaint)
        } else {
            canvas.drawRect(x - r, openY, x + r, closeY + 1, mRedPaint)
            canvas.drawRect(x - lineR, highY, x + lineR, lowY, mRedPaint)
        }
    }

    /**
     * draw选择器
     */
    private fun drawSelector(view: BaseKLineChartView, canvas: Canvas) {
        val metrics = mSelectorTextPaint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent

        val index = view.selectedIndex
        val padding = ViewUtil.Dp2Px(mContext, 5f).toFloat()
        val margin = ViewUtil.Dp2Px(mContext, 5f).toFloat()
        var width = 0f
        val left: Float
        val top = margin + view.topPadding
        val height = padding * 8 + textHeight * 5

        val point = view.getItem(index) as ICandle
        val strings = ArrayList<String>()
        strings.add(view.adapter?.getDate(index) ?: "")
        strings.add("高:" + point.highPrice)
        strings.add("低:" + point.lowPrice)
        strings.add("开:" + point.openPrice)
        strings.add("收:" + point.closePrice)

        for (s in strings) {
            width = maxOf(width, mSelectorTextPaint.measureText(s))
        }
        width += padding * 2

        val x = view.translateXtoX(view.getX(index))
        left = if (x > view.chartWidth / 2) {
            margin
        } else {
            view.chartWidth - width - margin
        }

        val r = RectF(left, top, left + width, top + height)
        canvas.drawRoundRect(r, padding, padding, mSelectorBackgroundPaint)
        var y = top + padding * 2 + (textHeight - metrics.bottom - metrics.top) / 2

        for (s in strings) {
            canvas.drawText(s, left + padding, y, mSelectorTextPaint)
            y += textHeight + padding
        }
    }

    /** 设置蜡烛宽度 */
    fun setCandleWidth(candleWidth: Float) {
        mCandleWidth = candleWidth
    }

    /** 设置蜡烛线宽度 */
    fun setCandleLineWidth(candleLineWidth: Float) {
        mCandleLineWidth = candleLineWidth
    }

    /** 设置ma5颜色 */
    fun setMa5Color(color: Int) {
        ma5Paint.color = color
    }

    /** 设置ma10颜色 */
    fun setMa10Color(color: Int) {
        ma10Paint.color = color
    }

    /** 设置ma30颜色 */
    fun setMa30Color(color: Int) {
        ma30Paint.color = color
    }

    /** 设置选择器文字颜色 */
    fun setSelectorTextColor(color: Int) {
        mSelectorTextPaint.color = color
    }

    /** 设置选择器文字大小 */
    fun setSelectorTextSize(textSize: Float) {
        mSelectorTextPaint.textSize = textSize
    }

    /** 设置选择器背景 */
    fun setSelectorBackgroundColor(color: Int) {
        mSelectorBackgroundPaint.color = color
    }

    /** 设置曲线宽度 */
    fun setLineWidth(width: Float) {
        ma30Paint.strokeWidth = width
        ma10Paint.strokeWidth = width
        ma5Paint.strokeWidth = width
        mLinePaint.strokeWidth = width
    }

    /** 设置文字大小 */
    fun setTextSize(textSize: Float) {
        ma30Paint.textSize = textSize
        ma10Paint.textSize = textSize
        ma5Paint.textSize = textSize
    }

    /** 蜡烛是否实心 */
    fun setCandleSolid(candleSolid: Boolean) {
        mCandleSolid = candleSolid
    }
}
