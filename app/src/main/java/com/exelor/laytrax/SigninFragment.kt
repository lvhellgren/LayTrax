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

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth

class SigninFragment : Fragment(), View.OnClickListener {

    private val TAG = "SigninFragment"
    private lateinit var signinView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
        (activity as MainActivity).headerText!!.text = getText(R.string.signin_page_title)

        // Get any UI data saved in preferences
        val prefs = context!!.getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        val email = prefs.getString(MainActivity.EMAIL, "")

        // Include data in UI
        this.signinView = inflater.inflate(R.layout.signin_fragment, container, false)
        (this.signinView.findViewById<View>(R.id.email_field) as TextView).text = email

        // Create login button click handler
        (this.signinView.findViewById<View>(R.id.login_button) as Button).setOnClickListener(this)

        // Create service request button click handler
        (this.signinView.findViewById<View>(R.id.service_request_button) as Button).setOnClickListener(this)

        return this.signinView
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.login_button -> {
                val data = buildData(this.signinView)
                initTracking(data)
            }
            R.id.service_request_button -> {
                showServiceRequestFragment()
            }
        }
    }

    /**
     * Get email address from the view
     */
    private fun buildData(view: View): Data {
        return Data.Builder()
            .putString(
                MainActivity.EMAIL,
                (view.findViewById<View>(R.id.email_field) as EditText).text.toString()
            )
            .build()
    }


    /**
     * Sign in and continue
     */
    @NonNull
    private fun initTracking(data: Data) {
        val email: String = data.getString(MainActivity.EMAIL) ?: ""
        val password = (this.signinView.findViewById<View>(R.id.password_field) as EditText).text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(activity, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty() || password.length < 8) {
            Toast.makeText(activity, getString(R.string.invalid_password), Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(R.layout.alert_dialog)
        builder.setMessage(R.string.signing_in_msg)
        val dialog = builder.create()

        dialog.show()
        try {
            FirebaseAuth.getInstance()!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity as MainActivity) { task ->
                    if (task.isSuccessful) {
                        showStartFragment()
                    } else {
                        Toast.makeText(activity, getString(R.string.sign_in_error), Toast.LENGTH_SHORT).show()
                    }
                    savePreferences(data)
                    dialog.dismiss()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sign In error: $e")
            dialog.dismiss()
        }

    }

    private fun savePreferences(data: Data) {
        val pref = activity!!
            .applicationContext
            .getSharedPreferences(MainActivity.SHARED_PREFS_NAME, 0)
        val editor = pref.edit()

        editor.putString(MainActivity.EMAIL, data.getString(MainActivity.EMAIL))

        editor.apply()
    }

    /**
     * For switching to the start view
     */
    private fun showStartFragment() {
        val fragment = StartFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * For switching to the service request view
     */
    private fun showServiceRequestFragment() {
        val fragment = ServiceRequestFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}





