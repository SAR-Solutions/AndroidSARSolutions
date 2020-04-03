package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dev.chrisbanes.insetter.doOnApplyWindowInsets
import kotlinx.android.synthetic.main.fragment_shift_detail.*

class ShiftDetailFragment : Fragment(), OnMapReadyCallback {

    companion object ARGS {
        val CACHED_SHIFT = "CACHED_SHIFT"
    }

    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

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
        nav.hideBottomNavBar?.let { it(true) }
        (requireActivity() as MainActivity).enableTransparentSystemBars(true)

        back_button.doOnApplyWindowInsets { view, insets, initialState ->
            view.setMargins(
                view, view.marginLeft, insets.systemGestureInsets.top
                , view.marginRight, view.marginBottom
            )
        }

        cachedShiftReport =
            (arguments?.getSerializable(CACHED_SHIFT) ?: savedInstanceState?.getSerializable(
                CACHED_SHIFT
            )) as LocationsInShiftReport
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(CACHED_SHIFT, cachedShiftReport)

        var mapViewBundle =
            outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        mMapView!!.onSaveInstanceState(mapViewBundle)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        // Move camera to first point
        cachedShiftReport.locationList?.let {
            val firstLoc = cachedShiftReport.locationList!![0]
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(firstLoc.latitude, firstLoc.longitude), 15F))
        }

        cachedShiftReport.locationList?.forEachIndexed { index, cacheLocation ->
            googleMap
                .addMarker(MarkerOptions().position(LatLng(cacheLocation.latitude, cacheLocation.longitude))
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