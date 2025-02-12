package com.example.currentlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.currentlocation.databinding.ActivityMain2Binding
import java.util.Locale

class MainActivity2 : AppCompatActivity() {
    private var currentLocation: Location? = null
    lateinit var locationManager: LocationManager
    lateinit var binding: ActivityMain2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

       binding.btnLocation.setOnClickListener {
           getLocation()
       }
    }



    private fun requestLocationPermission() {
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (!fineLocationGranted && !coarseLocationGranted) {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (!checkPermission()) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasGps && !hasNetwork) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        val gpsLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateLocation(location, "GPS")
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val networkLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateLocation(location, "Network")
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0f,
                gpsLocationListener
            )
        }

        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,  // FIXED
                5000,
                0f,
                networkLocationListener
            )
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateLocation(location: Location, provider: String) {
        if (currentLocation == null || location.accuracy < currentLocation!!.accuracy) {
            currentLocation = location
            binding.tvLatitude.text = "Latitude: ${currentLocation?.latitude}"
            binding.tvLongitude.text = "Longitude: ${currentLocation?.longitude}"

            // Use Geocoder to get the country name
            val geocoder = Geocoder(this, Locale.getDefault())
            val addressList: MutableList<Address>? = geocoder.getFromLocation(
                currentLocation!!.latitude, currentLocation!!.longitude, 1
            )

            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList?.get(0)
                    val countryName = address?.countryName
                    binding.tvCountry.text = "Country: $countryName"
                } else {
                    binding.tvCountry.text = "Country: Not available"
                }
            }

            Toast.makeText(this, "Location updated via $provider", Toast.LENGTH_SHORT).show()
        }
    }

}
