package com.amitabhtours.invoice.ui.trips

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.amitabhtours.invoice.data.TripEntity
import com.amitabhtours.invoice.data.TripPointEntity
import com.amitabhtours.invoice.data.TripStatus
import com.amitabhtours.invoice.domain.Fmt
import com.amitabhtours.invoice.domain.Num
import com.amitabhtours.invoice.tracking.TrackingService
import com.amitabhtours.invoice.tracking.TrackingState
import com.amitabhtours.invoice.ui.app
import com.amitabhtours.invoice.ui.components.ConfirmDialog
import com.amitabhtours.invoice.ui.theme.extra
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Returns a function that asks for permissions (with an explanation) and starts tracking. */
@Composable
fun rememberStartTracking(action: String = TrackingService.ACTION_START): () -> Unit {
    val context = LocalContext.current
    var explain by remember { mutableStateOf(false) }
    var locationOff by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        TrackingService.send(context, action)
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (!TrackingService.locationEnabled(context)) locationOff = true
            else if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else TrackingService.send(context, action)
        } else denied = true
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            icon = { Icon(Icons.Rounded.GpsFixed, null) },
            title = { Text("Allow location for trip tracking") },
            text = {
                Text(
                    "The app uses your phone's GPS only while a trip is being tracked, to measure the distance and draw the route on the invoice. " +
                        "Choose \"While using the app\" (or \"Precise\") on the next screen. A notification shows while tracking is on."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explain = false
                    locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { explain = false }) { Text("Not now") } },
        )
    }
    if (locationOff) {
        ConfirmDialog(
            title = "Turn on location",
            text = "Your phone's location (GPS) is switched off. Turn it on, then tap Start trip again.",
            confirm = "Open settings",
            onConfirm = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onDismiss = { locationOff = false },
        )
    }
    if (denied) {
        ConfirmDialog(
            title = "Location permission needed",
            text = "Without location permission the app cannot measure the trip. You can still make invoices using the odometer. To allow it later: App info → Permissions → Location → Allow while using the app.",
            confirm = "App settings",
            onConfirm = { openAppSettings(context) },
            onDismiss = { denied = false },
            dismiss = "OK",
        )
    }

    return {
        when {
            !TrackingService.hasPermission(context) -> explain = true
            !TrackingService.locationEnabled(context) -> locationOff = true
            Build.VERSION.SDK_INT >= 33 -> notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> TrackingService.send(context, action)
        }
    }
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** Live trip card for the Home screen. */
@Composable
fun LiveTripCard(trip: TripEntity, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val live by TrackingState.live.collectAsState()
    val running = live?.tripId == trip.id
    val km = (if (running) live!!.distanceMeters else trip.distanceMeters) / 1000.0
    val ms = if (running) live!!.activeMs else trip.activeMs()
    val paused = trip.status == TripStatus.PAUSED || (running && live!!.paused)
    val continueTracking = rememberStartTracking(TrackingService.ACTION_CONTINUE)
    var confirmEnd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appRef = app()

    val hero = extra.hero
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(hero).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dot = when {
                !running -> extra.danger
                paused -> MaterialTheme.colorScheme.secondary
                else -> Color(0xFF4ADE80)
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    !running -> "Tracking stopped — tap Continue"
                    paused -> "Paused — meter stopped"
                    live?.lastFixAt == null -> "Waiting for GPS signal…"
                    else -> "Recording trip"
                },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpen) { Text("Map", color = MaterialTheme.colorScheme.secondary) }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Num.clean(Math.round(km * 10) / 10.0), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(" km", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            Text(Fmt.clock(ms), color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text("Started ${Fmt.time(trip.startedAt)}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!running) {
                Button(
                    onClick = continueTracking,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Continue") }
            } else if (paused) {
                Button(
                    onClick = { TrackingService.send(context, TrackingService.ACTION_RESUME) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume") }
            } else {
                FilledTonalButton(
                    onClick = { TrackingService.send(context, TrackingService.ACTION_PAUSE) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Icon(Icons.Rounded.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause") }
            }
            OutlinedButton(
                onClick = { confirmEnd = true },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Icon(Icons.Rounded.Stop, null); Spacer(Modifier.width(6.dp)); Text("End trip") }
        }
    }
    if (confirmEnd) {
        ConfirmDialog(
            title = "End this trip?",
            text = "Distance and route will be saved. You can then create the invoice for this trip.",
            confirm = "End trip",
            onConfirm = {
                if (TrackingState.running) TrackingService.send(context, TrackingService.ACTION_END)
                else scope.launch { appRef.repo.endTripDirect(trip.id) }
            },
            onDismiss = { confirmEnd = false },
        )
    }
}

/** OpenStreetMap view showing a route. */
@Composable
fun RouteMap(points: List<TripPointEntity>, modifier: Modifier = Modifier, follow: Boolean = false, interactive: Boolean = true) {
    val context = LocalContext.current
    val routeColor = AColor.parseColor("#14213D")
    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactive)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(18.5204, 73.8567)) // Pune
        }
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    AndroidView(factory = { map }, modifier = modifier, update = { m ->
        m.overlays.clear()
        val geo = points.map { GeoPoint(it.lat, it.lng) }
        if (geo.isNotEmpty()) {
            val line = Polyline(m).apply {
                setPoints(geo)
                outlinePaint.color = routeColor
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = Paint.Cap.ROUND
            }
            m.overlays.add(line)
            val start = Marker(m).apply {
                position = geo.first(); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); title = "Start"
                icon = pin(GradientDrawable.OVAL, routeColor)
            }
            m.overlays.add(start)
            if (geo.size > 1) {
                val end = Marker(m).apply {
                    position = geo.last(); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); title = if (follow) "Now" else "End"
                    icon = pin(if (follow) GradientDrawable.OVAL else GradientDrawable.RECTANGLE, AColor.parseColor("#F5B82E"))
                }
                m.overlays.add(end)
            }
            m.post {
                if (follow || geo.size == 1) {
                    m.controller.setCenter(geo.last())
                    if (m.zoomLevelDouble < 14) m.controller.setZoom(16.0)
                } else {
                    runCatching { m.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(geo).increaseByScale(1.25f), false, 48) }
                }
            }
        }
        m.invalidate()
    })
}

private fun pin(shape: Int, fill: Int) = GradientDrawable().apply {
    this.shape = shape
    setColor(fill)
    setStroke(7, AColor.WHITE)
    if (shape == GradientDrawable.RECTANGLE) cornerRadius = 8f
    setSize(46, 46)
}

@Composable
fun TripStats(trip: TripEntity, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Distance", "${Num.clean(Math.round(trip.km * 10) / 10.0)} km")
        Stat("Driving time", Fmt.duration(trip.activeMs()))
        val avg = if (trip.activeMs() > 60_000) trip.km / (trip.activeMs() / 3_600_000.0) else null
        Stat("Avg speed", avg?.let { "${Num.clean(Math.round(it * 10) / 10.0)} km/h" } ?: "—")
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun rememberApp() = app()
