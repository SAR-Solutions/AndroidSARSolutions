package com.example.sarsolutions

import android.Manifest
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.location.LocationSettingsRequest
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.location.LocationManager
import android.content.Context.LOCATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import android.Manifest.permission
import android.content.*
import com.karumi.dexter.Dexter
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.IBinder
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.karumi.dexter.listener.PermissionRequest

@Suppress("LABEL_NAME_CLASH")
class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var myReceiver: MyReceiver
    private var service: LocationUpdatesService? = null
    private var isServiceBound : Boolean = false

    val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            this@MainActivity.service = null
            isServiceBound = false;
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder : LocationUpdatesService.LocalBinder= service as LocationUpdatesService.LocalBinder
            this@MainActivity.service = binder.service
            isServiceBound = true
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myReceiver = MyReceiver()
        setContentView(R.layout.activity_main)


        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {/* ... */
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {/* ... */
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {/* ... */
                }
            }).check()
    }

    override fun onStart() {
        super.onStart()
        service?.requestLocationUpdates()
        bindService(Intent(this, LocationUpdatesService::class.java), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause()
    }

    override fun onStop() {
        if (isServiceBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onStop()
    }

    class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val location = intent?.getParcelableArrayExtra(LocationUpdatesService.EXTRA_LOCATION) as Location?
            if (location != null) {
                Toast.makeText(context, Utils.getLocationText(location),Toast.LENGTH_SHORT).show();
            }
        }
    }
}
