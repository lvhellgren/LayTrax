package com.exelor.laytrax

data class LocationAddress(
    var subThoroughfare: String?,
    var thoroughfare: String?,
    var locality: String?,
    var area: String?,
    var postalCode: String?,
    var subAdminArea: String,
    var countryName: String?
)