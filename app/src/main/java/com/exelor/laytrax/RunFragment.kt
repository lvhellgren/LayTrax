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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth

class RunFragment : Fragment(), View.OnClickListener {

    private val TAG = "RunFragment"

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
            R.string.current_spacing, getString(R.string.spacing_label_2),
            prefs.getLong(MainActivity.SPACING, 0)
        )
        (view.findViewById<View>(R.id.interval_value) as TextView)
            .text = getString(
            R.string.current_interval, getString(R.string.interval_label),
            prefs.getLong(MainActivity.INTERVAL, 0),
            prefs.getString(MainActivity.INTERVAL_UNIT, "")?.toLowerCase()
        )

        // Stop button
        val button = view!!.findViewById<View>(R.id.stop_button) as Button
        button.setOnClickListener(this)

        return view
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.stop_button -> {
                stopTracking()
                showStartFragment()
            }
        }
    }

    private fun stopTracking() {
        WorkManager.getInstance(activity!!).cancelAllWorkByTag(MainActivity.TRACKING_WORKER)
        WorkManager.getInstance(activity!!).pruneWork()

        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth!!.currentUser != null) {
            firebaseAuth.signOut()
        }

        Toast.makeText(activity, getString(R.string.stopped), Toast.LENGTH_SHORT).show()
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
