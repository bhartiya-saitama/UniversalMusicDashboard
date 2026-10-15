package com.universalmusic.dashboard

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val bg = Color(0xFF0C111C)
private val muted = Color(0xFFA6B2C4)
private val accent = Color(0xFFB9D6FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (4 or 1024 or 4096 or 512 or 2)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg)) { Dashboard() } }
    }
}

private fun hasAccess(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return enabled.split(":").any { ComponentName.unflattenFromString(it)?.packageName == context.packageName }
}

private fun sessions(context: Context): List<MediaController> = try {
    val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    manager.getActiveSessions(ComponentName(context, MediaListener::class.java))
        .sortedByDescending { it.playbackState?.state == PlaybackState.STATE_PLAYING }
} catch (_: SecurityException) { emptyList() } catch (_: Exception) { emptyList() }

private fun Bitmap?.safeBitmap() = this?.takeUnless { it.isRecycled }

@Composable
private fun Dashboard() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasAccess(context)) }
    var available by remember { mutableStateOf<List<MediaController>>(emptyList()) }
    var selectedToken by remember { mutableStateOf<android.media.session.MediaSession.Token?>(null) }
    var seconds by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            granted = hasAccess(context)
            available = if (granted) sessions(context) else emptyList()
            seconds = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val active = available.firstOrNull { it.sessionToken == selectedToken } ?: available.firstOrNull()
    val metadata = active?.metadata
    val state = active?.playbackState
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Nothing playing"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Play music in an Android app"
    val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Universal Music Dashboard"
    val duration = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
    val basePosition = state?.position ?: 0L
    val elapsed = if (state?.state == PlaybackState.STATE_PLAYING) ((seconds - state.lastPositionUpdateTime).coerceAtLeast(0L) * state.playbackSpeed).toLong() else 0L
    val position = (basePosition + elapsed).coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
    val isPlaying = state?.state == PlaybackState.STATE_PLAYING
    val bitmap = remember(metadata) {
        (metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)).safeBitmap()
    }
    val actions = state?.actions ?: 0L
    fun supports(action: Long) = (actions and action) != 0L

    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 36.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = accent)
            Spacer(Modifier.width(10.dp))
            Text("UNIVERSAL MUSIC", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text(if (granted) "MEDIA ACCESS ON" else "ACCESS REQUIRED", color = if (granted) muted else Color(0xFFFFC77D), fontSize = 11.sp)
            if (!granted) {
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Enable access") }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().weight(0.95f).clip(RoundedCornerShape(22.dp)).background(Color(0xFF263044)), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Album artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(130.dp), tint = muted)
            }
            Spacer(Modifier.width(38.dp))
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.Center) {
                Text(album.uppercase(), color = accent, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(14.dp))
                Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(artist, fontSize = 19.sp, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(28.dp))
                Slider(value = seeking ?: if (duration > 0) position.toFloat() / duration else 0f, onValueChange = { seeking = it }, onValueChangeFinished = {
                    if (duration > 0 && supports(PlaybackState.ACTION_SEEK_TO)) active?.transportControls?.seekTo(((seeking ?: 0f) * duration).toLong())
                    seeking = null
                }, enabled = duration > 0 && supports(PlaybackState.ACTION_SEEK_TO), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(position), color = muted, fontSize = 12.sp)
                    Text(if (duration > 0) formatMs(duration) else "--:--", color = muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconButton(onClick = { active?.transportControls?.skipToPrevious() }, enabled = supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS), modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(36.dp)) }
                    FilledIconButton(onClick = { if (isPlaying) active?.transportControls?.pause() else active?.transportControls?.play() }, enabled = active != null && (supports(PlaybackState.ACTION_PLAY) || supports(PlaybackState.ACTION_PAUSE) || supports(PlaybackState.ACTION_PLAY_PAUSE)), modifier = Modifier.size(72.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { active?.transportControls?.skipToNext() }, enabled = supports(PlaybackState.ACTION_SKIP_TO_NEXT), modifier = Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(36.dp)) }
                }
                Spacer(Modifier.height(14.dp))
                Text(if (active == null) "No active local media session" else "Source: ${appName(context, active.packageName)}", color = muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        if (available.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(available, key = { it.sessionToken.hashCode() }) { controller ->
                    FilterChip(selected = controller.sessionToken == (active?.sessionToken), onClick = { selectedToken = controller.sessionToken }, label = { Text(appName(context, controller.packageName)) })
                }
            }
        } else Text("LOCAL PLAYBACK  •  REMOTE DEVICES COMING IN V0.2", color = muted, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}

private fun appName(context: Context, packageName: String): String = try {
    val info = context.packageManager.getApplicationInfo(packageName, 0)
    context.packageManager.getApplicationLabel(info).toString()
} catch (_: Exception) { packageName }

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
