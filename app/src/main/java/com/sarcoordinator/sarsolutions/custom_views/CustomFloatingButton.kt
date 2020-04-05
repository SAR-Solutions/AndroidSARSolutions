package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout
import com.sarcoordinator.sarsolutions.R
import kotlinx.android.synthetic.main.custom_floating_button.view.*

class CustomFloatingButton(context: Context, attrs: AttributeSet) : MotionLayout(context, attrs) {

    init {
        inflate(context, R.layout.custom_floating_button, this)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomFloatingButton,
            0, 0
        ).apply {
            custom_button.setImageDrawable(getDrawable(R.styleable.CustomFloatingButton_Icon))
            custom_button.setColorFilter(
                getColor(
                    R.styleable.CustomFloatingButton_Tint,
                    android.R.color.black
                )
            )
            recycle()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        custom_button.setOnClickListener(l)
    }
}