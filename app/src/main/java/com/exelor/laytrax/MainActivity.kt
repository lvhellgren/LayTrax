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

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    var headerText: TextView? = null

    private var liveData: LiveData<List<WorkInfo>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        this.headerText = findViewById(R.id.header_text)

        if (findViewById<View>(R.id.fragment_container) != null) {
            selectPage()
        }
    }

    /**
     * If tracking is in progress, navigate to the run page, otherwise, the sign-in page.
     */
    private fun selectPage() {

        liveData = WorkManager.getInstance(applicationContext)
            .getWorkInfosByTagLiveData(TRACKING_WORKER)
        liveData!!.observe(this, Observer { workInfos: List<WorkInfo> ->
            if (workInfos.isEmpty()) {
                if (FirebaseAuth.getInstance()?.currentUser?.email != null) {
                    showStartPage()
                } else {
                    showSigninPage()
                }
            } else {
                showRunPage()
            }
        })
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
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    companion object {
        const val SHARED_PREFS_NAME = "TraxInfo"
        const val ACCOUNT_ID = "accountId"
        const val EMAIL = "email"
        const val TRACKING_WORKER = "tracking"

        const val INTERVAL = "interval"
        const val INTERVAL_SECONDS_DEFAULT = 5L
        const val INTERVAL_SECONDS_MIN = 2L
        const val INTERVAL_SECONDS_MAX = 60 * 12L
        const val INTERVAL_MINUTES_DEFAULT = 15L
        const val INTERVAL_MINUTES_MIN = 15L
        const val INTERVAL_MINUTES_MAX = 60 * 24L

        const val SPACING = "spacing"
        const val SPACING_DEFAULT = 5L
        const val SPACING_MIN = 5L

        const val INTERVAL_UNIT = "timeUnit"
        const val SECONDS = "Seconds"
        const val MINUTES = "Minutes"

        const val COLLECTION_NAME = "tracks"
    }
}
