package com.example.taller2

import android.content.DialogInterface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller2.databinding.ActivityMapBinding
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.io.IOException

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private var lightSensorListener: SensorEventListener? = null
    // Initialize the geocoder
    private lateinit var mGeocoder: Geocoder
    private var polyline: PolylineOptions? = null

    private var light: Boolean = true

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
                        if (addresses != null && !addresses.isEmpty()) {
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
    }

    override fun onResume() {
        super.onResume()

        if (lightSensorListener != null) {
            sensorManager.registerListener(
                lightSensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()

        if (lightSensorListener != null) {
            sensorManager.unregisterListener(lightSensorListener)
        }
    }

    /*private fun findAddress() {
        val addressString = mAddress.text.toString()
        if (addressString.isNotEmpty()) {
            try {
                val addresses = mGeocoder.getFromLocationName(addressString, 2,
                    lowerLeftLatitude, lowerLeftLongitude,
                    upperRightLatitude, upperRightLongitude
                )
                if (addresses != null && !addresses.isEmpty()) {
                    val addressResult = addresses[0]
                    val position = LatLng(addressResult.latitude, addressResult.longitude)
                    mMap.addMarker(
                        MarkerOptions().position(position)
                            .title(addressResult.featureName)
                            .snippet(addressResult.getAddressLine(0))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                } else {
                    Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
        }
    }*/
}