// Copyright (c) 2019 Lars Hellgren (lars@exelor.com).
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

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

const val LAST_LATITUDE = "lastLatitude"
const val LAST_LONGITUDE = "lastLongitude"

class LocationWorker(
    private val context: Context,
    private val params: WorkerParameters
) : Worker(context, params) {

    private val TAG = "LocationWorker"
    private var repeat = false
    private var stopped = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val prefs = applicationContext.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)

    override fun doWork(): Result {
        Log.d(TAG, "Starting")

        try {
            repeat = prefs.getString(MainActivity.INTERVAL_UNIT, "") == MainActivity.SECONDS

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            requestLocationUpdates()

            if (repeat) {
                val intervalSeconds = prefs.getLong(MainActivity.INTERVAL, MainActivity.INTERVAL_SECONDS_DEFAULT)
                try {
                    Thread.sleep(intervalSeconds * 1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, e.toString())
                }

                // Start a new worker unless explicitly stopped
                if (!stopped) {
                    startWorker()
                }
            }

            return Result.success()
        } catch (e: Throwable) {
            Log.d(TAG, e.toString())

            return Result.failure()
        }

    }

    private fun startWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java)
            .setConstraints(constraints)
            .addTag(MainActivity.TRACKING_WORKER)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    override fun onStopped() {
        stopped = true
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    val results = FloatArray(1)

                    val lastLatitude = prefs.getDouble(LAST_LATITUDE, 0.0)
                    val lastLongitude = prefs.getDouble(LAST_LONGITUDE, 0.0)

                    Location.distanceBetween(
                        lastLatitude,
                        lastLongitude,
                        location!!.latitude,
                        location.longitude,
                        results
                    )

                    val spacing = results.get(0)
                    val spacingThreshold = prefs.getLong(MainActivity.SPACING, MainActivity.SPACING_DEFAULT)
                    if (results.get(0) >= spacingThreshold) {
                        updateDb(location, spacing)

                        val editor = prefs.edit()
                        editor.putDouble(LAST_LATITUDE, location.latitude)
                        editor.putDouble(LAST_LONGITUDE, location.longitude)
                        editor.apply()
                    }
                }
        }
    }

    /**
     * Sends location data to Firestore DB
     */
    private fun updateDb(location: Location?, spacing: Float) {
        if (location !== null) {
            val db = FirebaseFirestore.getInstance()
            val geocoder = Geocoder(context, Locale.getDefault())

            val locationEntity = location.toLocationEntity()

            locationEntity.unitId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            locationEntity.account = prefs.getString(MainActivity.ACCOUNT_ID, "")
            locationEntity.email = prefs.getString(MainActivity.EMAIL, "")
            locationEntity.distanceMoved = spacing.toLong()

            val addresses = geocoder.getFromLocation(locationEntity.latitude, locationEntity.longitude, 1)
            if (addresses.size > 0) {
                val address: Address = addresses.get(0)
                locationEntity.address = address.toLocationAddress()
            }

            db.collection(MainActivity.COLLECTION_NAME)
                .add(locationEntity)
                .addOnSuccessListener { Log.d(TAG,
                    "Successful write to location collection")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.toString())
                }
        } else {
            Log.e(TAG, "Did not get location")
        }
    }

    /**
     * Extension function for object mapping
     */
    private fun Location.toLocationEntity() = LocationEntity(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy,
        speed = speed,
        bearing = bearing,
        timestamp = time,
        datetime = Date(time).toString()
    )

    /**
     * Extension function for object mapping
     */
    private fun Address.toLocationAddress() = LocationAddress(
        street = subThoroughfare,
        locality = locality,
        area = adminArea,
        postalCode = postalCode,
        country = countryName
    )

    /**
     * Extension function used for storing double values in preferences
     */
    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    /**
     * Extension function used for obtaining double values from preferences
     */
    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))
}
