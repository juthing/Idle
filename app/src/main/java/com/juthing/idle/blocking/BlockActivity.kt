package com.juthing.idle.blocking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.juthing.idle.core.ui.theme.IdleTheme
import com.juthing.idle.data.system.NfcTagReader
import com.juthing.idle.domain.repository.ThemeMode
import com.juthing.idle.ui.MainViewModel
import com.juthing.idle.ui.block.BlockScreen
import com.juthing.idle.ui.block.BlockViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The full-screen block, shown over the app the user tried to open.
 *
 * A separate activity from [com.juthing.idle.MainActivity] so that it can be launched on top of
 * another app without dragging Idle's own screens into that task.
 *
 * Leaving the screen sends the user home rather than back: going back would land on the blocked
 * app, which would immediately trigger the block again and feel like a loop.
 */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    private val viewModel: BlockViewModel by viewModels()
    private val themeViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var nfcTagReader: NfcTagReader

    /** The app this screen is currently blocking, kept so that [onResume] re-checks the right one. */
    private var blockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!adopt(intent)) return

        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            IdleTheme(darkTheme = darkTheme) {
                BlockScreen(
                    nfcTagReader = nfcTagReader,
                    onDismiss = ::goHome,
                    viewModel = viewModel,
                )
            }
        }
    }

    /**
     * Takes over for a second app while the screen is already up.
     *
     * The activity is `singleTask`, so opening another blocked app delivers a new intent here
     * instead of creating a second instance. Reading it is what keeps the screen from insisting
     * on the app the user has already left.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        adopt(intent)
    }

    /**
     * Re-checks the block when the screen comes back to the foreground.
     *
     * The user may have unlocked from elsewhere, or the period may have ended while they were
     * away in Android settings.
     */
    override fun onResume() {
        super.onResume()
        blockedPackage?.let(viewModel::start)
    }

    /**
     * Leaves nothing behind once the screen is no longer in front.
     *
     * The block screen is a moment, not a place: a stale instance sitting in the back stack would
     * come back on its own the next time the task was resumed.
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    /** @return whether the intent named an app to block; the screen has no reason to exist if not. */
    private fun adopt(intent: Intent): Boolean {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null) {
            finish()
            return false
        }
        blockedPackage = packageName
        viewModel.start(packageName)
        return true
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "blocked_package"

        /** The intent that shows the block screen for [packageName]. */
        fun intentFor(context: Context, packageName: String): Intent =
            Intent(context, BlockActivity::class.java).putExtra(EXTRA_PACKAGE, packageName)
    }
}
