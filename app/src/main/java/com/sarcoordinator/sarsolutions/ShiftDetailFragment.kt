package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.Navigation
import com.sarcoordinator.sarsolutions.util.setMargins
import kotlinx.android.synthetic.main.fragment_shift_detail.*

class ShiftDetailFragment : Fragment(), OnMapReadyCallback {

    companion object ARGS {
        val CACHED_SHIFT = "CACHED_SHIFT"
        private val INFO_CARD = "InfoCard"
    }

    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

    private var isInfoCardVisible = false
    private val nav: Navigation by lazy { Navigation.getInstance() }
    private lateinit var cachedShiftReport: LocationsInShiftReport

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

        val parentLayout = view.findViewById<ConstraintLayout>(R.id.shift_detail_parent)

        var constraintSet1 = ConstraintSet()
        constraintSet1.clone(requireContext(), R.layout.fragment_shift_detail)
        var constraintSet2 = ConstraintSet()
        constraintSet2.clone(requireContext(), R.layout.fragment_shift_detail_alt)

        cachedShiftReport =
            (arguments?.getSerializable(CACHED_SHIFT) ?: savedInstanceState?.getSerializable(
                CACHED_SHIFT
            )) as LocationsInShiftReport

        savedInstanceState?.getBoolean(INFO_CARD)?.let {
            isInfoCardVisible = it
        }

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

        cachedShiftReport.locationList?.forEachIndexed { index, cacheLocation ->
            googleMap
                .addMarker(
                    MarkerOptions().position(
                        LatLng(
                            cacheLocation.latitude,
                            cacheLocation.longitude
                        )
                    )
                        .title("Track number ${(index + 1)}")
                )
        }
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