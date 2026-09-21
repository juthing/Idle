package com.juthing.idle.data.mapper

import com.juthing.idle.data.local.entity.UnlockGrantEntity
import com.juthing.idle.data.local.entity.UnlockMethodEntity
import com.juthing.idle.domain.model.UnlockGrant
import com.juthing.idle.domain.model.UnlockMethod

fun UnlockMethodEntity.toDomain(): UnlockMethod = UnlockMethod(
    id = id,
    name = name,
    type = type,
    secretHash = secretHash,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    createdAt = createdAt,
)

fun UnlockMethod.toEntity(createdAt: Long): UnlockMethodEntity = UnlockMethodEntity(
    id = id,
    name = name,
    type = type,
    secretHash = secretHash,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    createdAt = if (this.createdAt == 0L) createdAt else this.createdAt,
)

fun UnlockGrantEntity.toDomain(): UnlockGrant = UnlockGrant(
    id = id,
    ruleId = ruleId,
    packageName = packageName,
    grantedAt = grantedAt,
    expiresAt = expiresAt,
)
