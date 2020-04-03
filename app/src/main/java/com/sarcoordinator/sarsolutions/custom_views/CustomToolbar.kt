package com.sarcoordinator.sarsolutions.custom_views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.custom_toolbar.view.*
import kotlinx.android.synthetic.main.custom_toolbar_expanded.view.*


class CustomToolbar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    enum class Style {
        Expanded, Shrunk
    }

    private var style: Style
    private lateinit var backButton: ImageButton

    init {

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomToolbar,
            0, 0
        ).apply {
            try {
                if (getInt(R.styleable.CustomToolbar_Style, 0) == 1) {
                    inflate(context, R.layout.custom_toolbar_expanded, this@CustomToolbar)
                    heading_expanded.text = getString(R.styleable.CustomToolbar_Title)
                    style = Style.Expanded
                    backButton = back_button_expanded
                    backButton.visibility = View.VISIBLE
                    backButton.isClickable = true
                    backButton.setOnClickListener { getActivity()?.onBackPressed() }
                } else {
                    inflate(context, R.layout.custom_toolbar, this@CustomToolbar)
                    heading.text = getString(R.styleable.CustomToolbar_Title)
                    style = Style.Shrunk
                }
            } finally {
                recycle()
            }
        }
    }

    fun setBackPressedListener(onClickListener: OnClickListener) =
        backButton.setOnClickListener(onClickListener)

    fun setHeading(heading: String) {
        when (style) {
            Style.Shrunk -> {
                this.heading.text = heading
            }
            Style.Expanded -> {
                heading_expanded.text = heading
            }
        }
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