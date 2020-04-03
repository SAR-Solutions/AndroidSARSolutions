package com.sarcoordinator.sarsolutions.custom_views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.custom_back_button.view.*

class CustomBackButton(context: Context, attrs: AttributeSet) : MotionLayout(context, attrs) {

    init {
        inflate(context, R.layout.custom_back_button, this)
        custom_back_button.setOnClickListener { getActivity()?.onBackPressed() }
    }

    // Source: https://android.googlesource.com/platform/frameworks/support/+/refs/heads/marshmallow-release/v7/mediarouter/src/android/support/v7/app/MediaRouteButton.java#262
    private fun getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}