package com.sarcoordinator.sarsolutions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.transition.TransitionInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.sarcoordinator.sarsolutions.util.ISharedElementFragment
import com.sarcoordinator.sarsolutions.util.LocationService
import com.sarcoordinator.sarsolutions.util.Navigation
import kotlinx.android.synthetic.main.fragment_track.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class TrackFragment : Fragment(R.layout.fragment_track), ISharedElementFragment {

    companion object ArgsTags {
        const val CASE_ID = "CASE_ID"
    }

    private val REQUEST_IMAGE_CAPTURE = 1
    private val nav: Navigation = Navigation.getInstance()
    private var service: LocationService? = null
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var viewModel: SharedViewModel
    private lateinit var currentShiftId: String
    private var isRetryNetworkFab = false
    private lateinit var caseId: String
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: ImagesAdapter

    override fun getSharedElement(): View? = toolbar_track

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        caseId = arguments?.getString(CASE_ID)!!

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        location_service_fab.hide()
        initFabClickListener()
        validateNetworkConnectivity()

        toolbar_track.setBackPressedListener(View.OnClickListener { requireActivity().onBackPressed() })
    }

    private fun validateNetworkConnectivity() {
        // If service is already running, disregard network state
        if (!viewModel.isShiftActive) {
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
        if (!viewModel.currentCase.value?.id.equals(caseId)) {
            enableLoadingState(true)
            viewModel.currentCase.value = null
            viewModel.getCaseDetails(caseId).observe(viewLifecycleOwner, Observer { case ->
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
                if (!viewModel.isShiftActive) {
                    requestLocPermission()
                } else {
                    enableStartTrackingFab()
                    service?.completeShift()

                    location_service_fab.isClickable = false

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

                                    val shiftReportFragment = ShiftReportFragment().apply {
                                        arguments = Bundle().apply {
                                            putString(ShiftReportFragment.SHIFT_ID, currentShiftId)
                                        }
                                    }

                                    nav.pushFragment(null, shiftReportFragment)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    // Setup everything related to the images card
    private fun setupImagesCardView() {
        val externalCaseImageDir = requireActivity()
            .getExternalFilesDir(
                Environment.DIRECTORY_PICTURES +
                        "/${viewModel.currentCase.value!!.caseName}"
            )?.listFiles()?.asList()

        // Setup image recycler view
        viewManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        viewAdapter = ImagesAdapter(nav, viewModel.getImageList(externalCaseImageDir).value!!)
        image_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        // Observe and populate list on change
        viewModel.getImageList().observe(viewLifecycleOwner, Observer {
            val size = it.size
            image_number_text_view.text = size.toString()
            viewAdapter.setData(it)
        })

        // Capture image
        capture_photo.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.resolveActivity(requireActivity().packageManager)?.also {

                    val caseName = viewModel.currentCase.value!!.caseName
                    // Create intent to request for camera app launch
                    val photoFile = GlobalUtil.createImageFile(
                        caseName,
                        requireActivity().getExternalFilesDir(
                            Environment.DIRECTORY_PICTURES +
                                    "/$caseName/"
                        )!!
                    )

                    photoFile.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            "sarcoordinator.sarsolutions.provider",
                            it
                        )

                        viewModel.currentImagePath = photoFile.absolutePath
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
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
        toolbar_track.setHeading(case.caseName)
        setupImagesCardView()
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
            sharedPrefs.getBoolean(SettingsTabFragment.TESTING_MODE_PREFS, false)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imagePath = viewModel.currentImagePath
            viewModel.addImagePathToList(imagePath)
        }
    }

    override fun onResume() {
        super.onResume()
        // Rebind service if an instance of a service exists
        if (viewModel.isShiftActive)
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
        if (viewModel.isShiftActive) {
            // Service is alive and running but needs to be bound back to activity
            bindService()
            enableStopTrackingFab()
            location_desc.text = viewModel.lastUpdatedText
        }
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
