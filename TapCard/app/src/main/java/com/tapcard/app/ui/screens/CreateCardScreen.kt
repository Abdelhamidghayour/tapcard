package com.tapcard.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.PatternsCompat
import coil.compose.AsyncImage
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.ImageStore
import com.tapcard.app.data.cloud.CloudMode
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.model.SocialLink
import com.tapcard.app.data.ocr.OcrPrefillStore
import com.tapcard.app.ui.components.CardSurface
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.theme.CardAccents
import com.tapcard.app.ui.theme.CardTemplates
import com.tapcard.app.ui.theme.SocialPlatforms
import com.tapcard.app.ui.theme.accentBrush
import java.util.UUID

/**
 * Unified create/edit editor with live preview.
 * - create mode (cardId == null)
 * - edit mode (cardId != null) — updates the existing card
 * - OCR prefill from the paper-card scanner is consumed on entry
 * Photos/logos: local mode persists via ImageStore; cloud mode also uploads
 * to Firebase Storage with progress.
 */
@Composable
fun CreateCardScreen(
    cardId: String? = null,
    onCancel: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val existing = cardId?.let { CardRepository.cardById(it) }
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var jobTitle by rememberSaveable { mutableStateOf(existing?.jobTitle ?: "") }
    var company by rememberSaveable { mutableStateOf(existing?.company ?: "") }
    var phone by rememberSaveable { mutableStateOf(existing?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(existing?.email ?: "") }
    var website by rememberSaveable { mutableStateOf(existing?.website ?: "") }
    var address by rememberSaveable { mutableStateOf(existing?.address ?: "") }
    var about by rememberSaveable { mutableStateOf(existing?.about ?: "") }
    var templateIndex by rememberSaveable { mutableStateOf(existing?.templateIndex ?: 0) }
    var accentIndex by rememberSaveable { mutableStateOf(existing?.accentIndex ?: 0) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    var socials by rememberSaveable {
        mutableStateOf(existing?.socials ?: emptyList())
    }
    var profileImage by rememberSaveable { mutableStateOf(existing?.profileImageUrl) }
    var logoImage by rememberSaveable { mutableStateOf(existing?.logoUrl) }
    var socialMenuOpenFor by rememberSaveable { mutableStateOf(-1) }

    // Consume OCR prefill once (scan → editor handoff)
    LaunchedEffect(cardId) {
        OcrPrefillStore.consume()?.let { p ->
            if (name.isBlank()) name = p.name ?: ""
            if (jobTitle.isBlank()) jobTitle = p.jobTitle ?: ""
            if (company.isBlank()) company = p.company ?: ""
            if (phone.isBlank()) phone = p.phone ?: ""
            if (email.isBlank()) email = p.email ?: ""
            if (website.isBlank()) website = p.website ?: ""
            if (address.isBlank()) address = p.address ?: ""
        }
    }

    val existingCards by CardRepository.cards.collectAsState()
    val uploadState by CardRepository.uploadState.collectAsState()

    val nameError = attemptedSave && name.isBlank()
    val emailError = attemptedSave && email.isNotBlank() &&
        !PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
    val phoneError = attemptedSave && phone.isNotBlank() &&
        !android.util.Patterns.PHONE.matcher(phone).matches()
    val isDuplicate = existing == null && existingCards.any {
        it.name.equals(name.trim(), ignoreCase = true) &&
            it.jobTitle.equals(jobTitle.trim(), ignoreCase = true) &&
            it.company.equals(company.trim(), ignoreCase = true)
    }
    val duplicateError = attemptedSave && isDuplicate

    val draft = BusinessCard(
        id = existing?.id ?: "draft",
        name = name,
        jobTitle = jobTitle,
        company = company,
        phone = phone.ifBlank { null },
        email = email.ifBlank { null },
        website = website.ifBlank { null },
        address = address.ifBlank { null },
        about = about.ifBlank { null },
        socials = socials,
        accentIndex = accentIndex,
        templateIndex = templateIndex,
        profileImageUrl = profileImage,
        logoUrl = logoImage,
        slug = existing?.slug ?: "",
    )

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && existing != null) {
            val local = ImageStore.persist(context, uri)
            if (local != null) profileImage = local
            if (CloudMode.isConfigured) CardRepository.uploadCardImage(existing.id, uri, "profile")
        }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && existing != null) {
            val local = ImageStore.persist(context, uri)
            if (local != null) logoImage = local
            if (CloudMode.isConfigured) CardRepository.uploadCardImage(existing.id, uri, "logo")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (existing == null) "Create Business Card" else "Edit Business Card",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // ---- Live preview ----
        Text(
            "LIVE PREVIEW",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        CardSurface(
            card = draft,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        )

        SectionHeader("Photos")
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ImageSlot(
                label = "Profile photo",
                image = profileImage,
                size = 72.dp,
                enabled = existing != null,
                onPick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = {
                    profileImage = null
                    existing?.let { CardRepository.update(it.copy(profileImageUrl = null)) }
                },
            )
            ImageSlot(
                label = "Company logo",
                image = logoImage,
                size = 72.dp,
                enabled = existing != null,
                onPick = {
                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = {
                    logoImage = null
                    existing?.let { CardRepository.update(it.copy(logoUrl = null)) }
                },
            )
        }
        if (existing == null) {
            Text(
                "Save the card first, then add photos to it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
        when (val up = uploadState) {
            is com.tapcard.app.data.UploadState.Uploading ->
                Text("Uploading… ${up.progressPercent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp))
            is com.tapcard.app.data.UploadState.Failed ->
                Text("Upload failed: ${up.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
            else -> Unit
        }

        SectionHeader("Personal identity")
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name *") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Name is required") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text("Job title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeader("Professional information")
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About me / bio (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeader("Contact details")
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                isError = phoneError,
                supportingText = if (phoneError) {
                    { Text("Enter a valid phone number") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Enter a valid email address") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeader("Social profiles")
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            socials.forEachIndexed { index, social ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AssistChip(
                            onClick = { socialMenuOpenFor = index },
                            label = { Text(social.platform) },
                        )
                        DropdownMenu(
                            expanded = socialMenuOpenFor == index,
                            onDismissRequest = { socialMenuOpenFor = -1 },
                        ) {
                            SocialPlatforms.forEach { (platform, _) ->
                                DropdownMenuItem(
                                    text = { Text(platform) },
                                    onClick = {
                                        socials = socials.mapIndexed { i, s ->
                                            if (i == index) s.copy(platform = platform) else s
                                        }
                                        socialMenuOpenFor = -1
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = social.url,
                        onValueChange = { v ->
                            socials = socials.mapIndexed { i, s ->
                                if (i == index) s.copy(url = v) else s
                            }
                        },
                        label = { Text("${social.platform} URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        socials = socials.filterIndexed { i, _ -> i != index }
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove link")
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    socials = socials + SocialLink(SocialPlatforms.first().first, "")
                },
                enabled = socials.size < 6,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("+ Add social link")
            }
        }

        SectionHeader("Appearance")
        Text(
            "Template",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardTemplates.forEachIndexed { index, template ->
                val selected = index == templateIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .clickable { templateIndex = index }
                        .padding(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(template.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Aa", color = template.ink, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        template.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Accent",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardAccents.forEachIndexed { index, _ ->
                val selected = index == accentIndex
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentBrush(index))
                        .border(
                            width = if (selected) 2.5.dp else 0.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                        )
                        .clickable { accentIndex = index },
                )
            }
        }

        if (duplicateError) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "An identical card already exists — change the name, role or company.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = if (existing == null) "Create card" else "Save changes",
            onClick = {
                attemptedSave = true
                if (name.isNotBlank() && !emailError && !phoneError && !duplicateError) {
                    if (existing != null) {
                        CardRepository.update(
                            existing.copy(
                                name = name.trim(),
                                jobTitle = jobTitle.trim(),
                                company = company.trim(),
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                website = website.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                about = about.trim().ifBlank { null },
                                socials = socials,
                                accentIndex = accentIndex,
                                templateIndex = templateIndex,
                                profileImageUrl = profileImage,
                                logoUrl = logoImage,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                        onSaved(existing.id)
                    } else {
                        val card = CardRepository.create(
                            BusinessCard(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                jobTitle = jobTitle.trim(),
                                company = company.trim(),
                                phone = phone.trim().ifBlank { null },
                                email = email.trim().ifBlank { null },
                                website = website.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                about = about.trim().ifBlank { null },
                                socials = socials,
                                accentIndex = accentIndex,
                                templateIndex = templateIndex,
                            ),
                        )
                        onSaved(card.id)
                    }
                }
            },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(56.dp),
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun ImageSlot(
    label: String,
    image: String?,
    size: Dp,
    enabled: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(enabled = enabled, onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (image != null) {
            Text(
                "Remove",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(onClick = onClear),
            )
        }
    }
}
