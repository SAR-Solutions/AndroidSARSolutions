@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.sarcoordinator.sarsolutions.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.LocationManager
import android.net.ConnectivityManager
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.snackbar.Snackbar
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.SettingsTabFragment
import timber.log.Timber
import java.io.ByteArrayOutputStream
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


    /**
     * Returns true and false depending on if location service is enabled
     */
    fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Sets theme based on given string
     * Also saves theme in shared preferences
     * String must be one of the THEME_* constants; else an error is thrown
     */
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

    fun getThemePreference(preferences: SharedPreferences): Int {
        val def = preferences.getString(THEME_PREFS, THEME_DEFAULT.toString())!!
        return try {
            def.toInt()
        } catch (exception: NumberFormatException) {
            THEME_DEFAULT
        }
    }

    // Returns whether theme is light or dark
    fun getCurrentTheme(resources: Resources, preferences: SharedPreferences): Int {
        val themePreference = getThemePreference(preferences)
        if (themePreference == THEME_DEFAULT) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> return THEME_LIGHT
                Configuration.UI_MODE_NIGHT_YES -> return THEME_DARK
            }
            return -1
        } else {
            return themePreference
        }
    }

    fun setCurrentTheme(sharedPrefs: SharedPreferences) {
        val def = getThemePreference(sharedPrefs)
        setTheme(sharedPrefs, def)
    }

    // Create file with unique name and returns File
    fun createImageFile(caseName: String, storageDir: File): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HH:mm:ss").format(Date())
        return File.createTempFile("${caseName}_${timeStamp}", ".jpg", storageDir)
    }

    private fun getPrimaryColorFromTheme(context: Context): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, value, true)
        return value.data
    }

    // Returns byte array for image after fixing orientation
    fun fixImageOrientation(imagePath: String, orientation: Int): ByteArray {
        var bitmapImage = BitmapFactory.decodeFile(imagePath)
        val matrix = Matrix()
        when (orientation) {
            6 -> {
                matrix.postRotate(90F)
            }
            3 -> {
                matrix.postRotate(180F)
            }
            8 -> {
                matrix.postRotate(270F)
            }
        }
        bitmapImage = Bitmap.createBitmap(
            bitmapImage,
            0,
            0,
            bitmapImage.width,
            bitmapImage.height,
            matrix,
            true
        )

        val baos = ByteArrayOutputStream()
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray()
    }

    fun setGoogleMapsTheme(activity: Activity, googleMap: GoogleMap) {
        val isDarkTheme =
            getCurrentTheme(
                activity.resources,
                activity.getPreferences(Context.MODE_PRIVATE)
            ) == THEME_DARK

        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)

        // Enable dark map if current theme is dark
        if (isDarkTheme) {
            when (sharedPref.getString(
                SettingsTabFragment.MAP_DARK_THEME_PREFS,
                SettingsTabFragment.MapDarkThemes.STANDARD.name
            )) {
                SettingsTabFragment.MapDarkThemes.STANDARD.name -> setMapsTheme(
                    activity,
                    googleMap,
                    R.raw.dark_std_map_theme
                )
                SettingsTabFragment.MapDarkThemes.Night.name -> setMapsTheme(
                    activity,
                    googleMap,
                    R.raw.dark_night_map_theme
                )
            }
        } else {
            val currentLightTheme = sharedPref.getString(
                SettingsTabFragment.MAP_LIGHT_THEME_PREFS,
                SettingsTabFragment.MapLightThemes.STANDARD.name
            )
            if (currentLightTheme == SettingsTabFragment.MapLightThemes.SNOW.name) {
                setMapsTheme(activity, googleMap, R.raw.light_snow_map_theme)
            }
        }
    }

    fun getThemedPolyLineOptions(context: Context): PolylineOptions =
        PolylineOptions().apply {
            endCap(RoundCap())
            jointType(JointType.ROUND)
            color(getPrimaryColorFromTheme(context))
        }

    private fun setMapsTheme(activity: Activity, googleMap: GoogleMap, themeId: Int) {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    activity, themeId
                )
            )
            if (!success) {
                Timber.e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Timber.e("Can't find style. Error: ", e)
        }
    }
}

// Notify observers of change; adding item to list doesn't notify observers
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}

fun View.setMargins(l: Int, t: Int, r: Int, b: Int) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val p = layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(l, t, r, b)
        requestLayout()
    }
}

