package com.example.sarsolutions

import android.Manifest
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.system.exitProcess


class MainFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentShiftId: String? = null
    private val db = FirebaseFirestore.getInstance()

    val viewModel: MainViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        createLocationRequest()

        // Restore state depending on view model
        restoreState()

        handlePermissions()

    }

    // Ask for locational permission and handle response
    private fun handlePermissions() {

        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    onPermissionGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied)
                        closeNow()
                    else {
                        Toast.makeText(
                            context,
                            "Permission not granted",
                            Toast.LENGTH_LONG
                        ).show()
                        closeNow()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    // Setup for after permissions are granted
    private fun onPermissionGranted() {

        // Toggle test mode
        test_button.setOnClickListener {
            if (viewModel.isTestingEnabled) { // Disable Testing
                viewModel.isTestingEnabled = false
                test_button.text = "Enable Test Mode"
                test_button.setBackgroundColor(resources.getColor(R.color.warning))
            } else { // Enable Testing
                viewModel.isTestingEnabled = true
                test_button.text = "Disable Test Mode"
                test_button.setBackgroundColor(resources.getColor(R.color.error))
            }
        }

        start_button.setOnClickListener {
            // Change button state/functionality
            if (!viewModel.isSearching) { // Start searching
                viewModel.isSearching = true
                start_button.text = "Stop"
                start_button.setBackgroundColor(resources.getColor(R.color.error))
                startTracking()
            } else { // Stop searching and update Loc
                fusedLocationClient.removeLocationUpdates(locationCallback)
                val shift = Shift(
                    "YlNtlx3VTh6rAv6KC9dU",
                    "oKrbMcbPVJ6yiErezkX3",
                    Calendar.getInstance().time.toString(),
                    BuildConfig.VERSION_NAME,
                    viewModel.getLocationList()
                )

                /** NOTE: currentShiftId in this context is always going to be null for the demo. This code is here for the future **/
                if (currentShiftId == null) { // Add if no current shift id exists
                    db.collection(if (viewModel.isTestingEnabled) "TestShift" else "Shift")
                        .add(shift)
                        .addOnSuccessListener { docRef ->
                            Log.d("SAR", "Added document with id ${docRef.id}")
                        }
                } else { // Update and merge if current shift id exists
                    db.collection(if (viewModel.isTestingEnabled) "TestShift" else "Shift")
                        .document(currentShiftId!!)
                        .set(shift, SetOptions.merge())
                }
                viewModel.endShift()
                viewModel.isSearching = false
                start_button.text = "Start"
                start_button.setBackgroundColor(resources.getColor(R.color.success))
                currentShiftId = null
            }
        }
    }

    // Close app on permission denied
    private fun closeNow() {
        activity!!.finishAffinity()
        exitProcess(0)
    }

    // Restore view state on configuration changes
    private fun restoreState() {
        if (viewModel.isSearching) {
            location_id.text = viewModel.getLastUpdated()
            start_button.text = "Stop"
            start_button.setBackgroundColor(resources.getColor(R.color.error))
            startTracking()
        }

        if (viewModel.isTestingEnabled) {
            test_button.text = "Disable Test Mode"
            test_button.setBackgroundColor(resources.getColor(R.color.error))
        }

    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        } // update every 5 seconds
    }

    private fun startTracking() {
        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult ?: return
                for (location in locationResult.locations) {

                    // Don't record if already exists
                    if (viewModel.existsInList(GeoPoint(location.latitude, location.longitude)))
                        continue
                    (GeoPoint(location.latitude, location.longitude))
                    viewModel.addToList((GeoPoint(location.latitude, location.longitude)))
                    viewModel.setLastUpdated("Last updated at \n" + Calendar.getInstance().time.toString())
                    location_id.text = viewModel.getLastUpdated()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

}
