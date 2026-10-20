package com.shelfie.app.domain.model

import java.time.Instant

/**
 * The account's household: people who share one pantry, shopping list, recipe history and meal
 * plan. A solo account is [HouseholdRole.NONE].
 */
data class Household(
    val role: HouseholdRole,
    /** The owner first. */
    val members: List<HouseholdMember>,
    /** The owner's current invite code, if one is still valid. */
    val invite: HouseholdInvite?,
    val maxMembers: Int,
)

enum class HouseholdRole { NONE, OWNER, MEMBER }

data class HouseholdMember(val email: String, val isOwner: Boolean)

data class HouseholdInvite(val code: String, val expiresAt: Instant)

/** Typed failures of household actions, carried inside `Result.failure`. */
sealed class HouseholdException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotSignedIn : HouseholdException("Not signed in")

    /** The code is unknown or expired. */
    class InvalidCode(cause: Throwable? = null) : HouseholdException("Invalid invite code", cause)

    /** Full household, own household, or the user still has members of their own. */
    class Conflict(val serverMessage: String?, cause: Throwable? = null) : HouseholdException("Household conflict", cause)

    class Network(cause: Throwable) : HouseholdException("Network error", cause)

    class Server(cause: Throwable) : HouseholdException("Server error", cause)
}

/** The plan and today's AI usage, as counted by the server. */
data class QuotaStatus(
    val isPremium: Boolean,
    val premiumUntil: Instant?,
    val resetsAt: Instant,
    val scansUsed: Int,
    val scanLimit: Int,
    val recipesUsed: Int,
    val recipeLimit: Int,
)
