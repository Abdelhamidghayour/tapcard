package com.tapcard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.util.PatternsCompat
import com.tapcard.app.data.auth.AuthManager
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.theme.BrandGradientColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Email/password auth: login + registration + password reset in one flow.
 * Shown full-screen in CLOUD mode when signed out. Never shown in LOCAL mode.
 */
@Composable
fun AuthScreen(initialMode: AuthMode = AuthMode.LOGIN) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val emailValid = PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
    val passwordValid = password.length >= 6
    val canSubmit = when (mode) {
        AuthMode.RESET -> emailValid
        else -> emailValid && passwordValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(BrandGradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = when (mode) {
                AuthMode.LOGIN -> "Welcome back"
                AuthMode.REGISTER -> "Create your account"
                AuthMode.RESET -> "Reset password"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Sign in to sync your cards to the cloud",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            if (mode != AuthMode.RESET) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            info?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            GradientButton(
                text = when (mode) {
                    AuthMode.LOGIN -> "Log in"
                    AuthMode.REGISTER -> "Create account"
                    AuthMode.RESET -> "Send reset email"
                },
                enabled = canSubmit && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        info = null
                        val result = when (mode) {
                            AuthMode.LOGIN -> AuthManager.login(email, password)
                            AuthMode.REGISTER -> AuthManager.register(email, password)
                            AuthMode.RESET -> AuthManager.sendPasswordReset(email)
                        }
                        busy = false
                        result.fold(
                            onSuccess = {
                                if (mode == AuthMode.RESET) {
                                    info = "Reset email sent — check your inbox."
                                }
                                // LOGIN/REGISTER success flips AuthState → UI gate switches screens.
                            },
                            onFailure = { error = it.message },
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            )

            Row {
                TextButton(onClick = {
                    mode = when (mode) {
                        AuthMode.LOGIN -> AuthMode.REGISTER
                        else -> AuthMode.LOGIN
                    }
                    error = null
                }) {
                    Text(
                        when (mode) {
                            AuthMode.LOGIN -> "No account? Create one"
                            else -> "Have an account? Log in"
                        },
                    )
                }
            }
            if (mode == AuthMode.LOGIN) {
                TextButton(onClick = {
                    mode = AuthMode.RESET
                    error = null
                }) {
                    Text("Forgot password?")
                }
            }
            Text(
                text = "Your locally stored cards are imported to your account automatically after sign-in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

enum class AuthMode { LOGIN, REGISTER, RESET }
