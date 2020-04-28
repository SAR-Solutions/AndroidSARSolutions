package com.sarcoordinator.sarsolutions

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
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
import com.google.android.material.transition.MaterialFade
import com.sarcoordinator.sarsolutions.adapters.ImagesAdapter
import com.sarcoordinator.sarsolutions.custom_views.LargeInfoView
import com.sarcoordinator.sarsolutions.models.Case
import com.sarcoordinator.sarsolutions.util.*
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.card_case_details.view.*
import kotlinx.android.synthetic.main.fragment_track.*
import kotlinx.android.synthetic.main.view_circular_button.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class TrackFragment : Fragment(), OnMapReadyCallback {

    private lateinit var locationServiceManager: LocationServiceManager

    companion object ArgsTags {
        const val CASE_ID = "CASE_ID"
        const val LOCATION_TRACKING_STATUS = "LOCATION_TRACKING_STATUS"
    }

    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    private val REQUEST_IMAGE_CAPTURE = 1

    private lateinit var viewModel: SharedViewModel
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: ImagesAdapter
    private lateinit var sharedPrefs: SharedPreferences

    private var enableMap: Boolean = true

    private val nav: Navigation by lazy { Navigation.getInstance() }
    private var isRetryNetworkFab = false
    private lateinit var caseId: String
    private var stopLocationTracking = false

    private var markedListIndexPointer = 0
    private var locSet: MutableSet<LatLng> = mutableSetOf()
    private var lastLocPoint: LatLng? = null

    private var mMapView: MapView? = null
    private lateinit var mGoogleMap: GoogleMap

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

        locationServiceManager =
            LocationServiceManager.getInstance(requireActivity() as MainActivity)


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

        // Set shared element transitions
        sharedElementEnterTransition = TransitionInflater.from(context)
            .inflateTransition(android.R.transition.move)

        enterTransition = MaterialFade.create(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track, container, false)

        bottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.case_info_card))

        mMapView = view.findViewById(R.id.map)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capture_photo_fab.hide()
//        location_service_fab.hide()

        enableMap = !sharedPrefs.getBoolean(TabSettingsFragment.LOW_BANDWIDTH_PREFS, false)

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

        // Observe location service status and update UI accordingly
        locationServiceManager.getServiceStatusObservable().observe(viewLifecycleOwner, Observer {
            if (it) {
                // Active
                enableStopTrackingFab()
                observeService()
            } else {
                // Inactive
                enableStartTrackingFab()
            }
        })

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

    private fun setupViewInsets() {
        back_button_view.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom
            )
        }
        info_button_view.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom
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
            bottomSheet.peekHeight = insets.systemGestureInsets.bottom + 200
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

    private fun initFabClickListener() {
        location_service_fab.setOnClickListener {
            // Retry for network connectivity
            if (isRetryNetworkFab) {
                isRetryNetworkFab = false
                validateNetworkConnectivity()
            } else {
                // Start new service, if there isn't a service running already
                if (!locationServiceManager.getServiceStatus()) {
                    stopLocationTracking = false
                    startLocationService()
                } else {
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

    private fun validateNetworkConnectivity() {

        // If service is not running, enable retry network state
        if (!locationServiceManager.getServiceStatus()) {
            if (!GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), case_info_card)) {
                enableRetryNetworkState()
                return
            }
        } else {
            // If service is already running, disregard network state
            // Service is running
            enableStopTrackingFab()
        }

        // If coming from retry network fab, change case info card visibility and desc text
        case_info_card.visibility = View.VISIBLE
        shift_info_text_view.text = getString(R.string.start_tracking_desc)

        setupInterface()

        // Stop fab was clicked already but fragment was detached
        if (stopLocationTracking) {
            completeShiftAndStopService()
            return
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
        return
    }

    private fun completeShiftAndStopService() {
        // Change view state
        location_service_fab.isClickable = false
        val progressCircle = CircularProgressDrawable(requireContext()).apply {
            strokeWidth = 10f
        }

        location_service_fab.setImageDrawable(progressCircle)
        progressCircle.start()
        shift_info_text_view.text = getString(R.string.waiting_for_server)

        lifecycleScope.launch(IO) {
            // Wait for server to respond with current shift id
            while (viewModel.currentShiftId.isNullOrEmpty())
                delay(1000)

            lifecycleScope.launch(Main) {
                locationServiceManager.stopLocationService().observe(viewLifecycleOwner, Observer {
                    if (it) {
                        viewModel.numberOfVehicles = 0
                        nav.pushFragment(ShiftReportFragment(), Navigation.TabIdentifiers.HOME)
                    }
                })
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
        viewAdapter = ImagesAdapter(
            nav,
            viewModel.getImageList(externalCaseImageDir).value ?: ArrayList()
        )
        case_info_card.parent_layout.image_recycler_view.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        // Observe and populate list on change
        viewModel.getImageList().observe(viewLifecycleOwner, Observer {
            val size = it.size
            case_info_card.parent_layout.image_number_text_view.text = size.toString()
            if (size == 0) {
                case_info_card.parent_layout.image_recycler_view.visibility = View.GONE
                case_info_card.parent_layout.no_images_view.visibility = View.VISIBLE
            } else {
                case_info_card.parent_layout.image_recycler_view.visibility = View.VISIBLE
                case_info_card.parent_layout.no_images_view.visibility = View.GONE
                viewAdapter.setData(it)
            }
        })
    }

    // Disable shimmer, show case info layout; vice-versa
    private fun enableLoadingState(enable: Boolean) {
        if (enable) {
            case_info_card.shimmer_parent_layout.visibility = View.VISIBLE
            case_info_card.parent_layout.visibility = View.GONE
        } else {
            case_info_card.shimmer_parent_layout.visibility = View.GONE
            case_info_card.parent_layout.visibility = View.VISIBLE
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

        val primColor = getThemeColor(requireContext(), R.attr.colorPrimary)
        location_service_fab.backgroundTintList = ColorStateList.valueOf(primColor)
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

        case_info_card.shimmer_parent_layout.visibility = View.GONE
        case_info_card.parent_layout.visibility = View.GONE
        shift_info_text_view.text = getString(R.string.no_network_desc)
    }

    // Set case information
    private fun populateViewWithCase(case: Case) {
        enableStartTrackingFab()
        case_info_card.parent_layout.case_id.text = case.id
        case_info_card.parent_layout.reporter_name.text = case.reporterName
        case_info_card.parent_layout.missing_person.text =
            listToOrderedListString(case.missingPersonName)
        case_info_card.parent_layout.equipment_used.text =
            listToOrderedListString(case.equipmentUsed)
        case_info_card.parent_layout.case_title.text = case.caseName
        setupImagesCardView()
    }

    // Update UI by observing viewModel data
    private fun observeService() {
        locationServiceManager.getShiftIdObservable().observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.currentShiftId = it
            }
        })

        locationServiceManager.getShiftInfoObservable()
            .observe(viewLifecycleOwner, Observer { lastUpdated ->
                shift_info_text_view.text = lastUpdated
            })

        locationServiceManager.getShiftErrorsObservable()
            .observe(viewLifecycleOwner, Observer { error ->
                error?.let {
                    enableStartTrackingFab()
                    when (error) {
                        LocationService.ShiftErrors.START_SHIFT -> {
                            Timber.e("Shift failed to start")
                            locationServiceManager.stopLocationService()
                            viewModel.completeShiftReportSubmission()
                            validateNetworkConnectivity()
                        }
                        LocationService.ShiftErrors.PUT_LOCATIONS -> {
                            Timber.e("All locations could not posted")
                            viewModel.addLocationsToCache(locationServiceManager.getUnsyncedLocationLists())
                        }
                        LocationService.ShiftErrors.PUT_END_TIME -> {
                            Timber.e("Posting end time failed")
                            viewModel.addEndTimeToCache(locationServiceManager.getEndTime())
                        }
                        LocationService.ShiftErrors.GET_SHIFT_ID -> {
                            Timber.e("Getting shift id failed")
                            Toast.makeText(
                                requireContext(),
                                "Failed to start shift",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> Timber.e("Unhandled shift error")
                    }
                }
            })

        if (enableMap) {
            locationServiceManager.getLocationListObservable()
                .observe(viewLifecycleOwner, Observer { locationList ->
                    if (::mGoogleMap.isInitialized)
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
                                        mGoogleMap.moveCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                latLng,
                                                15F
                                            )
                                        )
                                        mGoogleMap.addMarker(
                                            MarkerOptions().position(latLng)
                                                .title(getString(R.string.Start))
                                        )
                                    } else {
                                        val polyLine =
                                            GlobalUtil.getThemedPolyLineOptions(requireContext())
                                        polyLine.add(lastLocPoint, latLng)
                                        mGoogleMap.addPolyline(polyLine)
                                    }
                                } else {
                                    Timber.d("Location already exists")
                                }
                                lastLocPoint = latLng
                            }
                        }
                })
        }
    }

    // Starts service
    private fun startLocationService() {
        val obs = locationServiceManager.startLocationService(
            sharedPrefs.getBoolean(TabSettingsFragment.TESTING_MODE_PREFS, false),
            viewModel.currentCase.value!!
        )
        obs.observe(viewLifecycleOwner, Observer {
            if (it != -1) {
                if (it == 1) {
                    viewModel.isShiftActive = true
                    shift_info_text_view.text = getString(R.string.starting_location_service)
                } else if (it == 0) {
                    Timber.e("Loc permission denied")
                }
                // Remove observer after handling result
                obs.removeObservers(viewLifecycleOwner)
            }
        })
    }

    // Get image from activity result
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
            case_info_card.parent_layout.missing_person.text = getString(R.string.missing_people)
            var text = ""
            for (i in 0 until list.count() - 1) {
                text += "${list[i]}\n"
            }
            text += list[list.count() - 1]
            text
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        GlobalUtil.setGoogleMapsTheme(requireActivity(), googleMap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationServiceManager.unbindService()
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
