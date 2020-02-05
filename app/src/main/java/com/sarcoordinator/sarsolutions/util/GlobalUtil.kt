package com.sarcoordinator.sarsolutions.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.R
import timber.log.Timber
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

    fun isNetworkConnectivityAvailable(activity: Activity, view: View?): Boolean {
        val cm =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            if (cm.activeNetworkInfo.isConnected) {
                true
            } else {
                Timber.e("Not network connectivity")
                if (view != null)
                    Snackbar.make(
                        view,
                        "Device isn't connected to the internet",
                        Snackbar.LENGTH_LONG
                    )
                        .setBackgroundTint(
                            ContextCompat.getColor(
                                activity.applicationContext,
                                R.color.error
                            )
                        )
                        .show()
                false
            }
        } catch (e: Exception) {
            Timber.e("Error validating network connectivity:\n$e")
            if (view != null)
                Snackbar.make(view, "Error validating network connectivity", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(
                        ContextCompat.getColor(
                            activity.applicationContext,
                            R.color.error
                        )
                    )
                    .show()
            false
        }
    }
}