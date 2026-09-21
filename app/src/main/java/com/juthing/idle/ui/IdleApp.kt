package com.juthing.idle.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.juthing.idle.core.ui.tap
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.data.system.PermissionChecker
import com.juthing.idle.ui.navigation.AboutRoute
import com.juthing.idle.ui.navigation.AppearanceRoute
import com.juthing.idle.ui.navigation.BlockingSettingsRoute
import com.juthing.idle.ui.navigation.CreateMethodRoute
import com.juthing.idle.ui.navigation.PeriodsRoute
import com.juthing.idle.ui.navigation.PermissionsRoute
import com.juthing.idle.ui.navigation.RuleEditRoute
import com.juthing.idle.ui.navigation.RuleUnlockRoute
import com.juthing.idle.ui.navigation.SettingsRoute
import com.juthing.idle.ui.navigation.TimersRoute
import com.juthing.idle.ui.navigation.TopLevelDestination
import com.juthing.idle.ui.navigation.UnlockMethodsRoute
import com.juthing.idle.ui.periods.PeriodsScreen
import com.juthing.idle.ui.ruleedit.RuleEditScreen
import com.juthing.idle.ui.ruleunlock.RuleUnlockScreen
import com.juthing.idle.ui.settings.AboutScreen
import com.juthing.idle.ui.settings.AppearanceScreen
import com.juthing.idle.ui.settings.BlockingSettingsScreen
import com.juthing.idle.ui.settings.PermissionsScreen
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
    permissionChecker: PermissionChecker,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val haptics = LocalHapticFeedback.current

    /**
     * The tab the bar draws as current.
     *
     * Tracked rather than read from the destination: the screens a tab leads to — writing a rule,
     * registering a method, a settings page — are their own destinations, and matching on the
     * destination alone would un-highlight the whole bar the moment the user went one level
     * deeper. The tab that led there stays lit until another tab is chosen.
     */
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = TopLevelDestination.entries[selectedTabIndex]

    LaunchedEffect(currentDestination) {
        val landed = TopLevelDestination.entries.indexOfFirst { destination ->
            currentDestination?.hasRouteOf(destination) == true
        }
        if (landed >= 0) selectedTabIndex = landed
    }

    Scaffold(
        // The bar is the only thing this Scaffold owns. Every screen below draws its own top app
        // bar and handles the status bar itself; letting this one add the inset too is what put a
        // second, empty status bar's worth of space above every screen.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = destination == selectedTab

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) haptics.tap()
                            navController.navigateToTopLevel(destination)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.icon
                                },
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
                    onOpenAppearance = { navController.navigate(AppearanceRoute) },
                    onOpenBlocking = { navController.navigate(BlockingSettingsRoute) },
                    onOpenUnlockMethods = { navController.navigate(UnlockMethodsRoute) },
                    onOpenPermissions = { navController.navigate(PermissionsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                )
            }
            composable<AppearanceRoute> {
                AppearanceScreen(onBack = { navController.popBackStack() })
            }
            composable<BlockingSettingsRoute> {
                BlockingSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<PermissionsRoute> {
                PermissionsScreen(
                    permissionChecker = permissionChecker,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<AboutRoute> {
                AboutScreen(onBack = { navController.popBackStack() })
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
                    // A method created from a rule editor is handed straight back to it, so that
                    // the person who went looking for one does not have to find it again.
                    onCreated = { methodId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(CREATED_METHOD_ID, methodId)
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<RuleEditRoute> { entry ->
                RuleEditScreen(
                    createdMethodId = entry.savedStateHandle.get<Long>(CREATED_METHOD_ID),
                    onCreatedMethodConsumed = { entry.savedStateHandle.remove<Long>(CREATED_METHOD_ID) },
                    onCreateMethod = { navController.navigate(CreateMethodRoute) },
                    onUnlock = { ruleId -> navController.navigate(RuleUnlockRoute(ruleId)) },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<RuleUnlockRoute> {
                RuleUnlockScreen(
                    nfcTagReader = nfcTagReader,
                    onDone = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

/** The key a freshly created unlock method travels back to the rule editor under. */
private const val CREATED_METHOD_ID = "created_method_id"

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

private fun NavDestination.hasRouteOf(destination: TopLevelDestination): Boolean =
    when (destination) {
        TopLevelDestination.PERIODS -> hasRoute(PeriodsRoute::class)
        TopLevelDestination.TIMERS -> hasRoute(TimersRoute::class)
        TopLevelDestination.SETTINGS -> hasRoute(SettingsRoute::class)
    }
