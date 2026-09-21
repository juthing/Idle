package com.juthing.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.juthing.idle.core.ui.theme.IdleTheme
import com.juthing.idle.ui.IdleApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity hosting the whole Compose UI.
 *
 * The blocking overlay lives in its own activity so that it can be shown on top of
 * other apps without dragging this one into the back stack.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IdleTheme {
                IdleApp()
            }
        }
    }
}
