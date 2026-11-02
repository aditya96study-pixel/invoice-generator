package com.amitabhtours.invoice.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.MainActivity
import com.amitabhtours.invoice.R
import com.amitabhtours.invoice.data.TripStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android does not allow location tracking to restart by itself after a reboot, so a trip
 * that was running is paused and the driver gets a notification to continue it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as App
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = app.db.trips()
                val t = dao.current() ?: return@launch
                if (t.status == TripStatus.ACTIVE) {
                    dao.update(t.copy(status = TripStatus.PAUSED, pausedSince = System.currentTimeMillis()))
                }
                notify(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = "reminders"
        if (nm.getNotificationChannel(channel) == null) {
            nm.createNotificationChannel(NotificationChannel(channel, context.getString(R.string.general_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Trip tracking paused")
            .setContentText("Your phone restarted during a trip. Tap to continue tracking.")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(77, n)
    }
}
