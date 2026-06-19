package com.kabarinpacar.app.data.model

data class UserProfile(
    val userId: String = "",
    val pairId: String = "",
    val partnerId: String = ""
)

data class PartnerStatus(
    val activity: String = "",
    val note: String = "",
    val locationName: String = "",
    val timestamp: Long = 0L
)
