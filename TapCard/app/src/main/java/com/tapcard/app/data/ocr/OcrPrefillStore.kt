package com.tapcard.app.data.ocr

/**
 * One-shot handoff of OCR-parsed fields into the create/edit form.
 * Consumed exactly once by the editor (process-local handoff).
 */
object OcrPrefillStore {
    @Volatile
    private var pending: OcrFieldParser.ParsedFields? = null

    fun offer(fields: OcrFieldParser.ParsedFields) {
        pending = fields
    }

    fun consume(): OcrFieldParser.ParsedFields? {
        val p = pending
        pending = null
        return p
    }
}
