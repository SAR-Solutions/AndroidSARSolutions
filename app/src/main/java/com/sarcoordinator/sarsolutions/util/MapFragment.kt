package com.sarcoordinator.sarsolutions.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.sarcoordinator.sarsolutions.R
import com.sarcoordinator.sarsolutions.models.CacheLocation

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        val LOCATIONS_KEY = "LocationsKey"
    }

    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

    private lateinit var locationList: ArrayList<CacheLocation>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }

        locationList = arguments!!.getParcelableArrayList(LOCATIONS_KEY)!!

        mMapView = view.findViewById(R.id.map)
        mMapView?.onCreate(mapViewBundle)
        mMapView?.getMapAsync(this)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        locationList.forEach {
            googleMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).title(it.locationId.toString()))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 20F))
        }
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