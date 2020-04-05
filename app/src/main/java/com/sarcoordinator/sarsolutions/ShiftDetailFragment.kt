package com.sarcoordinator.sarsolutions

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.GlobalUtil.THEME_DARK
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import kotlinx.android.synthetic.main.card_shift_details.*
import kotlinx.android.synthetic.main.card_shift_details.view.*
import kotlinx.android.synthetic.main.fragment_shift_detail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ShiftDetailFragment : Fragment(), OnMapReadyCallback {

    companion object ARGS {
        val CACHED_SHIFT = "CACHED_SHIFT"
        private val INFO_CARD = "InfoCard"
    }

    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

    private lateinit var viewModel: SharedViewModel
    private var isInfoCardVisible = false
    private val nav: Navigation by lazy { Navigation.getInstance() }
    private lateinit var cachedShiftReport: LocationsInShiftReport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[SharedViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shift_detail, container, false)

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }

        mMapView = view.findViewById(R.id.map)
        mMapView?.onCreate(mapViewBundle)
        mMapView?.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cachedShiftReport =
            (arguments?.getSerializable(CACHED_SHIFT) ?: savedInstanceState?.getSerializable(
                CACHED_SHIFT
            )) as LocationsInShiftReport

        populateShiftInfoCard()

        savedInstanceState?.getBoolean(INFO_CARD)?.let {
            isInfoCardVisible = it
        }

        setupViewState()

        sync_button.setOnClickListener {
            sync_button.isEnabled = false

            if (GlobalUtil.isNetworkConnectivityAvailable(requireActivity(), requireView())) {
                progress_bar.visibility = View.VISIBLE
                viewModel.submitShiftReportFromCache(
                    cachedShiftReport,
                    resources.getStringArray(R.array.vehicle_array).toList()
                )
                    .invokeOnCompletion {
                        CoroutineScope(Dispatchers.Main).launch {
                            nav.popFragment()
                        }
                    }
            } else {
                progress_bar.visibility = View.GONE
                sync_button.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "No internet connection available",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    private fun populateShiftInfoCard() {
        info_card.case_title.text = cachedShiftReport.shiftReport.caseName
        info_card.cache_time.text = cachedShiftReport.shiftReport.cacheTime
        info_card.end_time.text = cachedShiftReport.shiftReport.endTime
        info_card.search_duration.text = cachedShiftReport.shiftReport.searchDuration.plus(" hours")
    }

    private fun setupViewState() {

        val parentLayout = requireView().findViewById<ConstraintLayout>(R.id.shift_detail_parent)

        var constraintSet1 = ConstraintSet()
        constraintSet1.clone(requireContext(), R.layout.fragment_shift_detail)
        var constraintSet2 = ConstraintSet()
        constraintSet2.clone(requireContext(), R.layout.fragment_shift_detail_alt)

        nav.hideBottomNavBar?.let { it(true) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)

        back_button.setOnApplyWindowInsetsListener { v, insets ->
            back_button.setMargins(
                insets.systemGestureInsets.left, insets.systemGestureInsets.top
                , v.marginRight, v.marginBottom
            )
            insets
        }

        info_button.setOnApplyWindowInsetsListener { v, insets ->
            info_button.setMargins(
                v.marginLeft, insets.systemGestureInsets.top,
                insets.systemGestureInsets.right, v.marginBottom
            )
            insets
        }

        back_button.setOnClickListener {
            requireActivity().onBackPressed()
        }

        info_button.setOnClickListener {
            var constraintSet = constraintSet2
            if (isInfoCardVisible) {
                constraintSet = constraintSet1
                isInfoCardVisible = false
            } else {
                isInfoCardVisible = true
            }
            constraintSet = setConstraintSetInsets(constraintSet)

            TransitionManager.beginDelayedTransition(parentLayout)
            constraintSet.applyTo(parentLayout)
        }
    }

    private fun setConstraintSetInsets(constraintSet: ConstraintSet): ConstraintSet {
        val insets = requireActivity().window.decorView.rootWindowInsets
        if (insets != null) {
            constraintSet.knownIds.forEach {
                if (it != R.id.map && it != R.id.shift_detail_parent) {
                    constraintSet.setMargin(it, ConstraintSet.TOP, insets.systemGestureInsets.top)
                    constraintSet.setMargin(
                        it,
                        ConstraintSet.BOTTOM,
                        insets.systemGestureInsets.bottom
                    )
                    constraintSet.setMargin(
                        it,
                        ConstraintSet.START,
                        insets.systemGestureInsets.left
                    )
                    constraintSet.setMargin(it, ConstraintSet.END, insets.systemGestureInsets.right)
                }
            }
        }
        return constraintSet
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(CACHED_SHIFT, cachedShiftReport)
        outState.putBoolean(INFO_CARD, isInfoCardVisible)

        var mapViewBundle =
            outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        mMapView!!.onSaveInstanceState(mapViewBundle)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        val isDarkTheme =
            GlobalUtil.getCurrentTheme(
                resources,
                requireActivity().getPreferences(Context.MODE_PRIVATE)
            ) == THEME_DARK

        // Enable dark map if current theme is dark
        if (isDarkTheme) {
            try {
                // Customise the styling of the base map using a JSON object defined
                // in a raw resource file.
                val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(), R.raw.dark_map_theme
                    )
                )
                if (!success) {
                    Timber.e("Style parsing failed.")
                }
            } catch (e: Resources.NotFoundException) {
                Timber.e("Can't find style. Error: ", e)
            }

        }

        googleMap.uiSettings.isZoomControlsEnabled = true

        val insets = requireActivity().window.decorView.rootWindowInsets.systemGestureInsets
        googleMap.setPadding(insets.left, insets.top, insets.right, insets.bottom)
        // Move camera to first point
        cachedShiftReport.locationList?.let {
            val firstLoc = cachedShiftReport.locationList!![0]
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        firstLoc.latitude,
                        firstLoc.longitude
                    ), 15F
                )
            )
        }

        val googleLocList = ArrayList<LatLng>()

        cachedShiftReport.locationList?.forEachIndexed { index, cacheLocation ->
            googleLocList.add(
                LatLng(
                    cacheLocation.latitude,
                    cacheLocation.longitude
                )
            )

            // Set start marker
            if (index == 0) {
                googleMap.addMarker(
                    MarkerOptions().position(googleLocList[index])
                        .title("Start")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }

            // Set start marker
            if (index == cachedShiftReport.locationList!!.size - 1) {
                googleMap.addMarker(
                    MarkerOptions().position(googleLocList[index])
                        .title("End")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }
        }

        // Set path style options
        val polyLineOptions = PolylineOptions().apply {
            endCap(RoundCap())
            jointType(JointType.ROUND)
            color(getPrimaryColorFromTheme())
        }

        polyLineOptions.addAll(googleLocList)

        googleMap.addPolyline(polyLineOptions)


    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mMapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView?.onStop()
    }

    override fun onPause() {
        mMapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        nav.hideBottomNavBar?.let { it(false) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(false)
        mMapView?.onDestroy()
        mMapView = null
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView?.onLowMemory()
    }

    private fun getPrimaryColorFromTheme(): Int {
        val value = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorPrimary, value, true)
        return value.data
    }
}