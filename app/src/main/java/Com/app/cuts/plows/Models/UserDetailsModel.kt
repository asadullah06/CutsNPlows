package Com.app.cuts.plows.Models

import java.io.Serializable

data class UserDetailsModel(
    val userName: String,
    val userRole: String,
    val userContactNo: String,
    val userProfileImage: String,
    val providerRating: Float,
    val providerUserId: String = "",
    val providerStatus: String = "",
    val userDistance: Double = 0.0,


) : Serializable