package com.sarcoordinator.sarsolutions

import android.Manifest
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Black status bar for old android version
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            window.statusBarColor = Color.BLACK

        setContentView(R.layout.activity_main)

        // Ask for locational permission and handle response
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    return
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied)
                    finish()
                    else {
                        Toast.makeText(
                            applicationContext,
                            "Permission not granted",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()

        val navController = findNavController(R.id.nav_host_fragment)

        if (savedInstanceState == null) {
            // If user is logged in, navigate to case list fragment
            if (FirebaseAuth.getInstance().currentUser != null) {
                navController.navigate(LoginFragmentDirections.actionLoginFragmentToCasesFragment())
            }
        }

        setSupportActionBar(appbar)

        // Set loginFragment and caseFragment as top-level destinations to prevent showing back
        //  on these screens
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.loginFragment, R.id.casesFragment))
        appbar.setupWithNavController(navController, appBarConfiguration)

        // Set toolbar title depending on current destination
        supportActionBar?.title = navController.currentDestination?.label
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toggle_theme -> {
                // Get current theme
                when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

