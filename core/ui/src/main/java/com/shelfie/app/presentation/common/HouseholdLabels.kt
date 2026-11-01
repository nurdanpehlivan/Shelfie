package com.shelfie.app.presentation.common

import androidx.annotation.StringRes
import com.shelfie.app.core.ui.R
import com.shelfie.app.domain.model.HouseholdAction

/** What happened, as a short label: "Added", "Thrown away". */
@get:StringRes
val HouseholdAction.activityLabelRes: Int
    get() = when (this) {
        HouseholdAction.ADDED -> R.string.activity_added
        HouseholdAction.USED -> R.string.activity_used
        HouseholdAction.USED_SOME -> R.string.activity_used_some
        HouseholdAction.WASTED -> R.string.activity_wasted
        HouseholdAction.REMOVED -> R.string.activity_removed
        HouseholdAction.FROZE -> R.string.activity_froze
        HouseholdAction.THAWED -> R.string.activity_thawed
        HouseholdAction.LIST_ADDED -> R.string.activity_list_added
        HouseholdAction.LIST_BOUGHT -> R.string.activity_list_bought
        HouseholdAction.RECIPE_SAVED -> R.string.activity_recipe_saved
    }

/** A household member as shown to the others: the part of the email before the "@". */
fun displayName(email: String): String = email.substringBefore('@').ifBlank { email }
