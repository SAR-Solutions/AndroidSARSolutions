package com.sarcoordinator.sarsolutions.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.R
import timber.log.Timber
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


// Singleton class to access common methods
object GlobalUtil {

    private const val THEME_PREFS = "THEME_PREFS"
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_DEFAULT = 2

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

    /**
     * Returns true and false depending on network connection availability
     * If false, shows a snackbar with reference to passed view with error message
     * Snackbar will not be shown is showSnackbar is set to false
     */
    fun isNetworkConnectivityAvailable(
        activity: Activity,
        view: View?,
        showSnackbar: Boolean = true
    ): Boolean {
        val cm =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return try {
            if (cm.activeNetworkInfo.isConnected) {
                true
            } else {
                Timber.e("No network connectivity")
                if (view != null && showSnackbar)
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
            if (view != null && showSnackbar)
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

    // Sets theme based on given string
    // Also saves theme in shared preferences
    // String must be one of the THEME_* constants; else an error is thrown
    fun setTheme(sharedPref: SharedPreferences?, theme: Int) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_DEFAULT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> throw Exception("Invalid argument passed to setThemePref: argument = $theme")
        }
        sharedPref?.let {
            with(it.edit()) {
                putString(THEME_PREFS, theme.toString())
                commit()
            }
        }
    }

    fun getTheme(preferences: SharedPreferences, resources: Resources): Int {
        val def = preferences.getString(THEME_PREFS, getThemeMode(resources).toString())!!
        return try {
            def.toInt()
        } catch (exception: NumberFormatException) {
            THEME_DEFAULT
        }
    }

    // Returns whether theme is light or dark
    fun getThemeMode(resources: Resources): Int {
        resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK).let {
            return when (it) {
                Configuration.UI_MODE_NIGHT_YES -> THEME_DARK
                Configuration.UI_MODE_NIGHT_NO -> THEME_LIGHT
                else -> -1
            }
        }
    }

    fun setCurrentTheme(sharedPrefs: SharedPreferences, resources: Resources) {
        val def = getTheme(sharedPrefs, resources)
        setTheme(sharedPrefs, def)
    }

    // Create file with unique name and returns File
    fun createImageFile(caseName: String, storageDir: File): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HH:mm:ss").format(Date())
        return File.createTempFile("${caseName}_${timeStamp}", ".jpg", storageDir)
    }
}

// Notify observers of change; adding item to list doesn't notify observers
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}
