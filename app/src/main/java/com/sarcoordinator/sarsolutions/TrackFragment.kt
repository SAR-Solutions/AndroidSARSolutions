package com.sarcoordinator.sarsolutions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sarcoordinator.sarsolutions.services.LocationService
import kotlinx.android.synthetic.main.fragment_track.*
import timber.log.Timber

class TrackFragment : Fragment() {

    private lateinit var locationCallback: LocationCallback
    private var currentShiftId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private lateinit var locationService: LocationService
    private var service: LocationService? = null

    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_track, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Restore state depending on view model
        restoreState()

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
            if (viewModel.getBinder().value == null) { // Start new service
                requestLocPermission()
            }
            else { // Stop ongoing service
                stopLocationService()
                enableButtons()
            }
        }

        // Rough flow: startButton --> startLocationServices() --> viewModel binder
        viewModel.getBinder().observe(viewLifecycleOwner, Observer { binder ->
            // Either service was bound or unbound
            service = binder?.getService()
            observeService()
        })
    }

    private fun requestLocPermission() {
        // Ask for locational permission and handle response
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    disableButtons()
                    startLocationService()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied)
                        MaterialAlertDialogBuilder(context)
                            .setTitle("Permission Denied")
                            .setMessage("Location permission is needed to use this feature")
                            .setNegativeButton(getString(R.string.cancel), null)
                            .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                                // Open settings
                                val settingsIntent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID))
                                startActivity(settingsIntent)
                            }
                            .show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Location Permission Required")
                        .setMessage("Location permission is needed to use this feature")
                        .setPositiveButton(getString(R.string.ok)) {_, _ -> token.continuePermissionRequest()}
                        .setOnCancelListener { token.cancelPermissionRequest() }
                        .show()
                }

            })
            .withErrorListener {
                Timber.e("Unexpected error requesting permission")
                Toast.makeText(context, "Unexpected error, try again", Toast.LENGTH_LONG).show()
            }
            .check()
    }

    private fun observeService() {
        service?.getLastUpdated()?.observe(viewLifecycleOwner, Observer { lastUpdated ->
            location_id.text = lastUpdated
        })
    }

    private fun startLocationService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        serviceIntent.putExtra(LocationService.isTestMode, viewModel.isTestingEnabled)
        ContextCompat.startForegroundService(context!!, serviceIntent)
        bindService()
    }

    private fun bindService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        activity?.bindService(
            serviceIntent,
            viewModel.getServiceConnection(),
            Context.BIND_AUTO_CREATE
        )
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        unbindService()
        activity?.stopService(serviceIntent)
        // Deletes reference to binder in viewmodel
        // Will not be re-bind on configuration change
        // NOTE: Must call unbindService() before removeService()
        viewModel.removeService()
    }

    private fun unbindService() {
        if (viewModel.getBinder().value != null)
            activity?.unbindService(viewModel.getServiceConnection())
    }

    override fun onResume() {
        super.onResume()
        // Rebind service if an instance of a service exists
        if (viewModel.getBinder().value != null)
            bindService()
    }

    override fun onPause() {
        super.onPause()
        viewModel.lastUpdatedText = location_id.text.toString()
        unbindService()
    }

    // Restore view state on configuration change
    private fun restoreState() {
        if (viewModel.getBinder().value != null) {
            disableButtons()
            // Service is alive and running but needs to be bound back to activity
            bindService()
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
        start_button.setBackgroundColor(resources.getColor(R.color.error))
    }

    private fun enableButtons() {
        start_button.text = getString(R.string.start)
        test_button.isEnabled = true
        start_button.setBackgroundColor(resources.getColor(R.color.success))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                if (viewModel.getBinder().value == null) {
                    auth.signOut()
                    findNavController().navigate(TrackFragmentDirections.actionTrackFragmentToLoginFragment())
                } else {
                    Snackbar.make(view!!, "Stop tracking to sign out", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

