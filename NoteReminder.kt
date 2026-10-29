package ru.momnotes.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object NoteReminderScheduler {
    private const val CHANNEL_ID = "note_reminders_v3"
    private const val ACTION_PREFIX = "ru.momnotes.app.NOTE_REMINDER."

    fun canScheduleExactly(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun schedule(context: Context, noteId: String, title: String, reminderAt: Long, type: String = "Уведомление и будильник") {
        if (reminderAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(noteId, reminderAt),
            reminderIntent(context, noteId, title, reminderAt, type),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExactly(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pending)
            } else {
                // Android 12+ requires user-granted exact-alarm access. Keep a best-effort inexact alarm meanwhile.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pending)
            }
        } catch (_: SecurityException) {
            try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pending) } catch (_: SecurityException) { }
        }
    }

    fun cancel(context: Context, noteId: String, reminderAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            reminderRequestCode(noteId, reminderAt),
            reminderIntent(context, noteId, "", reminderAt, "Уведомление и будильник"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun reminderRequestCode(noteId: String, reminderAt: Long): Int =
        31 * noteId.hashCode() + reminderAt.hashCode()

    private fun reminderIntent(context: Context, noteId: String, title: String, reminderAt: Long, type: String) =
        Intent(context, NoteReminderReceiver::class.java).apply {
            action = "$ACTION_PREFIX${noteId}_${reminderAt}"
            putExtra("note_id", noteId)
            putExtra("note_title", title)
            putExtra("reminder_at", reminderAt)
            putExtra("reminder_type", type)
        }

    internal fun showReminder(context: Context, intent: Intent) {
        val type = intent.getStringExtra("reminder_type") ?: "Уведомление и будильник"
        if (type == "Будильник" || type == "Уведомление и будильник") {
            AlarmRingingService.start(context, intent)
        } else {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                manager.notify(reminderRequestCode(
                    intent.getStringExtra("note_id").orEmpty(),
                    intent.getLongExtra("reminder_at", 0L)
                ), buildNotification(context, intent, ringing = false))
            } catch (_: SecurityException) {
                // Notification permission may have been denied by the user.
            }
        }
    }

    internal fun alarmNotificationId(intent: Intent): Int = reminderRequestCode(
        intent.getStringExtra("note_id").orEmpty(),
        intent.getLongExtra("reminder_at", 0L)
    )

    internal fun buildAlarmNotification(context: Context, intent: Intent) =
        buildNotification(context, intent, ringing = true)

    private fun buildNotification(context: Context, intent: Intent, ringing: Boolean): android.app.Notification {
        val noteId = intent.getStringExtra("note_id").orEmpty()
        val title = intent.getStringExtra("note_title").orEmpty().ifBlank { "Напоминание из заметок" }
        val reminderAt = intent.getLongExtra("reminder_at", 0L)
        val type = intent.getStringExtra("reminder_type") ?: "Уведомление и будильник"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Напоминания о заметках", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Напоминания, созданные в заметках"
                    setShowBadge(true)
                    setSound(null, null)
                }
            )
        }
        val openReminder = PendingIntent.getActivity(
            context,
            reminderRequestCode(noteId, reminderAt),
            Intent(context, ReminderAlarmActivity::class.java).apply {
                putExtra("note_id", noteId)
                putExtra("note_title", title)
                putExtra("reminder_at", reminderAt)
                putExtra("reminder_type", type)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(if (ringing) "Будильник звонит. Нажмите, чтобы открыть и остановить." else "Откройте, чтобы посмотреть напоминание")
            .setCategory(if (ringing) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(!ringing)
            .setOngoing(ringing)
            .setContentIntent(openReminder)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        if (ringing) {
            val stopIntent = PendingIntent.getService(
                context,
                alarmNotificationId(intent) xor 0x51A7,
                Intent(context, AlarmRingingService::class.java).setAction(AlarmRingingService.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Остановить", stopIntent)
        }
        return builder.build()
    }

    fun rescheduleSaved(context: Context) {
        val preferences = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
        val keys = preferences.all.keys.filter { it == "notes" || it.startsWith("notes_user_") }
        keys.forEach { key ->
            val notes = try { JSONArray(preferences.getString(key, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            for (index in 0 until notes.length()) {
                val note = notes.optJSONObject(index) ?: continue
                val id = note.optString("id")
                val title = note.optString("title")
                val reminders = linkedMapOf<Long, String>()
                val reminderTypes = note.optJSONObject("reminderTypes")
                val legacy = note.optLong("reminderAt", 0L)
                if (legacy > 0L) reminders[legacy] = reminderTypes?.optString(legacy.toString(), "Уведомление и будильник") ?: "Уведомление и будильник"
                val reminderArray = note.optJSONArray("reminders")
                if (reminderArray != null) for (i in 0 until reminderArray.length()) {
                    val time = reminderArray.optLong(i)
                    if (time > 0L) reminders[time] = reminderTypes?.optString(time.toString(), "Уведомление и будильник") ?: "Уведомление и будильник"
                }
                reminders.filterKeys { it > System.currentTimeMillis() }.forEach { (time, type) -> schedule(context, id, title, time, type) }
            }
        }
    }
}


class ReminderAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val noteTitle = intent.getStringExtra("note_title").orEmpty().ifBlank { "Напоминание" }
        val reminderAt = intent.getLongExtra("reminder_at", System.currentTimeMillis())
        val type = intent.getStringExtra("reminder_type") ?: "Будильник"
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val pulse by rememberInfiniteTransition(label = "alarm-pulse").animateFloat(
                initialValue = 0.96f, targetValue = 1.04f,
                animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse"
            )
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF7BE0A3), background = Color(0xFF101612), surface = Color(0xFF1B241E))) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C3827), Color(0xFF101612)))).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Surface(modifier = Modifier.size(116.dp).scale(pulse), shape = CircleShape, color = Color(0xFF254A34)) {
                            Box(contentAlignment = Alignment.Center) { Text("◷", fontSize = 58.sp, color = Color(0xFF9AE8B6)) }
                        }
                        Text("Напоминание", fontSize = 16.sp, color = Color(0xFFB6C9BB))
                        Text(noteTitle, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${formatReminderDate(reminderAt)} · ${if (type == "Будильник") "время наступило" else "пора вернуться к заметке"}", fontSize = 14.sp, color = Color(0xFFB6C9BB))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { this@ReminderAlarmActivity.stopService(Intent(this@ReminderAlarmActivity, AlarmRingingService::class.java)); finish() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Остановить", fontSize = 16.sp) }
                    }
                }
            }
        }
    }

}

class AlarmRingingService : Service() {
    companion object {
        internal const val ACTION_STOP = "ru.momnotes.app.action.STOP_NOTE_ALARM"

        fun start(context: Context, alarmIntent: Intent) {
            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                putExtras(alarmIntent)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    private var ringtone: android.media.Ringtone? = null
    private var vibrator: android.os.Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        notificationId = NoteReminderScheduler.alarmNotificationId(intent)
        startForeground(
            notificationId,
            NoteReminderScheduler.buildAlarmNotification(this, intent),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        startAlarmSound()
        return START_NOT_STICKY
    }

    private fun startAlarmSound() {
        stopAlarmSound()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mom_notes:note_alarm").apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L)
            }
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            ringtone = android.media.RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            val pattern = longArrayOf(0, 450, 250, 450, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION") vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
            // Keep the alarm notification available if audio or vibration is restricted.
        }
    }

    private fun stopAlarmSound() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        runCatching { wakeLock?.let { if (it.isHeld) it.release() } }
        ringtone = null
        vibrator = null
        wakeLock = null
    }

    override fun onDestroy() {
        stopAlarmSound()
        if (notificationId != 0) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }
        super.onDestroy()
    }
}

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NoteReminderScheduler.showReminder(context, intent)
    }
}

class NoteReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NoteReminderScheduler.rescheduleSaved(context)
    }
}

internal fun formatReminderDate(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
