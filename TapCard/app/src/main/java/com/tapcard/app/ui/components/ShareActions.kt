package com.tapcard.app.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.vcard.VCardBuilder
import java.io.File

/** Real device-level share/save actions shared by Preview and Dashboard. */

/** Writes a .vcf to cache and opens the Android share sheet. */
fun shareVCard(context: Context, card: BusinessCard) {
    try {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "${card.slug}.vcf")
        file.writeText(VCardBuilder.build(card))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Contact details for ${card.name} — also viewable at ${card.publicUrl}",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share contact"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the standard Android contact editor pre-filled from the card. */
fun insertIntoContacts(context: Context, card: BusinessCard) {
    try {
        val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, card.name)
            putExtra(android.provider.ContactsContract.Intents.Insert.JOB_TITLE, card.jobTitle)
            putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, card.company)
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, card.phone)
            putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, card.email)
            putExtra(
                android.provider.ContactsContract.Intents.Insert.NOTES,
                listOfNotNull(
                    card.website?.let { "Web: $it" },
                    "Digital card: ${card.publicUrl}",
                ).joinToString("\n"),
            )
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No contacts app available", Toast.LENGTH_SHORT).show()
    }
}
