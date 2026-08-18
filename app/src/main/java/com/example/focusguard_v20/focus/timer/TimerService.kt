package com.example.focusguard_v20.focus.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focusguard_v20.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var loopJob: Job? = null
    private var lastPhase: PomodoroPhase = TimerEngine.state.value.phase
    private val focusSessionRepo = FocusSessionRepository()
    private var focusStartAtMs: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.Start -> TimerEngine.start()
            Actions.Pause -> TimerEngine.pause()
            Actions.Reset -> TimerEngine.reset()
            Actions.SetAutoRepeat -> {
                val enabled = intent.getBooleanExtra(Extras.AutoRepeat, false)
                TimerEngine.setConfig(TimerEngine.state.value.config.copy(autoRepeat = enabled))
            }
        }

        val state = TimerEngine.state.value
        if (state.phase == PomodoroPhase.Focus && focusStartAtMs == null) {
            focusStartAtMs = System.currentTimeMillis()
        }

        startForeground(Ids.NotificationId, buildNotification())
        startLoopIfNeeded()

        // Keep running while timer is active; stop when idle to be polite.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        loopJob = null
        super.onDestroy()
    }

    private fun startLoopIfNeeded() {
        if (loopJob?.isActive == true) return
        loopJob =
            scope.launch {
                while (isActive) {
                    val changed = TimerEngine.tick()
                    val state = TimerEngine.state.value

                    if (state.phase != lastPhase) {
                        if (lastPhase == PomodoroPhase.Focus && state.phase == PomodoroPhase.Break) {
                            val endAt = System.currentTimeMillis()
                            val startAt = focusStartAtMs ?: (endAt - state.config.focusSeconds * 1000L)
                            val durationSec = ((endAt - startAt) / 1000L).coerceAtLeast(0L)
                            focusSessionRepo.recordFocusSession(
                                startAtMs = startAt,
                                endAtMs = endAt,
                                durationSec = durationSec,
                            )
                        }
                        beep()
                        lastPhase = state.phase
                        if (state.phase == PomodoroPhase.Focus) {
                            focusStartAtMs = System.currentTimeMillis()
                        } else {
                            focusStartAtMs = null
                        }
                    }

                    if (changed) {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(Ids.NotificationId, buildNotification())
                    }

                    if (!state.isRunning) {
                        // If paused/idle, stop foreground + service.
                        stopForeground(STOP_FOREGROUND_DETACH)
                        stopSelf()
                        return@launch
                    }

                    delay(250L)
                }
            }
    }

    private fun beep() {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(Ids.ChannelId)
        if (existing != null) return

        nm.createNotificationChannel(
            NotificationChannel(
                Ids.ChannelId,
                "Focus timer",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(): Notification {
        val state = TimerEngine.state.value
        val title = if (state.phase == PomodoroPhase.Focus) "Focus session" else "Break"
        val mins = state.remainingSeconds / 60
        val secs = state.remainingSeconds % 60
        val text = "Remaining ${mins}:${secs.toString().padStart(2, '0')}"

        return NotificationCompat.Builder(this, Ids.ChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    object Actions {
        const val Start = "TimerService.Start"
        const val Pause = "TimerService.Pause"
        const val Reset = "TimerService.Reset"
        const val SetAutoRepeat = "TimerService.SetAutoRepeat"
    }

    object Extras {
        const val AutoRepeat = "autoRepeat"
    }

    object Ids {
        const val ChannelId = "focus_timer"
        const val NotificationId = 1001
    }
}

