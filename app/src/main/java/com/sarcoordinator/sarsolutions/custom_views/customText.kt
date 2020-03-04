package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.sarcoordinator.sarsolutions.R

class CustomView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_custom_test, this)
    }
}