package com.example.sarsolutions

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.example.sarsolutions.services.LocationService
import com.google.android.gms.location.LocationCallback
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    private lateinit var locationCallback: LocationCallback
    private var currentShiftId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private lateinit var locationService: LocationService

    private val connection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            // Observe and update location string in view
            locationService.lastUpdated.observe(viewLifecycleOwner,
                Observer<String> { t -> location_id.text = t })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    private lateinit var viewModel: CasesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this)[CasesViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Restore state depending on view model
        restoreState()

        sign_out_button.setOnClickListener {
            auth.signOut()
            view?.findNavController()?.navigate(R.id.action_mainFragment_to_loginFragment)
        }

        // Toggle test mode
        test_button.setOnClickListener {
            if (viewModel.isTestingEnabled) { // Disable Testing
                viewModel.isTestingEnabled = false
                test_button.text = "Enable Test Mode"
                test_button.setBackgroundColor(resources.getColor(R.color.warning))
            } else { // Enable Testing
                viewModel.isTestingEnabled = true
                test_button.text = "Disable Test Mode"
                test_button.setBackgroundColor(resources.getColor(R.color.error))
            }
        }

        start_button.setOnClickListener {
            if(!LocationService.Status.isRunning) {
                disableButtons()
                startLocationService()
            }
            else {
                stopLocationService()
                enableButtons()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        serviceIntent.putExtra(LocationService.isTestMode, viewModel.isTestingEnabled)
        LocationService.Status.isRunning = true
        ContextCompat.startForegroundService(context!!, serviceIntent)
        activity?.bindService(serviceIntent, connection, 0)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        LocationService.Status.isRunning = false
        activity?.let {
            it.unbindService(connection)
            it.stopService(serviceIntent)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.lastUpdatedText = location_id.text.toString()
    }

    // Restore view state on configuration changes
    private fun restoreState() {
        if (LocationService.Status.isRunning) {
            disableButtons()
            // Find and rebind service
            val serviceIntent = Intent(context, LocationService::class.java)
            activity?.bindService(serviceIntent, connection, 0)

            location_id.text = viewModel.lastUpdatedText
        }

        if (viewModel.isTestingEnabled) {
            test_button.text = "Disable Test Mode"
            test_button.setBackgroundColor(resources.getColor(R.color.error))
        }
    }

    private fun disableButtons() {
        start_button.text = getString(R.string.stop)
        test_button.isEnabled = false
        sign_out_button.isEnabled = false
        start_button.setBackgroundColor(resources.getColor(R.color.error))
    }

    private fun enableButtons() {
        start_button.text = getString(R.string.start)
        test_button.isEnabled = true
        sign_out_button.isEnabled = true
        start_button.setBackgroundColor(resources.getColor(R.color.success))
    }

}
