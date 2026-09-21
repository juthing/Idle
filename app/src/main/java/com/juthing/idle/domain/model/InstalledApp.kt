package com.juthing.idle.domain.model

/**
 * An app the user can pick as the target of a rule.
 *
 * The icon is deliberately absent: it is an Android drawable, it cannot be compared or stored,
 * and loading it belongs to the UI layer. Only what identifies and names the app lives here.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
