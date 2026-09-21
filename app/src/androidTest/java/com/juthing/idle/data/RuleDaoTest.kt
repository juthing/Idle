package com.juthing.idle.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.juthing.idle.data.local.IdleDatabase
import com.juthing.idle.data.local.entity.PeriodDetailEntity
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleEntity
import com.juthing.idle.data.local.entity.RuleType
import com.juthing.idle.data.local.entity.UnlockMethodEntity
import com.juthing.idle.data.mapper.toDomain
import com.juthing.idle.domain.model.Rule
import com.juthing.idle.domain.model.UnlockMethodType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * Checks that a period survives a write and read unchanged, and that the foreign keys behave as
 * the schema intends: deleting a rule drops its details, deleting a method in use is refused.
 */
@RunWith(AndroidJUnit4::class)
class RuleDaoTest {

    private lateinit var database: IdleDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IdleDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun periodRoundTripsThroughTheDatabase() = runTest {
        val methodId = database.unlockMethodDao().insert(
            UnlockMethodEntity(
                name = "QR on the fridge",
                type = UnlockMethodType.QR,
                secretHash = "abc123",
                createdAt = 0,
            ),
        )
        val dao = database.ruleDao()
        val ruleId = dao.insertRule(
            RuleEntity(
                name = "Night",
                type = RuleType.PERIOD,
                unlockMethodId = methodId,
                createdAt = 0,
            ),
        )
        // 22:00 -> 07:00 on weekdays: a range that crosses midnight.
        dao.upsertPeriod(
            PeriodDetailEntity(
                ruleId = ruleId,
                daysOfWeek = 0b0011111,
                startMinute = 22 * 60,
                endMinute = 7 * 60,
            ),
        )
        dao.insertApps(listOf(RuleAppEntity(ruleId, "com.example.social")))

        val loaded = dao.getById(ruleId)?.toDomain() as Rule.Period

        assertEquals("Night", loaded.name)
        assertEquals(setOf("com.example.social"), loaded.packageNames)
        assertEquals(22 * 60, loaded.startMinute)
        assertEquals(7 * 60, loaded.endMinute)
        assertEquals(
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            loaded.daysOfWeek,
        )
    }

    @Test
    fun deletingARuleAlsoDropsItsDetails() = runTest {
        val methodId = database.unlockMethodDao().insert(
            UnlockMethodEntity(name = "Tag", type = UnlockMethodType.NFC, secretHash = "x", createdAt = 0),
        )
        val dao = database.ruleDao()
        val ruleId = dao.insertRule(
            RuleEntity(name = "Night", type = RuleType.PERIOD, unlockMethodId = methodId, createdAt = 0),
        )
        dao.upsertPeriod(PeriodDetailEntity(ruleId, 0b1111111, 0, 60))
        dao.insertApps(listOf(RuleAppEntity(ruleId, "com.example.social")))

        dao.delete(ruleId)

        assertNull(dao.getById(ruleId))
    }

    @Test
    fun anUnlockMethodStillGuardingARuleCannotBeDeleted() = runTest {
        val methodId = database.unlockMethodDao().insert(
            UnlockMethodEntity(name = "Tag", type = UnlockMethodType.NFC, secretHash = "x", createdAt = 0),
        )
        database.ruleDao().insertRule(
            RuleEntity(name = "Night", type = RuleType.PERIOD, unlockMethodId = methodId, createdAt = 0),
        )

        val failed = runCatching { database.unlockMethodDao().delete(methodId) }.isFailure

        assertTrue("Deleting a method in use must be refused by the foreign key", failed)
    }
}
