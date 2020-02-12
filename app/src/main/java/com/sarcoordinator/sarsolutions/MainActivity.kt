package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var navController: NavController
    private lateinit var sharedPrefs: SharedPreferences

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme needs to be applied before calling super.onCreate
        // Otherwise a new instance of this activity will be created
        loadUserPreferences()
        super.onCreate(savedInstanceState)

        // Disable navbar while shift is active
        viewModel.isShiftActive.observe(this, Observer {
            it?.let {
                bottom_nav_bar.menu.forEach { menuItem ->
                    menuItem.isEnabled = !it
                }
            }
        })

        // Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        navController = findNavController(R.id.nav_host_fragment)

        if (savedInstanceState == null) {
            if (auth.currentUser == null) {
                navController.navigate(
                    R.id.loginFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                )
            }
        }

        NavigationUI.setupWithNavController(bottom_nav_bar, navController)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            // Avoid showing bottom navigation view on login and reset password screens
            if (destination.id == R.id.loginFragment
                || destination.id == R.id.resetPasswordFragment
            )
                bottom_nav_bar.visibility = View.GONE
            else
                bottom_nav_bar.visibility = View.VISIBLE

            // If user is logged in, navigate to cases screen
            if (destination.id == R.id.loginFragment && auth.currentUser != null)
                controller.navigate(LoginFragmentDirections.actionLoginFragmentToCasesFragment())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Don't allow navigating back if LocationService is running
        if (viewModel.isShiftActive.value == true) {
            Snackbar.make(parent_layout, "Complete shift to go back", Snackbar.LENGTH_LONG)
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
        sharedPrefs = getPreferences(Context.MODE_PRIVATE)
        // Get system default theme
        GlobalUtil.setCurrentTheme(sharedPrefs, resources)
    }
}

