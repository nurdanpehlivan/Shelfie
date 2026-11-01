package com.shelfie.app.data.repository

import com.shelfie.app.data.remote.shelfie.ActivityDto
import com.shelfie.app.data.remote.shelfie.HouseholdDto
import com.shelfie.app.data.remote.shelfie.InviteDto
import com.shelfie.app.data.remote.shelfie.JoinHouseholdRequest
import com.shelfie.app.data.remote.shelfie.RemoveMemberRequest
import com.shelfie.app.data.remote.shelfie.ShelfieApiService
import com.shelfie.app.data.sync.AccountStore
import com.shelfie.app.data.sync.SyncManager
import com.shelfie.app.domain.model.Household
import com.shelfie.app.domain.model.HouseholdAction
import com.shelfie.app.domain.model.HouseholdActivity
import com.shelfie.app.domain.model.HouseholdException
import com.shelfie.app.domain.model.HouseholdInvite
import com.shelfie.app.domain.model.HouseholdMember
import com.shelfie.app.domain.model.HouseholdRole
import com.shelfie.app.domain.repository.HouseholdRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
) : HouseholdRepository {

    override suspend fun household(): Result<Household> = call { api.household(it).toDomain() }

    override suspend fun createInvite(): Result<HouseholdInvite> = call { api.createInvite(it).toDomain() }

    override suspend fun join(code: String): Result<Household> =
        call { api.joinHousehold(it, JoinHouseholdRequest(code.trim())).toDomain() }.alsoSync()

    override suspend fun leave(): Result<Household> = call { api.leaveHousehold(it).toDomain() }.alsoSync()

    override suspend fun removeMember(email: String): Result<Household> =
        call { api.removeMember(it, RemoveMemberRequest(email)).toDomain() }

    /** The last fetched feed; [unseenActivityCount] counts from it. */
    private val latestActivity = MutableStateFlow<List<HouseholdActivity>>(emptyList())

    override val unseenActivityCount: Flow<Int> =
        combine(latestActivity, accountStore.activitySeenId) { events, seenId ->
            events.count { !it.isMe && it.id > seenId }
        }

    override suspend fun activity(): Result<List<HouseholdActivity>> =
        call { bearer -> api.householdActivity(bearer).events.mapNotNull { it.toDomain() } }
            .onSuccess { latestActivity.value = it }

    override suspend fun markActivitySeen() {
        latestActivity.value.maxOfOrNull { it.id }?.let { accountStore.markActivitySeen(it) }
    }

    override suspend fun takeActivityToNotify(): List<HouseholdActivity> {
        val events = activity().getOrNull() ?: return emptyList()
        val newest = events.maxOfOrNull { it.id } ?: return emptyList()
        val notifiedId = accountStore.activityNotifiedId()
        accountStore.markActivityNotified(newest)
        if (notifiedId == null) return emptyList()
        val fresh = events.filter { !it.isMe && it.id > notifiedId }.sortedBy { it.id }
        if (fresh.isNotEmpty()) syncManager.sync()
        return fresh
    }

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

    /** `null` for an action this version doesn't know (added by a newer server). */
    private fun ActivityDto.toDomain(): HouseholdActivity? {
        val action = runCatching { HouseholdAction.valueOf(action.uppercase()) }.getOrNull() ?: return null
        return HouseholdActivity(
            id = id,
            actor = actor,
            isMe = isMe,
            action = action,
            name = name,
            emoji = emoji,
            at = Instant.ofEpochMilli(at),
        )
    }

    private fun InviteDto.toDomain() = HouseholdInvite(code = code, expiresAt = Instant.ofEpochMilli(expiresAt))
}
