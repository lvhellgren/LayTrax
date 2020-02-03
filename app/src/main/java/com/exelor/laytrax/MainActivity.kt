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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private val TAG: String = "*** MainActivity"

    var headerText: TextView? = null

    val REQUEST_CODE = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        this.headerText = findViewById(R.id.header_text)
    }

    override fun onStart() {
        super.onStart()

        if (hasPlayServices()) {
            if (findViewById<View>(R.id.fragment_container) != null) {
                requestPermissions()
            } else {
                this.headerText!!.text = getText(R.string.err_no_container)
            }
        } else {
            toast("Error: Google Play Services not available")
        }
    }

    private fun requestPermissions() {
        val hasForegroundPermission = ActivityCompat.checkSelfPermission(this,
            ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasForegroundPermission: $hasForegroundPermission")

        if (hasForegroundPermission) {
            val hasBackgroundPermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ||
                ActivityCompat.checkSelfPermission(this, ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "hasBackgroundPermission: $hasBackgroundPermission")
            if (hasBackgroundPermission) {
                selectPage()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(ACCESS_BACKGROUND_LOCATION), REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(ACCESS_FINE_LOCATION,
                    ACCESS_BACKGROUND_LOCATION), REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectPage()
        }
    }

    /* Checks if Google Play Services is available */
    private fun hasPlayServices(): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
    }

    /**
     * If tracking is in progress, navigate to the run page, otherwise, the sign-in or start page.
     */
    private fun selectPage() {
        if (!isServiceRunning(this, LocationService::class.java)) {
            if (FirebaseAuth.getInstance()?.currentUser?.email != null) {
                showStartPage()
            } else {
                showSigninPage()
            }
        } else {
            showRunPage()
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<LocationService>): Boolean {
        val manager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return service.foreground
            }
        }
        return false
    }

    private fun showSigninPage() {
        val fragment = SigninFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showStartPage() {
        val fragment = StartFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showRunPage() {
        val fragment = RunFragment()
        this.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val SHARED_PREFS_NAME = "TraxInfo"
        const val ACCOUNT_ID = "accountId"

        const val EMAIL = "email"

        const val TIME_INTERVAL = "timeInterval"
        const val TIME_INTERVAL_UNIT = "timeIntervalUnit"
        const val TIME_INTERVAL_DEFAULT = 5L
        const val TIME_INTERVAL_SECONDS_MIN = 5L

        const val SPACING = "spacing"
        const val SPACING_DEFAULT = 5L

        const val ACCURACY = "accuracy"

        const val COLLECTION_NAME = "device-events"

        const val PREVIOUS_LATITUDE = "lastLatitude"
        const val PREVIOUS_LONGITUDE = "lastLongitude"
    }
}
