package com.lingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF858B96)
private val Blue = Color(0xFF526FE5)
private val Pale = Color(0xFFF4F6FB)
private val Green = Color(0xFF58B889)
private val Line = Color(0xFFE8EAF0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("lingo_progress", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Pale, surface = Color.White, onSurface = Ink)) {
                LingoApp(
                    initialStage = prefs.getInt("stage", 1),
                    initialXp = prefs.getInt("xp", 0),
                    initialStreak = prefs.getInt("streak", 1),
                    onSave = { stage, xp -> prefs.edit().putInt("stage", stage).putInt("xp", xp).apply() }
                )
            }
        }
    }
}

@Composable
private fun LingoApp(initialStage: Int, initialXp: Int, initialStreak: Int, onSave: (Int, Int) -> Unit) {
    var stage by remember { mutableIntStateOf(initialStage.coerceAtLeast(1)) }
    var xp by remember { mutableIntStateOf(initialXp) }
    var streak by remember { mutableIntStateOf(initialStreak) }
    var tab by remember { mutableStateOf("Учиться") }
    var lesson by remember { mutableStateOf(false) }
    var skipTest by remember { mutableStateOf(false) }
    var question by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var correct by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf(false) }
    val questions = listOf(
        Triple("Как сказать «Привет»?", listOf("Hello", "Goodbye", "Thanks"), 0),
        Triple("Выбери перевод «Спасибо»", listOf("Please", "Thank you", "Sorry"), 1),
        Triple("Что значит «Good morning»?", listOf("Добрый вечер", "Доброе утро", "Спокойной ночи"), 1),
        Triple("Выбери правильное слово: I ___ Alex.", listOf("am", "is", "are"), 0),
        Triple("Как сказать «Пока»?", listOf("Hello", "Goodbye", "Maybe"), 1)
    )
    val q = questions[question.coerceIn(0, questions.lastIndex)]
    fun finishTest() {
        if (correct >= 3) {
            if (skipTest) stage += 1
            xp += if (skipTest) 80 else 25
            onSave(stage, xp)
        }
        lesson = false
        skipTest = false
        question = 0
        selected = -1
        correct = 0
        feedback = false
    }

    Scaffold(
        containerColor = Pale,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                listOf("Учиться", "Квесты", "Словарь", "Профиль").forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(when (item) {
                                "Учиться" -> Icons.Default.School
                                "Квесты" -> Icons.Default.Flag
                                "Словарь" -> Icons.Default.MenuBook
                                else -> Icons.Default.Person
                            }, contentDescription = item)
                        },
                        label = { Text(item, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, indicatorColor = Color(0xFFE9EDFF), selectedTextColor = Blue)
                    )
                }
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LINGO", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(when (tab) { "Учиться" -> "Продолжим?" ; "Квесты" -> "Твои квесты"; "Словарь" -> "Твои слова"; else -> "Профиль" }, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF934D), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("$streak", fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFB83D), modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$xp XP", fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                }
            }
            when (tab) {
                "Учиться" -> {
                    item {
                        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("АНГЛИЙСКИЙ", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text("Первые шаги", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("Этап $stage · Основы общения", color = Muted, fontSize = 13.sp)
                                    LinearProgressIndicator(progress = { 0.35f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Blue, trackColor = Line)
                                }
                                Spacer(Modifier.width(10.dp))
                                BearMascot(Modifier.size(104.dp))
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Твой путь", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            Text("Этапы", color = Muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        StageCard(stage, onStart = { lesson = true; skipTest = false }, onSkip = { lesson = true; skipTest = true })
                    }
                    item {
                        Text("Ежедневная цель", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Занимайся 5 минут", fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                                    Text("1 / 5 мин", color = Muted, fontSize = 12.sp)
                                }
                                LinearProgressIndicator(progress = { 0.2f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Green, trackColor = Line)
                            }
                        }
                    }
                }
                "Квесты" -> {
                    item { QuestCard("Первый контакт", "Пройди один урок сегодня", "0 / 1", Icons.Default.ChatBubbleOutline) }
                    item { QuestCard("Разминка для памяти", "Повтори 5 знакомых слов", "0 / 5", Icons.Default.Psychology) }
                    item { QuestCard("Не сбавляй темп", "Заработай 30 XP", "$xp / 30 XP", Icons.Default.Bolt) }
                    item { BearSpeech("У тебя три квеста. Даже я столько дел одновременно не планировал.") }
                }
                "Словарь" -> {
                    item { Text("Новые слова появятся здесь после уроков.", color = Muted, fontSize = 15.sp) }
                    item { WordCard("hello", "привет", "Приветствие") }
                    item { WordCard("thank you", "спасибо", "Вежливые выражения") }
                    item { WordCard("good morning", "доброе утро", "Приветствие") }
                }
                else -> {
                    item { BearSpeech("Это твой профиль. Тут можно будет следить за прогрессом. Не переживай, я тоже удивлён.") }
                    item { StatCard("Всего опыта", "$xp XP") }
                    item { StatCard("Серия занятий", "$streak дней") }
                    item { StatCard("Открытый этап", "$stage") }
                }
            }
        }
    }

    if (lesson) {
        AlertDialog(
            onDismissRequest = { lesson = false; question = 0; correct = 0; selected = -1; feedback = false },
            title = { Text(if (skipTest) "Проверка этапа" else "Мини-урок", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Вопрос ${question + 1} из 5", color = Muted, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { (question + 1) / 5f }, modifier = Modifier.fillMaxWidth())
                    Text(q.first, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    q.second.forEachIndexed { i, answer ->
                        OutlinedButton(
                            onClick = { if (!feedback) { selected = i; feedback = true; if (i == q.third) correct++ } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = when { feedback && i == q.third -> Color(0xFFE8F7EF); feedback && i == selected -> Color(0xFFFFEEEE); else -> Color.White })
                        ) { Text(answer, color = Ink) }
                    }
                    if (feedback) Text(if (selected == q.third) "Верно. Медведь одобряет." else "Не совсем. Запомним и двигаемся дальше.", color = if (selected == q.third) Green else Color(0xFFD36A6A), fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!feedback) return@TextButton
                    if (question == 4) finishTest() else { question++; selected = -1; feedback = false }
                }) { Text(if (question == 4) "Завершить" else "Далее") }
            },
            dismissButton = { TextButton(onClick = { lesson = false; question = 0; correct = 0; selected = -1; feedback = false }) { Text("Выйти") } }
        )
    }
}

@Composable
private fun StageCard(stage: Int, onStart: () -> Unit, onSkip: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFE9EDFF), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.School, null, tint = Blue, modifier = Modifier.padding(11.dp).size(22.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("ЭТАП $stage", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Знакомство и приветствия", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.LockOpen, null, tint = Green)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { index ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(color = if (index == 0) Blue else Pale, shape = CircleShape) {
                            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(if (index == 0) Icons.Default.PlayArrow else if (index == 3) Icons.Default.EmojiEvents else Icons.Default.MenuBook, null, tint = if (index == 0) Color.White else Muted)
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(if (index == 3) "Тест" else "Урок ${index + 1}", fontSize = 10.sp, color = Muted)
                    }
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text("Продолжить урок", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text("Проверить знания и пропустить этап")
            }
        }
    }
}

@Composable
private fun BearMascot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bear")
    val bob by transition.animateFloat(initialValue = 0f, targetValue = 5f, animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Canvas(modifier.offset(y = bob.dp)) {
        val s = size.minDimension
        val cx = size.width / 2
        val cy = size.height / 2
        val fur = Color(0xFF172D52)
        drawCircle(Color(0xFFE8EDFF), s * .48f, Offset(cx, cy + s * .02f))
        drawCircle(fur, s * .31f, Offset(cx, cy + s * .05f))
        drawCircle(fur, s * .105f, Offset(cx - s * .22f, cy - s * .18f))
        drawCircle(fur, s * .105f, Offset(cx + s * .22f, cy - s * .18f))
        drawCircle(Color(0xFF314D79), s * .055f, Offset(cx - s * .22f, cy - s * .18f))
        drawCircle(Color(0xFF314D79), s * .055f, Offset(cx + s * .22f, cy - s * .18f))
        drawCircle(Color(0xFF203D68), s * .245f, Offset(cx, cy + s * .02f))
        drawCircle(Color(0xFFDAE3F5), s * .13f, Offset(cx, cy + s * .11f))
        drawCircle(Color.White, s * .025f, Offset(cx - s * .085f, cy - s * .02f))
        drawCircle(Color.White, s * .025f, Offset(cx + s * .085f, cy - s * .02f))
        drawCircle(Color(0xFF101827), s * .012f, Offset(cx - s * .085f, cy - s * .02f))
        drawCircle(Color(0xFF101827), s * .012f, Offset(cx + s * .085f, cy - s * .02f))
        drawOval(Color(0xFF101827), Offset(cx - s * .035f, cy + s * .075f), Size(s * .07f, s * .045f))
        val star = Color(0xFFFFD66B)
        val p = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) s * .052f else s * .023f
            val a = Math.PI * i / 5 - Math.PI / 2
            val pt = Offset(cx + kotlin.math.cos(a).toFloat() * r, cy - s * .18f + kotlin.math.sin(a).toFloat() * r)
            if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
        }
        p.close()
        drawPath(p, star)
    }
}

@Composable
private fun BearSpeech(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BearMascot(Modifier.size(88.dp))
        Spacer(Modifier.width(10.dp))
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
            Text(message, Modifier.padding(14.dp), color = Ink, fontSize = 14.sp)
        }
    }
}

@Composable
private fun QuestCard(title: String, description: String, progress: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFE9EDFF), shape = RoundedCornerShape(12.dp)) { Icon(icon, null, tint = Blue, modifier = Modifier.padding(11.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Ink, fontWeight = FontWeight.Bold); Text(description, color = Muted, fontSize = 12.sp) }
            Text(progress, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable private fun WordCard(word: String, translation: String, category: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(word, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(translation, color = Muted); }
            Text(category, color = Blue, fontSize = 11.sp)
        }
    }
}
@Composable private fun StatCard(label: String, value: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, modifier = Modifier.weight(1f)); Text(value, color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}
