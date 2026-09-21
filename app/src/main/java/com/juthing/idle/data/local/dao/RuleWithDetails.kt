package com.juthing.idle.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.juthing.idle.data.local.entity.PeriodDetailEntity
import com.juthing.idle.data.local.entity.RuleAppEntity
import com.juthing.idle.data.local.entity.RuleEntity
import com.juthing.idle.data.local.entity.TimerDetailEntity

/**
 * A rule together with everything needed to turn it into a domain model.
 *
 * Exactly one of [period] and [timer] is non-null, decided by [RuleEntity.type]; the mapper
 * treats a row where that does not hold as corrupt rather than guessing.
 */
data class RuleWithDetails(
    @Embedded val rule: RuleEntity,
    @Relation(parentColumn = "id", entityColumn = "rule_id")
    val apps: List<RuleAppEntity>,
    @Relation(parentColumn = "id", entityColumn = "rule_id")
    val period: PeriodDetailEntity?,
    @Relation(parentColumn = "id", entityColumn = "rule_id")
    val timer: TimerDetailEntity?,
)
