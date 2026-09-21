package com.juthing.idle.data.mapper

import com.juthing.idle.data.local.dao.RuleWithDetails
import com.juthing.idle.data.local.entity.PeriodDetailEntity
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleEntity
import com.juthing.idle.data.local.entity.RuleType
import com.juthing.idle.data.local.entity.TimerDetailEntity
import com.juthing.idle.domain.model.Rule
import java.time.DayOfWeek

/**
 * Converts the 7-bit day mask stored in the database into [DayOfWeek] values.
 *
 * Bit 0 is Monday, matching `DayOfWeek.MONDAY.ordinal`.
 */
internal fun Int.toDaysOfWeek(): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { day -> this and (1 shl day.ordinal) != 0 }

/** Packs [DayOfWeek] values back into the 7-bit mask stored in the database. */
internal fun Set<DayOfWeek>.toDayMask(): Int =
    fold(0) { mask, day -> mask or (1 shl day.ordinal) }

/**
 * Turns a database row into a domain [Rule].
 *
 * @throws IllegalStateException when the detail row matching [RuleEntity.type] is missing, which
 *   would mean the database was written by something other than [toEntities].
 */
fun RuleWithDetails.toDomain(): Rule {
    val packageNames = apps.mapTo(mutableSetOf()) { it.packageName }
    return when (rule.type) {
        RuleType.PERIOD -> {
            val detail = checkNotNull(period) { "Period rule ${rule.id} has no schedule row" }
            Rule.Period(
                id = rule.id,
                name = rule.name,
                packageNames = packageNames,
                unlockMethodId = rule.unlockMethodId,
                enabled = rule.enabled,
                createdAt = rule.createdAt,
                daysOfWeek = detail.daysOfWeek.toDaysOfWeek(),
                startMinute = detail.startMinute,
                endMinute = detail.endMinute,
            )
        }

        RuleType.TIMER -> {
            val detail = checkNotNull(timer) { "Timer rule ${rule.id} has no quota row" }
            Rule.Timer(
                id = rule.id,
                name = rule.name,
                packageNames = packageNames,
                unlockMethodId = rule.unlockMethodId,
                enabled = rule.enabled,
                createdAt = rule.createdAt,
                dailyLimitMinutes = detail.dailyLimitMinutes,
            )
        }
    }
}

/** The database rows that make up a [Rule], ready to be written in one transaction. */
data class RuleRows(
    val rule: RuleEntity,
    val period: PeriodDetailEntity?,
    val timer: TimerDetailEntity?,
    val apps: List<RuleAppEntity>,
)

/**
 * Splits a domain [Rule] into the rows it is stored as.
 *
 * @param createdAt the creation timestamp to use for a rule that has never been saved.
 */
fun Rule.toEntities(createdAt: Long): RuleRows {
    val entity = RuleEntity(
        id = id,
        name = name,
        type = when (this) {
            is Rule.Period -> RuleType.PERIOD
            is Rule.Timer -> RuleType.TIMER
        },
        unlockMethodId = unlockMethodId,
        enabled = enabled,
        createdAt = if (this.createdAt == 0L) createdAt else this.createdAt,
    )
    return RuleRows(
        rule = entity,
        period = (this as? Rule.Period)?.let {
            PeriodDetailEntity(
                ruleId = id,
                daysOfWeek = it.daysOfWeek.toDayMask(),
                startMinute = it.startMinute,
                endMinute = it.endMinute,
            )
        },
        timer = (this as? Rule.Timer)?.let {
            TimerDetailEntity(ruleId = id, dailyLimitMinutes = it.dailyLimitMinutes)
        },
        apps = packageNames.map { RuleAppEntity(ruleId = id, packageName = it) },
    )
}
