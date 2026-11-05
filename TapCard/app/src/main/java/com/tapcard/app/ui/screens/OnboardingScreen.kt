package com.tapcard.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.provider.Settings
import com.tapcard.app.data.ThemeController
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.ui.components.CardSurface
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.theme.PlasmaGlow

/**
 * First-launch experience: "Your Identity. One Tap Away."
 * Premium, short, animated — no registration gate before the product is understood.
 * Motion loops respect the system animator scale (reduced-motion friendly).
 */
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

    // Floating demo card — the visual centerpiece
    val transition = rememberInfiniteTransition(label = "onboard")
    val floatY by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val tilt by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "tilt",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "shimmer",
    )

    val demoCard = BusinessCard(
        id = "demo",
        name = "Jordan Ellis",
        jobTitle = "Managing Partner",
        company = "Ellis & Rowe",
        phone = "+1 415 555 0100",
        email = "jordan@ellisrowe.com",
        website = "https://ellisrowe.com",
        socials = emptyList(),
        templateIndex = 0,
        slug = "jordan-ellis",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            text = "TAPCARD",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Your Identity.\nOne Tap Away.",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))

        Box(contentAlignment = Alignment.Center) {
            // Halo
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .alpha(if (reduceMotion) 0.25f else shimmer * 0.5f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)),
            )
            CardSurface(
                card = demoCard,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .then(
                        if (reduceMotion) Modifier
                        else Modifier.rotate(tilt).offset(y = floatY.dp),
                    ),
            )
        }

        Spacer(Modifier.height(36.dp))
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Benefit("Create your digital identity.", "A refined card with your name, role and contacts — live in seconds.")
            Benefit("Share instantly with QR.", "A single elegant code that opens your profile anywhere.")
            Benefit("Connect through NFC.", "Program a physical card with one tap of your phone.")
            Benefit("Make professional connections.", "Contacts saved to real address books — no app needed on their side.")
        }

        Spacer(Modifier.height(32.dp))
        GradientButton(
            text = "Get Started",
            onClick = {
                ThemeController.completeOnboarding()
                onGetStarted()
            },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(56.dp),
        )
        Text(
            text = "Free to start — no account required to explore.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
        )
    }
}

@Composable
private fun Benefit(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(top = 6.dp)
                .clip(CircleShape)
                .background(PlasmaGlow),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
