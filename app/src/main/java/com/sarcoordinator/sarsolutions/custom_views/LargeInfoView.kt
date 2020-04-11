package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
import android.util.AttributeSet
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
    }
}