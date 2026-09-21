package com.juthing.idle.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
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
 * A tab of the bottom navigation bar.
 *
 * @property route the type-safe navigation route this tab points at.
 * @property labelRes the localised label shown under the icon.
 * @property icon the outlined icon shown when the tab is not selected.
 * @property selectedIcon the filled icon shown when the tab is selected.
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
        selectedIcon = Icons.Filled.HourglassEmpty,
    ),
    SETTINGS(
        route = SettingsRoute,
        labelRes = R.string.nav_settings,
        icon = Icons.Outlined.Tune,
        selectedIcon = Icons.Filled.Tune,
    ),
}
