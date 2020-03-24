package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.CacheDatabase
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    private lateinit var nav: Navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme needs to be applied before calling super.onCreate
        // Otherwise a new instance of this activity will be created
        loadUserPreferences()
        super.onCreate(savedInstanceState)

//        val t =
//            getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/Test2")?.listFiles()?.asList()
//        t?.let {
//            it.forEach { file ->
//                val image = ExifInterface(file.absolutePath)
//                image.setAttribute("ImageDescription", "This was successful")
//                image.saveAttributes()
//                Timber.d("File contains ${image.getAttribute("ImageDescription")}")
//            }
//        }

        nav = Navigation.getInstance(supportFragmentManager, bottom_nav_bar) { hide ->
            parent_layout.setTransitionDuration(500)
            parent_layout.transitionToState(if (hide) R.id.hide_nav_bar else R.id.show_nav_bar)
        }

        val repo = LocalCacheRepository(CacheDatabase.getDatabase(application).casesDao())
        repo.allShiftReports.observeForever {
            it.forEach { obj ->
                obj.locationList
            }
        }

//         Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        if (savedInstanceState == null) {
            // Navigate to login screen if user isn't logged in
            if (auth.currentUser == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
                parent_layout.transitionToState(R.id.hide_nav_bar)
            } else {
                nav.setSelectedTab(Navigation.BackStackIdentifiers.HOME)
                parent_layout.transitionToState(R.id.show_nav_bar)
            }
        }
    }

    override fun onBackPressed() {
        if (auth.currentUser == null) {
            super.onBackPressed()
        } else {
            // If shift is active and current tab is 'home', prevent from going back
            if (nav.currentTab == Navigation.BackStackIdentifiers.HOME &&
                nav.getCurrentFragment() !is ImageDetailFragment && viewModel.isShiftActive
            ) {
                Snackbar.make(
                    nav.getCurrentFragment().requireView(),
                    "Complete shift to go back",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (nav.getCurrentFragment() is ImageDetailFragment &&
                viewModel.isUploadTaskActive
            ) {
                Snackbar.make(
                    nav.getCurrentFragment().requireView(),
                    "Image upload in progress",
                    Snackbar.LENGTH_LONG
                )
                    .show()
            } else if (!nav.handleOnBackPressed()) {
                finishAffinity()
            }
        }
    }

    // Load user preferences using shared preferences
    // NOTE: Call before super.onCreate as it sets the app theme
    private fun loadUserPreferences() {
        sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        // Get system default theme
        GlobalUtil.setCurrentTheme(sharedPrefs, resources)
        if (GlobalUtil.getTheme(sharedPrefs, resources) == GlobalUtil.THEME_DARK) {

            // Set navigationBarColor to elevated gray
            window.navigationBarColor = resources.getColor(R.color.lightGray)
        }
    }

    fun enableStatusBarColorForNestedFragment() {
        //TODO: Compelte implementing this
        if (GlobalUtil.getTheme(
                getPreferences(Context.MODE_PRIVATE),
                resources
            ) == GlobalUtil.THEME_DARK
        ) {
            window.statusBarColor = resources.getColor(R.color.lightGray)
        }
    }

    fun enableTransparentStatusBar(enableTransparency: Boolean) {
        window.apply {
            if (enableTransparency) {
                clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                )

                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                statusBarColor = Color.TRANSPARENT

                // If theme is light, show light navigation bar icons
                if (GlobalUtil.getTheme(sharedPrefs, resources) == GlobalUtil.THEME_LIGHT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        navigationBarColor = Color.BLACK
                    }
                }
            } else {
                // Clear previously set flags
                decorView.systemUiVisibility = 0

                // Restore system bar colors
                if (GlobalUtil.getTheme(sharedPrefs, resources) == GlobalUtil.THEME_DARK) {
                    // Set navigationBarColor to elevated gray
                    navigationBarColor = resources.getColor(R.color.lightGray)
                    statusBarColor = resources.getColor(R.color.gray)
                } else {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    statusBarColor = Color.WHITE
                    navigationBarColor = Color.WHITE
                }
            }
        }
    }
}

