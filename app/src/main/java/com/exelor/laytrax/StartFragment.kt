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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class StartFragment : Fragment() {

    private val TAG = "StartFragment"
    private lateinit var startView: View
    private val defaultIntervalValue: Long = 30

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
        (activity as MainActivity).headerText!!.text = getText(R.string.start_page_title)

        // Initialize the UI with data saved in preferences
        val prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)

        this.startView = inflater.inflate(R.layout.start_fragment, container, false)

        // User
        (startView.findViewById<View>(R.id.user_value) as TextView)
            .text = prefs.getString(MainActivity.EMAIL, "")

        // Account
        var accountId = prefs.getString(MainActivity.ACCOUNT_ID, "")
        (startView.findViewById<View>(R.id.account_id_field) as TextView).text = accountId

        // Time interval
        var interval = prefs.getLong(MainActivity.INTERVAL, defaultIntervalValue)
        (startView.findViewById<View>(R.id.interval_field) as TextView).text = interval.toString()

        // Footprint spacing
        var spacing = prefs.getLong(MainActivity.SPACING, MainActivity.SPACING_DEFAULT)
        var spacingField = startView.findViewById<View>(R.id.spacing_field)
        (spacingField as TextView).text = spacing.toString()

        // Time unit radio button
        var timeUnit: String = prefs.getString(MainActivity.INTERVAL_UNIT, MainActivity.MINUTES) as String
        val radioGroup = this.startView.findViewById<View>(R.id.units_group) as RadioGroup
        val id = if (timeUnit == MainActivity.SECONDS) R.id.seconds else R.id.minutes
        val checked = this.startView.findViewById<View>(id) as RadioButton
        radioGroup.check(checked.id)

        // Create footprint spacing value change listener
        spacingField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                var s = editable.toString()
                if (s.isNotEmpty()) {
                    var value = s.toLong()
                    if (value >= MainActivity.SPACING_MIN) {
                        val editor = prefs.edit()
                        editor.putLong(MainActivity.SPACING, value.toLong())
                        editor.commit()
                    } else {
                        Toast.makeText(
                            activity, getString(R.string.spacing_too_small)
                                    + " " + MainActivity.SPACING_MIN, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Create radio button click handler
        radioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, idx ->
            val radioButton = group.findViewById(idx) as RadioButton
            timeUnit = radioButton.getText().toString().substringBefore(" ")
            displayIntervalDefault(timeUnit)
        })

        // Add ranges to radio button labels
        (this.startView.findViewById<View>(R.id.minutes) as RadioButton)
            .setText(
                getString(R.string.minutes_unit_label) + " ("
                        + MainActivity.INTERVAL_MINUTES_MIN.toLong() + " - "
                        + MainActivity.INTERVAL_MINUTES_MAX.toLong() + ")"
            )
        (this.startView.findViewById<View>(R.id.seconds) as RadioButton)
            .setText(
                getString(R.string.seconds_unit_label) + " ("
                        + MainActivity.INTERVAL_SECONDS_MIN.toLong() + " - "
                        + MainActivity.INTERVAL_SECONDS_MAX.toLong() + ")"
            )


        // Create start button click handler
        val startButton = this.startView.findViewById<View>(R.id.start_button) as Button
        startButton.setOnClickListener(View.OnClickListener { view ->
            onStartClick(timeUnit)
        })

        // Create start button click handler
        val signOutButton = this.startView.findViewById<View>(R.id.sign_out_button) as Button
        signOutButton.setOnClickListener(View.OnClickListener { view ->
            onSignOutClick()
        })

        return this.startView
    }

    private fun onStartClick(timeUnit: String) {
        // Check location permission is granted - if it is not, request the permission from the user
        val permission = ContextCompat.checkSelfPermission(
            activity as MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity as MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        }

        if (validateAndSavePreferences(timeUnit)) {
            startWorker()
            showRunFragment()
        }
    }

    private fun validateAndSavePreferences(timeUnit: String): Boolean {
        val interval = (startView.findViewById<View>(R.id.interval_field) as EditText).text.toString().toLong()
        val accountId = (startView.findViewById<View>(R.id.account_id_field) as EditText).text.toString()

        var intervalMin: Long = 0
        var intervalMax: Long = 0
        if (timeUnit == MainActivity.SECONDS) {
            intervalMin = MainActivity.INTERVAL_SECONDS_MIN
            intervalMax = MainActivity.INTERVAL_SECONDS_MAX
        } else if (timeUnit == MainActivity.MINUTES) {
            intervalMin = MainActivity.INTERVAL_MINUTES_MIN
            intervalMax = MainActivity.INTERVAL_MINUTES_MAX
        }
        if (interval < intervalMin || interval > intervalMax) {
            Toast.makeText(activity, getString(R.string.interval_out_of_range), Toast.LENGTH_SHORT).show()
            return false
        }

        val prefs = activity!!
            .applicationContext
            .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)

        val editor = prefs.edit()
        editor.putString(MainActivity.INTERVAL_UNIT, timeUnit)
        editor.putLong(MainActivity.INTERVAL, interval)
        editor.putString(MainActivity.ACCOUNT_ID, accountId)
        editor.commit()

        return true
    }

    /**
     * Set the UI interval value to default value on a time unit change.
     */
    private fun displayIntervalDefault(unit: String) {
        val prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        val savedUnit = prefs.getString(MainActivity.INTERVAL_UNIT, "")
        if (unit != savedUnit) {
            var interval: Long

            if (unit == MainActivity.SECONDS) {
                interval = MainActivity.INTERVAL_SECONDS_DEFAULT
            } else {
                interval = MainActivity.INTERVAL_MINUTES_DEFAULT
            }

            var intervalField = startView.findViewById<View>(R.id.interval_field)
            (intervalField as TextView).text = interval.toString()

            val preferences = activity!!
                .applicationContext
                .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
            val editor = preferences.edit()
            editor.putString(MainActivity.INTERVAL_UNIT, unit)
            editor.commit()
        }
    }

    private fun startWorker() {
        val prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        var timeUnit = prefs.getString(MainActivity.INTERVAL_UNIT, MainActivity.MINUTES)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest: WorkRequest

        if (timeUnit == MainActivity.SECONDS) {
            Log.d(TAG, "Interval unit in seconds")
            workRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java)
                .setConstraints(constraints)
                .addTag(MainActivity.TRACKING_WORKER)
                .build()
        } else if (timeUnit == MainActivity.MINUTES) {
            Log.d(TAG, "Interval unit in minutes")
            workRequest = PeriodicWorkRequest.Builder(LocationWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(MainActivity.TRACKING_WORKER)
                .build()
        } else {
            throw(Exception("Invalid interval time"))
        }

        WorkManager.getInstance(activity!!.applicationContext).enqueue(workRequest)

        Toast.makeText(activity, getString(R.string.service_started), Toast.LENGTH_SHORT).show()
    }

    /**
     * For switching to the run view
     */
    private fun showRunFragment() {
        val fragment = RunFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun onSignOutClick() {
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth!!.currentUser != null) {
            firebaseAuth.signOut()
        }

        val activity = activity
        val intent = Intent(activity, MainActivity::class.java)
        activity!!.startActivity(intent)
        activity.finish()
    }
}
