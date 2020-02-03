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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.exelor.laytrax.MainActivity.Companion.TIME_INTERVAL_DEFAULT
import com.google.firebase.auth.FirebaseAuth

class StartFragment : Fragment() {

    private val TAG = "*** StartFragment"
    private lateinit var startView: View
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        bundle: Bundle?
    ): View? {
        (activity as MainActivity).headerText!!.text = getText(R.string.start_page_title)

        // Initialize the UI with data saved in preferences
        prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)

        this.startView = inflater.inflate(R.layout.start_fragment, container, false)

        // User
        (startView.findViewById<View>(R.id.user_value) as TextView)
            .text = prefs.getString(MainActivity.EMAIL, "")

        // Account
        val accountId = prefs.getString(MainActivity.ACCOUNT_ID, "")
        (startView.findViewById<View>(R.id.account_id_field) as TextView).text = accountId

        // Location spacing
        val spacing = prefs.getLong(MainActivity.SPACING, MainActivity.SPACING_DEFAULT)
        val spacingField = startView.findViewById<View>(R.id.spacing_field)
        (spacingField as TextView).text = spacing.toString()

        // Create step spacing value change listener
        spacingField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val s = editable.toString()
                if (s.isEmpty()) {
                    toast("${getString(R.string.spacing_label)} must be numeric")
                    val editor = prefs.edit()
                    editor.putLong(MainActivity.SPACING, 0L)
                    editor.apply()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Time interval
        val interval = prefs.getLong(MainActivity.TIME_INTERVAL, TIME_INTERVAL_DEFAULT)
        val intervalField = startView.findViewById<View>(R.id.time_interval)
        (intervalField as TextView).text = interval.toString()

        // Create time value change listener
        intervalField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val s = editable.toString()
                if (s.isEmpty()) {
                    toast("${getString(R.string.time_interval_label)} must be numeric")
                    val editor = prefs.edit()
                    editor.putLong(MainActivity.TIME_INTERVAL, 0L)
                    editor.apply()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Time unit radio buttons
        val timeUnit: String = prefs.getString(
            MainActivity.TIME_INTERVAL_UNIT,
            getString(R.string.minutes_interval_label)
        ) as String
        val timeUnitRadioGroup =
            this.startView.findViewById<View>(R.id.time_unit_group) as RadioGroup
        val timeUnitId =
            if (timeUnit == getString(R.string.minutes_interval_label)) R.id.minutes_interval else R.id.seconds_interval
        val timeUnitChecked = this.startView.findViewById<View>(timeUnitId) as RadioButton
        timeUnitRadioGroup.check(timeUnitChecked.id)

        // Create radio button click handler
        timeUnitRadioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, idx ->
            val radioButton = group.findViewById(idx) as RadioButton
            val setting = radioButton.text.toString().substringBefore(" ")
            val editor = prefs.edit()
            editor.putString(MainActivity.TIME_INTERVAL_UNIT, setting)
            editor.apply()
        })

        // Accuracy radio buttons
        val accuracy: String = prefs.getString(
            MainActivity.ACCURACY,
            getString(R.string.high_accuracy_label)
        ) as String
        val radioGroup = this.startView.findViewById<View>(R.id.accuracy_group) as RadioGroup
        val id =
            if (accuracy == getString(R.string.high_accuracy_label)) R.id.high_accuracy else R.id.low_accuracy
        val checked = this.startView.findViewById<View>(id) as RadioButton
        radioGroup.check(checked.id)

        // Create radio button click handler
        radioGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, idx ->
            val radioButton = group.findViewById(idx) as RadioButton
            val setting = radioButton.text.toString().substringBefore(" ")
            val editor = prefs.edit()
            editor.putString(MainActivity.ACCURACY, setting)
            editor.apply()
        })

        // Create start button click handler
        val startButton = this.startView.findViewById<View>(R.id.start_button) as Button
        startButton.setOnClickListener(View.OnClickListener { view ->
            onStartClick()
        })

        // Create sign-out  button click handler
        val signOutButton = this.startView.findViewById<View>(R.id.sign_out_button) as Button
        signOutButton.setOnClickListener(View.OnClickListener { view ->
            onSignOutClick()
        })

        return this.startView
    }

    private fun onStartClick() {
        // Check for location granularity permissions being granted
        if (ContextCompat.checkSelfPermission(
                activity as MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            toast(getString(R.string.insufficient_permission))
            return
        }

        // Check for the location settings options being turned on
        val locationManager: LocationManager =
            context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        ) {
            toast(getString(R.string.location_setting))
            return
        }

        if (validateAndSavePreferences()) {
            val startIntent = Intent(context, LocationService::class.java)
            context?.startForegroundService(startIntent)
            showRunFragment()
        } else {
            showStartFragment()
        }
    }

    private fun validateAndSavePreferences(): Boolean {
        val timeIntervalText =
            (startView.findViewById<View>(R.id.time_interval) as EditText).text.toString()
        if (timeIntervalText.isEmpty()) {
            toast("${getString(R.string.time_interval_label)} must be numeric")
            return false
        }
        val timeInterval = timeIntervalText.toLong()
        var timeIntervalSeconds: Long

        val timeUnit = prefs.getString(MainActivity.TIME_INTERVAL_UNIT, "")
        if (timeUnit == getString(R.string.seconds_interval_label)) {
            timeIntervalSeconds = timeInterval
        } else {
            timeIntervalSeconds = timeInterval * 60
        }

        if (timeIntervalSeconds < MainActivity.TIME_INTERVAL_SECONDS_MIN) {
            toast(getString(R.string.interval_too_low, MainActivity.TIME_INTERVAL_SECONDS_MIN))
            return false
        }

        val accountId =
            (startView.findViewById<View>(R.id.account_id_field) as EditText).text.toString()
        if (accountId.isEmpty() || accountId.isBlank()) {
            toast(getString(R.string.account_missing))
            return false
        }

        val spacing =
            (startView.findViewById<View>(R.id.spacing_field) as EditText).text.toString().toLong()

        val prefs = activity!!
            .applicationContext
            .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)

        val editor = prefs.edit()
        editor.putString(MainActivity.ACCOUNT_ID, accountId)
        editor.putLong(MainActivity.SPACING, spacing)
        editor.putLong(MainActivity.TIME_INTERVAL, timeInterval)
        editor.apply()

        return true
    }

    private fun showStartFragment() {
        val fragment = StartFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
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

    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
