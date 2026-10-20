package com.shelfie.app.data.remote.shelfie

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST

/** Account and sync API of the Shelfie Worker (backend/src/auth.ts, backend/src/sync.ts). */
interface ShelfieApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: CredentialsRequest): SessionResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: CredentialsRequest): SessionResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header(AUTHORIZATION) bearer: String)

    @POST("api/sync")
    suspend fun sync(@Header(AUTHORIZATION) bearer: String, @Body request: SyncRequest): SyncResponse

    @DELETE("api/account")
    suspend fun deleteAccount(@Header(AUTHORIZATION) bearer: String)

    companion object {
        const val AUTHORIZATION = "Authorization"

        fun bearer(token: String) = "Bearer $token"
    }
}

@Serializable
data class CredentialsRequest(val email: String, val password: String, val installId: String)

@Serializable
data class SessionResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(val id: String, val email: String)

@Serializable
data class SyncRequest(val cursor: Long, val changes: List<SyncChangeDto>)

@Serializable
data class SyncResponse(val cursor: Long, val hasMore: Boolean, val records: List<SyncChangeDto>)

/** One record's full state; [data] is `null` for a deletion. */
@Serializable
data class SyncChangeDto(
    val collection: String,
    val syncId: String,
    val data: JsonObject? = null,
    val deleted: Boolean = false,
    val changedAt: Long,
)
