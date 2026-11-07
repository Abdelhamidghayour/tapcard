package com.tapcard.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.tapcard.app.R
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.qr.QrCodeGenerator

/**
 * Home-screen QR widget (Tapni "widget" parity): shows the active card's QR
 * + name. Long-press → resizes; tapping opens the app. Refreshed whenever the
 * app opens or the active card changes.
 */
class QrWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, manager, appWidgetIds)
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, QrWidgetProvider::class.java),
            )
            if (ids.isNotEmpty()) refresh(context, manager, ids)
        }

        fun refresh(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
            val card: BusinessCard? = CardRepository.activeCard()
            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_qr)
                if (card != null) {
                    views.setTextViewText(R.id.widget_name, card.name)
                    views.setTextViewText(R.id.widget_role, card.jobTitle.ifBlank { card.company })
                    val qr: Bitmap = QrCodeGenerator.generate(card.publicUrl, sizePx = 512)
                    views.setImageViewBitmap(R.id.widget_qr, qr)
                } else {
                    views.setTextViewText(R.id.widget_name, "TapCard")
                    views.setTextViewText(R.id.widget_role, "Open the app to create a card")
                    views.setViewVisibility(R.id.widget_qr, android.view.View.GONE)
                }
                views.setViewVisibility(R.id.widget_qr, android.view.View.VISIBLE)
                val open = android.app.PendingIntent.getActivity(
                    context,
                    0,
                    android.content.Intent(context, com.tapcard.app.MainActivity::class.java)
                        .setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, open)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
