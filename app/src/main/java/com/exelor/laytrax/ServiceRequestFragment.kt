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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class ServiceRequestFragment : Fragment(), View.OnClickListener {
    private val TAG = "ServiceRequestFragment"

    private lateinit var requestView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as MainActivity).headerText!!.text = getText(R.string.service_request)

        requestView = inflater.inflate(R.layout.service_request_fragment, container, false)

        // Create service request button click handler
        (requestView.findViewById<View>(R.id.request_send_button) as Button).setOnClickListener(this)

        // Create service request cancel button click handler
        (requestView.findViewById<View>(R.id.request_cancel_button) as Button).setOnClickListener(this)

        return requestView
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.request_send_button -> {
                val email = (requestView.findViewById<View>(R.id.admin_email_field) as EditText).text.toString()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(activity, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                } else {
                    sendRequest(email)
                }
            }
            R.id.request_cancel_button -> {
                showStartFragment()
            }
        }
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

    private fun sendRequest(email: String) {
        val deviceId = Settings.Secure.getString(
            context?.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.request_email_subject))
        intent.putExtra(Intent.EXTRA_TEXT, "${getString(R.string.request_email_text)} $deviceId")
        if (intent.resolveActivity(activity!!.packageManager) != null) {
            startActivity(intent)
        }
    }
}