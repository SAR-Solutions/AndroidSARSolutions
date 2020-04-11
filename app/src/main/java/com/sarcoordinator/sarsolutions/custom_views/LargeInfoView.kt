package com.sarcoordinator.sarsolutions.custom_views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.LinearLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.large_info_view.view.*

class LargeInfoView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LargeInfoView,
            0, 0
        ).apply {
            try {
                inflate(context, R.layout.large_info_view, this@LargeInfoView)

                // Set icon
                val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LargeInfoView)
                val drawable = typedArray.getDrawable(R.styleable.LargeInfoView_Icon)
                icon.setImageDrawable(drawable)

                heading.text = getString(R.styleable.LargeInfoView_Heading)
                message.text = getString(R.styleable.LargeInfoView_Message)

            } finally {
                recycle()
            }
        }

        // Play animation after layout is inflated
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                startAnim()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun startAnim() {
        val imgCenter = (icon.y + icon.height) / 2
        val headingCenter = (heading.y + heading.height) / 2

        val iconAlphaAnim = ObjectAnimator.ofFloat(icon, View.ALPHA, icon.alpha, 1F)
        val headingAlphaAnim = ObjectAnimator.ofFloat(heading, View.ALPHA, heading.alpha, 1F)
        val headingDropAnim = ObjectAnimator.ofFloat(heading, View.Y, imgCenter, heading.y)
        val messageAlphaAnim = ObjectAnimator.ofFloat(message, View.ALPHA, message.alpha, 1F)
        val messageDropAnim = ObjectAnimator.ofFloat(message, View.Y, headingCenter, message.y)

        val messageAnim = AnimatorSet().apply {
            play(messageAlphaAnim)
                .with(messageDropAnim)
        }

        val headingAnim = AnimatorSet().apply {
            play(headingAlphaAnim)
                .with(headingDropAnim)
                .with(messageAnim)
        }

        AnimatorSet().apply {
            play(headingAnim)
                .after(iconAlphaAnim)
            start()
        }
    }
}