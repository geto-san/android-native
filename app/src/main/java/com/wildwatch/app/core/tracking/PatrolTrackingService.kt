package com.wildwatch.app.core.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wildwatch.app.MainActivity
import com.wildwatch.app.R
import com.wildwatch.app.core.data.auth.AuthRepository
import com.wildwatch.app.core.data.location.LocationRepository
import com.wildwatch.app.core.data.patrol.PatrolRepository
import com.wildwatch.app.core.database.RoutePoint
import com.wildwatch.app.core.sync.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

private const val NOTIFICATION_CHANNEL_ID = "patrol_tracking"
private const val NOTIFICATION_ID = 42
private const val LOCATION_UPDATE_INTERVAL_MS = 30_000L

// Foreground service so location updates keep flowing while a ranger has the
// app backgrounded mid-patrol - the persistent notification is what lets this
// run on ACCESS_FINE_LOCATION alone, without needing the much more sensitive
// ACCESS_BACKGROUND_LOCATION permission (that's only required for location
// access with no foreground service and no visible UI).
@AndroidEntryPoint
class PatrolTrackingService : Service() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var patrolRepository: PatrolRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var syncScheduler: SyncScheduler

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null
    private var activePatrolId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTracking()
            else -> startTracking(intent?.getStringExtra(EXTRA_PARK_ID))
        }
        return START_NOT_STICKY
    }

    private fun startTracking(parkId: String?) {
        if (trackingJob != null) return // already tracking - ignore a duplicate start

        startForegroundWithNotification()
        trackingJob = serviceScope.launch {
            val rangerUid = authRepository.currentUser.first()?.uid ?: run {
                Timber.w("PatrolTrackingService started with no signed-in ranger - stopping")
                stopSelf()
                return@launch
            }
            val patrol = patrolRepository.startPatrol(rangerUid, parkId)
            activePatrolId = patrol.id

            locationRepository.observeLocationUpdates(LOCATION_UPDATE_INTERVAL_MS)
                .onEach { location ->
                    patrolRepository.appendPoint(
                        patrol.id,
                        RoutePoint(location.latitude, location.longitude, Instant.now().toString()),
                    )
                }
                .launchIn(this)
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        val patrolId = activePatrolId
        if (patrolId != null) {
            serviceScope.launch {
                patrolRepository.stopPatrol(patrolId)
                syncScheduler.triggerImmediatePatrolSync()
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    private fun startForegroundWithNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Patrol tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shown while background patrol GPS tracking is active" }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Patrol tracking active")
            .setContentText("Recording your route in the background")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.wildwatch.app.action.STOP_PATROL"
        private const val EXTRA_PARK_ID = "park_id"

        fun startIntent(context: Context, parkId: String?): Intent =
            Intent(context, PatrolTrackingService::class.java).putExtra(EXTRA_PARK_ID, parkId)

        fun stopIntent(context: Context): Intent =
            Intent(context, PatrolTrackingService::class.java).setAction(ACTION_STOP)
    }
}
