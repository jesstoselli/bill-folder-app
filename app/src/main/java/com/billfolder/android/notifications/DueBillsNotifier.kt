package com.billfolder.android.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.billfolder.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Mostra o aviso do dia. */
fun interface DueBillsNotifier {
    fun show(message: DueBillsMessage)
}

/** As notificações do app podem aparecer (permissão no 13+ e não desligadas nas configurações). */
fun notificationsAllowed(context: Context): Boolean {
    val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@Singleton
class SystemDueBillsNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : DueBillsNotifier {

    override fun show(message: DueBillsMessage) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_due_bills))
                .setDescription(context.getString(R.string.notification_channel_due_bills_description))
                .build(),
        )
        val style = if (message.lines.isEmpty()) {
            NotificationCompat.BigTextStyle().bigText(message.body)
        } else {
            NotificationCompat.InboxStyle()
                .setBigContentTitle(message.title)
                .setSummaryText(message.summary)
                .also { inbox -> message.lines.forEach(inbox::addLine) }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()

        // Mesmo id todo dia: o aviso de hoje substitui o de ontem se ainda estiver lá.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val CHANNEL_ID = "due_bills"
        const val NOTIFICATION_ID = 1001
    }
}
