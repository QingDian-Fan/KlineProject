package com.common.kline

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.OverScroller
import android.widget.RelativeLayout
import androidx.core.view.GestureDetectorCompat

/**
 * 可以滑动和放大的view
 * Created by tian on 2016/5/3.
 */
abstract class ScrollAndScaleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr),
    GestureDetector.OnGestureListener,
    ScaleGestureDetector.OnScaleGestureListener {

    protected var mScrollX = 0
    protected var mDetector: GestureDetectorCompat = GestureDetectorCompat(context, this)
    protected var mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)

    @JvmField
    var isLongPress = false

    private val mScroller = OverScroller(context)

    protected var touch = false

    protected var mScaleX = 1f
    protected var mScaleXMax = 2f
    protected var mScaleXMin = 0.5f

    private var mMultipleTouch = false
    private var mScrollEnable = true
    private var mScaleEnable = true
    private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var mDownX = 0f
    private var mDownY = 0f
    private var mParentDisallowIntercept = false

    private var mX = 0f

    init {
        setWillNotDraw(false)
    }

    override fun onDown(e: MotionEvent): Boolean = false

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isLongPress && !isMultipleTouch()) {
            scrollBy(Math.round(distanceX), 0)
            return true
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        isLongPress = true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (!isTouch() && isScrollEnable()) {
            mScroller.fling(
                mScrollX, 0,
                Math.round(velocityX / mScaleX), 0,
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                0, 0
            )
        }
        return true
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (!isTouch()) {
                scrollTo(mScroller.currX, mScroller.currY)
            } else {
                mScroller.forceFinished(true)
            }
        }
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollTo(mScrollX - Math.round(x / mScaleX), 0)
    }

    override fun scrollTo(x: Int, y: Int) {
        if (!isScrollEnable()) {
            mScroller.forceFinished(true)
            return
        }
        val oldX = mScrollX
        mScrollX = x
        if (mScrollX < getMinScrollX()) {
            mScrollX = getMinScrollX()
            onRightSide()
            mScroller.forceFinished(true)
        } else if (mScrollX > getMaxScrollX()) {
            mScrollX = getMaxScrollX()
            onLeftSide()
            mScroller.forceFinished(true)
        }
        onScrollChanged(mScrollX, 0, oldX, 0)
        invalidate()
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (!isScaleEnable()) {
            return false
        }
        val oldScale = mScaleX
        mScaleX *= detector.scaleFactor
        if (mScaleX < mScaleXMin) {
            mScaleX = mScaleXMin
        } else if (mScaleX > mScaleXMax) {
            mScaleX = mScaleXMax
        } else {
            onScaleChanged(mScaleX, oldScale)
        }
        return true
    }

    protected open fun onScaleChanged(scale: Float, oldScale: Float) {
        invalidate()
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 按压手指超过1个
        if (event.pointerCount > 1) {
            isLongPress = false
        }
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                touch = true
                mX = event.x
                mDownX = event.x
                mDownY = event.y
                requestParentDisallowIntercept(true)
            }
            MotionEvent.ACTION_MOVE -> {
                handleParentIntercept(event)
                // 长按之后移动
                if (isLongPress) {
                    onLongPress(event)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> invalidate()
            MotionEvent.ACTION_UP -> {
                if (mX == event.x) {
                    if (isLongPress) {
                        isLongPress = false
                    }
                }
                requestParentDisallowIntercept(false)
                touch = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                requestParentDisallowIntercept(false)
                isLongPress = false
                touch = false
                invalidate()
            }
        }
        mMultipleTouch = event.pointerCount > 1
        mDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        return true
    }

    private fun handleParentIntercept(event: MotionEvent) {
        val dX = event.x - mDownX
        val dY = event.y - mDownY
        if (event.pointerCount > 1) {
            requestParentDisallowIntercept(true)
        } else if (Math.abs(dX) > mTouchSlop || Math.abs(dY) > mTouchSlop) {
            requestParentDisallowIntercept(Math.abs(dX) > Math.abs(dY))
        }
    }

    private fun requestParentDisallowIntercept(disallowIntercept: Boolean) {
        if (mParentDisallowIntercept == disallowIntercept) {
            return
        }
        mParentDisallowIntercept = disallowIntercept
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    /** 滑到了最左边 */
    abstract fun onLeftSide()

    /** 滑到了最右边 */
    abstract fun onRightSide()

    /** 是否在触摸中 */
    fun isTouch(): Boolean = touch

    /** 获取位移的最小值 */
    abstract fun getMinScrollX(): Int

    /** 获取位移的最大值 */
    abstract fun getMaxScrollX(): Int

    /** 设置ScrollX */
    override fun setScrollX(value: Int) {
        mScrollX = value
        scrollTo(value, 0)
    }

    /** 是否是多指触控 */
    fun isMultipleTouch(): Boolean = mMultipleTouch

    protected fun checkAndFixScrollX() {
        if (mScrollX < getMinScrollX()) {
            mScrollX = getMinScrollX()
            mScroller.forceFinished(true)
        } else if (mScrollX > getMaxScrollX()) {
            mScrollX = getMaxScrollX()
            mScroller.forceFinished(true)
        }
    }

    fun getScaleXMax(): Float = mScaleXMax

    fun getScaleXMin(): Float = mScaleXMin

    open fun isScrollEnable(): Boolean = mScrollEnable

    open fun isScaleEnable(): Boolean = mScaleEnable

    /** 设置缩放的最大值 */
    fun setScaleXMax(scaleXMax: Float) {
        mScaleXMax = scaleXMax
    }

    /** 设置缩放的最小值 */
    fun setScaleXMin(scaleXMin: Float) {
        mScaleXMin = scaleXMin
    }

    /** 设置是否可以滑动 */
    open fun setScrollEnable(scrollEnable: Boolean) {
        mScrollEnable = scrollEnable
    }

    /** 设置是否可以缩放 */
    open fun setScaleEnable(scaleEnable: Boolean) {
        mScaleEnable = scaleEnable
    }

    override fun getScaleX(): Float = mScaleX
}
