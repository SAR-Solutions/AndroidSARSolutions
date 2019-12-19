package com.sarcoordinator.sarsolutions

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)

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
        // Set loginFragment and caseFragment as top-level destinations to prevent showing back
        //  on these screens
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.loginFragment, R.id.casesFragment))

        appbar.setupWithNavController(navController, appBarConfiguration)

        if (savedInstanceState == null) {
            val auth = FirebaseAuth.getInstance()
            val graph = navController.graph

            // If user is logged in, navigate to case list fragment
            if (auth.currentUser != null) {
                graph.startDestination = R.id.casesFragment
                navController.graph = graph
            }
        }
    }
}

