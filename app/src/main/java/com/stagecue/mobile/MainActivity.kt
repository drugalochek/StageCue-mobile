package com.stagecue.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF171B22)
private val Panel = Color(0xFF232A34)
private val Panel2 = Color(0xFF2B3441)
private val Blue = Color(0xFF3987E8)
private val Txt = Color(0xFFE6EAF0)
private val Muted = Color(0xFF9AA6B5)

data class CueTrack(val uri: String, val name: String, val slot: Int)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val prefs = getSharedPreferences("stagecue", MODE_PRIVATE)
        val saved = mutableStateListOf<CueTrack>()
        try {
            val array = JSONArray(prefs.getString("tracks", "[]"))
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                saved.add(CueTrack(o.getString("uri"), o.getString("name"), o.getInt("slot")))
            }
        } catch (_: Exception) { }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Blue, background = Bg, surface = Panel,
                onBackground = Txt, onSurface = Txt
            )) {
                StageCueScreen(
                    tracks = saved,
                    player = player,
                    onSave = {
                        val a = JSONArray()
                        saved.forEach { t ->
                            a.put(JSONObject().put("uri", t.uri).put("name", t.name).put("slot", t.slot))
                        }
                        prefs.edit().putString("tracks", a.toString()).apply()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageCueScreen(
    tracks: MutableList<CueTrack>,
    player: ExoPlayer,
    onSave: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentUri by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Фонотека") }
    var editing by remember { mutableStateOf<CueTrack?>(null) }
    var editName by remember { mutableStateOf("") }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            val text = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?.takeIf { it.isNotBlank() } ?: "Аудиофайл"
            if (tracks.none { it.uri == uri.toString() }) {
                val used = tracks.map { it.slot }.toSet()
                val slot = (1..40).firstOrNull { it !in used } ?: 0
                tracks.add(CueTrack(uri.toString(), text, slot))
            }
        }
        onSave()
    }

    fun playTrack(track: CueTrack) {
        currentUri = track.uri
        player.setMediaItem(MediaItem.fromUri(Uri.parse(track.uri)))
        player.prepare()
        player.play()
    }

    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Blue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("SC", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("StageCue", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Txt)
                Text("СЦЕНИЧЕСКИЙ АУДИОПЛЕЕР", fontSize = 9.sp, color = Muted, letterSpacing = 1.2.sp)
            }
            Surface(color = Panel2, shape = RoundedCornerShape(10.dp)) {
                Text("MOBILE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Фонотека", "Плеер").forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue.copy(alpha = .22f),
                        selectedLabelColor = Txt,
                        labelColor = Muted
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        if (selectedTab == "Фонотека") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Моя фонотека", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${tracks.size} фонограмм", color = Muted, fontSize = 12.sp)
                }
                Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Добавить")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (tracks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Фонотека пока пуста", color = Txt, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Добавь аудиофайлы с телефона", color = Muted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(tracks, key = { _, t -> t.uri }) { index, track ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (currentUri == track.uri) Panel2 else Panel, RoundedCornerShape(12.dp))
                                .clickable { playTrack(track) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(38.dp).background(Blue.copy(alpha = .18f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                Text(if (track.slot == 0) "—" else track.slot.toString().padStart(2, '0'), color = Blue, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text(if (currentUri == track.uri && playing) "Сейчас играет" else "Нажми для воспроизведения", color = Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                editing = track
                                editName = track.name
                            }) { Text("✎", color = Muted, fontSize = 18.sp) }
                            IconButton(onClick = {
                                if (currentUri == track.uri) { player.stop(); currentUri = null }
                                tracks.remove(track)
                                onSave()
                            }) { Icon(Icons.Default.Delete, null, tint = Muted) }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("СЕЙЧАС ИГРАЕТ", color = Blue, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Text(tracks.firstOrNull { it.uri == currentUri }?.name ?: "Ничего не выбрано",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = Txt)
                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    IconButton(onClick = { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Txt, modifier = Modifier.size(32.dp))
                    }
                    FilledIconButton(onClick = {
                        if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
                    }, modifier = Modifier.size(68.dp)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }) {
                        Icon(Icons.Default.SkipNext, null, tint = Txt, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Выбери фонограмму во вкладке «Фонотека»", color = Muted, fontSize = 12.sp)
            }
        }

        Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tracks.firstOrNull { it.uri == currentUri }?.name ?: "Плеер остановлен",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(if (playing) "Воспроизведение" else "Готов к работе", color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = {
                    if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
                }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Blue)
                }
            }
        }
    }

    editing?.let { track ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Переименовать фонограмму") },
            text = {
                OutlinedTextField(value = editName, onValueChange = { editName = it }, singleLine = true, label = { Text("Название") })
            },
            confirmButton = {
                TextButton(onClick = {
                    val index = tracks.indexOfFirst { it.uri == track.uri }
                    if (index >= 0 && editName.isNotBlank()) tracks[index] = track.copy(name = editName.trim())
                    onSave()
                    editing = null
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Отмена") } }
        )
    }
}
