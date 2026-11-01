package com.shelfie.app.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shelfie.app.di.ApplicationScope
import com.shelfie.app.domain.repository.AccountRepository
import com.shelfie.app.work.HouseholdActivityWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging. The server sends a data-only "household_activity" message when
 * someone else in the household changes something; the worker then fetches the feed, pulls the
 * changes and posts the notification in this phone's language.
 */
@AndroidEntryPoint
class FrizzyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var accountRepository: AccountRepository

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == TYPE_HOUSEHOLD_ACTIVITY) HouseholdActivityWorker.runNow(this)
    }

    /** A new token replaces the old one on the server; signed out, it's sent at the next sign-in. */
    override fun onNewToken(token: String) {
        applicationScope.launch { accountRepository.registerPushToken(token) }
    }

    private companion object {
        const val TYPE_HOUSEHOLD_ACTIVITY = "household_activity"
    }
}
