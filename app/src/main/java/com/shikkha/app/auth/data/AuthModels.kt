package com.shikkha.app.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_level") val classLevel: Int,
    val medium: String,
    val role: String = "student",
    @SerialName("approval_status") val approvalStatus: String = "pending",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Payload for self-registration (Spec 2.1). Only these columns are insertable. */
@Serializable
data class ProfileInsert(
    val id: String,
    val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_level") val classLevel: Int,
    val medium: String,
)

/**
 * App entry state after session resolution (Spec 2.2/2.5).
 * The backend profile, not the client, decides which state applies.
 */
sealed interface RootState {
    data object Loading : RootState
    data object LoggedOut : RootState
    data class Pending(val profile: ProfileDto) : RootState
    data class Rejected(val profile: ProfileDto) : RootState
    data class Blocked(val profile: ProfileDto) : RootState
    data class Ready(val profile: ProfileDto) : RootState
}

sealed interface RegisterResult {
    data object ProfileCreated : RegisterResult
    data object EmailConfirmationRequired : RegisterResult
    data class Failure(val message: String) : RegisterResult
}
