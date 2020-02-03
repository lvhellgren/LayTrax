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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.exelor.laytrax.MainActivity.Companion.ACCURACY
import com.exelor.laytrax.MainActivity.Companion.TIME_INTERVAL_UNIT

class RunFragment : Fragment(), View.OnClickListener {

    private val classTag = "*** RunFragment"

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as MainActivity).headerText!!.text = getText(R.string.running)

        val view = inflater.inflate(R.layout.run_fragment, container, false)

        // Display current settings
        val prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        (view.findViewById<View>(R.id.user_value) as TextView)
            .text = getString(
            R.string.current_user, getString(R.string.email_label),
            prefs.getString(MainActivity.EMAIL, "")
        )
        (view.findViewById<View>(R.id.account_value) as TextView)
            .text = getString(
            R.string.current_account, getString(R.string.account_label),
            prefs.getString(MainActivity.ACCOUNT_ID, "")
        )
        (view.findViewById<View>(R.id.spacing_value) as TextView)
            .text = getString(
            R.string.current_spacing,
            getString(R.string.spacing_label_2),
            prefs.getLong(MainActivity.SPACING, 0)
        )
        (view.findViewById<View>(R.id.time_interval_value) as TextView)
            .text = getString(
            R.string.current_time_interval,
            getString(R.string.time_interval_label),
            prefs.getLong(MainActivity.TIME_INTERVAL, 0),
            prefs.getString(TIME_INTERVAL_UNIT, "")?.toLowerCase()
        )
        (view.findViewById<View>(R.id.accuracy_value) as TextView)
            .text = getString(
            R.string.current_accuracy,
            getString(R.string.accuracy_label),
            prefs.getString(ACCURACY, "")
        )

        // Stop button
        val button = view!!.findViewById<View>(R.id.stop_button) as Button
        button.setOnClickListener(this)

        // Version Info
        (view.findViewById<View>(R.id.version) as TextView)
            .text = getString(
            R.string.current_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        return view
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.stop_button -> {
                stopService()
                showStartFragment()
            }
        }
    }

    private fun stopService() {
        Log.d(classTag, "stopService")
        val stopIntent = Intent(context, LocationService::class.java)
        context?.stopService(stopIntent)
    }

    /**
     * Switch back to the start view
     */
    private fun showStartFragment() {
        val activity = activity
        val intent = Intent(activity, MainActivity::class.java)
        activity!!.startActivity(intent)
        activity.finish()
    }

}
