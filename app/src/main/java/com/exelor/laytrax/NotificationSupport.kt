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

import android.R.drawable.stat_notify_more
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.core.app.NotificationCompat


const val SERVICE_CHANNEL_ID = "LayTrax.LocationService"
const val SMALL_ICON = stat_notify_more

class NotificationSupport(context: Context, appName: String) : ContextWrapper(context) {
    private val TAG = "*** NotificationSupport"

    init {
        createNotificationChannel(
            NotificationManager.IMPORTANCE_HIGH, SERVICE_CHANNEL_ID,
            appName, "Location service status"
        )
    }

    companion object {
        var notificationId = 1
    }

    /**
     * Create a notification channel. Requires Android API 26 or higher
     */
    private fun createNotificationChannel(
        importance: Int,
        id: String,
        name: String,
        description: String
    ) {
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        getManager()?.createNotificationChannel(channel)
    }

    /**
     * Build service status notification
     */
    fun buildServiceNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(applicationContext, SERVICE_CHANNEL_ID)
            .setSmallIcon(SMALL_ICON)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(Color.BLUE)
            .build()
    }

    /**
     * Send service status notification to notification drawer
     */
    fun sendServiceNotification(title: String, text: String) {
        val notification = buildServiceNotification(title, text)
        getManager()?.notify(notificationId++, notification)
    }

    private fun getManager(): NotificationManager? {
        return applicationContext.getSystemService(NotificationManager::class.java)
    }
}