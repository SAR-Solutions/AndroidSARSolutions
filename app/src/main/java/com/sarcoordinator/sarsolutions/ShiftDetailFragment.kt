package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.GlobalUtil
import com.sarcoordinator.sarsolutions.util.GlobalUtil.getThemedPolyLineOptions
import com.sarcoordinator.sarsolutions.util.GlobalUtil.setGoogleMapsTheme
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.card_shift_details.*
import kotlinx.android.synthetic.main.card_shift_details.view.*
import kotlinx.android.synthetic.main.fragment_shift_detail.*
import kotlinx.android.synthetic.main.view_circular_button.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        back_button.image_button.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_arrow_back_24))

        back_button.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }

        info_button.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                initialState.margins.left + insets.systemGestureInsets.left,
                initialState.margins.top + insets.systemGestureInsets.top,
                initialState.margins.right + insets.systemGestureInsets.right,
                initialState.margins.bottom + insets.systemGestureInsets.bottom
            )
        }

        back_button.image_button.setOnClickListener {
            requireActivity().onBackPressed()
        }

        info_button.image_button.setOnClickListener {
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

        setGoogleMapsTheme(requireActivity(), googleMap)

        googleMap.uiSettings.isZoomControlsEnabled = true

        val insets = requireActivity().window.decorView.rootWindowInsets.systemGestureInsets
        googleMap.setPadding(insets.left, insets.top, insets.right, insets.bottom)

        val googleLocList = ArrayList<LatLng>()

        if (cachedShiftReport.locationList.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "No locations recorded for this shift",
                Toast.LENGTH_LONG
            ).show()
        }

        cachedShiftReport.locationList?.forEachIndexed { index, cacheLocation ->

            googleLocList.add(
                LatLng(
                    cacheLocation.latitude,
                    cacheLocation.longitude
                )
            )

            // Set start marker
            when (index) {
                0 -> {
                    googleMap.addMarker(
                        MarkerOptions().position(googleLocList[index])
                            .title("Start")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                    // Focus camera on starting point
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                cacheLocation.latitude,
                                cacheLocation.longitude
                            ), 15F
                        )
                    )
                }
                cachedShiftReport.locationList!!.size - 1 -> {  // Set end marker
                    googleMap.addMarker(
                        MarkerOptions().position(googleLocList[index])
                            .title("End")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
            }
        }

        // Set path style options
        val polyLineOptions = getThemedPolyLineOptions(requireContext())
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
}