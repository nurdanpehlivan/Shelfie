package com.shelfie.app.data.remote.shelfie

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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

    @GET("api/household")
    suspend fun household(@Header(AUTHORIZATION) bearer: String): HouseholdDto

    @POST("api/household/invite")
    suspend fun createInvite(@Header(AUTHORIZATION) bearer: String): InviteDto

    @POST("api/household/join")
    suspend fun joinHousehold(@Header(AUTHORIZATION) bearer: String, @Body request: JoinHouseholdRequest): HouseholdDto

    @POST("api/household/leave")
    suspend fun leaveHousehold(@Header(AUTHORIZATION) bearer: String): HouseholdDto

    @POST("api/household/remove")
    suspend fun removeMember(@Header(AUTHORIZATION) bearer: String, @Body request: RemoveMemberRequest): HouseholdDto

    /** Plan and today's AI usage; [bearer] is optional (signed-out users are counted per install). */
    @GET("api/quota")
    suspend fun quota(
        @Header(PROXY_TOKEN_HEADER) proxyToken: String,
        @Header(INSTALL_HEADER) installId: String,
        @Header(AUTHORIZATION) bearer: String?,
    ): QuotaDto

    @POST("api/billing/verify")
    suspend fun verifyPurchase(@Header(AUTHORIZATION) bearer: String, @Body request: VerifyPurchaseRequest): VerifyPurchaseResponse

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val PROXY_TOKEN_HEADER = "x-shelfie-token"
        const val INSTALL_HEADER = "x-shelfie-install"

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

/** [space] is the data space synced with: the account's own, or its household owner's. */
@Serializable
data class SyncResponse(val cursor: Long, val hasMore: Boolean, val records: List<SyncChangeDto>, val space: String? = null)

@Serializable
data class HouseholdDto(
    /** "none", "owner" or "member". */
    val role: String,
    val members: List<HouseholdMemberDto>,
    val invite: InviteDto? = null,
    val maxMembers: Int,
)

@Serializable
data class HouseholdMemberDto(val email: String, val isOwner: Boolean, val joinedAt: Long? = null)

@Serializable
data class InviteDto(val code: String, val expiresAt: Long)

@Serializable
data class JoinHouseholdRequest(val code: String)

@Serializable
data class RemoveMemberRequest(val email: String)

@Serializable
data class QuotaDto(
    val plan: String,
    val premiumUntil: Long? = null,
    val resetsAt: Long,
    val scan: UsageDto,
    val recipe: UsageDto,
)

@Serializable
data class UsageDto(val used: Int, val limit: Int)

@Serializable
data class VerifyPurchaseRequest(val productId: String, val purchaseToken: String)

@Serializable
data class VerifyPurchaseResponse(val entitled: Boolean, val premiumUntil: Long? = null)

/** One record's full state; [data] is `null` for a deletion. */
@Serializable
data class SyncChangeDto(
    val collection: String,
    val syncId: String,
    val data: JsonObject? = null,
    val deleted: Boolean = false,
    val changedAt: Long,
)
