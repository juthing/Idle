package com.juthing.idle.domain

import com.google.common.truth.Truth.assertThat
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.usecase.TimerEvaluator
import org.junit.Test

class TimerEvaluatorTest {

    private val evaluator = TimerEvaluator()

    private val twoHourTimer = Rule.Timer(
        id = 1,
        name = "Video",
        packageNames = setOf("com.example.tube", "com.example.tok"),
        unlockMethodId = 1,
        dailyLimitMinutes = 120,
    )

    @Test
    fun `a fresh timer is not exhausted`() {
        assertThat(evaluator.isExhausted(twoHourTimer, usedMillis = 0)).isFalse()
    }

    @Test
    fun `a timer one minute short of its limit is not exhausted`() {
        assertThat(evaluator.isExhausted(twoHourTimer, usedMillis = 119 * 60_000L)).isFalse()
    }

    @Test
    fun `a timer is exhausted exactly on its limit`() {
        assertThat(evaluator.isExhausted(twoHourTimer, usedMillis = 120 * 60_000L)).isTrue()
    }

    @Test
    fun `a timer stays exhausted past its limit`() {
        assertThat(evaluator.isExhausted(twoHourTimer, usedMillis = 200 * 60_000L)).isTrue()
    }

    @Test
    fun `remaining time counts down from the limit`() {
        assertThat(evaluator.remainingMillis(twoHourTimer, usedMillis = 30 * 60_000L))
            .isEqualTo(90 * 60_000L)
    }

    @Test
    fun `remaining time never goes negative`() {
        assertThat(evaluator.remainingMillis(twoHourTimer, usedMillis = 500 * 60_000L)).isEqualTo(0)
    }
}
