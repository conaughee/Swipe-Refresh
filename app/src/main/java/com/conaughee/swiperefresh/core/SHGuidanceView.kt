package com.conaughee.swiperefresh.core

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.conaughee.swiperefresh.drawable.SHCircleProgressBar
import com.conaughee.swiperefresh.util.DipUtils

class SHGuidanceView : LinearLayout {
    private var circleProgressBar: SHCircleProgressBar? = null
    private var tvLoad: TextView? = null

    constructor(context: Context?) : super(context) {
        setupViews()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setupViews()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setupViews()
    }

    private fun setupViews() {
        this.orientation = HORIZONTAL
        this.gravity = Gravity.CENTER
        circleProgressBar = SHCircleProgressBar(context)
        val lp = LayoutParams(
            DipUtils.dipToPx(context, DEFAULT_CIRCLE_SIZE.toFloat()).toInt(), DipUtils.dipToPx(
                context, DEFAULT_CIRCLE_SIZE.toFloat()
            ).toInt()
        )
        lp.rightMargin = DipUtils.dipToPx(context, 10f).toInt()
        addView(circleProgressBar, lp)
        tvLoad = TextView(context)
        addView(tvLoad)
    }

    fun setGuidanceView(view: View?) {
        if (view == null) return
        removeAllViews()
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(view, lp)
    }

    fun setGuidanceView(@LayoutRes layoutResID: Int) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutResID, null) ?: return
        removeAllViews()
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(view, lp)
    }

    fun setText(loadtText: String?) {
        if (tvLoad != null) tvLoad!!.text = loadtText
    }

    fun setTextColor(color: Int) {
        if (tvLoad != null) tvLoad!!.setTextColor(color)
    }

    fun setProgressBgColor(color: Int) {
        if (circleProgressBar != null) circleProgressBar!!.setBackgroundColor(color)
    }

    fun setProgressColor(color: Int) {
        if (circleProgressBar != null) circleProgressBar!!.setColorSchemeColors(color)
    }

    fun startAnimation() {
        if (circleProgressBar != null) circleProgressBar!!.start()
    }

    fun setStartEndTrim(startAngle: Float, endAngle: Float) {
        if (circleProgressBar != null) circleProgressBar!!.setStartEndTrim(startAngle, endAngle)
    }

    fun stopAnimation() {
        if (circleProgressBar != null) circleProgressBar!!.stop()
    }

    fun setProgressRotation(rotation: Float) {
        if (circleProgressBar != null) circleProgressBar!!.setProgressRotation(rotation)
    }

    companion object {
        private const val DEFAULT_CIRCLE_SIZE = 36
    }
}