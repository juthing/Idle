package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.usecase.ReconcileUsageUseCase
import com.juthing.idle.fake.FakeClock
import com.juthing.idle.fake.FakeSystemUsageSource
import com.juthing.idle.fake.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReconcileUsageUseCaseTest {

    private val tube = "com.example.tube"
    private val clock = FakeClock()

    @Test
    fun `a gap in our own counting is filled from the system figures`() = runTest {
        // A reboot lost most of the session: the system saw an hour, we recorded ten minutes.
        val repository = FakeUsageRepository(mutableMapOf(tube to 10 * 60_000L))
        val useCase = ReconcileUsageUseCase(
            FakeSystemUsageSource(usage = mapOf(tube to 60 * 60_000L)),
            repository,
            clock,
        )

        useCase()

        assertThat(repository.todayUsage()[tube]).isEqualTo(60 * 60_000L)
    }

    @Test
    fun `our own count wins when it is the larger one`() = runTest {
        // The system aggregates coarsely and can lag behind; never hand time back.
        val repository = FakeUsageRepository(mutableMapOf(tube to 90 * 60_000L))
        val useCase = ReconcileUsageUseCase(
            FakeSystemUsageSource(usage = mapOf(tube to 60 * 60_000L)),
            repository,
            clock,
        )

        useCase()

        assertThat(repository.todayUsage()[tube]).isEqualTo(90 * 60_000L)
    }

    @Test
    fun `an app the system saw but we never did is recorded`() = runTest {
        val repository = FakeUsageRepository()
        val useCase = ReconcileUsageUseCase(
            FakeSystemUsageSource(usage = mapOf(tube to 25 * 60_000L)),
            repository,
            clock,
        )

        useCase()

        assertThat(repository.todayUsage()[tube]).isEqualTo(25 * 60_000L)
    }

    @Test
    fun `without usage access nothing is touched`() = runTest {
        val repository = FakeUsageRepository(mutableMapOf(tube to 10 * 60_000L))
        val useCase = ReconcileUsageUseCase(
            FakeSystemUsageSource(permitted = false, usage = mapOf(tube to 60 * 60_000L)),
            repository,
            clock,
        )

        useCase()

        assertThat(repository.todayUsage()[tube]).isEqualTo(10 * 60_000L)
    }
}
