package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var navController: NavController

    private val viewModel: SharedViewModel by lazy {
        ViewModelProviders.of(this)[SharedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme needs to be applied before calling super.onCreate
        // Otherwise a new instance of this activity will be created
        loadUserPreferences()
        super.onCreate(savedInstanceState)


        // Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        navController = findNavController(R.id.nav_host_fragment)

        if (savedInstanceState == null) {
            // If user is logged in, navigate to case list fragment
            if (auth.currentUser != null) {
                navController.navigate(LoginFragmentDirections.actionLoginFragmentToCasesFragment())
            }
        }

        // Set loginFragment and caseFragment as top-level destinations to prevent showing back
        //  on these screens
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.loginFragment, R.id.casesFragment))

        // Don't use the following:
        // toolbar.setupWithNavController(navController, appBarConfiguration)
        // Reason : https://stackoverflow.com/questions/55904485/custom-navigate-up-behavior-for-certain-fragment-using-navigation-component
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Set toolbar title depending on current destination
        supportActionBar?.title = navController.currentDestination?.label

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.cases_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out -> {
                if (viewModel.getBinder().value == null) {
                    auth.signOut()
                    navController.navigate(
                        R.id.loginFragment,
                        null,
                        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    )
                } else {
                    Snackbar.make(parent_layout, "Stop tracking to sign out", Snackbar.LENGTH_LONG)
                        .show()
                }
                true
            }

            R.id.settings -> {
                if (viewModel.getBinder().value == null) {
                    navController.navigate(R.id.settingFragment)
                } else {
                    Snackbar.make(
                        parent_layout,
                        "Click the 'stop' button to go to settings",
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Don't allow navigating back if LocationService is running
        if (viewModel.getBinder().value != null) {
            Snackbar.make(parent_layout, "Click the 'stop' button to go back", Snackbar.LENGTH_LONG)
                .show()
            return true
        } else if (!navController.popBackStack()) {
            return navController.navigateUp()
        }
        return true
    }

    override fun onBackPressed() {
        if (!onSupportNavigateUp())
            super.onBackPressed()
    }

    // Load user preferences using shared preferences
    // NOTE: Call before super.onCreate as it sets the app theme
    private fun loadUserPreferences() {
        val sharedPrefs = getPreferences(Context.MODE_PRIVATE)

        // Get system default theme
        val defaultTheme: String =
            this.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK).let {
                when (it) {
                    Configuration.UI_MODE_NIGHT_YES -> SettingsFragment.THEME_DARK
                    Configuration.UI_MODE_NIGHT_NO -> SettingsFragment.THEME_LIGHT
                    else -> SettingsFragment.THEME_DEFAULT
                }
            }

        // Set theme
        when (sharedPrefs.getString(SettingsFragment.THEME_PREFS, defaultTheme)) {
            SettingsFragment.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SettingsFragment.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

