package com.shelfie.app.data.sync

import com.shelfie.app.data.remote.shelfie.SpaceSettingsDto

/**
 * The settings a household shares (today: when an item turns "critical"), kept on the phone and
 * synced with the space by [SyncManager]: a local change waits until it is sent; the server's
 * copy is taken unless a newer local change is still waiting.
 */
interface SharedSettingsStore {

    /** The local change not sent yet, or `null`. */
    suspend fun pending(): SpaceSettingsDto?

    /** Applies the space's settings (or, with `null`, just confirms nothing newer is waiting). */
    suspend fun reconcile(remote: SpaceSettingsDto?, sent: SpaceSettingsDto?)
}
