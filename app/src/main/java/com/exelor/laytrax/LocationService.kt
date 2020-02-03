// Copyright (c) 2020 Lars Hellgren (lars@exelor.com).
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE

package com.exelor.laytrax

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


class LocationService : Service() {
    private val TAG = "*** LocationService"
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRunning = false
    private lateinit var prefs: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var crashlytics: FirebaseCrashlytics
    private var lastLocation: Location? = null
    private var minSpacing: Float = 0f

    companion object {
        val locationRequest: LocationRequest? = LocationRequest.create().apply {
            priority = PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // Other apps cannot bind to the service
        return null
    }

    override fun onCreate() {
        super.onCreate()

        prefs = applicationContext.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crashlytics = FirebaseCrashlytics.getInstance()

        val notificationSupport =
            NotificationSupport(applicationContext, getString(R.string.app_name))
        val notification = notificationSupport.buildServiceNotification(
            getString(R.string.notification_title),
            getString(R.string.notification_text_start)
        )
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runService()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    private fun runService() {
        if (!isRunning) {
            isRunning = true

            // WakeLock prevents Doze Mode
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService::lock").apply {
                        acquire()
                    }
                }

            minSpacing = prefs.getLong(MainActivity.SPACING, 0).toFloat()

            val timeInterval = prefs.getLong(MainActivity.TIME_INTERVAL, 0)
            var timeIntervalSeconds: Long

            val timeUnit = prefs.getString(MainActivity.TIME_INTERVAL_UNIT, "")
            if (timeUnit == getString(R.string.seconds_interval_label)) {
                timeIntervalSeconds = timeInterval
            } else {
                timeIntervalSeconds = timeInterval * 60
            }

            locationRequest?.interval = timeIntervalSeconds * 1000L
            locationRequest?.fastestInterval = timeIntervalSeconds * 1000L
            locationRequest?.smallestDisplacement = 0f

            val accuracy = prefs.getString(MainActivity.ACCURACY, "")
            if (accuracy == getString(R.string.low_accuracy_label)) {
                locationRequest?.priority = LocationRequest.PRIORITY_LOW_POWER
            }

//            val spacing = prefs.getLong(MainActivity.SPACING, 0)
//            locationRequest?.smallestDisplacement = spacing.toFloat()

            toast(getString(R.string.service_started))

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    /* For getting the last known location of the device. Does not need localization hardware to be active */
    private fun lastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Last known latitude: ${location.latitude}")
                    lastLocation = location
                } else {
                    Log.d(TAG, "Last known location is null")
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(TAG, e.toString())
                toast("Could not get last location")
            }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.locations.last()
            if (location != null) {
                val previousLatitude = prefs.getDouble(MainActivity.PREVIOUS_LATITUDE, 0.0)
                val previousLongitude = prefs.getDouble(MainActivity.PREVIOUS_LONGITUDE, 0.0)

                var previousLocationBearing = 0.0F
                if (previousLatitude + previousLongitude != 0.0) {
                    val previousLocation = Location("")
                    previousLocation.latitude = previousLatitude
                    previousLocation.longitude = previousLongitude
                    previousLocationBearing = previousLocation.bearingTo(location)
                }

                val editor = prefs.edit()
                editor.putDouble(MainActivity.PREVIOUS_LATITUDE, location.latitude)
                editor.putDouble(MainActivity.PREVIOUS_LONGITUDE, location.longitude)
                editor.apply()

                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    previousLatitude, previousLongitude, results
                )
                var spacing = 0f
                if (results.isNotEmpty()) {
                    spacing = results.get(0)
                }

                if (spacing >= minSpacing) {
                    updateDb(location, spacing, previousLocationBearing)
                }
            } else {
                Log.e(TAG, "Did not get location update")
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability?) {
            super.onLocationAvailability(locationAvailability)
            if (!locationAvailability!!.isLocationAvailable) {
                Log.e(TAG,"No location availability")
            }
        }
    }

    /* Sends location data to Firestore DB */
    private fun updateDb(location: Location?, spacing: Float, previousLocationBearing: Float) {
        if (location !== null) {
            toast("Updating location")

            val db = FirebaseFirestore.getInstance()
            val geocoder = Geocoder(applicationContext, Locale.getDefault())

            val locationDoc = location.locationDoc()

            locationDoc.deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            locationDoc.accountId = prefs.getString(MainActivity.ACCOUNT_ID, "")
            locationDoc.email = prefs.getString(MainActivity.EMAIL, "")
            locationDoc.stepLength = spacing.toLong()
            locationDoc.previousEventBearing = previousLocationBearing
            locationDoc.hasAccuracy = location.hasAccuracy()
            locationDoc.hasAltitude = location.hasAltitude()
            locationDoc.hasBearing = location.hasBearing()
            locationDoc.hasSpeed = location.hasSpeed()

            try {
                val addresses = geocoder.getFromLocation(locationDoc.latitude, locationDoc.longitude, 1)
                if (addresses.size > 0) {
                    val address: Address = addresses[0]
                    locationDoc.address = address.toLocationAddress()
                }
            } catch (e: Exception) {
                crashlytics.log("Geocoding error getting address")
                crashlytics.recordException(e)
            }

            val id = db.collection(MainActivity.COLLECTION_NAME)
                .document()
                .id
            locationDoc.documentId = id

            db.collection(MainActivity.COLLECTION_NAME)
                .document(id)
                .set(locationDoc)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                    toast("Database ${e.message!!}")
                    Log.e(TAG, "updateDb: $e")
                    crashlytics.log("Databas update failure: $e")
                    crashlytics.recordException(e)
                }
        } else {
            Log.e(TAG, "No location value")
        }
    }

    private fun stopService() {
        toast(getString(R.string.service_stopped))
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)

            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)

            val notificationSupport =
                NotificationSupport(applicationContext, getString(R.string.app_name))
            notificationSupport.sendServiceNotification(
                getString(R.string.notification_title),
                getString(R.string.notification_text_stop)
            )

            stopSelf()
        } catch (e: Exception) {
            crashlytics.log("Error stopping service")
            crashlytics.recordException(e)
        }
        isRunning = false
    }


    /**
     * Extension function for object mapping
     */
    private fun Location.locationDoc() = LocationDoc(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        bearing = bearing,
        accuracy = accuracy,
        speed = speed,
        deviceTime = Timestamp(Date(time))
    )

    /**
     * Extension function for object mapping
     */
    private fun Address.toLocationAddress() = LocationAddress(
        subThoroughfare = subThoroughfare,
        thoroughfare = thoroughfare,
        locality = locality,
        subAdminArea = subAdminArea,
        area = adminArea,
        postalCode = postalCode,
        countryName = countryName
    )

    /**
     * Extension function used for storing double values in preferences
     */
    private fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    /**
     * Extension function used for obtaining double values from preferences
     */
    private fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(
            getLong(
                key,
                java.lang.Double.doubleToRawLongBits(default)
            )
        )

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}