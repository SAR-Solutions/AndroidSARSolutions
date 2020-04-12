package com.sarcoordinator.sarsolutions

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sarcoordinator.sarsolutions.adapters.ImagesAdapter
import com.sarcoordinator.sarsolutions.custom_views.LargeInfoView
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.LocationService
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.card_case_details.view.*
import kotlinx.android.synthetic.main.fragment_track.*
import kotlinx.android.synthetic.main.view_circular_button.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class TrackFragment : Fragment(), OnMapReadyCallback {
    companion object ArgsTags {
        const val CASE_ID = "CASE_ID"
        const val LOCATION_TRACKING_STATUS = "LOCATION_TRACKING_STATUS"
    }

    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    private val REQUEST_IMAGE_CAPTURE = 1

    private var enableMap: Boolean = true

    private val nav: Navigation by lazy { Navigation.getInstance() }
    private var service: LocationService? = null
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var viewModel: SharedViewModel
    private var isRetryNetworkFab = false
    private lateinit var caseId: String
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: ImagesAdapter
    private var stopLocationTracking = false

    // Strictly only for mapView
    private val isLocationServiceRunning = MutableLiveData<Boolean>().also {
        it.postValue(false)
    }

    private var markedListIndexPointer = 0
    private var locSet: MutableSet<LatLng> = mutableSetOf()
    private var lastLocPoint: LatLng? = null

    private var mMapView: MapView? = null
    private lateinit var bottomSheet: BottomSheetBehavior<MaterialCardView>

    private val fabHiddenListener = object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) {
            Timber.d("FAB Hidden")
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
            capture_photo_fab.hide()
        }
    }

    private val fabShownListener = object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) {
            Timber.d("FAB Shown")
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
            capture_photo_fab.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Exit if location is not enabled
        if (!GlobalUtil.isLocationEnabled(requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {

            MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_baseline_gps_off_24)
                .setTitle(getString(R.string.gps_disabled))
                .setMessage(getString(R.string.enable_gps_prompt))
                .setNegativeButton(getString(R.string.back)) { dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.dismiss()
                    nav.popFragment()
                }
                .setPositiveButton(getString(R.string.enable_gps)) { dialogInterface: DialogInterface, i: Int ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    dialogInterface.dismiss()
                    nav.popFragment()
                }
                .setOnCancelListener {
                    nav.popFragment()
                }
                .show()
        }

        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Get arguments / Restore state
        caseId = arguments?.getString(CASE_ID) ?: savedInstanceState?.getString(CASE_ID)!!
        savedInstanceState?.getBoolean(LOCATION_TRACKING_STATUS)?.let {
            stopLocationTracking = it
        }

        // Set shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track, container, false)

        mMapView = view.findViewById(R.id.map)

        enableMap = !sharedPrefs.getBoolean(SettingsTabFragment.LOW_BANDWIDTH_PREFS, false)

        if (enableMap) {
            // Required map setup
            var mapViewBundle: Bundle? = null
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
            }
            mMapView?.onCreate(mapViewBundle)
            mMapView?.getMapAsync(this)
        } else {
            mMapView?.visibility = View.GONE
            view.findViewById<LargeInfoView>(R.id.low_bandwidth_layout).visibility = View.VISIBLE
        }

        bottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.case_info_card))

        view.findViewById<FloatingActionButton>(R.id.capture_photo_fab).hide()
        view.findViewById<FloatingActionButton>(R.id.location_service_fab).hide()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewInsets()
        setupCircularButtons()
        initFabClickListener()
        initCaseInfoBottomSheet()
        validateNetworkConnectivity()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CASE_ID, caseId)
        outState.putBoolean(LOCATION_TRACKING_STATUS, stopLocationTracking)

        if (enableMap) {
            // Required Map callback
            var mapViewBundle =
                outState.getBundle(MAPVIEW_BUNDLE_KEY)
            if (mapViewBundle == null) {
                mapViewBundle = Bundle()
                outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
            }
            mMapView!!.onSaveInstanceState(mapViewBundle)
        }
    }

    private fun setupCircularButtons() {
        back_button_view.image_button.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_arrow_back_24))
        back_button_view.image_button.setOnClickListener { requireActivity().onBackPressed() }
        info_button_view.image_button.setOnClickListener {
            bottomSheet.state = when (bottomSheet.state) {
                BottomSheetBehavior.STATE_HIDDEN -> BottomSheetBehavior.STATE_HALF_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                else -> BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupViewInsets() {
        back_button_view.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
        info_button_view.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
        info_view_layout.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
        case_info_card.parent_layout.doOnApplyWindowInsets { view, insets, initialState ->
//            bottomSheet.peekHeight = bottomSheet.peekHeight + insets.systemGestureInsets.bottom
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
        location_service_fab.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }
    }

    private fun validateNetworkConnectivity() {
        // If service is already running, disregard network state
        if (!viewModel.isShiftActive) {
            if (!GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), case_info_card)) {
                enableRetryNetworkState()
                return
            }
        }
        // If coming from retry network fab, change case info card visibility and desc text
        case_info_card.visibility = View.VISIBLE
        shift_info_text_view.text = getString(R.string.start_tracking_desc)

        setupInterface()

        // Stop fab was clicked already but fragment was detached
        if (stopLocationTracking) {
            stopLocationService()
            completeShiftAndStopService()
            return
        }

        // Restore state depending on view model
        if (viewModel.isShiftActive) {
            // Service is alive and running but needs to be bound back to activity
            bindService()
            enableStopTrackingFab()
        }
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
            enableLoadingState(false)
            enableStartTrackingFab()
        }

        viewModel.getBinder().observe(viewLifecycleOwner, Observer { binder ->
            // Either service was bound or unbound
            service = binder?.getService()
            if (service != null) {
                isLocationServiceRunning.postValue(true)
                observeService()
            }
        })
    }

    private fun initFabClickListener() {
        location_service_fab.setOnClickListener {
            // Retry for network connectivity
            if (isRetryNetworkFab) {
                isRetryNetworkFab = false
                validateNetworkConnectivity()
            } else {
                // Start new service, if there isn't a service running already
                if (!viewModel.isShiftActive) {
                    stopLocationTracking = false
                    requestLocPermission()
                } else {
                    // Stop ongoing service
                    stopLocationTracking = true
                    completeShiftAndStopService()
                }
            }
        }

        // Capture image
        capture_photo_fab.setOnClickListener {
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

    // Bottom Sheet stuff
    private fun initCaseInfoBottomSheet() {
        bottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    Toast.makeText(
                        requireContext(),
                        "Click on the info button to see case information",
                        Toast.LENGTH_LONG
                    ).show()
            }
        })
    }

    private fun completeShiftAndStopService() {
        // Change view state
        enableStartTrackingFab()
        location_service_fab.isClickable = false
        val progressCircle = CircularProgressDrawable(requireContext()).apply {
            strokeWidth = 10f
        }
        location_service_fab.setImageDrawable(progressCircle)
        progressCircle.start()
        shift_info_text_view.text = getString(R.string.waiting_for_server)

        if (service != null) {
            service?.completeShift()?.invokeOnCompletion {
                lifecycleScope.launch(IO) {

                    // Delay till shiftId is fetched
                    while (viewModel.currentShiftId.isNullOrEmpty())
                        delay(1000)

                    withContext(Main) {
                        if(context != null) {
                            stopLocationService()
                            navigateToShiftReportFragment()
                        }
                    }
                }
            }
        } else {
            // Service is not running
            navigateToShiftReportFragment()
        }
    }

    private fun navigateToShiftReportFragment() {
        viewModel.numberOfVehicles = 0
        nav.pushFragment(ShiftReportFragment(), Navigation.TabIdentifiers.HOME)
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
        viewAdapter = ImagesAdapter(
            nav,
            viewModel.getImageList(externalCaseImageDir).value ?: ArrayList()
        )
        case_info_card.image_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        // Observe and populate list on change
        viewModel.getImageList().observe(viewLifecycleOwner, Observer {
            val size = it.size
            case_info_card.image_number_text_view.text = size.toString()
            if (size == 0) {
                case_info_card.image_recycler_view.visibility = View.GONE
                case_info_card.no_images_view.visibility = View.VISIBLE
            } else {
                case_info_card.image_recycler_view.visibility = View.VISIBLE
                case_info_card.no_images_view.visibility = View.GONE
                viewAdapter.setData(it)
            }
        })
    }

    // Disable shimmer, show case info layout; vice-versa
    private fun enableLoadingState(enable: Boolean) {
        if (enable) {
            case_info_card.track_fragment_shimmer.visibility = View.VISIBLE
            case_info_card.case_info_layout.visibility = View.GONE
        } else {
            case_info_card.track_fragment_shimmer.visibility = View.GONE
            case_info_card.case_info_layout.visibility = View.VISIBLE
        }
    }

    private fun enableStartTrackingFab() {
        capture_photo_fab.hide()
        location_service_fab.show()
        location_service_fab.removeOnShowAnimationListener(fabShownListener)
        location_service_fab.removeOnHideAnimationListener(fabHiddenListener)

        location_service_fab.setImageDrawable(
            resources.getDrawable(
                R.drawable.ic_baseline_play_arrow_24,
                requireContext().theme
            )
        )

        location_service_fab.backgroundTintList = resources.getColorStateList(R.color.newBlue)
    }

    private fun enableStopTrackingFab() {
        capture_photo_fab.show()
        location_service_fab.addOnShowAnimationListener(fabShownListener)
        location_service_fab.addOnHideAnimationListener(fabHiddenListener)

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

        case_info_card.case_info_layout.visibility = View.GONE
        shift_info_text_view.text = getString(R.string.no_network_desc)
    }

    // Set case information
    private fun populateViewWithCase(case: Case) {
        enableStartTrackingFab()
        case_info_card.case_id.text = case.id
        case_info_card.reporter_name.text = case.reporterName
        case_info_card.missing_person.text = listToOrderedListString(case.missingPersonName)
        case_info_card.equipment_used.text = listToOrderedListString(case.equipmentUsed)
        case_info_card.case_title.text = case.caseName
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
                shift_info_text_view.text = lastUpdated
            })
            it.getShiftId().observe(viewLifecycleOwner, Observer { shiftId ->
                viewModel.currentShiftId = shiftId
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
                            viewModel.addLocationsToCache(service!!.getListOfUnsyncedLocations())
                        }
                        LocationService.ShiftErrors.PUT_END_TIME -> {
                            Timber.e("Posting end time failed")
                            viewModel.addEndTimeToCache(service!!.getEndTime()!!)
                        }
                        LocationService.ShiftErrors.GET_SHIFT_ID -> {
                            Timber.e("Getting shift id failed")
                            Toast.makeText(requireContext(), "Failed to start shift", Toast.LENGTH_LONG).show()
                        }
                        else -> Timber.e("Unhandled shift error")
                    }
                }
            })
        }
    }

    // Starts service, calls bindService and enableStopTrackingFab
    private fun startLocationService() {
        shift_info_text_view.text = getString(R.string.starting_location_service)
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
        unbindService()
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
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
        if (viewModel.isServiceBound) {
            viewModel.isServiceBound = false
            activity?.unbindService(viewModel.getServiceConnection())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            val imagePath = viewModel.currentImagePath

            if (resultCode == Activity.RESULT_CANCELED) {
                // Delete the file that was made
                File(imagePath).delete()
            } else if (resultCode == Activity.RESULT_OK) {
                viewModel.addImagePathToList(imagePath)
            }
        }
    }

    // Convert list of string to a ordered string
    private fun listToOrderedListString(list: List<String>): String {
        if (list.count() <= 0)
            return "None"
        return if (list.count() == 1) {
            list[0]
        } else {
            case_info_card.missing_person.text = getString(R.string.missing_people)
            var text = ""
            for (i in 0 until list.count() - 1) {
                text += "${list[i]}\n"
            }
            text += list[list.count() - 1]
            text
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        GlobalUtil.setGoogleMapsTheme(requireActivity(), googleMap)
        isLocationServiceRunning.observe(viewLifecycleOwner, Observer {
            if (it) {
                // Observe locations
                service?.getAllLocations()?.observe(viewLifecycleOwner, Observer { locationList ->
                    if (locationList.isNotEmpty()) {
                        val locationListToDraw =
                            locationList.subList(markedListIndexPointer, locationList.size)
                        markedListIndexPointer = locationList.size

                        locationListToDraw.forEach { location ->
                            val latLng =
                                LatLng(location.latitude, location.longitude)
                            if (locSet.add(latLng)) {
                                // Show starting marker and focus on it
                                if (lastLocPoint == null) {
                                    googleMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            latLng,
                                            15F
                                        )
                                    )
                                    googleMap.addMarker(
                                        MarkerOptions().position(latLng)
                                            .title(getString(R.string.Start))
                                    )
                                } else {
                                    val polyLine =
                                        GlobalUtil.getThemedPolyLineOptions(requireContext())
                                    polyLine.add(lastLocPoint, latLng)
                                    googleMap.addPolyline(polyLine)
                                }
                            } else {
                                Timber.d("Location already exists")
                            }
                            lastLocPoint = latLng
                        }
                    }
                })
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbindService()
    }

    override fun onResume() {
        super.onResume()
        if (enableMap)
            mMapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        nav.hideBottomNavBar?.let { it(true) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)
        if (enableMap)
            mMapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        if (enableMap)
            mMapView?.onStop()
    }

    override fun onPause() {
        if (enableMap)
            mMapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (enableMap) {
            mMapView?.onDestroy()
            mMapView = null
        }
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (enableMap)
            mMapView?.onLowMemory()
    }
}
