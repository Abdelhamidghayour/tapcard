package com.tapcard.app.data.auth

import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.tapcard.app.data.cloud.CloudMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Authentication state observed by the UI. */
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val uid: String, val email: String?) : AuthState
}

/**
 * Email/password authentication backed by Firebase Auth. Sessions persist
 * via Firebase's token store (auto sign-in on cold start).
 */
object AuthManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUid: String?
        get() = (authState.value as? AuthState.LoggedIn)?.uid

    val isEnabled: Boolean get() = CloudMode.isConfigured

    private val listener = FirebaseAuth.AuthStateListener { auth ->
        _authState.value = auth.currentUser.toState()
    }

    fun start() {
        val auth = CloudMode.auth ?: run {
            _authState.value = AuthState.LoggedOut // LOCAL mode: no gate
            return
        }
        _authState.value = auth.currentUser.toState()
        auth.addAuthStateListener(listener)
    }

    fun stop() {
        CloudMode.auth?.removeAuthStateListener(listener)
    }

    private fun FirebaseUser?.toState(): AuthState = when {
        CloudMode.auth == null -> AuthState.LoggedOut
        this == null -> AuthState.LoggedOut
        else -> AuthState.LoggedIn(uid = uid, email = email)
    }

    suspend fun register(email: String, password: String): Result<Unit> = try {
        requireAuth().createUserWithEmailAndPassword(email.trim(), password).await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(authError(t, "Account creation"))
    }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        requireAuth().signInWithEmailAndPassword(email.trim(), password).await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(authError(t, "Login"))
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        requireAuth().sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(authError(t, "Password reset"))
    }

    fun logout() {
        CloudMode.auth?.signOut()
    }

    /** Future extension point: Google Sign-In via Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        requireAuth().signInWithCredential(credential).await()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(authError(t, "Google sign-in"))
    }

    private fun requireAuth(): FirebaseAuth =
        CloudMode.auth ?: error("Firebase is not configured (local mode)")

    private fun authError(t: Throwable, op: String): AuthError {
        val code = (t as? FirebaseAuthException)?.errorCode
        val message = when (code) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "That email already has an account — try logging in."
            "ERROR_INVALID_EMAIL" -> "That email address looks invalid."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak — use at least 6 characters."
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> "Wrong email or password."
            "ERROR_USER_NOT_FOUND" -> "No account found with that email."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts — please wait and try again."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error — check your connection."
            else -> "$op failed: ${t.message ?: "unknown error"}"
        }
        return AuthError(message, t)
    }

    class AuthError(message: String, cause: Throwable) : Exception(message, cause)
}
