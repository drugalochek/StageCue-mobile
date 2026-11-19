package com.lingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF20242C)
internal val Muted = Color(0xFF858B96)
internal val Blue = Color(0xFF526FE5)
internal val Pale = Color(0xFFF4F6FB)
internal val Green = Color(0xFF58B889)
internal val Line = Color(0xFFE8EAF0)

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
                    initialCourse = prefs.getString("course", "en") ?: "en",
                    initialGoal = prefs.getInt("daily_goal", 10),
                    onboardingDone = prefs.getBoolean("onboarding_done", false),
                    onSave = { stage, xp -> prefs.edit().putInt("stage", stage).putInt("xp", xp).apply() },
                    onSaveSettings = { course, goal -> prefs.edit().putString("course", course).putInt("daily_goal", goal).putBoolean("onboarding_done", true).apply() }
                )
            }
        }
    }
}

@Composable
private fun LingoApp(initialStage: Int, initialXp: Int, initialStreak: Int, initialCourse: String, initialGoal: Int, onboardingDone: Boolean, onSave: (Int, Int) -> Unit, onSaveSettings: (String, Int) -> Unit) {
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
    var bearEmotion by remember { mutableIntStateOf(1) }
    var courseId by remember { mutableStateOf(initialCourse) }
    var dailyGoal by remember { mutableIntStateOf(initialGoal) }
    var setupDone by remember { mutableStateOf(onboardingDone) }
    var completedLessons by remember { mutableIntStateOf(0) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    val course = CourseCatalog.find(courseId)
    val unit = course.firstUnit
    val questions = unit.quiz
    val q = questions[question.coerceIn(0, questions.lastIndex)]
    fun finishTest() {
        if (correct >= 3) {
            bearEmotion = 8
            if (skipTest) stage += 1
            xp += if (skipTest) 80 else 25
            onSave(stage, xp)
        }
        if (correct < 3) bearEmotion = 13
        lesson = false
        skipTest = false
        question = 0
        selected = -1
        correct = 0
        feedback = false
    }

    if (!setupDone) {
        OnboardingScreen(initialCourse = courseId) { chosenCourse, chosenGoal, startingLevel ->
            courseId = chosenCourse
            dailyGoal = chosenGoal
            setupDone = true
            onSaveSettings(chosenCourse, chosenGoal)
            if (startingLevel == "placement") { lesson = true; skipTest = true }
        }
        return
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
                    item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it; question = 0; selected = -1; feedback = false; onSaveSettings(it, dailyGoal) }) }
                    item {
                        Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(course.name.uppercase(), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text(unit.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(course.nativeName + " · " + unit.goal, color = Muted, fontSize = 13.sp)
                                    LinearProgressIndicator(progress = { (completedLessons % 4) / 4f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Blue, trackColor = Line)
                                }
                                Spacer(Modifier.width(10.dp))
                                BearMascot(Modifier.size(104.dp), emotion = bearEmotion)
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
                    item { DialogueCard(course = course) }
                    item { ReadingCard(reading = unit.reading) }
                    items(CourseCatalog.grammar(course.id).size) { index -> GrammarCard(point = CourseCatalog.grammar(course.id)[index]) }
                    CourseCatalog.writing(course.id)?.let { task -> item { WritingCard(task = task) } }
                    item {
                        Text("Ежедневная цель", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Занимайся $dailyGoal минут", fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                                    Text("0 / $dailyGoal мин", color = Muted, fontSize = 12.sp)
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
                                        item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it }) }
                    items(unit.vocabulary.size) { index -> val word = unit.vocabulary[index]; WordCard(word.term, word.meaning, word.note) }
                }
                else -> {
                    item { BearSpeech("Твои языки, страны и главы курсов. Выбирай, куда двигаться дальше.") }
                    item { Text("Мои курсы", color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
                    item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it; onSaveSettings(it, dailyGoal) }) }
                    items(CourseCatalog.courses.size) { index ->
                        val item = CourseCatalog.courses[index]
                        Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { courseId = item.id; tab = "Учиться"; onSaveSettings(item.id, dailyGoal) }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                FlagBadge(item.id, Modifier.size(width = 42.dp, height = 28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = Ink, fontWeight = FontWeight.Bold)
                                    Text(item.nativeName + " · " + item.firstUnit.title, color = Muted, fontSize = 12.sp)
                                }
                                Text(if (item.id == courseId) "Активен" else "Открыть", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Text("Главы · " + course.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    items(course.units.size) { index ->
                        val chapter = course.units[index]
                        Surface(color = Color.White, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("ГЛАВА ${index + 1}", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chapter.title, color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text(chapter.goal, color = Muted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.LockOpen, null, tint = Green)
                            }
                        }
                    }
                    item { StatCard("Ежедневная цель", "$dailyGoal минут") }
                    item { StatCard("Всего опыта", "$xp XP") }
                    item { StatCard("Серия занятий", "$streak дней") }
                    item { StatCard("Открытый этап", "$stage") }
                }
            }
        }
    }

    if (lesson) {
        LessonFlow(
            course = course,
            placement = skipTest,
            onExit = { lesson = false; skipTest = false },
            onComplete = { score, total ->
                xp += score * 5
                completedLessons++
                if (score >= total * 0.7f) stage++
                bearEmotion = if (score >= total * 0.7f) 8 else 13
                onSave(stage, xp)
                lesson = false
                skipTest = false
            }
        )
    }
}


@Composable
private fun CoursePicker(course: LanguageCourse, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onExpandedChange(true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
            FlagBadge(course.id, Modifier.size(width = 32.dp, height = 22.dp))
            Spacer(Modifier.width(10.dp))
            Text(course.name + " · " + course.nativeName, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(onClick = { onExpandedChange(true) }) { Icon(Icons.Default.Add, contentDescription = "Добавить курс") }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            CourseCatalog.courses.forEach { item ->
                DropdownMenuItem(
                    leadingIcon = { FlagBadge(item.id, Modifier.size(width = 30.dp, height = 20.dp)) },
                    text = { Text(item.name + " · " + item.nativeName) },
                    onClick = { onSelect(item.id); onExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
private fun DialogueCard(course: LanguageCourse) {
    val dialogue = course.firstUnit.dialogue
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ДИАЛОГ · " + course.nativeName.uppercase(), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(course.firstUnit.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            dialogue.forEach { line ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(line.speaker, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(line.text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(line.translation, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReadingCard(reading: ReadingText) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("ЧИТАЕМ И ПОНИМАЕМ", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(reading.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(reading.body, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
            var showTranslation by remember { mutableStateOf(false) }
            TextButton(onClick = { showTranslation = !showTranslation }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showTranslation) "Скрыть перевод" else "Показать перевод")
            }
            AnimatedVisibility(showTranslation) { Text(reading.translation, color = Muted, fontSize = 14.sp, lineHeight = 21.sp) }
            reading.questions.forEach { (prompt, answer) ->
                var showAnswer by remember { mutableStateOf(false) }
                Column {
                    Text(prompt, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    TextButton(onClick = { showAnswer = !showAnswer }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (showAnswer) answer else "Показать ответ")
                    }
                }
            }
        }
    }
}


@Composable
private fun GrammarCard(point: GrammarPoint) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("КАК СТРОИТЬ ФРАЗЫ", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(point.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Surface(color = Pale, shape = RoundedCornerShape(10.dp)) {
                Text(point.pattern, Modifier.fillMaxWidth().padding(12.dp), color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Text(point.explanation, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            point.examples.forEach { (example, translation) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(example, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(translation, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}


@Composable
private fun WritingCard(task: WritingTask) {
    var answer by remember(task.title) { mutableStateOf("") }
    var showSample by remember(task.title) { mutableStateOf(false) }
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("СОБСТВЕННЫЙ ТЕКСТ", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(task.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(task.situation, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            Text(task.instruction, color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text(task.starter, color = Muted) },
                label = { Text("Твой вариант") }
            )
            TextButton(onClick = { showSample = !showSample }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showSample) "Скрыть пример" else "Посмотреть пример ответа")
            }
            AnimatedVisibility(showSample) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(task.sample, color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
                    Text(task.translation, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            Text("Не копируй пример дословно. Меняй детали и строй собственные фразы.", color = Muted, fontSize = 12.sp)
        }
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
internal fun BearMascot(modifier: Modifier = Modifier, emotion: Int = 1) {
    val sprites = ImageBitmap.imageResource(R.drawable.bear_emotions)
    val transition = rememberInfiniteTransition(label = "bear")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    AnimatedContent(targetState = emotion, label = "bear-expression") { frame ->
        Canvas(modifier.offset(y = bob.dp)) {
        val frameWidth = sprites.width / 4
        val frameHeight = sprites.height / 4
        val frameIndex = frame.coerceIn(0, 15)
        drawImage(
            image = sprites,
            srcOffset = IntOffset((frameIndex % 4) * frameWidth, (frameIndex / 4) * frameHeight),
            srcSize = IntSize(frameWidth, frameHeight),
            dstOffset = IntOffset(0, ((size.height - size.width * frameHeight.toFloat() / frameWidth) / 2f).toInt()),
            dstSize = IntSize(size.width.toInt(), (size.width * frameHeight.toFloat() / frameWidth).toInt())
        )
        }
    }
}

@Composable
private fun BearSpeech(message: String, emotion: Int = 2) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BearMascot(Modifier.size(88.dp), emotion = emotion)
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
