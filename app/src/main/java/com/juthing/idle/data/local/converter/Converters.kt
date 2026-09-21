package com.juthing.idle.data.local.converter

import androidx.room.TypeConverter
import com.juthing.idle.data.local.entity.RuleType
import com.juthing.idle.domain.model.UnlockMethodType

/**
 * Enum converters for Room.
 *
 * Enums are stored by name rather than by ordinal so that reordering a constant cannot silently
 * change the meaning of existing rows.
 */
class Converters {

    @TypeConverter
    fun ruleTypeToString(value: RuleType): String = value.name

    @TypeConverter
    fun stringToRuleType(value: String): RuleType = RuleType.valueOf(value)

    @TypeConverter
    fun unlockMethodTypeToString(value: UnlockMethodType): String = value.name

    @TypeConverter
    fun stringToUnlockMethodType(value: String): UnlockMethodType = UnlockMethodType.valueOf(value)
}
