package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.custom_toolbar.view.*

class CustomToolbar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.custom_toolbar, this)

//        heading.text = attrs.getAttributeValue(R.attr.Title)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomToolbar,
        0, 0).apply {
            try {
                heading.text = getString(R.styleable.CustomToolbar_Title)
            } finally {
                recycle()
            }
        }
    }
}