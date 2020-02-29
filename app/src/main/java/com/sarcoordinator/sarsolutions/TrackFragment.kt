package com.sarcoordinator.sarsolutions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.LocationService
import dev.chrisbanes.insetter.doOnApplyWindowInsets
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
    private var isRetryNetworkFab = false

    private val args by navArgs<TrackFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.doOnApplyWindowInsets { view, insets, initialState ->
            view.updatePadding(
                top = initialState.paddings.top + insets.systemWindowInsetTop,
                bottom = initialState.paddings.bottom
            )
        }

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        NavigationUI.setupWithNavController(toolbar, findNavController())

//         Main activity handles back navigation
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        location_service_fab.hide()
        initFabClickListener()
        validateNetworkConnectivity()
    }


    override fun onResume() {
        super.onResume()
        // Rebind service if an instance of a service exists
        if (viewModel.isShiftActive.value == true)
            bindService()
    }

    override fun onPause() {
        super.onPause()
        viewModel.lastUpdatedText = location_desc.text.toString()
        // Keep service running but detach from viewmodel
        unbindService()
    }

    // Restore view state on configuration change
    private fun restoreState() {
        if (viewModel.isShiftActive.value == true) {
            // Service is alive and running but needs to be bound back to activity
            bindService()
            enableStopTrackingFab()
            location_desc.text = viewModel.lastUpdatedText
        }
    }

    private fun validateNetworkConnectivity() {
        // If service is already running, disregard network state
        if (viewModel.isShiftActive.value == null || viewModel.isShiftActive.value == false) {
            if (!GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), requireView())) {
                enableRetryNetworkState()
                return
            }
        }
        // If coming from retry network fab, change case info card visibility and desc text
        case_info_material_card.visibility = View.VISIBLE
        location_desc.text = getString(R.string.start_tracking_desc)

        setupInterface()
    }

    private fun setupInterface() {

        // Only fetch data if detailed case isn't in cache
        if (!viewModel.currentCase.value?.id.equals(args.caseId)) {
            enableLoadingState(true)
            viewModel.currentCase.value = null
            viewModel.getCaseDetails(args.caseId).observe(viewLifecycleOwner, Observer { case ->
                if (case != null) {
                    populateViewWithCase(case)
                    enableLoadingState(false)
                    enableStartTrackingFab()
                }
            })
        } else {
            // Fetch from cache
            populateViewWithCase(viewModel.currentCase.value!!)
            enableStartTrackingFab()

            // Restore state depending on view model
            restoreState()
        }


        viewModel.getBinder().observe(viewLifecycleOwner, Observer { binder ->
            // Either service was bound or unbound
            service = binder?.getService()
            observeService()
        })
    }

    private fun initFabClickListener() {
        location_service_fab.setOnClickListener {
            if (isRetryNetworkFab) {
                isRetryNetworkFab = false
                validateNetworkConnectivity()
            } else {
                // Start new service, if there isn't a service running already
                if (viewModel.isShiftActive.value == null || viewModel.isShiftActive.value == false) {
                    requestLocPermission()
                } else {
                    enableStartTrackingFab()
                    service?.completeShift()

                    location_service_fab.isEnabled = false

                    // Init, set and start circular progress bar
                    val progressCircle = CircularProgressDrawable(requireContext()).apply {
                        strokeWidth = 10f
                    }
                    location_service_fab.setImageDrawable(progressCircle)
                    progressCircle.start()

                    location_desc.text = getString(R.string.waiting_for_server)

                    // Delay till shiftId is fetched
                    CoroutineScope(IO).launch {
                        while (!::currentShiftId.isInitialized)
                            delay(1000)

                        // Observe service and shut down once it has finished syncing
                        withContext(Main) {
                            service?.isServiceSyncRunning()?.observe(viewLifecycleOwner, Observer {
                                if (!it) {
                                    // Stop ongoing service
                                    stopLocationService()

                                    findNavController().navigate(
                                        TrackFragmentDirections.actionTrackFragmentToShiftReportFragment(
                                            currentShiftId
                                        )
                                    )
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    // Disable shimmer, show case info layout; vice-versa
    private fun enableLoadingState(enable: Boolean) {
        if (enable) {
            track_fragment_shimmer.visibility = View.VISIBLE
            case_info_material_card.visibility = View.GONE
        } else {
            track_fragment_shimmer.visibility = View.GONE
            case_info_material_card.visibility = View.VISIBLE
        }
    }

    private fun enableStartTrackingFab() {
        location_service_fab.show()

        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_play_arrow_24,
                requireContext().theme
            )
        )

        location_service_fab.backgroundTintList = resources.getColorStateList(R.color.newBlue)
    }

    private fun enableStopTrackingFab() {
        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_stop_24,
                requireContext().theme
            )
        )
        location_service_fab.backgroundTintList = resources.getColorStateList(R.color.orange)
    }

    private fun enableRetryNetworkState() {
        isRetryNetworkFab = true
        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_refresh_24,
                requireContext().theme
            )
        )
        location_service_fab.backgroundTintList = resources.getColorStateList(R.color.newRed)
        location_service_fab.visibility = View.VISIBLE

        case_info_material_card.visibility = View.GONE
        location_desc.text = getString(R.string.no_network_desc)
    }

    // Set case information
    private fun populateViewWithCase(case: Case) {
        (requireActivity() as MainActivity).supportActionBar?.title = case.id
        id_value_tv.text = case.id
        reporter_value_tv.text = case.reporterName
        missing_person_value_tv.text = listToOrderedListString(case.missingPersonName)
        equipment_value_tv.text = listToOrderedListString(case.equipmentUsed)
        toolbar.title = case.caseName
    }

    private fun requestLocPermission() {
        // Ask for locational permission and handle response
        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
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
            it.getServiceInfo().observe(viewLifecycleOwner, Observer { lastUpdated ->
                location_desc.text = lastUpdated
            })
            it.getShiftId().observe(viewLifecycleOwner, Observer { shiftId ->
                currentShiftId = shiftId
            })

            // Handle shift errors
            it.hasShiftEndedWithError().observe(viewLifecycleOwner, Observer { error ->
                error?.let {
                    enableStartTrackingFab()
                    when (error) {
                        LocationService.ShiftErrors.START_SHIFT -> {
                            Timber.e("Shift failed to start")
                            stopLocationService()
                            viewModel.completeShiftReportSubmission()
                            validateNetworkConnectivity()
                        }
                        LocationService.ShiftErrors.PUT_LOCATIONS -> {
                            Timber.e("All locations could not posted")
                            viewModel.addLocationsToCache(service!!.getSyncList(), currentShiftId)
                        }
                        LocationService.ShiftErrors.PUT_END_TIME -> {
                            Timber.e("Posting end time failed")
                            viewModel.addEndTimeToCache(service!!.getEndTime()!!, currentShiftId)
                        }
                        else -> Timber.e("Unhandled shift error")
                    }
                }
            })
        }
    }

    // Starts service, calls bindService and enableStopTrackingFab
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
        enableStopTrackingFab()
    }

    // Stops service and calls unbindService
    private fun stopLocationService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        unbindService()
        activity?.stopService(serviceIntent)
        // Deletes reference to binder in viewmodel
        // Will not be re-bind on configuration change
        // NOTE: Must call unbindService() before removeService()
        viewModel.removeService()
    }

    // Attach viewmodel to running service
    private fun bindService() {
        val serviceIntent = Intent(context, LocationService::class.java)
        activity?.bindService(
            serviceIntent,
            viewModel.getServiceConnection(),
            Context.BIND_AUTO_CREATE
        )
    }

    // Detach viewmodel from running service
    private fun unbindService() {
        if (viewModel.getBinder().value != null)
            activity?.unbindService(viewModel.getServiceConnection())
    }

    // Convert list of string to a ordered string
    private fun listToOrderedListString(list: List<String>): String {
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

fun View.setMargins(
    leftMarginDp: Int? = null,
    topMarginDp: Int? = null,
    rightMarginDp: Int? = null,
    bottomMarginDp: Int? = null
) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        leftMarginDp?.run { params.leftMargin = this.dpToPx(context) }
        topMarginDp?.run { params.topMargin = this.dpToPx(context) }
        rightMarginDp?.run { params.rightMargin = this.dpToPx(context) }
        bottomMarginDp?.run { params.bottomMargin = this.dpToPx(context) }
        requestLayout()
    }
}

fun Int.dpToPx(context: Context): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics).toInt()
}

