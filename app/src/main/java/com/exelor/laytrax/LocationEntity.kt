package com.exelor.laytrax


data class LocationEntity(
    var latitude: Double,
    var longitude: Double,
    var altitude: Double?,
    var bearing: Float?,
    var accuracy: Float?,
    var speed: Float?,
    var timestamp: Long?,
    var datetime: String?
) {
    var documentId: String = ""
    var deviceId: String? = null
    var account: String? = null
    var email: String? = null
    var address: LocationAddress? = null
    var stepLength: Long? = 0
    var bearingForward: Float? = null
    var previousBearing: Float? = 0.0F
    var hasAccuracy: Boolean = false
    var hasAltitude: Boolean = false
    var hasBearing: Boolean = false
    var hasSpeed: Boolean = false
}