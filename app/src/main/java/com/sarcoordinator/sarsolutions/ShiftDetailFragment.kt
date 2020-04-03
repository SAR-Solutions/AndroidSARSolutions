package com.sarcoordinator.sarsolutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sarcoordinator.sarsolutions.models.LocationsInShiftReport
import com.sarcoordinator.sarsolutions.util.CustomFragment
import kotlinx.android.synthetic.main.fragment_shift_detail.*

class ShiftDetailFragment : Fragment(), CustomFragment, OnMapReadyCallback {

    companion object ARGS {
        val CACHED_SHIFT = "CACHED_SHIFT"
    }

    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

    private lateinit var cachedShiftReport: LocationsInShiftReport

    override fun getSharedElement(): View = toolbar_detailed_shift

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

        // TODO: Fetch from savedInstace if arguments is null
        cachedShiftReport = arguments?.getSerializable(CACHED_SHIFT) as LocationsInShiftReport

        toolbar_detailed_shift.setHeading(cachedShiftReport.shiftReport.caseName.toString())
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle =
            outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        mMapView!!.onSaveInstanceState(mapViewBundle)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        cachedShiftReport.locationList?.forEach {
            googleMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).title(it.locationId.toString()))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 20F))
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
        mMapView?.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView?.onLowMemory()
    }
}