package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.sarcoordinator.sarsolutions.util.CacheDatabase
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.LocalCacheRepository
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefs: SharedPreferences

    private val viewModel: SharedViewModel by lazy {
        ViewModelProvider(this)[SharedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme needs to be applied before calling super.onCreate
        // Otherwise a new instance of this activity will be created
        loadUserPreferences()
        super.onCreate(savedInstanceState)

        val repo = LocalCacheRepository(CacheDatabase.getDatabase(application).casesDao())
        repo.allShiftReports.observeForever {
            it.forEach { obj ->
                obj.locationList
            }
        }

        // Disable navbar while shift is active
        viewModel.isShiftActive.observe(this, Observer {
            it?.let {
                bottom_nav_bar.menu.forEach { menuItem ->
                    menuItem.isEnabled = !it
                }
            }
        })

//         Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        if (savedInstanceState == null) {
            // Navigate to login screen if user isn't logged in
            if (auth.currentUser == null) {
                navigateToLoginScreen()
            } else {
                navigateToCasesScreen()
            }
        }

        bottom_nav_bar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home_dest -> {
                    val casesFragment = CasesFragment()
                    navigateToFragment(casesFragment, true)
                }
                R.id.failed_shifts_dest -> {
                    val failedShiftsFragment = FailedShiftsFragment()
                    navigateToFragment(failedShiftsFragment, true)
                }
                R.id.settings_dest -> {
                    val settingsFragment = SettingsFragment()
                    navigateToFragment(settingsFragment, true)
                }
            }
            return@setOnNavigationItemSelectedListener true
        }
    }


    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            if(addToBackStack)
                addToBackStack(null)
        }.commit()
    }

    fun navigateToLoginScreen() {
        val loginFragment = LoginFragment()
        navigateToFragment(loginFragment, false)
        bottom_nav_bar.visibility = View.GONE
    }

    fun navigateToCasesScreen() {
        val casesFragment = CasesFragment()
        navigateToFragment(casesFragment, false)
        bottom_nav_bar.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        // Don't allow navigating back if LocationService is running
        if (viewModel.isShiftActive.value == true) {
            Snackbar.make(parent_layout, "Complete shift to go back", Snackbar.LENGTH_LONG)
                .show()
            return true
        } else if(supportFragmentManager.backStackEntryCount > 0){
            supportFragmentManager.popBackStack()
            return true
        }
        return false
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
        if (GlobalUtil.getTheme(sharedPrefs, resources) == GlobalUtil.THEME_DARK) {
            // Set navigationBarColor to elevated gray
            window.navigationBarColor = Color.parseColor("#2D2D2D")
            window.statusBarColor = resources.getColor(R.color.gray)

        }
    }
}

