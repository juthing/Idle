package com.juthing.idle.domain.repository

import com.juthing.idle.domain.model.InstalledApp

/** Lists the apps on the device that a rule can target. */
interface InstalledAppsRepository {

    /**
     * Every launchable app, sorted by label.
     *
     * Apps without a launcher entry are left out: the user cannot open them, so blocking them
     * would only add noise to the picker.
     */
    suspend fun getLaunchableApps(): List<InstalledApp>

    /** The display name of [packageName], or the package name itself if the app is gone. */
    suspend fun labelFor(packageName: String): String
}
