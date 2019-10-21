package com.exelor.laytrax

import com.google.firebase.Timestamp


data class LocationDoc(
    var latitude: Double,
    var longitude: Double,
    var altitude: Double?,
    var bearing: Float?,
    var accuracy: Float?,
    var speed: Float?,
    var deviceTime: Timestamp?
) {
    var eventType = "MOVE"
    var documentId: String = ""
    var deviceId: String? = null
    var accountId: String? = null
    var email: String? = null
    var address: LocationAddress? = null
    var stepLength: Long? = 0
    var bearingForward: Float? = null
    var previousEventBearing: Float? = 0.0F
    var hasAccuracy: Boolean = false
    var hasAltitude: Boolean = false
    var hasBearing: Boolean = false
    var hasSpeed: Boolean = false
}