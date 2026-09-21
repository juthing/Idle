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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null) {
            finish()
            return
        }
        viewModel.start(packageName)

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
     * Re-checks the block when the screen comes back to the foreground.
     *
     * The user may have unlocked from elsewhere, or the period may have ended while they were
     * away in Android settings.
     */
    override fun onResume() {
        super.onResume()
        intent.getStringExtra(EXTRA_PACKAGE)?.let(viewModel::start)
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
