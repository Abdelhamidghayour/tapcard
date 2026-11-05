package com.tapcard.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.themeStore by preferencesDataStore(name = "tapcard_prefs")

enum class ThemeMode(val storage: String) {
    LIGHT("light"), DARK("dark"), SYSTEM("system");

    companion object {
        /** PLASMA identity: dark-first. Existing stored prefs still win. */
        fun from(storage: String?): ThemeMode =
            entries.firstOrNull { it.storage == storage } ?: DARK
    }
}

/** Persisted Light/Dark/System theme preference (defaults to Light). */
object ThemeController {

    private val KEY = stringPreferencesKey("theme_mode")
    private val KEY_ONBOARDED = stringPreferencesKey("onboarding_done")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var context: Context? = null

    private val _mode = MutableStateFlow(ThemeMode.DARK)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun init(appContext: Context) {
        if (context != null) return
        synchronized(this) {
            if (context != null) return
            context = appContext.applicationContext
            val prefs = runBlocking { context!!.themeStore.data.first() }
            _mode.value = ThemeMode.from(prefs[KEY])
            _onboardingDone.value = prefs[KEY_ONBOARDED] == "true"
        }
    }

    fun setMode(newMode: ThemeMode) {
        _mode.value = newMode
        val ctx = context ?: return
        scope.launch { ctx.themeStore.edit { it[KEY] = newMode.storage } }
    }

    fun completeOnboarding() {
        _onboardingDone.value = true
        val ctx = context ?: return
        scope.launch { ctx.themeStore.edit { it[KEY_ONBOARDED] = "true" } }
    }
}
