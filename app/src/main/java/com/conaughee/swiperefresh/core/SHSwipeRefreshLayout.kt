package com.conaughee.swiperefresh.core

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AbsListView
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.conaughee.swiperefresh.R

class SHSwipeRefreshLayout : FrameLayout, NestedScrollingParent {
    private var parentHelper: NestedScrollingParentHelper? = null
    private var onRefreshListener: SHSOnRefreshListener? = null

    /**
     * On refresh Callback, call on start refresh
     */
    interface SHSOnRefreshListener {
        fun onRefresh()
        fun onLoading()
        fun onRefreshPulStateChange(percent: Float, state: Int)
        fun onLoadmorePullStateChange(percent: Float, state: Int)
    }

    internal open class WXRefreshAnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    }

    private val headerView: SHGuidanceView? = SHGuidanceView(context)
    private val footerView: SHGuidanceView? = SHGuidanceView(context)
    private var mTargetView: View? = null

    // Enable PullRefresh and Loadmore
    var isRefreshEnable = true
    var isLoadmoreEnable = true

    // Is Refreshing
    @Volatile
    var isRefreshing = false
        private set

    // RefreshView Height
    private var guidanceViewHeight = 0f

    // RefreshView Over Flow Height
    private var guidanceViewFlowHeight = 0f

    // Drag Action
    private var mCurrentAction = -1
    private var isConfirm = false

    // RefreshView Attrs
    private var mGuidanceViewBgColor = 0
    private var mGuidanceViewTextColor = 0
    private var mProgressBgColor = 0
    private var mProgressColor = 0
    private var mRefreshDefaulText: String? = ""
    private var mLoadDefaulText: String? = ""

    constructor(context: Context) : super(context) {
        initAttrs(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttrs(context, attrs)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        if (childCount > 1) {
            throw RuntimeException("WXSwipeLayout should not have more than one child")
        }
        parentHelper = NestedScrollingParentHelper(this)
        guidanceViewHeight = dipToPx(context, GUIDANCE_VIEW_HEIGHT.toFloat())
        guidanceViewFlowHeight = guidanceViewHeight * 1.5.toFloat()
        if (isInEditMode && attrs == null) {
            return
        }
        var resId: Int
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SHSwipeRefreshLayout)
        val resources = context.resources

        //Indicator
        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_guidance_view_bg_color, -1)
        mGuidanceViewBgColor = if (resId == -1) {
            ta.getColor(
                R.styleable.SHSwipeRefreshLayout_guidance_view_bg_color,
                Color.WHITE
            )
        } else {
            resources.getColor(resId)
        }

        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_guidance_text_color, -1)
        mGuidanceViewTextColor = if (resId == -1) {
            ta.getColor(
                R.styleable.SHSwipeRefreshLayout_guidance_text_color,
                Color.BLACK
            )
        } else {
            resources.getColor(resId)
        }

        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_progress_bg_color, -1)
        mProgressBgColor = if (resId == -1) {
            ta.getColor(
                R.styleable.SHSwipeRefreshLayout_progress_bg_color,
                Color.WHITE
            )
        } else {
            resources.getColor(resId)
        }

        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_progress_bar_color, -1)
        mProgressColor = if (resId == -1) {
            ta.getColor(
                R.styleable.SHSwipeRefreshLayout_progress_bar_color,
                Color.RED
            )
        } else {
            resources.getColor(resId)
        }

        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_refresh_text, -1)
        mRefreshDefaulText = if (resId == -1) {
            ta.getString(R.styleable.SHSwipeRefreshLayout_refresh_text)
        } else {
            resources.getString(resId)
        }

        resId = ta.getResourceId(R.styleable.SHSwipeRefreshLayout_load_text, -1)
        mLoadDefaulText = if (resId == -1) {
            ta.getString(R.styleable.SHSwipeRefreshLayout_load_text)
        } else {
            resources.getString(resId)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mTargetView = getChildAt(0)
        setGuidanceView()
    }

    /**
     * Init refresh view or loading view
     */
    private fun setGuidanceView() {
        // SetUp HeaderView
        var lp = LayoutParams(LayoutParams.MATCH_PARENT, 0)
        headerView!!.setStartEndTrim(0f, 0.75f)
        headerView.setText(mRefreshDefaulText)
        headerView.setTextColor(mGuidanceViewTextColor)
        headerView.setBackgroundColor(mGuidanceViewBgColor)
        headerView.setProgressBgColor(mProgressBgColor)
        headerView.setProgressColor(mProgressColor)
        addView(headerView, lp)

        // SetUp FooterView
        lp = LayoutParams(LayoutParams.MATCH_PARENT, 0)
        lp.gravity = Gravity.BOTTOM
        footerView!!.setStartEndTrim(0.5f, 1.25f)
        footerView.setText(mLoadDefaulText)
        footerView.setTextColor(mGuidanceViewTextColor)
        footerView.setBackgroundColor(mGuidanceViewBgColor)
        footerView.setProgressBgColor(mProgressBgColor)
        footerView.setProgressColor(mProgressColor)
        addView(footerView, lp)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (!isRefreshEnable && !isLoadmoreEnable) {
            false
        } else super.onInterceptTouchEvent(ev)
    }

    /*********************************** NestedScrollParent  */
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return true
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        parentHelper!!.onNestedScrollAccepted(child, target, axes)
    }

    /**
     * Callback on TouchEvent.ACTION_CANCLE or TouchEvent.ACTION_UP
     * handler : refresh or loading
     * @param child : child view of SwipeLayout,RecyclerView or Scroller
     */
    override fun onStopNestedScroll(child: View) {
        parentHelper!!.onStopNestedScroll(child)
        handlerAction()
    }

    /**
     * With child view to processing move events
     * @param target the child view
     * @param dx move x
     * @param dy move y
     * @param consumed parent consumed move distance
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (!isRefreshEnable && !isLoadmoreEnable) {
            return
        }

        // Prevent Layout shake
        if (Math.abs(dy) > 200) {
            return
        }
        if (!isConfirm) {
            if (dy < 0 && !canChildScrollUp()) {
                mCurrentAction = ACTION_PULL_REFRESH
                isConfirm = true
            } else if (dy > 0 && !canChildScrollDown()) {
                mCurrentAction = ACTION_LOADMORE
                isConfirm = true
            }
        }
        if (moveGuidanceView(-dy.toFloat())) {
            consumed[1] += dy
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
    }

    override fun getNestedScrollAxes(): Int {
        return parentHelper!!.nestedScrollAxes
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return false
    }

    /**
     * Adjust the refresh or loading view according to the size of the gesture
     *
     * @param distanceY move distance of Y
     */
    private fun moveGuidanceView(distanceY: Float): Boolean {
        if (isRefreshing) {
            return false
        }
        if (!canChildScrollUp() && isRefreshEnable && mCurrentAction == ACTION_PULL_REFRESH) {
            // Pull Refresh
            val lp = headerView!!.layoutParams as LayoutParams
            lp.height += distanceY.toInt()
            if (lp.height < 0) {
                lp.height = 0
            }
            if (lp.height > guidanceViewFlowHeight) {
                lp.height = guidanceViewFlowHeight.toInt()
            }
            if (onRefreshListener != null) {
                if (lp.height >= guidanceViewHeight) {
                    onRefreshListener!!.onRefreshPulStateChange(
                        lp.height / guidanceViewHeight,
                        OVER_TRIGGER_POINT
                    )
                } else {
                    onRefreshListener!!.onRefreshPulStateChange(
                        lp.height / guidanceViewHeight,
                        NOT_OVER_TRIGGER_POINT
                    )
                }
            }
            if (lp.height == 0) {
                isConfirm = false
                mCurrentAction = -1
            }
            headerView.layoutParams = lp
            headerView.setProgressRotation(lp.height / guidanceViewFlowHeight)
            moveTargetView(lp.height.toFloat())
            return true
        } else if (!canChildScrollDown() && isLoadmoreEnable && mCurrentAction == ACTION_LOADMORE) {
            // Load more
            val lp = footerView!!.layoutParams as LayoutParams
            lp.height -= distanceY.toInt()
            if (lp.height < 0) {
                lp.height = 0
            }
            if (lp.height > guidanceViewFlowHeight) {
                lp.height = guidanceViewFlowHeight.toInt()
            }
            if (onRefreshListener != null) {
                if (lp.height >= guidanceViewHeight) {
                    onRefreshListener!!.onLoadmorePullStateChange(
                        lp.height / guidanceViewHeight,
                        OVER_TRIGGER_POINT
                    )
                } else {
                    onRefreshListener!!.onLoadmorePullStateChange(
                        lp.height / guidanceViewHeight,
                        NOT_OVER_TRIGGER_POINT
                    )
                }
            }
            if (lp.height == 0) {
                isConfirm = false
                mCurrentAction = -1
            }
            footerView.layoutParams = lp
            footerView.setProgressRotation(lp.height / guidanceViewFlowHeight)
            moveTargetView(-lp.height.toFloat())
            return true
        }
        return false
    }

    /**
     * Adjust contentView(Scroller or List) at refresh or loading time
     * @param h Height of refresh view or loading view
     */
    private fun moveTargetView(h: Float) {
        mTargetView!!.translationY = h
    }

    /**
     * Decide on the action refresh or loadmore
     */
    private fun handlerAction() {
        if (isRefreshing) {
            return
        }
        isConfirm = false
        var lp: LayoutParams
        if (isRefreshEnable && mCurrentAction == ACTION_PULL_REFRESH) {
            lp = headerView!!.layoutParams as LayoutParams
            if (lp.height >= guidanceViewHeight) {
                startRefresh(lp.height)
                if (onRefreshListener != null) onRefreshListener!!.onRefreshPulStateChange(
                    1f,
                    START
                )
            } else if (lp.height > 0) {
                resetHeaderView(lp.height)
            } else {
                resetRefreshState()
            }
        }
        if (isLoadmoreEnable && mCurrentAction == ACTION_LOADMORE) {
            lp = footerView!!.layoutParams as LayoutParams
            if (lp.height >= guidanceViewHeight) {
                startLoadmore(lp.height)
                if (onRefreshListener != null) onRefreshListener!!.onLoadmorePullStateChange(
                    1f,
                    START
                )
            } else if (lp.height > 0) {
                resetFootView(lp.height)
            } else {
                resetLoadmoreState()
            }
        }
    }

    /**
     * Start Refresh
     * @param headerViewHeight
     */
    private fun startRefresh(headerViewHeight: Int) {
        isRefreshing = true
        val animator = ValueAnimator.ofFloat(headerViewHeight.toFloat(), guidanceViewHeight)
        animator.addUpdateListener { animation ->
            val lp = headerView!!.layoutParams as LayoutParams
            lp.height = (animation.animatedValue as Float).toFloat().toInt()
            headerView.layoutParams = lp
            moveTargetView(lp.height.toFloat())
        }
        animator.addListener(object : WXRefreshAnimatorListener() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                headerView!!.startAnimation()
                //TODO updateLoadText
                if (onRefreshListener != null) {
                    onRefreshListener!!.onRefresh()
                }
            }
        })
        animator.duration = 300
        animator.start()
    }

    /**
     * Reset refresh state
     * @param headerViewHeight
     */
    private fun resetHeaderView(headerViewHeight: Int) {
        headerView!!.stopAnimation()
        headerView.setStartEndTrim(0f, 0.75f)
        val animator = ValueAnimator.ofFloat(headerViewHeight.toFloat(), 0f)
        animator.addUpdateListener { animation ->
            val lp = headerView.layoutParams as LayoutParams
            lp.height = (animation.animatedValue as Float).toFloat().toInt()
            headerView.layoutParams = lp
            moveTargetView(lp.height.toFloat())
        }
        animator.addListener(object : WXRefreshAnimatorListener() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                resetRefreshState()
            }
        })
        animator.duration = 300
        animator.start()
    }

    private fun resetRefreshState() {
        isRefreshing = false
        isConfirm = false
        mCurrentAction = -1
        //TODO updateLoadText
    }

    /**
     * Start loadmore
     * @param headerViewHeight
     */
    private fun startLoadmore(headerViewHeight: Int) {
        isRefreshing = true
        val animator = ValueAnimator.ofFloat(headerViewHeight.toFloat(), guidanceViewHeight)
        animator.addUpdateListener { animation ->
            val lp = footerView!!.layoutParams as LayoutParams
            lp.height = (animation.animatedValue as Float).toFloat().toInt()
            footerView.layoutParams = lp
            moveTargetView(-lp.height.toFloat())
        }
        animator.addListener(object : WXRefreshAnimatorListener() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                footerView!!.startAnimation()
                //TODO updateLoadText
                if (onRefreshListener != null) {
                    onRefreshListener!!.onLoading()
                }
            }
        })
        animator.duration = 300
        animator.start()
    }

    /**
     * Reset loadmore state
     * @param headerViewHeight
     */
    private fun resetFootView(headerViewHeight: Int) {
        footerView!!.stopAnimation()
        footerView.setStartEndTrim(0.5f, 1.25f)
        val animator = ValueAnimator.ofFloat(headerViewHeight.toFloat(), 0f)
        animator.addUpdateListener { animation ->
            val lp = footerView.layoutParams as LayoutParams
            lp.height = (animation.animatedValue as Float).toFloat().toInt()
            footerView.layoutParams = lp
            moveTargetView(-lp.height.toFloat())
        }
        animator.addListener(object : WXRefreshAnimatorListener() {
            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                resetLoadmoreState()
            }
        })
        animator.duration = 300
        animator.start()
    }

    private fun resetLoadmoreState() {
        isRefreshing = false
        isConfirm = false
        mCurrentAction = -1
        //TODO updateLoadText
    }

    /**
     * Whether child view can scroll up
     * @return
     */
    fun canChildScrollUp(): Boolean {
        if (mTargetView == null) {
            return false
        }
        return if (Build.VERSION.SDK_INT < 14) {
            if (mTargetView is AbsListView) {
                val absListView = mTargetView as AbsListView
                (absListView.childCount > 0
                        && (absListView.firstVisiblePosition > 0 || absListView.getChildAt(0)
                    .top < absListView.paddingTop))
            } else {
                ViewCompat.canScrollVertically(mTargetView, -1) || mTargetView!!.scrollY > 0
            }
        } else {
            ViewCompat.canScrollVertically(mTargetView, -1)
        }
    }

    /**
     * Whether child view can scroll down
     * @return
     */
    fun canChildScrollDown(): Boolean {
        if (mTargetView == null) {
            return false
        }
        return if (Build.VERSION.SDK_INT < 14) {
            if (mTargetView is AbsListView) {
                val absListView = mTargetView as AbsListView
                if (absListView.childCount > 0) {
                    val lastChildBottom = absListView.getChildAt(absListView.childCount - 1)
                        .bottom
                    (absListView.lastVisiblePosition == absListView.adapter.count - 1
                            && lastChildBottom <= absListView.measuredHeight)
                } else {
                    false
                }
            } else {
                ViewCompat.canScrollVertically(mTargetView, 1) || mTargetView!!.scrollY > 0
            }
        } else {
            ViewCompat.canScrollVertically(mTargetView, 1)
        }
    }

    fun dipToPx(context: Context, value: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics)
    }

    fun setOnRefreshListener(onRefreshListener: SHSOnRefreshListener?) {
        this.onRefreshListener = onRefreshListener
    }

    /**
     * Callback on refresh finish
     */
    fun finishRefresh() {
        if (mCurrentAction == ACTION_PULL_REFRESH) {
            resetHeaderView(headerView?.measuredHeight ?: 0)
        }
    }

    /**
     * Callback on loadmore finish
     */
    fun finishLoadmore() {
        if (mCurrentAction == ACTION_LOADMORE) {
            resetFootView(footerView?.measuredHeight ?: 0)
        }
    }

    fun setHeaderView(@LayoutRes layoutResID: Int) {
        headerView!!.setGuidanceView(layoutResID)
    }

    fun setHeaderView(view: View?) {
        headerView!!.setGuidanceView(view)
    }

    fun setFooterView(@LayoutRes layoutResID: Int) {
        footerView!!.setGuidanceView(layoutResID)
    }

    fun setFooterView(view: View?) {
        footerView!!.setGuidanceView(view)
    }

    fun setRefreshViewText(text: String?) {
        headerView!!.setText(text)
    }

    fun setLoaderViewText(text: String?) {
        footerView!!.setText(text)
    }

    companion object {
        const val NOT_OVER_TRIGGER_POINT = 1
        const val OVER_TRIGGER_POINT = 2
        const val START = 3
        private const val GUIDANCE_VIEW_HEIGHT = 80
        private const val ACTION_PULL_REFRESH = 0
        private const val ACTION_LOADMORE = 1
    }
}