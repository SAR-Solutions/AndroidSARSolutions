package com.example.sarsolutions

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(appbar)

        // Starting off in login fragment where appbar needs to be hidden
        appbar.visibility = View.INVISIBLE

        val auth = FirebaseAuth.getInstance()
        val navController = findNavController(R.id.nav_host_fragment)
        val graph = navController.graph

        // If user is logged in, navigate to case list fragment
        if (auth.currentUser != null) {
            graph.startDestination = R.id.casesFragment
            navController.graph = graph
        }

        appbar.setupWithNavController(navController, AppBarConfiguration(navController.graph))

        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.casesFragment)
                appbar.visibility = View.VISIBLE
            else if (destination.id == R.id.loginFragment)
                appbar.visibility = View.INVISIBLE
        }
    }
}

