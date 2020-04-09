package com.sarcoordinator.sarsolutions.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.util.ObjectsCompat
import androidx.core.view.children

/**
 * Custom FrameLayout to pass insets on to children
 */
class WindowInsetsFrameLayout : FrameLayout {
    private var lastInsets: WindowInsets? = null

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!,
        attrs
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context!!, attrs, defStyle)

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (!ObjectsCompat.equals(lastInsets, insets)) {
            lastInsets = insets
            requestLayout()
        }
        return insets.consumeSystemWindowInsets()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (lastInsets != null) {
            children.forEach { child ->
                if (child.visibility != View.GONE) {
                    child.dispatchApplyWindowInsets(lastInsets)
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}