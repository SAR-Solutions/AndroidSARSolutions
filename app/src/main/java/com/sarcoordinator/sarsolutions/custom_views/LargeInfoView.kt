package com.sarcoordinator.sarsolutions.custom_views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.custom_large_info_view.view.*

class LargeInfoView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var enableButton = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LargeInfoView,
            0, 0
        ).apply {
            try {
                inflate(context, R.layout.custom_large_info_view, this@LargeInfoView)

                // Set icon
                val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LargeInfoView)
                val drawable = typedArray.getDrawable(R.styleable.LargeInfoView_Icon)
                icon.setImageDrawable(drawable)

                heading.text = getString(R.styleable.LargeInfoView_Heading)
                message.text = getString(R.styleable.LargeInfoView_Message)

            } finally {
                recycle()
            }

            // Play animation after layout is inflated
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    startAnim()
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    fun setButtonClickListener(clickListener: OnClickListener) {
        enableButton = true
        button.setOnClickListener(clickListener)
        button.alpha = 1F
    }

    fun removeButtonClickListener() {
        enableButton = false
        button.setOnClickListener(null)
        button.alpha = 0F
    }

    private fun startAnim() {

        // Reset view alpha
        icon.alpha = 0F
        heading.alpha = 0F
        message.alpha = 0F
        button.alpha = 0F

        val imgCenter = (icon.y + icon.height) / 2
        val headingCenter = (heading.y + heading.height) / 2
        val messageCenter = (message.y + message.height) / 2

        // Skip animation as view is not inflated yet
        if (imgCenter == 0F || headingCenter == 0F || messageCenter == 0F) {
            icon.alpha = 1F
            heading.alpha = 1F
            message.alpha = 1F
            if (enableButton)
                button.alpha = 1F
            return
        }

        val iconAlphaAnim = ObjectAnimator.ofFloat(icon, View.ALPHA, icon.alpha, 1F)
        val headingAlphaAnim = ObjectAnimator.ofFloat(heading, View.ALPHA, heading.alpha, 1F)
        val headingDropAnim = ObjectAnimator.ofFloat(heading, View.Y, imgCenter, heading.y)
        val messageAlphaAnim = ObjectAnimator.ofFloat(message, View.ALPHA, message.alpha, 1F)
        val messageDropAnim = ObjectAnimator.ofFloat(message, View.Y, headingCenter, message.y)
        val buttonAlphaAnim = ObjectAnimator.ofFloat(button, View.ALPHA, button.alpha, 1F)
        val buttonDropAnim = ObjectAnimator.ofFloat(button, View.Y, messageCenter, button.y)

        val buttonAnim = AnimatorSet().apply {
            play(buttonAlphaAnim)
                .with(buttonDropAnim)
        }

        val messageAnim = AnimatorSet().apply {
            play(messageAlphaAnim)
                .with(messageDropAnim)
        }

        val headingAnim = AnimatorSet().apply {
            val anim = play(headingAlphaAnim)
                .with(headingDropAnim)
                .with(messageAnim)

            if (enableButton)
                anim.with(buttonAnim)
        }

        AnimatorSet().apply {
            play(headingAnim)
                .after(iconAlphaAnim)
            start()
        }
    }
}