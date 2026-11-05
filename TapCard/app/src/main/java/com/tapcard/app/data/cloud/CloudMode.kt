package com.tapcard.app.data.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Decides whether the app runs in CLOUD mode (Firebase configured) or LOCAL
 * mode (v1.1 behavior). Configuration lives in res/values/firebase_config.xml
 * — no google-services.json or Gradle plugin required, so the project always
 * builds even before a Firebase project exists.
 */
object CloudMode {

    private var enabled = false

    /** True only when the user enabled cloud mode AND replaced placeholder values. */
    val isConfigured: Boolean get() = enabled

    private var app: FirebaseApp? = null

    /** Call once from Application.onCreate. Safe to call repeatedly. */
    fun init(context: Context) {
        if (app != null) return
        val res = context.resources
        val apiKey = res.getString(
            res.getIdentifier("firebase_api_key", "string", context.packageName),
        )
        val projectId = res.getString(
            res.getIdentifier("firebase_project_id", "string", context.packageName),
        )
        val appId = res.getString(
            res.getIdentifier("firebase_app_id", "string", context.packageName),
        )
        enabled = res.getBoolean(
            res.getIdentifier("cloud_mode_enabled", "bool", context.packageName),
        ) && !apiKey.startsWith("PASTE_") && !appId.startsWith("PASTE_") &&
            projectId.isNotBlank() && projectId != "tapcard-placeholder"

        if (!enabled) return
        app = runCatching {
            FirebaseApp.initializeApp(
                context,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setStorageBucket("$projectId.appspot.com")
                    .build(),
            )
        }.getOrNull()
    }

    val auth: FirebaseAuth? get() = app?.let { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    val firestore: FirebaseFirestore? get() = app?.let { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }
    val storage: FirebaseStorage? get() = app?.let { runCatching { FirebaseStorage.getInstance() }.getOrNull() }
}
