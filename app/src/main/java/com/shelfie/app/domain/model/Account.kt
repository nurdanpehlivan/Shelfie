package com.shelfie.app.domain.model

import java.time.Instant

/** The signed-in sync account. */
data class Account(
    val email: String,
    /** End of the last complete sync, or `null` before the first one. */
    val lastSyncAt: Instant?,
)

/** Typed failures of sign-in and sign-up, carried inside `Result.failure`. */
sealed class AccountException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class InvalidCredentials(cause: Throwable? = null) : AccountException("Wrong email or password", cause)

    class EmailTaken(cause: Throwable? = null) : AccountException("Email already registered", cause)

    /** Email not valid or password too short; the form checks this first, the server again. */
    class InvalidInput(cause: Throwable? = null) : AccountException("Invalid email or password", cause)

    class TooManyAttempts(cause: Throwable? = null) : AccountException("Too many attempts", cause)

    class Network(cause: Throwable) : AccountException("Network error", cause)

    class Server(cause: Throwable) : AccountException("Server error", cause)
}

/** Typed failures of a sync run, carried inside `Result.failure`. */
sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The server no longer accepts the session; the app has signed out locally. */
    class SessionExpired(cause: Throwable) : SyncException("Session expired", cause)

    class Network(cause: Throwable) : SyncException("Network error", cause)

    class Server(val httpCode: Int?, cause: Throwable) : SyncException("Sync failed (HTTP $httpCode)", cause)
}
