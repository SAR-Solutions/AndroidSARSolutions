package com.sarcoordinator.sarsolutions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.LocationService
import kotlinx.android.synthetic.main.fragment_track.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class TrackFragment : Fragment(R.layout.fragment_track) {

    private var service: LocationService? = null
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var viewModel: SharedViewModel

    private lateinit var currentShiftId: String

    private val args by navArgs<TrackFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Restore state depending on view model
        restoreState()

        // Only fetch data if detailed case isn't in cache
        if (!viewModel.currentCase.value?.id.equals(args.caseId)) {
            enableLoadingState(true)
            viewModel.currentCase.value = null
            viewModel.getCaseDetails(args.caseId).observe(viewLifecycleOwner, Observer { case ->
                if (case != null) {
                    populateViewWithCase(case)
                    enableLoadingState(false)
                }
            })
        } else {
            populateViewWithCase(viewModel.currentCase.value!!)
        }

        location_service_fab.setOnClickListener {
            if (viewModel.getBinder().value == null) { // Start new service
                requestLocPermission()
            } else { // Stop ongoing service
                stopLocationService()
                enableButtons()

                CoroutineScope(IO).launch {
                    // Delay till shiftId is fetched
                    //TODO: Show loading status of some kind
                    while (!::currentShiftId.isInitialized)
                        delay(1000)
                    withContext(Main) {
                        findNavController().navigate(
                            TrackFragmentDirections.actionTrackFragmentToShiftReportFragment(
                                currentShiftId
                            )
                        )
                    }
                }
            }
        }

        viewModel.getBinder().observe(viewLifecycleOwner, Observer { binder ->
            // Either service was bound or unbound
            service = binder?.getService()
            observeService()
        })
    }

    private fun enableLoadingState(enable: Boolean) {
        if (enable) {
            track_fragment_shimmer.visibility = View.VISIBLE
            case_info_material_card.visibility = View.GONE
            location_service_fab.hide()
        } else {
            track_fragment_shimmer.visibility = View.GONE
            case_info_material_card.visibility = View.VISIBLE
            location_service_fab.show()
        }
    }

    // Set case information
    private fun populateViewWithCase(case: Case) {
        (requireActivity() as MainActivity).supportActionBar?.title = case.id
        id_value_tv.text = case.id
        reporter_value_tv.text = case.reporterName
        missing_person_value_tv.text = listToOrderedList(case.missingPersonName)
        equipment_value_tv.text = listToOrderedList(case.equipmentUsed)
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
                        .setPositiveButton(getString(R.string.ok)) { _, _ -> token.continuePermissionRequest() }
                        .setOnCancelListener { token.cancelPermissionRequest() }
                        .show()
                }

            })
            .withErrorListener {
                Timber.e("Unexpected error requesting location permission")
                Toast.makeText(context, "Unexpected error, try again", Toast.LENGTH_LONG).show()
            }
            .onSameThread()
            .check()
    }

    // Update UI by observing viewModel data
    private fun observeService() {
        service?.let {
            it.getLastUpdated().observe(viewLifecycleOwner, Observer { lastUpdated ->
                location_desc.text = lastUpdated
            })
            it.getShiftId().observe(viewLifecycleOwner, Observer { shiftId ->
                currentShiftId = shiftId
            })
        }
    }

    private fun startLocationService() {
        location_desc.text = getString(R.string.starting_location_service)
        // Pass required extras and start location service
        val serviceIntent = Intent(context, LocationService::class.java)
        serviceIntent.putExtra(
            LocationService.isTestMode,
            sharedPrefs.getBoolean(SettingsFragment.TESTING_MODE_PREFS, false)
        )
        serviceIntent.putExtra(
            LocationService.case,
            viewModel.currentCase.value
        )
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
        viewModel.lastUpdatedText = location_desc.text.toString()
        unbindService()
    }

    // Restore view state on configuration change
    private fun restoreState() {
        if (viewModel.getBinder().value != null) {
            disableButtons()
            // Service is alive and running but needs to be bound back to activity
            bindService()
            location_desc.text = viewModel.lastUpdatedText
        }
    }

    private fun disableButtons() {
        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_stop_24,
                requireContext().theme
            )
        )
        location_service_fab.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.error))
    }

    private fun enableButtons() {
        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_stop_24,
                requireContext().theme
            )
        )
        location_service_fab.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.orange))
    }

    // Convert list of string to a ordered string
    private fun listToOrderedList(list: List<String>): String {
        if (list.count() <= 0)
            return "None"
        return if (list.count() == 1) {
            list[0]
        } else {
            missing_person_tv.text = getString(R.string.missing_people)
            var text = ""
            for (i in 0 until list.count() - 1) {
                text += "${list[i]}\n"
            }
            text += list[list.count() - 1]
            text
        }
    }
}

