package com.sarcoordinator.sarsolutions.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


// Singleton class to access common methods
object GlobalUtil {

    fun convertEpochToDate(epochDate: Long): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            .format(Date(Timestamp(epochDate * 1000).time))
    }

    // Source: https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard
    fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view: View? = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }
}