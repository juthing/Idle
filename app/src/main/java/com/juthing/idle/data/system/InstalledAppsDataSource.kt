package com.juthing.idle.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.juthing.idle.domain.model.InstalledApp
import com.juthing.idle.domain.repository.InstalledAppsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the installed apps through [PackageManager].
 *
 * Idle itself is filtered out of the list: letting the user block Idle would lock them out of the
 * only screen where a block can be lifted.
 */
@Singleton
class InstalledAppsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InstalledAppsRepository {

    private val packageManager: PackageManager get() = context.packageManager

    override suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map(::toInstalledApp)
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override suspend fun labelFor(packageName: String): String = withContext(Dispatchers.IO) {
        runCatching {
            packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
        }.getOrDefault(packageName)
    }

    private fun toInstalledApp(resolveInfo: ResolveInfo) = InstalledApp(
        packageName = resolveInfo.activityInfo.packageName,
        label = resolveInfo.loadLabel(packageManager).toString(),
    )
}
