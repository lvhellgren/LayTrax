package com.exelor.laytrax


data class LocationEntity(
    var latitude: Double,
    var longitude: Double,
    var altitude: Double?,
    var accuracy: Float?,
    var speed: Float?,
    var bearing: Float?,
    var timestamp: Long?,
    var datetime: String?

) {
    var unitId: String? = null
    var account: String? = null
    var email: String? = null
    var address: LocationAddress? = null
    var distanceMoved: Long? = 0
}