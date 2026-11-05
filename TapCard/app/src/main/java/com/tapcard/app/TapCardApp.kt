package com.tapcard.app

import android.app.Application
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.ThemeController
import com.tapcard.app.data.auth.AuthManager
import com.tapcard.app.data.auth.AuthState
import com.tapcard.app.data.cloud.CloudMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TapCardApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ThemeController.init(this)
        CloudMode.init(this)
        CardRepository.init(this)
        if (CloudMode.isConfigured) {
            AuthManager.start()
            AuthManager.authState
                .onEach { state ->
                    when (state) {
                        is AuthState.LoggedIn -> CardRepository.onLoggedIn(state.uid)
                        AuthState.LoggedOut -> CardRepository.onLoggedOut()
                        AuthState.Loading -> Unit
                    }
                }
                .launchIn(appScope)
        }
    }
}
