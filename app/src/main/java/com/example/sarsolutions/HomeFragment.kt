package com.example.sarsolutions

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.android.synthetic.main.fragment_home.*

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment(private val locationClient: FusedLocationProviderClient) : Fragment() {

    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val viewManager = LinearLayoutManager(context)
        val viewAdapter = MyAdapter()

//        test_recycler_view.apply {
//            setHasFixedSize(true)
//            layoutManager = viewManager
//            adapter = viewAdapter
//        }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
            Toast.makeText(context, "GPS available", Toast.LENGTH_LONG).show()
        else
            Toast.makeText(context, "GPS unavailable", Toast.LENGTH_LONG).show()

        val locationRequest  = LocationRequest().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                super.onLocationResult(locationResult)
//                for(location in locationResult!!.locations){
//                    locationText.text = location.longitude.toString()
//                    Toast.makeText(context, location.toString(), Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        locationClient.lastLocation.addOnSuccessListener { location ->
            if(location != null ) {
                locationText.text = location.toString()
                Toast.makeText(context, "Location is $location", Toast.LENGTH_LONG).show()
            }
        }
    }
}
