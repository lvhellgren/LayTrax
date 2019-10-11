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
import com.exelor.laytrax.MainActivity.Companion.COLLECTION_NAME
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

const val LAST_LATITUDE = "lastLatitude"
const val LAST_LONGITUDE = "lastLongitude"

/** <test-data>
 *
 * For use with the emulator */


const val useTestData = false
const val ignoreSpacingThreshold = false;

val COORDS = arrayOf(
    doubleArrayOf(33.634221774746806, -117.87835762491568),
    doubleArrayOf(33.63069369602855, -117.88553852388304),
    doubleArrayOf(33.65813818374289, -117.91751178161758),
    doubleArrayOf(33.703409061422626, -117.93241745295722),
    doubleArrayOf(33.63459185798198, -117.88492469561425)
)

const val BIG_CANYON = 1
const val CdMHS = 0
const val COSTCO = 3
const val HOME = 4
const val HOME_DEPOT = 2

const val LOC = HOME

const val LAT = 0
const val LON = 1

/** </test-data> */

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
            // To support a repeat interval less than 600 seconds (there is a 10 minute worker duration limit),
            // the worker starts a new worker instance to handle the next interval. WorkManager can automatically
            // handle repeat intervals of 15 minutes or more.
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
            .build()

        val workRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java)
            .setConstraints(constraints)
            .addTag(MainActivity.TRACKING_WORKER)
            .build()

        // Remove completed worker entries from the database
        WorkManager.getInstance(context).pruneWork()

        // Submit the new request
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
                    if (location != null) {
                        val results = FloatArray(1)
                        Log.d(TAG, "Got location")

                        if (useTestData) {
                            location.latitude = COORDS[LOC][LAT]
                            location.longitude = COORDS[LOC][LON]
                        }

                        val lastLatitude = prefs.getDouble(LAST_LATITUDE, 0.0)
                        val lastLongitude = prefs.getDouble(LAST_LONGITUDE, 0.0)

                        Location.distanceBetween(
                            lastLatitude,
                            lastLongitude,
                            location!!.latitude,
                            location.longitude,
                            results
                        )

                        val spacing = results[0]
                        val spacingThreshold = prefs.getLong(MainActivity.SPACING, MainActivity.SPACING_DEFAULT)
                        if (results[0] >= spacingThreshold || ignoreSpacingThreshold) {

                            var previousBearing = 0.0F;
                            if (lastLatitude + lastLongitude != 0.0) {
                                val lastLocation = Location("");
                                lastLocation.setLatitude(lastLatitude);
                                lastLocation.setLongitude(lastLongitude);
                                previousBearing = lastLocation.bearingTo(location);
                            }

                            updateDb(location, spacing, previousBearing)

                            val editor = prefs.edit()
                            editor.putDouble(LAST_LATITUDE, location.latitude)
                            editor.putDouble(LAST_LONGITUDE, location.longitude)
                            editor.apply()
                        }
                    } else {
                        Log.e(TAG, "Did not get lastLocation")
                    }
                }
        }
    }

    /**
     * Sends location data to Firestore DB
     */
    private fun updateDb(location: Location?, spacing: Float, previousBearing: Float) {
        if (location !== null) {
            val db = FirebaseFirestore.getInstance()
            val geocoder = Geocoder(context, Locale.getDefault())

            val locationDoc = location.locationDoc()

            locationDoc.deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            locationDoc.account = prefs.getString(MainActivity.ACCOUNT_ID, "")
            locationDoc.email = prefs.getString(MainActivity.EMAIL, "")
            locationDoc.stepLength = spacing.toLong()
            locationDoc.previousBearing = previousBearing
            locationDoc.hasAccuracy = location.hasAccuracy();
            locationDoc.hasAltitude = location.hasAltitude();
            locationDoc.hasBearing = location.hasBearing();
            locationDoc.hasSpeed = location.hasSpeed();

            val addresses = geocoder.getFromLocation(locationDoc.latitude, locationDoc.longitude, 1)
            if (addresses.size > 0) {
                val address: Address = addresses[0]
                locationDoc.address = address.toLocationAddress()
            }

            val id = db.collection(MainActivity.COLLECTION_NAME)
                .document()
                .id;
            locationDoc.documentId = id

            db.collection(MainActivity.COLLECTION_NAME)
                .document(id)
                .set(locationDoc)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Successful write to $COLLECTION_NAME collection"
                    )
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
    private fun Location.locationDoc() = LocationDoc(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        bearing = bearing,
        accuracy = accuracy,
        speed = speed,
        timestamp = time,
        datetime = Date(time).toString()
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
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))
}
