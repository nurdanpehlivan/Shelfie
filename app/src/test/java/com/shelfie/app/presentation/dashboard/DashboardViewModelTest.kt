package com.shelfie.app.presentation.dashboard

import com.shelfie.app.domain.model.AppSettings
import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.model.StorageLocation
import com.shelfie.app.domain.repository.PantryRepository
import com.shelfie.app.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 22)
    private val chicken = PantryItem(
        id = 1,
        name = "Tavuk",
        category = Category.MEAT,
        purchaseDate = today,
        expiryDate = today.plusDays(1),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val items = MutableStateFlow(listOf(chicken))
    private val pantry = mockk<PantryRepository> {
        every { observePantryItems() } returns items
        coEvery { updatePantryItem(any()) } just runs
    }
    private val settings = mockk<SettingsRepository> { every { settings } returns flowOf(AppSettings()) }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `freeze from the item sheet moves the item to the freezer and closes the sheet`() = runTest {
        val viewModel = DashboardViewModel(pantry, mockk(), settings, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onIntent(DashboardIntent.OpenItem(1))
        assertEquals(chicken, viewModel.editItem.value)

        viewModel.onIntent(DashboardIntent.Freeze(1))
        runCurrent()

        assertNull(viewModel.editItem.value)
        coVerify {
            pantry.updatePantryItem(
                match {
                    it.location == StorageLocation.FREEZER &&
                        it.expiryDate == today.plusDays(Category.MEAT.freezerShelfLifeDays.toLong())
                },
            )
        }
    }
}
