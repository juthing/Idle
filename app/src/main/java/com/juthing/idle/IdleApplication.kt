package com.juthing.idle

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Only exists to install the Hilt dependency graph; no work is done at startup so
 * that cold launch stays immediate.
 */
@HiltAndroidApp
class IdleApplication : Application()
