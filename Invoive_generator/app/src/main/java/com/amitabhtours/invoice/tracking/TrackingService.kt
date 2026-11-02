package com.amitabhtours.invoice.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.amitabhtours.invoice.App
import com.amitabhtours.invoice.MainActivity
import com.amitabhtours.invoice.R
import com.amitabhtours.invoice.data.TripEntity
import com.amitabhtours.invoice.data.TripPointEntity
import com.amitabhtours.invoice.data.TripStatus
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Num
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

/** Live state of the running tracker, observed by the UI. */
data class LiveTracking(
    val tripId: Long,
    val distanceMeters: Double,
    val activeMs: Long,
    val paused: Boolean,
    val lastAccuracy: Float?,
    val lastFixAt: Long?,
    val lastLat: Double?,
    val lastLng: Double?,
)

object TrackingState {
    internal val _live = MutableStateFlow<LiveTracking?>(null)
    val live: StateFlow<LiveTracking?> = _live

    /** True only while the service is running in this process. */
    val running: Boolean get() = _live.value != null

    internal val _ended = MutableStateFlow<Long?>(null)
    /** Trip id that has just ended (UI offers "Create invoice"). */
    val justEnded: StateFlow<Long?> = _ended
    fun consumeEnded() { _ended.value = null }
}

class TrackingService : LifecycleService() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_CONTINUE = "continue"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_END = "end"
        private const val CHANNEL = "tracking"
        private const val NOTIF_ID = 42

        const val MAX_ACCURACY_M = 30f
        const val MAX_SPEED_KMH = 150.0

        fun send(context: Context, action: String) {
            val i = Intent(context, TrackingService::class.java).setAction(action)
            if (action == ACTION_START || action == ACTION_CONTINUE) ContextCompat.startForegroundService(context, i)
            else context.startService(i)
        }

        fun hasPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun locationEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        }

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, context.getString(R.string.tracking_channel), NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Shows distance and time while a trip is being tracked"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    private val app get() = application as App
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val mutex = Mutex()
    private var trip: TripEntity? = null
    private var last: TripPointEntity? = null
    private var ticker: Job? = null
    private var updatesOn = false
    private var rejectedInARow = 0
    private var lastElapsedNanos = 0L

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val locs = result.locations
            lifecycleScope.launch { locs.forEach { onLocation(it) } }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> { goForeground(); lifecycleScope.launch { startNew() } }
            ACTION_CONTINUE -> { goForeground(); lifecycleScope.launch { continueExisting() } }
            ACTION_PAUSE -> lifecycleScope.launch { pause() }
            ACTION_RESUME -> lifecycleScope.launch { resume() }
            ACTION_END -> lifecycleScope.launch { end() }
            null -> { goForeground(); lifecycleScope.launch { continueExisting() } }
        }
        return START_STICKY
    }

    private fun goForeground() {
        ensureChannel(this)
        val n = buildNotification(0.0, 0, paused = false)
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, n,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            // Missing permission or not allowed from background.
            stopSelf()
        }
    }

    private suspend fun startNew() = mutex.withLock {
        val dao = app.db.trips()
        val existing = dao.current()
        if (existing != null) {
            trip = existing
            last = dao.lastPoint(existing.id)
        } else {
            val now = System.currentTimeMillis()
            val id = dao.insert(TripEntity(startedAt = now))
            trip = dao.get(id)
            last = null
        }
        afterLoad()
    }

    private suspend fun continueExisting() = mutex.withLock {
        val dao = app.db.trips()
        val existing = dao.current()
        if (existing == null) {
            stopEverything()
            return@withLock
        }
        trip = existing
        last = dao.lastPoint(existing.id)
        afterLoad()
    }

    private fun afterLoad() {
        val t = trip ?: return
        publish()
        if (t.status == TripStatus.ACTIVE) startUpdates() else stopUpdates()
        startTicker()
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        if (updatesOn || !hasPermission(this)) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .setMinUpdateDistanceMeters(10f)
            .setWaitForAccurateLocation(true)
            .build()
        client.requestLocationUpdates(req, callback, Looper.getMainLooper())
        updatesOn = true
    }

    private fun stopUpdates() {
        if (!updatesOn) return
        client.removeLocationUpdates(callback)
        updatesOn = false
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = lifecycleScope.launch {
            while (isActive) {
                publish()
                delay(1000)
            }
        }
    }

    private suspend fun onLocation(loc: Location) = mutex.withLock {
        val t = trip ?: return@withLock
        if (t.status != TripStatus.ACTIVE) return@withLock
        // Ignore old cached fixes (a phone may hand over a location from hours ago at the start).
        val ageSec = (android.os.SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1e9
        if (ageSec > 30) return@withLock
        val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
        if (acc > MAX_ACCURACY_M) {
            TrackingState._live.value = TrackingState._live.value?.copy(lastAccuracy = acc)
            return@withLock
        }
        val prev = last
        var add = 0.0
        var segment = t.segment
        val nowElapsed = loc.elapsedRealtimeNanos
        if (prev != null && prev.segment == t.segment) {
            val res = FloatArray(1)
            Location.distanceBetween(prev.lat, prev.lng, loc.latitude, loc.longitude, res)
            // Device uptime clock is more reliable than the fix's wall-clock time.
            val dtSec = if (lastElapsedNanos > 0 && nowElapsed > lastElapsedNanos) (nowElapsed - lastElapsedNanos) / 1e9
            else (loc.time - prev.time).coerceAtLeast(1000) / 1000.0
            val kmh = res[0] / dtSec * 3.6
            if (kmh > MAX_SPEED_KMH) {
                rejectedInARow++
                // A single bad fix must not lock the trip: after a few rejections, re-anchor here
                // without adding the jump to the distance.
                if (rejectedInARow < 4) return@withLock
                segment = t.segment + 1
                // Nothing real recorded yet: the earlier point(s) were a bad start fix, drop them.
                if (t.distanceMeters < 50) app.db.trips().deletePoints(t.id)
            } else {
                add = res[0].toDouble()
            }
        }
        rejectedInARow = 0
        lastElapsedNanos = nowElapsed
        val p = TripPointEntity(tripId = t.id, time = System.currentTimeMillis(), lat = loc.latitude, lng = loc.longitude, accuracy = acc, segment = segment)
        val dao = app.db.trips()
        dao.insertPoint(p)
        val updated = t.copy(distanceMeters = t.distanceMeters + add, segment = segment)
        dao.update(updated)
        trip = updated
        last = p
        publish()
    }

    private suspend fun pause() = mutex.withLock {
        val t = trip ?: app.db.trips().current() ?: return@withLock
        if (t.status != TripStatus.ACTIVE) return@withLock
        val updated = t.copy(status = TripStatus.PAUSED, pausedSince = System.currentTimeMillis())
        app.db.trips().update(updated)
        trip = updated
        stopUpdates()
        publish()
    }

    private suspend fun resume() = mutex.withLock {
        val t = trip ?: app.db.trips().current() ?: return@withLock
        if (t.status != TripStatus.PAUSED) return@withLock
        val now = System.currentTimeMillis()
        val extra = t.pausedSince?.let { now - it } ?: 0L
        val updated = t.copy(status = TripStatus.ACTIVE, pausedSince = null, pausedMs = t.pausedMs + extra, segment = t.segment + 1)
        app.db.trips().update(updated)
        trip = updated
        startUpdates()
        publish()
    }

    private suspend fun end() {
        val ended: TripEntity
        mutex.withLock {
            val dao = app.db.trips()
            val t = trip ?: dao.current()
            if (t == null) {
                stopEverything()
                return
            }
            val now = System.currentTimeMillis()
            val extra = t.pausedSince?.let { now - it } ?: 0L
            ended = t.copy(status = TripStatus.ENDED, endedAt = now, pausedSince = null, pausedMs = t.pausedMs + extra)
            dao.update(ended)
            trip = null
        }
        stopUpdates()
        // Fill in addresses (best effort; needs internet on most phones).
        val dao = app.db.trips()
        val first = dao.firstPoint(ended.id)
        val lastP = dao.lastPoint(ended.id)
        val startAddr = first?.let { reverseGeocode(it.lat, it.lng) } ?: ""
        val endAddr = lastP?.let { reverseGeocode(it.lat, it.lng) } ?: ""
        dao.update(ended.copy(startAddress = startAddr, endAddress = endAddr))
        TrackingState._ended.value = ended.id
        stopEverything()
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching ""
            @Suppress("DEPRECATION")
            val list = Geocoder(this@TrackingService, Locale.ENGLISH).getFromLocation(lat, lng, 1)
            englishOnly(list?.firstOrNull()?.getAddressLine(0) ?: "")
        }.getOrDefault("")
    }

    /** Keeps the English (Latin-script) parts of an address, e.g. drops Devanagari duplicates. */
    private fun englishOnly(address: String): String =
        address.split(",").mapNotNull { part ->
            val kept = part.trim().split(Regex("\\s+")).filter { w -> w.none { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.DEVANAGARI } }
            kept.joinToString(" ").trim().takeIf { it.isNotEmpty() }
        }.distinct().joinToString(", ")

    private fun stopEverything() {
        ticker?.cancel()
        stopUpdates()
        TrackingState._live.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish() {
        val t = trip ?: return
        val now = System.currentTimeMillis()
        val live = LiveTracking(
            tripId = t.id,
            distanceMeters = t.distanceMeters,
            activeMs = t.activeMs(now),
            paused = t.status == TripStatus.PAUSED,
            lastAccuracy = last?.accuracy,
            lastFixAt = last?.time,
            lastLat = last?.lat,
            lastLng = last?.lng,
        )
        TrackingState._live.value = live
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            nm.notify(NOTIF_ID, buildNotification(t.distanceMeters, live.activeMs, live.paused))
        }
    }

    private fun buildNotification(meters: Double, activeMs: Long, paused: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String, req: Int) = PendingIntent.getService(
            this, req, Intent(this, TrackingService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val km = Num.clean(Math.round(meters / 100.0) / 10.0)
        val title = if (paused) "Trip paused — meter stopped" else "Trip in progress"
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$km km · ${Fmt.clock(activeMs)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .setColor(0xFF14213D.toInt())
            .addAction(0, if (paused) "Resume" else "Pause", action(if (paused) ACTION_RESUME else ACTION_PAUSE, 1))
            .addAction(0, "End trip", action(ACTION_END, 2))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        ticker?.cancel()
        stopUpdates()
        TrackingState._live.value = null
        super.onDestroy()
    }
}
