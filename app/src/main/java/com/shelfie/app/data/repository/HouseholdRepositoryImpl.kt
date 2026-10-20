package com.shelfie.app.data.repository

import com.shelfie.app.data.remote.gemini.GeminiConfig
import com.shelfie.app.data.remote.shelfie.HouseholdDto
import com.shelfie.app.data.remote.shelfie.InviteDto
import com.shelfie.app.data.remote.shelfie.JoinHouseholdRequest
import com.shelfie.app.data.remote.shelfie.RemoveMemberRequest
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.remote.shelfie.VerifyPurchaseRequest
import com.shelfie.app.data.sync.AccountStore
import com.shelfie.app.data.sync.SyncManager
import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdException
import com.shelfie.app.domain.model.HouseholdInvite
import com.shelfie.app.domain.model.HouseholdMember
import com.shelfie.app.domain.model.HouseholdRole
import com.shelfie.app.domain.model.QuotaStatus
import com.shelfie.app.domain.repository.HouseholdRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepositoryImpl @Inject constructor(
    private val api: ShelfieApiService,
    private val accountStore: AccountStore,
    private val syncManager: SyncManager,
    private val config: GeminiConfig,
) : HouseholdRepository {

    override suspend fun household(): Result<Household> = call { api.household(it).toDomain() }

    override suspend fun createInvite(): Result<HouseholdInvite> = call { api.createInvite(it).toDomain() }

    override suspend fun join(code: String): Result<Household> =
        call { api.joinHousehold(it, JoinHouseholdRequest(code.trim())).toDomain() }.alsoSync()

    override suspend fun leave(): Result<Household> = call { api.leaveHousehold(it).toDomain() }.alsoSync()

    override suspend fun removeMember(email: String): Result<Household> =
        call { api.removeMember(it, RemoveMemberRequest(email)).toDomain() }

    override suspend fun quota(): Result<QuotaStatus> = guarded {
        val session = accountStore.currentSession()
        val dto = api.quota(
            proxyToken = config.proxyToken,
            installId = accountStore.installId(),
            bearer = session?.let { ShelfieApiService.bearer(it.token) },
        )
        QuotaStatus(
            isPremium = dto.plan == "premium",
            premiumUntil = dto.premiumUntil?.let(Instant::ofEpochMilli),
            resetsAt = Instant.ofEpochMilli(dto.resetsAt),
            scansUsed = dto.scan.used,
            scanLimit = dto.scan.limit,
            recipesUsed = dto.recipe.used,
            recipeLimit = dto.recipe.limit,
        )
    }

    override suspend fun verifyPurchase(productId: String, purchaseToken: String): Result<Boolean> =
        call { api.verifyPurchase(it, VerifyPurchaseRequest(productId, purchaseToken)).entitled }

    /** Moving between households changes the sync space; syncing now merges the data at once. */
    private suspend fun <T> Result<T>.alsoSync(): Result<T> {
        if (isSuccess) syncManager.sync()
        return this
    }

    private suspend fun <T> call(block: suspend (bearer: String) -> T): Result<T> {
        val session = accountStore.currentSession() ?: return Result.failure(HouseholdException.NotSignedIn())
        return guarded { block(ShelfieApiService.bearer(session.token)) }
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Result.failure(
            when (e.code()) {
                400, 404 -> HouseholdException.InvalidCode(e)
                409 -> HouseholdException.Conflict(e.serverMessage(), e)
                else -> HouseholdException.Server(e)
            },
        )
    } catch (e: IOException) {
        Result.failure(HouseholdException.Network(e))
    } catch (e: Exception) {
        Result.failure(HouseholdException.Server(e))
    }

    private fun HttpException.serverMessage(): String? = runCatching {
        val body = response()?.errorBody()?.string().orEmpty()
        Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
    }.getOrNull()

    private fun HouseholdDto.toDomain() = Household(
        role = when (role) {
            "owner" -> HouseholdRole.OWNER
            "member" -> HouseholdRole.MEMBER
            else -> HouseholdRole.NONE
        },
        members = members.map { HouseholdMember(email = it.email, isOwner = it.isOwner) },
        invite = invite?.toDomain(),
        maxMembers = maxMembers,
    )

    private fun InviteDto.toDomain() = HouseholdInvite(code = code, expiresAt = Instant.ofEpochMilli(expiresAt))
}
