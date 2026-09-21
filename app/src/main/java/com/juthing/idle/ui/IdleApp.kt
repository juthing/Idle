package com.juthing.idle.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.ui.navigation.CreateMethodRoute
import com.juthing.idle.ui.navigation.PeriodsRoute
import com.juthing.idle.ui.navigation.RuleEditRoute
import com.juthing.idle.ui.navigation.SettingsRoute
import com.juthing.idle.ui.navigation.TimersRoute
import com.juthing.idle.ui.navigation.TopLevelDestination
import com.juthing.idle.ui.navigation.UnlockMethodsRoute
import com.juthing.idle.ui.periods.PeriodsScreen
import com.juthing.idle.ui.ruleedit.RuleEditScreen
import com.juthing.idle.ui.settings.SettingsScreen
import com.juthing.idle.ui.timers.TimersScreen
import com.juthing.idle.ui.unlockmethods.CreateMethodScreen
import com.juthing.idle.ui.unlockmethods.UnlockMethodsScreen

/**
 * Hosts the three top-level sections behind a bottom navigation bar.
 *
 * Switching tabs never grows the back stack: each tab is restored to the state it was
 * left in, and the system back button always returns to the Periods tab.
 *
 * @param nfcTagReader passed down rather than injected where it is used: reader mode binds to the
 *   hosting activity, so the single instance has to be the one the activity knows about.
 */
@Composable
fun IdleApp(
    nfcTagReader: NfcTagReader,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy()?.any {
                        it.hasRouteOf(destination)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PeriodsRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable<PeriodsRoute> {
                PeriodsScreen(
                    onEditPeriod = { ruleId ->
                        navController.navigate(RuleEditRoute(ruleId = ruleId, isPeriod = true))
                    },
                )
            }
            composable<TimersRoute> {
                TimersScreen(
                    onEditTimer = { ruleId ->
                        navController.navigate(RuleEditRoute(ruleId = ruleId, isPeriod = false))
                    },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onOpenUnlockMethods = { navController.navigate(UnlockMethodsRoute) },
                )
            }
            composable<UnlockMethodsRoute> {
                UnlockMethodsScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(CreateMethodRoute) },
                )
            }
            composable<CreateMethodRoute> {
                CreateMethodScreen(
                    nfcTagReader = nfcTagReader,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<RuleEditRoute> {
                RuleEditScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Navigates to a top-level [destination], keeping a single entry per tab and
 * restoring whatever state that tab had when it was last visited.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun androidx.navigation.NavDestination.hierarchy(): Sequence<androidx.navigation.NavDestination> =
    generateSequence(this) { it.parent }

private fun androidx.navigation.NavDestination.hasRouteOf(destination: TopLevelDestination): Boolean =
    when (destination) {
        TopLevelDestination.PERIODS -> hasRoute(PeriodsRoute::class)
        TopLevelDestination.TIMERS -> hasRoute(TimersRoute::class)
        TopLevelDestination.SETTINGS -> hasRoute(SettingsRoute::class)
    }
