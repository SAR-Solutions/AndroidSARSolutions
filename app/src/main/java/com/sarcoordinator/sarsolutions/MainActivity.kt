package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.GlobalUtil.THEME_LIGHT
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val BACKSTACK = "BACKSTACK"
    private val TABSTACK = "TABSTACK"
    private val FRAGMENT_STATES_MAP = "FRAGMENT_STATES_MAP"

    // Required for the navigation the navigation component
    // to prevent saving fragment state on activity close
    private var endActivity: Boolean = false

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

        // Disable testing mode for release variant
        if (!BuildConfig.DEBUG) {
            sharedPrefs = getPreferences(Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putBoolean(SettingsTabFragment.TESTING_MODE_PREFS, false)
                commit()
            }
        }

        nav = Navigation.getInstance(
            supportFragmentManager,
            bottom_nav_bar
        ) { hide -> hideBottomNavBar(hide) }

        if (savedInstanceState != null) {

            // Recover from process death
            savedInstanceState.getSerializable(BACKSTACK)?.let {
                nav.setBackStack(it as HashMap<*, *>)
            }

            savedInstanceState.getSerializable(TABSTACK)?.let {
                val temp = Stack<Navigation.TabIdentifiers>()
                if(it is Stack<*>) {
                    temp.addAll(it as Stack<Navigation.TabIdentifiers>)
                } else {
                    temp.addAll(it as ArrayList<Navigation.TabIdentifiers>)
                }
                nav.setTabStack(temp)
            }

            savedInstanceState.getSerializable(FRAGMENT_STATES_MAP)?.let {
                nav.setFragmentStateMap(it as HashMap<String, Fragment.SavedState?>)
            }

        }

//       Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        // Navigate to login screen if user isn't logged in
        if (auth.currentUser == null) {
            // Hide nav bar in login fragment
            hideBottomNavBar(true)
            parent_layout.transitionToState(R.id.hide_nav_bar)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
            parent_layout.transitionToState(R.id.hide_nav_bar)
        } else {
            if (savedInstanceState == null) {
                nav.selectTab(Navigation.TabIdentifiers.HOME)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(BACKSTACK, nav.getBackStack())
        outState.putSerializable(TABSTACK, nav.getTabStack())
        outState.putSerializable(FRAGMENT_STATES_MAP, nav.getFragmentStateMap())
    }

    override fun onPause() {
        super.onPause()
        if (!endActivity)
            nav.saveCurrentFragmentState()
    }

    override fun onDestroy() {
        super.onDestroy()
        nav.onFragmentManagerDestroy()
    }

    override fun onBackPressed() {
        if (auth.currentUser == null) {
            super.onBackPressed()
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)!!
            // If shift is active and current tab is 'home', prevent from going back
            if (nav.currentTab == Navigation.TabIdentifiers.HOME &&
                currentFragment !is ImageDetailFragment
                && viewModel.isShiftActive
            ) {
                Snackbar.make(
                    currentFragment.requireView(),
                    "Complete shift to go back",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (currentFragment is ImageDetailFragment &&
                viewModel.isUploadTaskActive
            ) {
                Snackbar.make(
                    currentFragment.requireView(),
                    "Image upload in progress",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (viewModel.syncInProgress) {
                Snackbar.make(
                    currentFragment.requireView(),
                    "Shift sync in progress",
                    Snackbar.LENGTH_LONG
                ).show()
            } else if (!nav.popFragment()) {
                finishAffinity()
            }
        }
    }

    // Handle bottom nav bar state change
    private fun hideBottomNavBar(hide: Boolean) {
        val currentTheme =
            GlobalUtil.getCurrentTheme(resources, getPreferences(Context.MODE_PRIVATE))
        if (currentTheme == GlobalUtil.THEME_DARK) {
            window.navigationBarColor =
                resources.getColor(R.color.lightGray)
        } else if (currentTheme == GlobalUtil.THEME_LIGHT) {
            window.navigationBarColor =
                resources.getColor(android.R.color.white)
        }
        parent_layout.setTransitionDuration(500)
        parent_layout.transitionToState(if (hide) R.id.hide_nav_bar else R.id.show_nav_bar)
    }

    // Load user preferences using shared preferences
    // NOTE: Call before super.onCreate as it sets the app theme
    private fun loadUserPreferences() {
        sharedPrefs = getPreferences(Context.MODE_PRIVATE)

        // Get system default theme
        GlobalUtil.setCurrentTheme(sharedPrefs)
        if (GlobalUtil.getCurrentTheme(resources, sharedPrefs) == GlobalUtil.THEME_DARK) {

            // Set navigationBarColor to elevated gray
            window.navigationBarColor = resources.getColor(R.color.lightGray)
        }
    }

    fun enableTransparentStatusBar(enableTransparency: Boolean) {
        restoreSystemBars()
        window.apply {
            val currentTheme =
                GlobalUtil.getCurrentTheme(resources, getPreferences(Context.MODE_PRIVATE))
            if (enableTransparency) {
                clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                )

                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                statusBarColor = Color.TRANSPARENT

                // If theme is light, show light navigation bar icons
                if (currentTheme == THEME_LIGHT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        decorView.systemUiVisibility += View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        statusBarColor = Color.BLACK
                        navigationBarColor = Color.BLACK
                    }
                }
            } else {
                restoreSystemBars()
            }
        }
    }

    fun enableTransparentSystemBars(enableTransparency: Boolean) {
        restoreSystemBars()
        window.apply {
            if(enableTransparency) {
                setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                addFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                )
                if (GlobalUtil.getCurrentTheme(
                        resources,
                        getPreferences(Context.MODE_PRIVATE)
                    ) == THEME_LIGHT
                )
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                restoreSystemBars()
            }
        }
    }

    private fun restoreSystemBars() {
        val currentTheme =
            GlobalUtil.getCurrentTheme(resources, getPreferences(Context.MODE_PRIVATE))

        window.apply {
            // Clear previously set flags
            decorView.systemUiVisibility = 0
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            // Restore system bar colors
            if (currentTheme == GlobalUtil.THEME_DARK) {
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

