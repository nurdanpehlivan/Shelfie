package com.shelfie.app.work

import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class FreshnessCheckSchedulerTest {

    private val nine = LocalTime.of(9, 0)

    @Test
    fun `delays until 9 am today when it is earlier`() {
        val now = LocalDateTime.of(2026, 10, 8, 7, 30)
        assertEquals(Duration.ofMinutes(90), FreshnessCheckScheduler.delayUntilNext(now, nine))
    }

    @Test
    fun `delays until 9 am tomorrow when it is later or exactly 9`() {
        val afternoon = LocalDateTime.of(2026, 10, 8, 15, 0)
        assertEquals(Duration.ofHours(18), FreshnessCheckScheduler.delayUntilNext(afternoon, nine))

        val exactlyNine = LocalDateTime.of(2026, 10, 8, 9, 0)
        assertEquals(Duration.ofDays(1), FreshnessCheckScheduler.delayUntilNext(exactlyNine, nine))
    }

    @Test
    fun `the default 9 am check suggests breakfast`() {
        assertEquals(MealType.BREAKFAST, MealType.at(AppSettings().reminderTime))
        assertEquals(MealType.LUNCH, MealType.at(LocalTime.of(12, 30)))
        assertEquals(MealType.DINNER, MealType.at(LocalTime.of(19, 0)))
    }
}
