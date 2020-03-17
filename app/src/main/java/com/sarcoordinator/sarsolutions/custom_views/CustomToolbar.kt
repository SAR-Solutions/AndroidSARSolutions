package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
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

    private lateinit var style: Style
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
                } else {
                    inflate(context, R.layout.custom_toolbar, this@CustomToolbar)
                    heading.text = getString(R.styleable.CustomToolbar_Title)
                    style = Style.Shrunk
                    backButton = back_button
                }
            } finally {
                recycle()
            }
        }
    }

    fun setBackPressedListener(onClickListener: OnClickListener) {
        backButton.visibility = View.VISIBLE
        backButton.isClickable = true
        backButton.setOnClickListener(onClickListener)
    }

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
}