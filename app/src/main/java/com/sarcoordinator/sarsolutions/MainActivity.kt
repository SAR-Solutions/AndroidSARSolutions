package com.sarcoordinator.sarsolutions

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
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
        super.onCreate(savedInstanceState)

        // Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.nav_host_fragment)

        if (savedInstanceState == null) {
            // If user is logged in, navigate to case list fragment
            if (auth.currentUser != null) {
                navController.navigate(LoginFragmentDirections.actionLoginFragmentToCasesFragment())
            }
        }

        setSupportActionBar(toolbar)

        // Set loginFragment and caseFragment as top-level destinations to prevent showing back
        //  on these screens
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.loginFragment, R.id.casesFragment))

        // Don't use the following:
        // toolbar.setupWithNavController(navController, appBarConfiguration)
        // Reason : https://stackoverflow.com/questions/55904485/custom-navigate-up-behavior-for-certain-fragment-using-navigation-component
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Set toolbar title depending on current destination
        supportActionBar?.title = navController.currentDestination?.label

//        navController.addOnDestinationChangedListener { controller, destination, arguments ->
//            when(destination.id) {
//                R.id.loginFragment -> {
//                    oncreateop
//                }
//            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.cases_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.toggle_theme -> {
//                // Get current theme
//                when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
//                    Configuration.UI_MODE_NIGHT_YES -> {
//                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                    }
//                    Configuration.UI_MODE_NIGHT_NO -> {
//                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                    }
//                }
//            }
            R.id.sign_out -> {
                // Navigation is handled within fragments by overriding onOptionsItemSelected
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Don't allow navigating back if LocationService is running
        return if (viewModel.getBinder().value != null) {
            Snackbar.make(parent_layout, "Click the 'stop' button to go back", Snackbar.LENGTH_LONG)
                .show()
            true
        } else
            navController.navigateUp()
    }
}

