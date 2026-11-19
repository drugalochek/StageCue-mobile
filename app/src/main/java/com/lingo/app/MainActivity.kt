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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal val Ink = Color(0xFFF5F1FF)
internal val Muted = Color(0xFFB5AECF)
internal val Blue = Color(0xFF9B79FF)
internal val Pale = Color(0xFF0D0B20)
internal val Green = Color(0xFF59D9B0)
internal val Line = Color(0xFF39314F)
internal val Card = Color(0xFF1A1732)
internal val CardRaised = Color(0xFF252044)
internal val Gold = Color(0xFFFFC86B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("lingo_progress", MODE_PRIVATE)
        AppLanguage.setPreference(prefs.getString("app_language", "auto") ?: "auto")
        AppLanguage.updateSystemLanguage(resources.configuration.locales[0]?.language ?: Locale.getDefault().language)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Pale, surface = Card, onSurface = Ink, secondary = Gold, onPrimary = Color.White, onBackground = Ink, onSurfaceVariant = Muted)) {
                LingoApp(
                    initialStage = prefs.getInt("stage", 1),
                    initialXp = prefs.getInt("xp", 0),
                    initialStreak = prefs.getInt("streak", 1),
                    initialCourse = prefs.getString("course", "en") ?: "en",
                    initialGoal = prefs.getInt("daily_goal", 10),
                    initialAppLanguage = prefs.getString("app_language", "auto") ?: "auto",
                    onboardingDone = prefs.getBoolean("onboarding_done", false),
                    onSave = { stage, xp -> prefs.edit().putInt("stage", stage).putInt("xp", xp).apply() },
                    onSaveSettings = { course, goal -> prefs.edit().putString("course", course).putInt("daily_goal", goal).putBoolean("onboarding_done", true).apply() },
                    onSaveAppLanguage = { language -> prefs.edit().putString("app_language", language).apply() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLanguage.updateSystemLanguage(resources.configuration.locales[0]?.language ?: Locale.getDefault().language)
    }
}

@Composable
private fun LingoApp(initialStage: Int, initialXp: Int, initialStreak: Int, initialCourse: String, initialGoal: Int, initialAppLanguage: String, onboardingDone: Boolean, onSave: (Int, Int) -> Unit, onSaveSettings: (String, Int) -> Unit, onSaveAppLanguage: (String) -> Unit) {
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
    var appLanguageMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(initialAppLanguage) { AppLanguage.setPreference(initialAppLanguage) }
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
            NavigationBar(containerColor = Card, tonalElevation = 0.dp) {
                listOf("Учиться", "Квесты", "Словарь", "Магазин", "Профиль").forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(when (item) {
                                "Учиться" -> Icons.Default.School
                                "Квесты" -> Icons.Default.Flag
                                "Словарь" -> Icons.Default.MenuBook
                                "Магазин" -> Icons.Default.ShoppingBag
                                else -> Icons.Default.Person
                            }, contentDescription = item)
                        },
                        label = { Text(when(item) { "Учиться" -> ui("Учиться", "Learn"); "Квесты" -> ui("Квесты", "Quests"); "Словарь" -> ui("Словарь", "Words"); "Магазин" -> ui("Магазин", "Shop"); else -> ui("Профиль", "Profile") }, fontSize = 11.sp) },
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
                        Text("LINGO", fontSize = 13.sp, color = Blue, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text(when (tab) { "Учиться" -> ui("Продолжим?", "Keep learning?") ; "Квесты" -> ui("Твои квесты", "Your quests"); "Словарь" -> ui("Твои слова", "Your vocabulary"); "Магазин" -> ui("Магазин наград", "Rewards shop"); else -> ui("Профиль", "Profile") }, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
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
                        Surface(color = Card, shape = RoundedCornerShape(22.dp)) {
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
                            Text(ui("Твой путь", "Your path"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            Text(ui("Этапы", "Stages"), color = Muted, fontSize = 12.sp)
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
                        Text(ui("Ежедневная цель", "Daily goal"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Surface(color = Card, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text(ui("Занимайся $dailyGoal минут", "Study for $dailyGoal minutes"), fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                                    Text(if (AppLanguage.isEnglish) "0 / $dailyGoal min" else "0 / $dailyGoal мин", color = Muted, fontSize = 12.sp)
                                }
                                LinearProgressIndicator(progress = { 0.2f }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Green, trackColor = Line)
                            }
                        }
                    }
                }
                "Квесты" -> {
                    item { QuestCard(ui("Первый контакт", "First steps"), ui("Пройди один урок сегодня", "Complete one lesson today"), "0 / 1", Icons.Default.ChatBubbleOutline) }
                    item { QuestCard(ui("Разминка для памяти", "Memory warm-up"), ui("Повтори 5 знакомых слов", "Review 5 familiar words"), "0 / 5", Icons.Default.Psychology) }
                    item { QuestCard(ui("Не сбавляй темп", "Keep it going"), ui("Заработай 30 XP", "Earn 30 XP"), "$xp / 30 XP", Icons.Default.Bolt) }
                    item { BearSpeech(ui("У тебя три квеста. Даже я столько дел одновременно не планировал.", "Three quests. Even I would not plan that many things at once.")) }
                }
                "Словарь" -> {
                    item { Text(ui("Новые слова появятся здесь после уроков.", "New words will appear here after your lessons."), color = Muted, fontSize = 15.sp) }
                                        item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it }) }
                    items(unit.vocabulary.size) { index -> val word = unit.vocabulary[index]; WordCard(word.term, word.meaning, word.note) }
                }
                "Магазин" -> {
                    item { StoreHeader(gems = 120) }
                    item { StoreItemCard(ui("Звёздная рамка", "Star frame"), ui("Оформление профиля", "Profile decoration"), "80", "✦") }
                    item { StoreItemCard(ui("Ночная тема", "Night theme"), ui("Тёмно-фиолетовый фон профиля", "Dark purple profile background"), "100", "☾") }
                    item { StoreItemCard(ui("Золотая корона", "Golden crown"), ui("Праздничный аксессуар маскота", "A festive mascot accessory"), "150", "♛") }
                    item { StoreItemCard(ui("Эмоция: восторг", "Excited reaction"), ui("Новая реакция медведя", "A new bear reaction"), "60", "★") }
                    item { BearSpeech(ui("Собирай награды за занятия. Витрина пока пополняется.", "Earn rewards by studying. More items are coming soon.")) }
                }
                else -> {
                    item { AppLanguageSettingsCard(expanded = appLanguageMenuOpen, onExpandedChange = { appLanguageMenuOpen = it }, onSelect = { AppLanguage.setPreference(it); onSaveAppLanguage(it); appLanguageMenuOpen = false }) }
                    item { BearSpeech(ui("Твои языки, страны и главы курсов. Выбирай, куда двигаться дальше.", "Your languages, countries, and course chapters. Choose where to go next.")) }
                    item { Text(ui("Мои курсы", "My courses"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
                    item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it; onSaveSettings(it, dailyGoal) }) }
                    items(CourseCatalog.courses.size) { index ->
                        val item = CourseCatalog.courses[index]
                        Surface(color = Card, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { courseId = item.id; tab = "Учиться"; onSaveSettings(item.id, dailyGoal) }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                FlagBadge(item.id, Modifier.size(width = 42.dp, height = 28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = Ink, fontWeight = FontWeight.Bold)
                                    Text(item.nativeName + " · " + item.firstUnit.title, color = Muted, fontSize = 12.sp)
                                }
                                Text(if (item.id == courseId) ui("Активен", "Active") else ui("Открыть", "Open"), color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Text(ui("Главы · ", "Chapters · ") + course.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    items(course.units.size) { index ->
                        val chapter = course.units[index]
                        Surface(color = Card, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(ui("ГЛАВА", "CHAPTER") + " ${index + 1}", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chapter.title, color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text(chapter.goal, color = Muted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.LockOpen, null, tint = Green)
                            }
                        }
                    }
                    item { StatCard(ui("Ежедневная цель", "Daily goal"), if (AppLanguage.isEnglish) "$dailyGoal minutes" else "$dailyGoal минут") }
                    item { StatCard(ui("Всего опыта", "Total XP"), "$xp XP") }
                    item { StatCard(ui("Серия занятий", "Streak"), if (AppLanguage.isEnglish) "$streak days" else "$streak дней") }
                    item { StatCard(ui("Открытый этап", "Current stage"), "$stage") }
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
internal fun FlagBadge(id: String, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp))) {
        val w = size.width
        val h = size.height
        fun rect(color: Color, left: Float, top: Float, width: Float, height: Float) {
            drawRect(color, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(width, height))
        }
        when (id) {
            "en" -> {
                val stripe = h / 13f
                repeat(13) { i -> rect(if (i % 2 == 0) Color(0xFFB22234) else Color.White, 0f, i * stripe, w, stripe + 0.5f) }
                rect(Color(0xFF3C3B6E), 0f, 0f, w * 0.42f, h * 7f / 13f)
                for (row in 0..4) for (col in 0..4) {
                    drawCircle(Color.White, radius = (h * 0.025f).coerceAtLeast(0.7f),
                        center = Offset(w * (0.035f + col * 0.085f), h * (0.045f + row * 0.085f)))
                }
            }
            "ru" -> {
                rect(Color.White, 0f, 0f, w, h / 3f)
                rect(Color(0xFF0039A6), 0f, h / 3f, w, h / 3f)
                rect(Color(0xFFD52B1E), 0f, h * 2f / 3f, w, h / 3f)
            }
            "es" -> {
                rect(Color(0xFFAA151B), 0f, 0f, w, h * 0.25f)
                rect(Color(0xFFF1BF00), 0f, h * 0.25f, w, h * 0.5f)
                rect(Color(0xFFAA151B), 0f, h * 0.75f, w, h * 0.25f)
            }
            "de" -> {
                rect(Color(0xFF000000), 0f, 0f, w, h / 3f)
                rect(Color(0xFFDD0000), 0f, h / 3f, w, h / 3f)
                rect(Color(0xFFFFCE00), 0f, h * 2f / 3f, w, h / 3f)
            }
            "fr" -> {
                rect(Color(0xFF0055A4), 0f, 0f, w / 3f, h)
                rect(Color.White, w / 3f, 0f, w / 3f, h)
                rect(Color(0xFFEF4135), w * 2f / 3f, 0f, w / 3f, h)
            }
            "it" -> {
                rect(Color(0xFF009246), 0f, 0f, w / 3f, h)
                rect(Color.White, w / 3f, 0f, w / 3f, h)
                rect(Color(0xFFCE2B37), w * 2f / 3f, 0f, w / 3f, h)
            }
            "pt" -> {
                rect(Color(0xFF006600), 0f, 0f, w * 0.4f, h)
                rect(Color(0xFFFF0000), w * 0.4f, 0f, w * 0.6f, h)
                drawCircle(Color(0xFFFFD700), radius = h * 0.16f, center = Offset(w * 0.4f, h * 0.5f))
            }
            "tr" -> {
                rect(Color(0xFFE30A17), 0f, 0f, w, h)
                drawCircle(Color.White, radius = h * 0.29f, center = Offset(w * 0.43f, h * 0.5f))
                drawCircle(Color(0xFFE30A17), radius = h * 0.23f, center = Offset(w * 0.49f, h * 0.5f))
                drawCircle(Color.White, radius = h * 0.075f, center = Offset(w * 0.69f, h * 0.5f))
            }
            "ja" -> {
                rect(Color.White, 0f, 0f, w, h)
                drawCircle(Color(0xFFBC002D), radius = h * 0.29f, center = Offset(w * 0.5f, h * 0.5f))
            }
            else -> rect(Color.LightGray, 0f, 0f, w, h)
        }
        drawRoundRect(Color(0x55777777), style = Stroke(width = 1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
    }
}

@Composable
private fun AppLanguageSettingsCard(expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    val current = when (AppLanguage.preference) {
        "ru" -> ui("Русский", "Russian")
        "en" -> "English"
        else -> ui("Как на телефоне", "Use phone language")
    }
    Surface(color = Card, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(ui("ЯЗЫК ПРИЛОЖЕНИЯ", "APP LANGUAGE"), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(ui("Язык интерфейса", "Interface language"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(ui("Не влияет на язык, который ты изучаешь.", "This does not change the language you are learning."), color = Muted, fontSize = 12.sp)
            Box {
                OutlinedButton(onClick = { onExpandedChange(true) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(current, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    DropdownMenuItem(text = { Text(ui("Как на телефоне", "Use phone language")) }, onClick = { onSelect("auto") })
                    DropdownMenuItem(text = { Text(ui("Русский", "Russian")) }, onClick = { onSelect("ru") })
                    DropdownMenuItem(text = { Text("English") }, onClick = { onSelect("en") })
                }
            }
        }
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
        FilledIconButton(onClick = { onExpandedChange(true) }) { Icon(Icons.Default.Add, contentDescription = ui("Добавить курс", "Add course")) }
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
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("ДИАЛОГ · ", "DIALOGUE · ") + course.nativeName.uppercase(), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(ui("ЧИТАЕМ И ПОНИМАЕМ", "READING COMPREHENSION"), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(reading.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(reading.body, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
            var showTranslation by remember { mutableStateOf(false) }
            TextButton(onClick = { showTranslation = !showTranslation }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showTranslation) ui("Скрыть перевод", "Hide translation") else ui("Показать перевод", "Show translation"))
            }
            AnimatedVisibility(showTranslation) { Text(reading.translation, color = Muted, fontSize = 14.sp, lineHeight = 21.sp) }
            reading.questions.forEach { (prompt, answer) ->
                var showAnswer by remember { mutableStateOf(false) }
                Column {
                    Text(prompt, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    TextButton(onClick = { showAnswer = !showAnswer }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (showAnswer) answer else ui("Показать ответ", "Show answer"))
                    }
                }
            }
        }
    }
}


@Composable
private fun GrammarCard(point: GrammarPoint) {
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(ui("КАК СТРОИТЬ ФРАЗЫ", "HOW TO BUILD SENTENCES"), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(point.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Surface(color = CardRaised, shape = RoundedCornerShape(10.dp)) {
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
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("СОБСТВЕННЫЙ ТЕКСТ", "YOUR OWN WRITING"), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(task.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(task.situation, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            Text(task.instruction, color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text(task.starter, color = Muted) },
                label = { Text(ui("Твой вариант", "Your answer")) }
            )
            TextButton(onClick = { showSample = !showSample }, contentPadding = PaddingValues(0.dp)) {
                Text(if (showSample) ui("Скрыть пример", "Hide example") else ui("Посмотреть пример ответа", "View sample answer"))
            }
            AnimatedVisibility(showSample) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(task.sample, color = Ink, fontSize = 14.sp, lineHeight = 21.sp)
                    Text(task.translation, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            Text(ui("Не копируй пример дословно. Меняй детали и строй собственные фразы.", "Do not copy the sample word for word. Change the details and write your own sentences."), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StageCard(stage: Int, onStart: () -> Unit, onSkip: () -> Unit) {
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = CardRaised, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.School, null, tint = Blue, modifier = Modifier.padding(11.dp).size(22.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui("ЭТАП", "STAGE") + " $stage", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(ui("Знакомство и приветствия", "Introductions and greetings"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                        Text(if (index == 3) ui("Тест", "Test") else ui("Урок ${index + 1}", "Lesson ${index + 1}"), fontSize = 10.sp, color = Muted)
                    }
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text(ui("Продолжить урок", "Continue lesson"), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                Text(ui("Проверить знания и пропустить этап", "Test out of this stage"))
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
        Surface(color = Card, shape = RoundedCornerShape(18.dp)) {
            Text(message, Modifier.padding(14.dp), color = Ink, fontSize = 14.sp)
        }
    }
}

@Composable
private fun QuestCard(title: String, description: String, progress: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(color = Card, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFE9EDFF), shape = RoundedCornerShape(12.dp)) { Icon(icon, null, tint = Blue, modifier = Modifier.padding(11.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Ink, fontWeight = FontWeight.Bold); Text(description, color = Muted, fontSize = 12.sp) }
            Text(progress, color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable private fun WordCard(word: String, translation: String, category: String) {
    Surface(color = Card, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(word, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(translation, color = Muted); }
            Text(category, color = Blue, fontSize = 11.sp)
        }
    }
}
@Composable private fun StatCard(label: String, value: String) {
    Surface(color = Card, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, modifier = Modifier.weight(1f)); Text(value, color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun StoreHeader(gems: Int) {
    Surface(color = CardRaised, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(ui("КОЛЛЕКЦИЯ LINGO", "LINGO COLLECTION"), color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(ui("Укрась свой профиль", "Customize your profile"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(ui("Предметы и реакции для маскота", "Items and reactions for your mascot"), color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✦", color = Gold, fontSize = 30.sp)
                Text("$gems", color = Gold, fontWeight = FontWeight.Bold)
                Text(ui("кристаллов", "gems"), color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StoreItemCard(name: String, description: String, price: String, symbol: String) {
    var purchased by remember { mutableStateOf(false) }
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CardRaised, shape = RoundedCornerShape(15.dp)) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(description, color = Muted, fontSize = 11.sp)
                Text("✦ $price", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { purchased = true }, enabled = !purchased, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Text(if (purchased) ui("Выбрано", "Selected") else ui("Открыть", "Unlock"), fontSize = 11.sp)
            }
        }
    }
}
