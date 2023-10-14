package com.example.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.io.IOException

class MapActivity : AppCompatActivity(), OnMapReadyCallback, OnMyLocationButtonClickListener, OnLocationChangedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener
    // Initialize the geocoder
    private lateinit var mGeocoder: Geocoder
    private lateinit var polyline: PolylineOptions
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    private var light: Boolean = true

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the sensors
        sensorManager = getSystemService(SENSOR_SERVICE)
                as SensorManager
        lightSensor = sensorManager
            .getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Initialize the listener
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values[0] < 4000 && light) {
                    light = false
                    mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            this@MapActivity,
                            R.raw.style_night
                        )
                    )
                } else if (event.values[0] > 5000 && !light) {
                    light = true
                    mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            this@MapActivity,
                            R.raw.style_day
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
        }

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        mGeocoder = Geocoder(this)

        mMap.setOnMapLongClickListener { latLng ->
            // Crea un marcador en la posición del evento
            val marker = MarkerOptions().position(latLng)

            // Obtiene la dirección usando Geocoder
            try {
                val addresses = mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    marker.title(address.getAddressLine(0))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            googleMap.addMarker(marker)
        }

        binding.editTextText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == 66) {
                val addressString = binding.editTextText.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        val addresses = mGeocoder.getFromLocationName(addressString, 2)
                        if (!addresses.isNullOrEmpty()) {
                            val addressResult = addresses[0]
                            val position = LatLng(addressResult.latitude, addressResult.longitude)
                            mMap.addMarker(
                                MarkerOptions().position(position)
                                    .title(addressResult.featureName)
                                    .snippet(addressResult.getAddressLine(0))
                            )
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            false
        }

        binding.switch1.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                enableLocation()
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    mMap.isMyLocationEnabled = false
                }
            }
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onResume() {
        super.onResume()

        if (!::lightSensorListener.isInitialized) return
        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        if (!::lightSensorListener.isInitialized) return

        sensorManager.unregisterListener(lightSensorListener)
    }

    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            polyline = PolylineOptions()
            mMap.addPolyline(polyline)
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(
                this,
                "Se necesita el permiso para mostrar la ubicación actual",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED) {
                        mMap.isMyLocationEnabled = true
                        polyline = PolylineOptions()
                        mMap.addPolyline(polyline)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "No se concedió el permiso para mostrar la ubicación actual",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
            else -> {}
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::mMap.isInitialized) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            polyline = PolylineOptions()
            mMap.addPolyline(polyline)
        } else {
            binding.switch1.isChecked = false
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Ubicación actual", Toast.LENGTH_SHORT).show()
        return false
    }
    override fun onLocationChanged(p0: Location) {
        polyline.add(LatLng(p0.latitude, p0.longitude))
        mMap.addPolyline(polyline)
        Log.i("Location", "Location changed")
    }
}