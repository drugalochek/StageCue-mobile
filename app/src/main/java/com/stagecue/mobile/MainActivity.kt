package com.stagecue.mobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent as NativeKey
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF090B0F)
private val Top = Color(0xFF0D1118)
private val Panel = Color(0xFF141920)
private val Panel2 = Color(0xFF1A2635)
private val Line = Color(0xFF293442)
private val Blue = Color(0xFF4D9BFA)
private val Txt = Color(0xFFF0F4FA)
private val Muted = Color(0xFF8996A5)
private val StageFont = FontFamily(Font(R.font.nomor1))

data class CueTrack(val uri: String, val name: String, val slot: Int, val keyCode: Int = 0, val touchAction: String = "PLAY")

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val prefs = getSharedPreferences("stagecue", MODE_PRIVATE)
        val tracks = mutableStateListOf<CueTrack>()
        try {
            val arr = JSONArray(prefs.getString("tracks", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tracks.add(CueTrack(o.getString("uri"), o.getString("name"),
                    o.optInt("slot", 0), o.optInt("keyCode", 0), o.optString("touchAction", "PLAY")))
            }
        } catch (_: Exception) { }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Bg,
                surface = Panel, onBackground = Txt, onSurface = Txt), typography = Typography(defaultFontFamily = StageFont)) {
                StageCueScreen(tracks, player) {
                    val arr = JSONArray()
                    tracks.forEach { t -> arr.put(JSONObject().put("uri", t.uri)
                        .put("name", t.name).put("slot", t.slot).put("keyCode", t.keyCode).put("touchAction", t.touchAction)) }
                    prefs.edit().putString("tracks", arr.toString()).apply()
                }
            }
        }
    }
    override fun onDestroy() { player.release(); super.onDestroy() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageCueScreen(tracks: MutableList<CueTrack>, player: ExoPlayer, save: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Пэды") }
    var current by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CueTrack?>(null) }
    var name by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("") }
    var bind by remember { mutableStateOf("") }
    var touchAction by remember { mutableStateOf("PLAY") }
    val haptic = LocalHapticFeedback.current
    var search by remember { mutableStateOf("") }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                current = item?.localConfiguration?.uri?.toString()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) { pos = player.currentPosition.coerceAtLeast(0L)
            dur = player.duration.takeIf { it > 0 } ?: 0L; delay(250) }
    }
    fun play(track: CueTrack) {
        val queue = tracks.filter { it.slot in 1..40 }.sortedBy { it.slot }
        val list = if (queue.isEmpty()) listOf(track) else queue
        val index = list.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
        player.setMediaItems(list.map { MediaItem.fromUri(Uri.parse(it.uri)) }, index, 0L)
        current = track.uri; player.prepare(); player.play()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: Exception) { }
            val label = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?.takeIf { it.isNotBlank() } ?: "Аудиофайл"
            if (tracks.none { it.uri == uri.toString() }) {
                val used = tracks.map { it.slot }.toSet()
                tracks.add(CueTrack(uri.toString(), label, (1..40).firstOrNull { it !in used } ?: 0))
            }
        }
        save()
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && editing == null && tab != "Настройки") {
            val cue = tracks.firstOrNull { it.keyCode != 0 && it.keyCode == event.nativeKeyEvent.keyCode }
            if (cue != null) { play(cue); true } else false
        } else false
    }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = "StageCue logo",
                modifier = Modifier.size(46.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("StageCue", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("LIVE AUDIO CONTROL", fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp)
            }
            Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
                Text("LOCAL", Modifier.padding(9.dp), color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Пэды", "Фонотека", "Плеер", "Настройки").forEach { item ->
                Surface(color = if (tab == item) Panel2 else Color.Transparent,
                    shape = RoundedCornerShape(9.dp), modifier = Modifier.weight(1f).clickable { tab = item }) {
                    Text(item, Modifier.padding(vertical = 11.dp), textAlign = TextAlign.Center,
                        color = if (tab == item) Txt else Muted, fontSize = 11.sp,
                        fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        when (tab) {
            "Пэды" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Cue-панель", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Тап по пэду или клавиша с назначенным биндом", color = Muted, fontSize = 11.sp)
                    }
                    Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Text("Файлы")
                    }
                }
                Spacer(Modifier.height(12.dp))
                val cues = tracks.filter { it.slot in 1..40 }.sortedBy { it.slot }
                if (cues.isEmpty()) EmptyCueState()
                else LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cues, key = { it.uri }) { cue ->
                        val active = current == cue.uri
                        Surface(color = if (active) Color(0xFF183454) else Panel,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) Blue else Line),
                            modifier = Modifier.height(132.dp).combinedClickable(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); runTouchAction(cue, player, tracks, ::play) }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); editing = cue; name = cue.name; slot = cue.slot.toString(); bind = ""; touchAction = cue.touchAction }) ) {
                            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrackArtwork(cue.uri, Modifier.size(38.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(cue.slot.toString().padStart(2, '0'), color = Blue,
                                        fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.weight(1f))
                                    if (active) Text("LIVE", color = Blue, fontSize = 9.sp)
                                }
                                Text(cue.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
                                Text("ТАП: ${touchActionLabel(cue.touchAction)} · УДЕРЖАТЬ: НАСТРОИТЬ", color = Muted, fontSize = 8.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            "Фонотека" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Медиатека", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("${tracks.size} аудиофайлов · хранение на устройстве", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Icon(Icons.Default.Add, null, tint = Blue) }
                }
                OutlinedTextField(value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Поиск по фонограммам") }, shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(8.dp))
                val filtered = tracks.filter { it.name.contains(search, true) }.sortedBy { it.slot }
                if (filtered.isEmpty()) EmptyCueState()
                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(filtered, key = { it.uri }) { cue ->
                        Surface(color = if (current == cue.uri) Panel2 else Panel,
                            shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) {
                            Row(Modifier.fillMaxWidth().clickable { play(cue) }.padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(cue.slot.toString().padStart(2, '0'), color = Blue,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cue.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (cue.keyCode == 0) "Слот ${cue.slot} · без бинда"
                                        else "Слот ${cue.slot} · ${keyLabel(cue.keyCode)}",
                                        color = Muted, fontSize = 10.sp)
                                }
                                IconButton(onClick = {
                                    editing = cue; name = cue.name; slot = cue.slot.toString()
                                    bind = if (cue.keyCode == 0) "" else keyLetter(cue.keyCode); touchAction = cue.touchAction
                                }) { Text("✎", color = Muted) }
                                IconButton(onClick = {
                                    if (current == cue.uri) { player.stop(); current = null }
                                    tracks.remove(cue); save()
                                }) { Icon(Icons.Default.Delete, null, tint = Muted) }
                            }
                        }
                    }
                }
            }
            "Плеер" -> {
                val cue = tracks.firstOrNull { it.uri == current }
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    TrackArtwork(cue?.uri, Modifier.size(210.dp))
                    Spacer(Modifier.height(22.dp))
                    Text("NOW PLAYING", color = Blue, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(cue?.name ?: "Ничего не выбрано", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2)
                    Slider(value = if (dur > 0) pos.toFloat().coerceIn(0f, dur.toFloat()) else 0f,
                        onValueChange = { pos = it.toLong() },
                        onValueChangeFinished = { if (dur > 0) player.seekTo(pos) },
                        valueRange = 0f..dur.coerceAtLeast(1L).toFloat())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(timeText(pos), color = Muted, fontSize = 11.sp)
                        Text(timeText(dur), color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconButton(onClick = { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Txt, modifier = Modifier.size(34.dp))
                        }
                        FilledIconButton(onClick = {
                            if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
                        }, modifier = Modifier.size(66.dp)) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }) {
                            Icon(Icons.Default.SkipNext, null, tint = Txt, modifier = Modifier.size(34.dp))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    item {
                        Text("Настройки", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Бинды, слоты и подключение", color = Muted, fontSize = 11.sp)
                    }
                    item {
                        InfoPanel("Клавиатурные бинды",
                            "Назначай отдельную клавишу каждой фонограмме. Поддерживаются физические USB- и Bluetooth-клавиатуры.")
                    }
                    items(tracks.sortedBy { it.slot }, key = { "setting-" + it.uri }) { cue ->
                        Surface(color = Panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${cue.slot.toString().padStart(2, '0')} · ${cue.name}",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(if (cue.keyCode == 0) "Клавиша не назначена" else "Бинд: ${keyLabel(cue.keyCode)}",
                                        color = Muted, fontSize = 10.sp)
                                }
                                OutlinedButton(onClick = {
                                    editing = cue; name = cue.name; slot = cue.slot.toString()
                                    bind = if (cue.keyCode == 0) "" else keyLetter(cue.keyCode); touchAction = cue.touchAction
                                }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) {
                                    Text("Изменить", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    item {
                        Text("STAGECUE LINK", color = Blue, fontSize = 11.sp, letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold)
                        InfoPanel("Сценическая сеть",
                            "Следующий этап: телефон ведущего работает как хост в локальной Wi-Fi сети. Другие устройства подключаются как пульты и получают список cue, очередь и команды запуска. Сейчас сервер ещё не реализован: приложение честно работает офлайн.")
                        Surface(color = Color(0xFF302719), shape = RoundedCornerShape(7.dp)) {
                            Text("СЕРВЕР НЕ ПОДКЛЮЧЁН", Modifier.padding(8.dp), color = Color(0xFFE7B96B),
                                fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Surface(color = Top, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth().clickable {
                if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
            }) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                LinearProgressIndicator(progress = { if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Blue, trackColor = Line)
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                TrackArtwork(current, Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tracks.firstOrNull { it.uri == current }?.name ?: "StageCue готов",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (playing) "Воспроизведение" else "Остановлено", color = Muted, fontSize = 10.sp)
                }
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Blue)
                }
            }
        }
    }

    editing?.let { cue ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Настройка cue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Название") })
                    OutlinedTextField(value = slot, onValueChange = { slot = it.filter(Char::isDigit).take(2) },
                        singleLine = true, label = { Text("Номер пэда (1–40)") })
                    OutlinedTextField(value = bind, onValueChange = {
                        bind = it.uppercase().filter(Char::isLetterOrDigit).take(1)
                    }, singleLine = true, label = { Text("Клавиша бинда (A–Z, 0–9)") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                    Text("Сенсорное управление: тап выполняет выбранное действие, долгое нажатие открывает настройки пэда. Клавиша необязательна.", color = Muted, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Действие по тапу", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { touchAction = nextTouchAction(touchAction) }) { Text(touchActionLabel(touchAction)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val i = tracks.indexOfFirst { it.uri == cue.uri }
                    val newSlot = slot.toIntOrNull()?.takeIf { it in 1..40 } ?: cue.slot
                    val key = if (bind.isBlank()) 0 else NativeKey.keyCodeFromString("KEYCODE_${bind}")
                    if (i >= 0 && name.isNotBlank()) tracks[i] = cue.copy(name = name.trim(),
                        slot = newSlot, keyCode = if (key == NativeKey.KEYCODE_UNKNOWN) 0 else key, touchAction = touchAction)
                    save(); editing = null
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun EmptyCueState() {
    Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♫", color = Blue, fontSize = 42.sp)
            Text("Пока пусто", fontWeight = FontWeight.SemiBold)
            Text("Добавь аудиофайлы, чтобы заполнить cue-пэды.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoPanel(title: String, description: String) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text(description, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

private fun nextTouchAction(value: String) = when (value) { "PLAY" -> "TOGGLE"; "TOGGLE" -> "NEXT"; "NEXT" -> "STOP"; else -> "PLAY" }
private fun touchActionLabel(value: String) = when (value) { "TOGGLE" -> "Старт / пауза"; "NEXT" -> "Следующий cue"; "STOP" -> "Стоп"; else -> "Запустить cue" }
private fun runTouchAction(cue: CueTrack, player: ExoPlayer, tracks: List<CueTrack>, play: (CueTrack) -> Unit) {
    when (cue.touchAction) {
        "TOGGLE" -> if (player.currentMediaItem?.localConfiguration?.uri?.toString() == cue.uri && player.isPlaying) player.pause() else play(cue)
        "NEXT" -> if (player.hasNextMediaItem()) player.seekToNextMediaItem() else play(cue)
        "STOP" -> player.stop()
        else -> play(cue)
    }
}
@Composable
private fun TrackArtwork(uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        if (uri.isNullOrBlank()) null else try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uri))
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        } catch (_: Exception) { null }
    }
    Surface(modifier = modifier, color = Panel2, shape = RoundedCornerShape(9.dp)) {
        if (bitmap != null) Image(bitmap = bitmap, contentDescription = "Обложка аудиофайла", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SC", color = Blue, fontWeight = FontWeight.Black, fontSize = 13.sp) }
    }
}

private fun keyLabel(code: Int) = NativeKey.keyCodeToString(code).removePrefix("KEYCODE_")
private fun keyLetter(code: Int): String = keyLabel(code).takeIf { it.length == 1 } ?: ""
private fun timeText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
