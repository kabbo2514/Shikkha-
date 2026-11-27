package com.shikkha.app.auth.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All authorization decisions come from the backend profile row:
 * approval status, role, suspension (Spec 22.3). The client never decides.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient,
) {

    suspend fun resolveInitialState(): RootState {
        val user = client.auth.currentUserOrNull() ?: return RootState.LoggedOut
        val profile = fetchProfile(user.id) ?: return RootState.LoggedOut
        return stateFor(profile)
    }

    suspend fun signIn(email: String, password: String): Pair<RootState, String?> = try {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = client.auth.currentUserOrNull() ?: return RootState.LoggedOut to null
        val profile = fetchProfile(user.id)
        if (profile == null) {
            // Authenticated but no profile row: registration never completed.
            RootState.LoggedOut to null
        } else {
            stateFor(profile) to null
        }
    } catch (e: Exception) {
        // Covers wrong credentials, unverified email, network errors.
        RootState.LoggedOut to (e.message ?: "sign-in failed")
    }

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        username: String,
        classLevel: Int,
        medium: String,
    ): RegisterResult = try {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val user = client.auth.currentUserOrNull()
        if (user == null) {
            // Email confirmation is enabled on this project.
            RegisterResult.EmailConfirmationRequired
        } else {
            client.from("profiles").insert(
                ProfileInsert(
                    id = user.id,
                    username = username,
                    fullName = fullName,
                    classLevel = classLevel,
                    medium = medium,
                )
            )
            RegisterResult.ProfileCreated
        }
    } catch (e: Exception) {
        // Unique violation on username/email surfaces here as a generic postgrest error.
        RegisterResult.Failure(e.message ?: "registration failed")
    }

    suspend fun currentProfile(): ProfileDto? {
        val user = client.auth.currentUserOrNull() ?: return null
        return fetchProfile(user.id)
    }

    suspend fun refreshStatus(): RootState {
        val profile = currentProfile() ?: run {
            client.auth.signOut()
            return RootState.LoggedOut
        }
        return stateFor(profile)
    }

    suspend fun signOut() {
        runCatching { client.auth.signOut() }
    }

    private suspend fun fetchProfile(id: String): ProfileDto? = try {
        client.from("profiles")
            .select { filter { eq("id", id) } }
            .decodeSingle<ProfileDto>()
    } catch (e: Exception) {
        null
    }

    private fun stateFor(profile: ProfileDto): RootState = when (profile.approvalStatus) {
        "approved" -> RootState.Ready(profile)
        "pending" -> RootState.Pending(profile)
        "rejected" -> RootState.Rejected(profile)
        else -> RootState.Blocked(profile) // suspended / banned / deletion_pending
    }
}
