package com.common.kline

import android.animation.ValueAnimator
import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.view.GestureDetectorCompat
import com.common.kline.base.IAdapter
import com.common.kline.base.IChartDraw
import com.common.kline.base.IDateTimeFormatter
import com.common.kline.base.IValueFormatter
import com.common.kline.draw.MainDraw
import com.common.kline.draw.Status
import com.common.kline.entity.IKLine
import com.common.kline.formatter.TimeFormatter
import com.common.kline.formatter.ValueFormatter
import com.common.kline.utils.ViewUtil
import java.util.Date
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * k线图
 * Created by tian on 2016/5/3.
 */
abstract class BaseKLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollAndScaleView(context, attrs, defStyleAttr) {

    private var mChildDrawPosition = -1
    private var mTranslateX = Float.MIN_VALUE
    private var mWidth = 0
    private var mTopPadding = 0
    private var mChildPadding = 0
    private var mBottomPadding = 0
    private var mMainScaleY = 1f
    private var mVolScaleY = 1f
    private var mChildScaleY = 1f
    private var mDataLen = 0f
    private var mMainMaxValue = Float.MAX_VALUE
    private var mMainMinValue = Float.MIN_VALUE
    private var mMainHighMaxValue = 0f
    private var mMainLowMinValue = 0f
    private var mMainMaxIndex = 0
    private var mMainMinIndex = 0
    private var mVolMaxValue = Float.MAX_VALUE
    private var mVolMinValue = Float.MIN_VALUE
    private var mChildMaxValue = Float.MAX_VALUE
    private var mChildMinValue = Float.MIN_VALUE
    private var mStartIndex = 0
    private var mStopIndex = 0
    private var mPointWidth = 6f
    private var mGridRows = 4
    private var mGridColumns = 4

    private val mGridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mMaxMinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectedXLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectedYLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectPointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mSelectorFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 当前选中的索引 */
    var selectedIndex = 0
        private set

    private var mMainDraw: IChartDraw<Any>? = null
    private var mainDraw: MainDraw? = null
    private var mVolDraw: IChartDraw<Any>? = null

    private var mAdapter: IAdapter? = null

    private var isWR = false
    private var isShowChild = false

    private val mDataSetObserver: DataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            mItemCount = mAdapter?.getCount() ?: 0
            notifyChanged()
        }

        override fun onInvalidated() {
            mItemCount = mAdapter?.getCount() ?: 0
            notifyChanged()
        }
    }

    // 当前点的个数
    private var mItemCount = 0
    private var mChildDraw: IChartDraw<Any>? = null
    private val mChildDraws = mutableListOf<IChartDraw<Any>>()

    private var mValueFormatter: IValueFormatter? = null
    private var mDateTimeFormatter: IDateTimeFormatter? = null

    private val mAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
    private var mAnimationDuration = 500L
    private var mOverScrollRange = 0f

    private var mOnSelectedChangedListener: OnSelectedChangedListener? = null

    private var mMainRect: Rect? = null
    private var mVolRect: Rect? = null
    private var mChildRect: Rect? = null

    private var mLineWidth = 0f

    private var displayHeight = 0

    init {
        setWillNotDraw(false)
        mDetector = GestureDetectorCompat(context, this)
        mScaleDetector = ScaleGestureDetector(context, this)
        mTopPadding = resources.getDimension(R.dimen.chart_top_padding).toInt()
        mChildPadding = resources.getDimension(R.dimen.child_top_padding).toInt()
        mBottomPadding = resources.getDimension(R.dimen.chart_bottom_padding).toInt()

        mAnimator.duration = mAnimationDuration
        mAnimator.addUpdateListener { invalidate() }

        mSelectorFramePaint.strokeWidth = ViewUtil.Dp2Px(context, 0.6f).toFloat()
        mSelectorFramePaint.style = Paint.Style.STROKE
        mSelectorFramePaint.color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        displayHeight = h - mTopPadding - mBottomPadding
        initRect()
        setTranslateXFromScrollX(mScrollX)
    }

    private fun initRect() {
        if (isShowChild) {
            val mainHeight = (displayHeight * 0.6f).toInt()
            val volHeight = (displayHeight * 0.2f).toInt()
            val childHeight = (displayHeight * 0.2f).toInt()
            mMainRect = Rect(0, mTopPadding, mWidth, mTopPadding + mainHeight)
            mVolRect = Rect(0, mMainRect!!.bottom + mChildPadding, mWidth, mMainRect!!.bottom + volHeight)
            mChildRect = Rect(0, mVolRect!!.bottom + mChildPadding, mWidth, mVolRect!!.bottom + childHeight)
        } else {
            val mainHeight = (displayHeight * 0.75f).toInt()
            val volHeight = (displayHeight * 0.25f).toInt()
            mMainRect = Rect(0, mTopPadding, mWidth, mTopPadding + mainHeight)
            mVolRect = Rect(0, mMainRect!!.bottom + mChildPadding, mWidth, mMainRect!!.bottom + volHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(mBackgroundPaint.color)
        if (mWidth == 0 || mMainRect!!.height() == 0 || mItemCount == 0) {
            return
        }
        calculateValue()
        canvas.save()
        canvas.scale(1f, 1f)
        drawGird(canvas)
        drawK(canvas)
        drawText(canvas)
        drawMaxAndMin(canvas)
        drawValue(canvas, if (isLongPress) selectedIndex else mStopIndex)
        canvas.restore()
    }

    fun getMainY(value: Float): Float = (mMainMaxValue - value) * mMainScaleY + mMainRect!!.top

    fun getMainBottom(): Float = mMainRect!!.bottom.toFloat()

    fun getVolY(value: Float): Float = (mVolMaxValue - value) * mVolScaleY + mVolRect!!.top

    fun getChildY(value: Float): Float = (mChildMaxValue - value) * mChildScaleY + mChildRect!!.top

    /** 解决text居中的问题 */
    fun fixTextY(y: Float): Float {
        val fm = mTextPaint.fontMetrics
        return y + fm.descent - fm.ascent
    }

    /** 解决text居中的问题 */
    fun fixTextY1(y: Float): Float {
        val fm = mTextPaint.fontMetrics
        return y + (fm.descent - fm.ascent) / 2 - fm.descent
    }

    /** 画表格 */
    private fun drawGird(canvas: Canvas) {
        // -----------------------上方k线图------------------------
        // 横向的grid
        val rowSpace = (mMainRect!!.height() / mGridRows).toFloat()
        for (i in 0..mGridRows) {
            canvas.drawLine(0f, rowSpace * i + mMainRect!!.top, mWidth.toFloat(), rowSpace * i + mMainRect!!.top, mGridPaint)
        }
        // -----------------------下方子图------------------------
        if (mChildDraw != null) {
            canvas.drawLine(0f, mVolRect!!.bottom.toFloat(), mWidth.toFloat(), mVolRect!!.bottom.toFloat(), mGridPaint)
            canvas.drawLine(0f, mChildRect!!.bottom.toFloat(), mWidth.toFloat(), mChildRect!!.bottom.toFloat(), mGridPaint)
        } else {
            canvas.drawLine(0f, mVolRect!!.bottom.toFloat(), mWidth.toFloat(), mVolRect!!.bottom.toFloat(), mGridPaint)
        }
        // 纵向的grid
        val columnSpace = (mWidth / mGridColumns).toFloat()
        for (i in 1 until mGridColumns) {
            canvas.drawLine(columnSpace * i, 0f, columnSpace * i, mMainRect!!.bottom.toFloat(), mGridPaint)
            canvas.drawLine(columnSpace * i, mMainRect!!.bottom.toFloat(), columnSpace * i, mVolRect!!.bottom.toFloat(), mGridPaint)
            if (mChildDraw != null) {
                canvas.drawLine(columnSpace * i, mVolRect!!.bottom.toFloat(), columnSpace * i, mChildRect!!.bottom.toFloat(), mGridPaint)
            }
        }
    }

    /** 画k线图 */
    private fun drawK(canvas: Canvas) {
        // 保存之前的平移，缩放
        canvas.save()
        canvas.translate(mTranslateX * mScaleX, 0f)
        canvas.scale(mScaleX, 1f)
        for (i in mStartIndex..mStopIndex) {
            val currentPoint = getItem(i)!!
            val currentPointX = getX(i)
            val lastPoint = if (i == 0) currentPoint else getItem(i - 1)!!
            val lastX = if (i == 0) currentPointX else getX(i - 1)
            mMainDraw?.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i)
            mVolDraw?.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i)
            mChildDraw?.drawTranslated(lastPoint, currentPoint, lastX, currentPointX, canvas, this, i)
        }
        // 画选择线
        if (isLongPress) {
            val point = getItem(selectedIndex) as IKLine
            val x = getX(selectedIndex)
            val y = getMainY(point.closePrice)
            // k线图竖线
            canvas.drawLine(x, mMainRect!!.top.toFloat(), x, mMainRect!!.bottom.toFloat(), mSelectedYLinePaint)
            // k线图横线
            canvas.drawLine(-mTranslateX, y, -mTranslateX + mWidth / mScaleX, y, mSelectedXLinePaint)
            // 柱状图竖线
            canvas.drawLine(x, mMainRect!!.bottom.toFloat(), x, mVolRect!!.bottom.toFloat(), mSelectedYLinePaint)
            if (mChildDraw != null) {
                // 子线图竖线
                canvas.drawLine(x, mVolRect!!.bottom.toFloat(), x, mChildRect!!.bottom.toFloat(), mSelectedYLinePaint)
            }
        }
        // 还原 平移缩放
        canvas.restore()
    }

    /** 计算文本长度 */
    private fun calculateWidth(text: String): Int {
        val rect = Rect()
        mTextPaint.getTextBounds(text, 0, text.length, rect)
        return rect.width() + 5
    }

    /** 计算文本长度 */
    private fun calculateMaxMin(text: String): Rect {
        val rect = Rect()
        mMaxMinPaint.getTextBounds(text, 0, text.length, rect)
        return rect
    }

    /** 画文字 */
    private fun drawText(canvas: Canvas) {
        val fm = mTextPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val baseLine = (textHeight - fm.bottom - fm.top) / 2
        // --------------画上方k线图的值-------------
        if (mMainDraw != null) {
            canvas.drawText(formatValue(mMainMaxValue), mWidth - calculateWidth(formatValue(mMainMaxValue)).toFloat(), baseLine + mMainRect!!.top, mTextPaint)
            canvas.drawText(formatValue(mMainMinValue), mWidth - calculateWidth(formatValue(mMainMinValue)).toFloat(), mMainRect!!.bottom - textHeight + baseLine, mTextPaint)
            val rowValue = (mMainMaxValue - mMainMinValue) / mGridRows
            val rowSpace = (mMainRect!!.height() / mGridRows).toFloat()
            for (i in 1 until mGridRows) {
                val text = formatValue(rowValue * (mGridRows - i) + mMainMinValue)
                canvas.drawText(text, mWidth - calculateWidth(text).toFloat(), fixTextY(rowSpace * i + mMainRect!!.top), mTextPaint)
            }
        }
        // --------------画中间子图的值-------------
        if (mVolDraw != null) {
            canvas.drawText(
                mVolDraw!!.getValueFormatter().format(mVolMaxValue),
                mWidth - calculateWidth(formatValue(mVolMaxValue)).toFloat(), mMainRect!!.bottom + baseLine, mTextPaint
            )
        }
        // --------------画下方子图的值-------------
        if (mChildDraw != null) {
            canvas.drawText(
                mChildDraw!!.getValueFormatter().format(mChildMaxValue),
                mWidth - calculateWidth(formatValue(mChildMaxValue)).toFloat(), mVolRect!!.bottom + baseLine, mTextPaint
            )
        }
        // --------------画时间---------------------
        val columnSpace = (mWidth / mGridColumns).toFloat()
        var y = if (isShowChild) {
            mChildRect!!.bottom + baseLine + 5
        } else {
            mVolRect!!.bottom + baseLine + 5
        }

        val startX = getX(mStartIndex) - mPointWidth / 2
        val stopX = getX(mStopIndex) + mPointWidth / 2

        for (i in 1 until mGridColumns) {
            val translateX = xToTranslateX(columnSpace * i)
            if (translateX in startX..stopX) {
                val index = indexOfTranslateX(translateX)
                val text = mAdapter!!.getDate(index)
                canvas.drawText(text, columnSpace * i - mTextPaint.measureText(text) / 2, y, mTextPaint)
            }
        }

        var translateX = xToTranslateX(0f)
        if (translateX in startX..stopX) {
            canvas.drawText(mAdapter!!.getDate(mStartIndex), 0f, y, mTextPaint)
        }
        translateX = xToTranslateX(mWidth.toFloat())
        if (translateX in startX..stopX) {
            val text = mAdapter!!.getDate(mStopIndex)
            canvas.drawText(text, mWidth - mTextPaint.measureText(text), y, mTextPaint)
        }
        if (isLongPress) {
            // 画Y值
            val point = getItem(selectedIndex) as IKLine
            val w1 = ViewUtil.Dp2Px(context, 5f).toFloat()
            val w2 = ViewUtil.Dp2Px(context, 3f).toFloat()
            var r = textHeight / 2 + w2
            y = getMainY(point.closePrice)
            var x: Float
            var text = formatValue(point.closePrice)
            var textWidth = mTextPaint.measureText(text)
            if (translateXtoX(getX(selectedIndex)) < chartWidth / 2) {
                x = 1f
                val path = Path()
                path.moveTo(x, y - r)
                path.lineTo(x, y + r)
                path.lineTo(textWidth + 2 * w1, y + r)
                path.lineTo(textWidth + 2 * w1 + w2, y)
                path.lineTo(textWidth + 2 * w1, y - r)
                path.close()
                canvas.drawPath(path, mSelectPointPaint)
                canvas.drawPath(path, mSelectorFramePaint)
                canvas.drawText(text, x + w1, fixTextY1(y), mTextPaint)
            } else {
                x = mWidth - textWidth - 1 - 2 * w1 - w2
                val path = Path()
                path.moveTo(x, y)
                path.lineTo(x + w2, y + r)
                path.lineTo(mWidth - 2f, y + r)
                path.lineTo(mWidth - 2f, y - r)
                path.lineTo(x + w2, y - r)
                path.close()
                canvas.drawPath(path, mSelectPointPaint)
                canvas.drawPath(path, mSelectorFramePaint)
                canvas.drawText(text, x + w1 + w2, fixTextY1(y), mTextPaint)
            }

            // 画X值
            val date = mAdapter!!.getDate(selectedIndex)
            textWidth = mTextPaint.measureText(date)
            r = textHeight / 2
            x = translateXtoX(getX(selectedIndex))
            y = if (isShowChild) mChildRect!!.bottom.toFloat() else mVolRect!!.bottom.toFloat()

            if (x < textWidth + 2 * w1) {
                x = 1 + textWidth / 2 + w1
            } else if (mWidth - x < textWidth + 2 * w1) {
                x = mWidth - 1 - textWidth / 2 - w1
            }

            canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + baseLine + r, mSelectPointPaint)
            canvas.drawRect(x - textWidth / 2 - w1, y, x + textWidth / 2 + w1, y + baseLine + r, mSelectorFramePaint)
            canvas.drawText(date, x - textWidth / 2, y + baseLine + 5, mTextPaint)
        }
    }

    /** 画最大值最小值 */
    private fun drawMaxAndMin(canvas: Canvas) {
        if (!mainDraw!!.isLine) {
            // 绘制最大值和最小值
            var x = translateXtoX(getX(mMainMinIndex))
            var y = getMainY(mMainLowMinValue)
            var lowString = "── " + mMainLowMinValue.toString()
            // 计算文本宽度
            val lowStringWidth = calculateMaxMin(lowString).width()
            val lowStringHeight = calculateMaxMin(lowString).height()
            if (x < width / 2) {
                // 画右边
                canvas.drawText(lowString, x, y + lowStringHeight / 2, mMaxMinPaint)
            } else {
                // 画左边
                lowString = mMainLowMinValue.toString() + " ──"
                canvas.drawText(lowString, x - lowStringWidth, y + lowStringHeight / 2, mMaxMinPaint)
            }

            x = translateXtoX(getX(mMainMaxIndex))
            y = getMainY(mMainHighMaxValue)

            var highString = "── " + mMainHighMaxValue.toString()
            val highStringWidth = calculateMaxMin(highString).width()
            val highStringHeight = calculateMaxMin(highString).height()
            if (x < width / 2) {
                // 画右边
                canvas.drawText(highString, x, y + highStringHeight / 2, mMaxMinPaint)
            } else {
                // 画左边
                highString = mMainHighMaxValue.toString() + " ──"
                canvas.drawText(highString, x - highStringWidth, y + highStringHeight / 2, mMaxMinPaint)
            }
        }
    }

    /** 画值 */
    private fun drawValue(canvas: Canvas, position: Int) {
        val fm = mTextPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val baseLine = (textHeight - fm.bottom - fm.top) / 2
        if (position in 0 until mItemCount) {
            mMainDraw?.let {
                val y = mMainRect!!.top + baseLine - textHeight
                it.drawText(canvas, this, position, 0f, y)
            }
            mVolDraw?.let {
                val y = mMainRect!!.bottom + baseLine
                it.drawText(canvas, this, position, 0f, y)
            }
            mChildDraw?.let {
                val y = mVolRect!!.bottom + baseLine
                it.drawText(canvas, this, position, 0f, y)
            }
        }
    }

    fun dp2px(dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    fun sp2px(spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /** 格式化值 */
    fun formatValue(value: Float): String {
        if (mValueFormatter == null) {
            mValueFormatter = ValueFormatter()
        }
        return mValueFormatter!!.format(value)
    }

    /** 重新计算并刷新线条 */
    fun notifyChanged() {
        if (isShowChild && mChildDrawPosition == -1) {
            mChildDraw = mChildDraws[0]
            mChildDrawPosition = 0
        }
        if (mItemCount != 0) {
            mDataLen = (mItemCount - 1) * mPointWidth
            checkAndFixScrollX()
            setTranslateXFromScrollX(mScrollX)
        } else {
            scrollX = 0
        }
        invalidate()
    }

    /**
     * MA/BOLL切换及隐藏
     *
     * @param status MA/BOLL/NONE
     */
    fun changeMainDrawType(status: Status) {
        val draw = mainDraw
        if (draw != null && draw.status != status) {
            draw.status = status
            invalidate()
        }
    }

    private fun calculateSelectedX(x: Float) {
        selectedIndex = indexOfTranslateX(xToTranslateX(x))
        if (selectedIndex < mStartIndex) {
            selectedIndex = mStartIndex
        }
        if (selectedIndex > mStopIndex) {
            selectedIndex = mStopIndex
        }
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        val lastIndex = selectedIndex
        calculateSelectedX(e.x)
        if (lastIndex != selectedIndex) {
            onSelectedChanged(this, getItem(selectedIndex), selectedIndex)
        }
        invalidate()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        setTranslateXFromScrollX(mScrollX)
    }

    override fun onScaleChanged(scale: Float, oldScale: Float) {
        checkAndFixScrollX()
        setTranslateXFromScrollX(mScrollX)
        super.onScaleChanged(scale, oldScale)
    }

    /** 计算当前的显示区域 */
    private fun calculateValue() {
        if (!isLongPress) {
            selectedIndex = -1
        }
        mMainMaxValue = Float.MIN_VALUE
        mMainMinValue = Float.MAX_VALUE
        mVolMaxValue = Float.MIN_VALUE
        mVolMinValue = Float.MAX_VALUE
        mChildMaxValue = Float.MIN_VALUE
        mChildMinValue = Float.MAX_VALUE
        mStartIndex = indexOfTranslateX(xToTranslateX(0f))
        mStopIndex = indexOfTranslateX(xToTranslateX(mWidth.toFloat()))
        mMainMaxIndex = mStartIndex
        mMainMinIndex = mStartIndex
        mMainHighMaxValue = Float.MIN_VALUE
        mMainLowMinValue = Float.MAX_VALUE
        for (i in mStartIndex..mStopIndex) {
            val point = getItem(i) as IKLine
            mMainDraw?.let {
                mMainMaxValue = maxOf(mMainMaxValue, it.getMaxValue(point))
                mMainMinValue = minOf(mMainMinValue, it.getMinValue(point))
                if (mMainHighMaxValue != maxOf(mMainHighMaxValue, point.highPrice)) {
                    mMainHighMaxValue = point.highPrice
                    mMainMaxIndex = i
                }
                if (mMainLowMinValue != minOf(mMainLowMinValue, point.lowPrice)) {
                    mMainLowMinValue = point.lowPrice
                    mMainMinIndex = i
                }
            }
            mVolDraw?.let {
                mVolMaxValue = maxOf(mVolMaxValue, it.getMaxValue(point))
                mVolMinValue = minOf(mVolMinValue, it.getMinValue(point))
            }
            mChildDraw?.let {
                mChildMaxValue = maxOf(mChildMaxValue, it.getMaxValue(point))
                mChildMinValue = minOf(mChildMinValue, it.getMinValue(point))
            }
        }
        if (mMainMaxValue != mMainMinValue) {
            val padding = (mMainMaxValue - mMainMinValue) * 0.05f
            mMainMaxValue += padding
            mMainMinValue -= padding
        } else {
            // 当最大值和最小值都相等的时候 分别增大最大值和 减小最小值
            mMainMaxValue += abs(mMainMaxValue * 0.05f)
            mMainMinValue -= abs(mMainMinValue * 0.05f)
            if (mMainMaxValue == 0f) {
                mMainMaxValue = 1f
            }
        }

        if (abs(mVolMaxValue) < 0.01f) {
            mVolMaxValue = 15.00f
        }

        if (abs(mChildMaxValue) < 0.01f && abs(mChildMinValue) < 0.01f) {
            mChildMaxValue = 1f
        } else if (mChildMaxValue == mChildMinValue) {
            // 当最大值和最小值都相等的时候 分别增大最大值和 减小最小值
            mChildMaxValue += abs(mChildMaxValue * 0.05f)
            mChildMinValue -= abs(mChildMinValue * 0.05f)
            if (mChildMaxValue == 0f) {
                mChildMaxValue = 1f
            }
        }

        if (isWR) {
            mChildMaxValue = 0f
            if (abs(mChildMinValue) < 0.01f) {
                mChildMinValue = -10.00f
            }
        }
        mMainScaleY = mMainRect!!.height() * 1f / (mMainMaxValue - mMainMinValue)
        mVolScaleY = mVolRect!!.height() * 1f / (mVolMaxValue - mVolMinValue)
        if (mChildRect != null) {
            mChildScaleY = mChildRect!!.height() * 1f / (mChildMaxValue - mChildMinValue)
        }
        if (mAnimator.isRunning) {
            val value = mAnimator.animatedValue as Float
            mStopIndex = mStartIndex + Math.round(value * (mStopIndex - mStartIndex))
        }
    }

    /** 获取平移的最小值 */
    private fun getMinTranslateX(): Float {
        // 数据不足一屏时，从左边开始显示（否则会贴着右边显示，留下大片空白）
        return if (!isFullScreen()) {
            mPointWidth / 2
        } else {
            -mDataLen + mWidth / mScaleX - mPointWidth / 2
        }
    }

    /** 获取平移的最大值 */
    private fun getMaxTranslateX(): Float {
        if (!isFullScreen()) {
            return getMinTranslateX()
        }
        return mPointWidth / 2
    }

    override fun getMinScrollX(): Int = (-(mOverScrollRange / mScaleX)).toInt()

    override fun getMaxScrollX(): Int = Math.round(getMaxTranslateX() - getMinTranslateX())

    fun indexOfTranslateX(translateX: Float): Int = indexOfTranslateX(translateX, 0, mItemCount - 1)

    /** 在主区域画线 */
    fun drawMainLine(canvas: Canvas, paint: Paint, startX: Float, startValue: Float, stopX: Float, stopValue: Float) {
        canvas.drawLine(startX, getMainY(startValue), stopX, getMainY(stopValue), paint)
    }

    /** 在主区域画分时线 */
    fun drawMainMinuteLine(canvas: Canvas, paint: Paint, startX: Float, startValue: Float, stopX: Float, stopValue: Float) {
        val path = Path()
        path.moveTo(startX, (displayHeight + mTopPadding + mBottomPadding).toFloat())
        path.lineTo(startX, getMainY(startValue))
        path.lineTo(stopX, getMainY(stopValue))
        path.lineTo(stopX, (displayHeight + mTopPadding + mBottomPadding).toFloat())
        path.close()
        canvas.drawPath(path, paint)
    }

    /** 在子区域画线 */
    fun drawChildLine(canvas: Canvas, paint: Paint, startX: Float, startValue: Float, stopX: Float, stopValue: Float) {
        canvas.drawLine(startX, getChildY(startValue), stopX, getChildY(stopValue), paint)
    }

    /** 在成交量区域画线 */
    fun drawVolLine(canvas: Canvas, paint: Paint, startX: Float, startValue: Float, stopX: Float, stopValue: Float) {
        canvas.drawLine(startX, getVolY(startValue), stopX, getVolY(stopValue), paint)
    }

    /** 根据索引获取实体 */
    fun getItem(position: Int): Any? = mAdapter?.getItem(position)

    /** 根据索引索取x坐标 */
    fun getX(position: Int): Float = position * mPointWidth

    /** 数据适配器 */
    var adapter: IAdapter?
        get() = mAdapter
        set(value) {
            if (mAdapter != null) {
                mAdapter!!.unregisterDataSetObserver(mDataSetObserver)
            }
            mAdapter = value
            if (mAdapter != null) {
                mAdapter!!.registerDataSetObserver(mDataSetObserver)
                mItemCount = mAdapter!!.getCount()
            } else {
                mItemCount = 0
            }
            notifyChanged()
        }

    /** 设置当前子图 */
    fun setChildDraw(position: Int) {
        if (mChildDrawPosition != position) {
            if (!isShowChild) {
                isShowChild = true
                initRect()
            }
            mChildDraw = mChildDraws[position]
            mChildDrawPosition = position
            isWR = position == 5
            invalidate()
        }
    }

    /** 隐藏子图 */
    fun hideChildDraw() {
        mChildDrawPosition = -1
        isShowChild = false
        mChildDraw = null
        initRect()
        invalidate()
    }

    /** 给子区域添加画图方法 */
    fun addChildDraw(childDraw: IChartDraw<*>) {
        @Suppress("UNCHECKED_CAST")
        mChildDraws.add(childDraw as IChartDraw<Any>)
    }

    /** scrollX 转换为 TranslateX */
    private fun setTranslateXFromScrollX(scrollX: Int) {
        mTranslateX = scrollX + getMinTranslateX()
    }

    /** ValueFormatter */
    var valueFormatter: IValueFormatter?
        get() = mValueFormatter
        set(value) {
            mValueFormatter = value
        }

    /** DateTimeFormatter */
    var dateTimeFormatter: IDateTimeFormatter?
        get() = mDateTimeFormatter
        set(value) {
            mDateTimeFormatter = value
        }

    /** 格式化时间 */
    fun formatDateTime(date: Date?): String {
        if (mDateTimeFormatter == null) {
            mDateTimeFormatter = TimeFormatter()
        }
        return mDateTimeFormatter!!.format(date)
    }

    /** 获取主区域的 IChartDraw */
    fun getMainDraw(): IChartDraw<*>? = mMainDraw

    /** 设置主区域的 IChartDraw */
    fun setMainDraw(mainDraw: IChartDraw<*>) {
        @Suppress("UNCHECKED_CAST")
        mMainDraw = mainDraw as IChartDraw<Any>
        this.mainDraw = mMainDraw as MainDraw
    }

    fun getVolDraw(): IChartDraw<*>? = mVolDraw

    fun setVolDraw(volDraw: IChartDraw<*>) {
        @Suppress("UNCHECKED_CAST")
        mVolDraw = volDraw as IChartDraw<Any>
    }

    /** 二分查找当前值的index */
    fun indexOfTranslateX(translateX: Float, start: Int, end: Int): Int {
        if (end == start) {
            return start
        }
        if (end - start == 1) {
            val startValue = getX(start)
            val endValue = getX(end)
            return if (abs(translateX - startValue) < abs(translateX - endValue)) start else end
        }
        val mid = start + (end - start) / 2
        val midValue = getX(mid)
        return when {
            translateX < midValue -> indexOfTranslateX(translateX, start, mid)
            translateX > midValue -> indexOfTranslateX(translateX, mid, end)
            else -> mid
        }
    }

    /** 开始动画 */
    fun startAnimation() {
        mAnimator.start()
    }

    /** 设置动画时间 */
    fun setAnimationDuration(duration: Long) {
        mAnimator.duration = duration
    }

    /** 设置表格行数 */
    fun setGridRows(gridRows: Int) {
        mGridRows = if (gridRows < 1) 1 else gridRows
    }

    /** 设置表格列数 */
    fun setGridColumns(gridColumns: Int) {
        mGridColumns = if (gridColumns < 1) 1 else gridColumns
    }

    /** view中的x转化为TranslateX */
    fun xToTranslateX(x: Float): Float = -mTranslateX + x / mScaleX

    /** translateX转化为view中的x */
    fun translateXtoX(translateX: Float): Float = (translateX + mTranslateX) * mScaleX

    /** 获取上方padding */
    val topPadding: Float get() = mTopPadding.toFloat()

    /** 获取子图上方padding */
    fun getChildPadding(): Float = mChildPadding.toFloat()

    fun getmChildScaleYPadding(): Float = mChildPadding.toFloat()

    /** 获取图的宽度 */
    val chartWidth: Int get() = mWidth

    val childRect: Rect? get() = mChildRect

    val volRect: Rect get() = mVolRect!!

    /** 设置选择监听 */
    fun setOnSelectedChangedListener(l: OnSelectedChangedListener?) {
        mOnSelectedChangedListener = l
    }

    fun onSelectedChanged(view: BaseKLineChartView, point: Any?, index: Int) {
        mOnSelectedChangedListener?.onSelectedChanged(view, point, index)
    }

    /** 数据是否充满屏幕 */
    fun isFullScreen(): Boolean = mDataLen >= mWidth / mScaleX

    /** 设置超出右方后可滑动的范围 */
    fun setOverScrollRange(overScrollRange: Float) {
        mOverScrollRange = if (overScrollRange < 0) 0f else overScrollRange
    }

    /** 设置上方padding */
    fun setTopPadding(topPadding: Int) {
        mTopPadding = topPadding
    }

    /** 设置下方padding */
    fun setBottomPadding(bottomPadding: Int) {
        mBottomPadding = bottomPadding
    }

    /** 设置表格线宽度 */
    fun setGridLineWidth(width: Float) {
        mGridPaint.strokeWidth = width
    }

    /** 设置表格线颜色 */
    fun setGridLineColor(color: Int) {
        mGridPaint.color = color
    }

    /** 设置选择器横线宽度 */
    fun setSelectedXLineWidth(width: Float) {
        mSelectedXLinePaint.strokeWidth = width
    }

    /** 设置选择器横线颜色 */
    fun setSelectedXLineColor(color: Int) {
        mSelectedXLinePaint.color = color
    }

    /** 设置选择器竖线宽度 */
    fun setSelectedYLineWidth(width: Float) {
        mSelectedYLinePaint.strokeWidth = width
    }

    /** 设置选择器竖线颜色 */
    fun setSelectedYLineColor(color: Int) {
        mSelectedYLinePaint.color = color
    }

    /** 设置文字颜色 */
    open fun setTextColor(color: Int) {
        mTextPaint.color = color
    }

    /** 设置文字大小 */
    open fun setTextSize(textSize: Float) {
        mTextPaint.textSize = textSize
    }

    /** 设置最大值/最小值文字颜色 */
    fun setMTextColor(color: Int) {
        mMaxMinPaint.color = color
    }

    /** 设置最大值/最小值文字大小 */
    fun setMTextSize(textSize: Float) {
        mMaxMinPaint.textSize = textSize
    }

    /** 设置背景颜色 */
    override fun setBackgroundColor(color: Int) {
        mBackgroundPaint.color = color
    }

    /** 设置选中point 值显示背景 */
    fun setSelectPointColor(color: Int) {
        mSelectPointPaint.color = color
    }

    /** 选中点变化时的监听 */
    fun interface OnSelectedChangedListener {
        /**
         * 当选点中变化时
         *
         * @param view  当前view
         * @param point 选中的点
         * @param index 选中点的索引
         */
        fun onSelectedChanged(view: BaseKLineChartView, point: Any?, index: Int)
    }

    /** 获取文字大小 */
    fun getTextSize(): Float = mTextPaint.textSize

    /** 获取曲线宽度 */
    fun getLineWidth(): Float = mLineWidth

    /** 设置曲线的宽度 */
    open fun setLineWidth(lineWidth: Float) {
        mLineWidth = lineWidth
    }

    /** 设置每个点的宽度 */
    fun setPointWidth(pointWidth: Float) {
        mPointWidth = pointWidth
    }

    fun getGridPaint(): Paint = mGridPaint

    val textPaint: Paint get() = mTextPaint

    fun getBackgroundPaint(): Paint = mBackgroundPaint

    fun getDisplayHeight(): Int = displayHeight + mTopPadding + mBottomPadding
}
