package com.shelfie.app.domain.model

import java.time.LocalTime

/** Which meal a rescue recipe is for; picked from the time of day it is generated. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER;

    companion object {
        private val LUNCH_FROM: LocalTime = LocalTime.of(11, 0)
        private val DINNER_FROM: LocalTime = LocalTime.of(16, 0)

        fun at(time: LocalTime): MealType = when {
            time.isBefore(LUNCH_FROM) -> BREAKFAST
            time.isBefore(DINNER_FROM) -> LUNCH
            else -> DINNER
        }
    }
}
