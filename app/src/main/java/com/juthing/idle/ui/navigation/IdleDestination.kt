package com.juthing.idle.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.juthing.idle.R
import kotlinx.serialization.Serializable

/** Routes reachable from the bottom navigation bar. */
@Serializable
data object PeriodsRoute

@Serializable
data object TimersRoute

@Serializable
data object SettingsRoute

/**
 * The screen that creates or edits one rule.
 *
 * @property ruleId the rule being edited, or `0` to create a new one.
 * @property isPeriod whether the rule is a period; a timer otherwise. Carried in the route so the
 *   screen knows which fields to show before anything is loaded.
 */
@Serializable
data class RuleEditRoute(val ruleId: Long = 0, val isPeriod: Boolean)

/**
 * The screen that lifts the lock on a running rule so it can be edited.
 *
 * Reached from the rule editor itself, which is where someone discovers the rule is sealed.
 */
@Serializable
data class RuleUnlockRoute(val ruleId: Long)

/** The list of unlock methods. */
@Serializable
data object UnlockMethodsRoute

/**
 * The screen that registers a new unlock method.
 *
 * The identifier of whatever it creates is handed back to the screen that opened it, so a rule
 * being written can adopt the method without the user going to look for it again.
 */
@Serializable
data object CreateMethodRoute

/** Appearance settings, on their own page. */
@Serializable
data object AppearanceRoute

/** Blocking behaviour settings, on their own page. */
@Serializable
data object BlockingSettingsRoute

/** What Idle needs from Android, and whether it has it. */
@Serializable
data object PermissionsRoute

/** What Idle is and what it does not do. */
@Serializable
data object AboutRoute

/**
 * A tab of the bottom navigation bar.
 *
 * @property route the type-safe navigation route this tab points at.
 * @property labelRes the localised label shown under the icon.
 * @property icon the outlined icon shown when the tab is not selected.
 * @property selectedIcon the filled icon shown when the tab is selected, so the current section
 *   is legible from the shape alone rather than only from the highlight behind it.
 */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    PERIODS(
        route = PeriodsRoute,
        labelRes = R.string.nav_periods,
        icon = Icons.Outlined.Schedule,
        selectedIcon = Icons.Filled.Schedule,
    ),
    TIMERS(
        route = TimersRoute,
        labelRes = R.string.nav_timers,
        icon = Icons.Outlined.HourglassEmpty,
        selectedIcon = Icons.Filled.HourglassTop,
    ),
    SETTINGS(
        route = SettingsRoute,
        labelRes = R.string.nav_settings,
        icon = Icons.Outlined.Tune,
        selectedIcon = Icons.Filled.Tune,
    ),
}
