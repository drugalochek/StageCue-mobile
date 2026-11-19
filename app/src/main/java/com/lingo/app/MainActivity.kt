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
import androidx.compose.ui.graphics.Brush
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

internal val Ink = Color(0xFFF5F2FF)
internal val Muted = Color(0xFFAAA6D0)
internal val Blue = Color(0xFF9A78FF)
internal val Pale = Color(0xFF11102F)
internal val Green = Color(0xFF54D5B5)
internal val Line = Color(0xFF39366D)
internal val Card = Color(0xFF19183D)
internal val CardRaised = Color(0xFF25245A)
internal val Gold = Color(0xFFFFC85A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("lingo_progress", MODE_PRIVATE)
        AppLanguage.changePreference(prefs.getString("app_language", "auto") ?: "auto")
        AppLanguage.updateSystemLanguage(resources.configuration.locales[0]?.language ?: Locale.getDefault().language)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = Blue, background = Pale, surface = Card, onSurface = Ink, secondary = Gold, onPrimary = Color.White, onBackground = Ink, onSurfaceVariant = Muted, primaryContainer = CardRaised, secondaryContainer = CardRaised, surfaceVariant = CardRaised, outline = Line),
                shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))
            ) {
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
    var tab by remember { mutableStateOf("Главная") }
    var lesson by remember { mutableStateOf(false) }
    var skipTest by remember { mutableStateOf(false) }
    var question by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var correct by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf(false) }
    var bearEmotion by remember { mutableIntStateOf(2) }
    var courseId by remember { mutableStateOf(initialCourse) }
    var dailyGoal by remember { mutableIntStateOf(initialGoal) }
    var setupDone by remember { mutableStateOf(onboardingDone) }
    var completedLessons by remember { mutableIntStateOf(0) }
    var storeCategory by remember { mutableIntStateOf(0) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var appLanguageMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(initialAppLanguage) { AppLanguage.changePreference(initialAppLanguage) }
    val course = CourseCatalog.find(courseId)
    val unit = course.units[(stage - 1).coerceIn(0, course.units.lastIndex)]
    val courseProgress = stage.coerceAtMost(course.units.size).toFloat() / course.units.size.coerceAtLeast(1)
    val questions = unit.quiz
    val q = questions[question.coerceIn(0, questions.lastIndex)]
    fun finishTest() {
        if (correct >= 3) {
            bearEmotion = 8
            if (skipTest) stage = (stage + 1).coerceAtMost(course.units.size)
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
            NavigationBar(containerColor = Card.copy(alpha = 0.96f), tonalElevation = 0.dp, windowInsets = WindowInsets(0.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(24.dp))) {
                listOf("Главная", "Прогресс", "Квесты", "Магазин").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(when (item) {
                            "Главная" -> Icons.Default.Home
                            "Прогресс" -> Icons.Default.BarChart
                            "Квесты" -> Icons.Default.EmojiEvents
                            else -> Icons.Default.CardGiftcard
                        }, contentDescription = item) },
                        label = { Text(when(item) {
                            "Главная" -> ui("Главная", "Home")
                            "Прогресс" -> ui("Прогресс", "Progress")
                            "Квесты" -> ui("Квесты", "Quests")
                            else -> ui("Магазин", "Shop")
                        }, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Blue, indicatorColor = CardRaised, selectedTextColor = Blue, unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            when (tab) {
                "Главная" -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Blue, modifier = Modifier.size(27.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Lingo", fontSize = 25.sp, color = Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { tab = "Профиль" }) { Icon(Icons.Default.Settings, ui("Настройки", "Settings"), tint = Muted) }
                        }
                    }
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(248.dp)
                                .background(Brush.radialGradient(
                                    colors = listOf(Color(0xFF302B78).copy(alpha = .72f), Color(0xFF17163D).copy(alpha = .08f), Color.Transparent),
                                    center = Offset(160f, 250f), radius = 520f
                                ))
                        ) {
                            AmbientSparkles(Modifier.fillMaxSize())
                            BearMascot(Modifier.align(Alignment.BottomStart).size(238.dp), emotion = bearEmotion)
                            Surface(color = CardRaised, shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp, bottomEnd = 25.dp),
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp, bottom = 38.dp).widthIn(max = 148.dp)) {
                                Text(ui("Опять учиться? Ну ладно…", "Back to learning? Fine…"), Modifier.padding(horizontal = 15.dp, vertical = 17.dp), color = Ink, fontSize = 14.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                    item {
                        Surface(color = Card, shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FlagBadge(course.id, Modifier.size(43.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(course.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(ui("Уровень: A1", "Level: A1"), color = Muted, fontSize = 14.sp)
                                    }
                                    Text("${(courseProgress * 100).toInt()}%", color = Ink, fontWeight = FontWeight.SemiBold)
                                }
                                LinearProgressIndicator(progress = { courseProgress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = Blue, trackColor = Line)
                            }
                        }
                    }
                    item {
                        Button(onClick = { lesson = true; skipTest = false },
                            modifier = Modifier.fillMaxWidth().height(58.dp).background(Brush.horizontalGradient(listOf(Color(0xFF8E6BFF), Color(0xFFB17CFF))), RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                            Text(ui("Продолжить обучение  →", "Continue learning  →"), color = Color(0xFF15102F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                "Прогресс" -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { tab = "Главная" }) { Icon(Icons.Default.ArrowBack, null, tint = Muted) }
                            Column(Modifier.weight(1f)) {
                                Text(ui("Курс: " + course.name, "Course: " + course.name), color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(ui("Твой путь обучения", "Your learning path"), color = Muted, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(27.dp))
                        }
                    }
                    item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it; stage = 1; question = 0; selected = -1; feedback = false; onSaveSettings(it, dailyGoal); onSave(stage, xp) }) }
                    item { StageCard(stage, unit.title, unit.goal, onStart = { lesson = true; skipTest = false }, onSkip = { lesson = true; skipTest = true }) }
                    item { Text(ui("Материалы курса", "Course materials"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    item { DialogueCard(course = course) }
                    item { ReadingCard(reading = unit.reading) }
                    items(CourseCatalog.grammar(course.id).size) { index -> GrammarCard(point = CourseCatalog.grammar(course.id)[index]) }
                    CourseCatalog.writing(course.id)?.let { task -> item { WritingCard(task = task) } }
                    item {
                        Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(ui("Ежедневная цель", "Daily goal"), color = Ink, fontWeight = FontWeight.Bold)
                                Text(if (AppLanguage.isEnglish) "$dailyGoal minutes a day" else "$dailyGoal минут в день", color = Muted)
                                Text(ui("Счётчик времени пока не подключён — здесь не показываем выдуманный прогресс.", "Time tracking is not connected yet, so no progress is fabricated here."), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
                "Квесты" -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, null, tint = Blue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(ui("Квесты", "Quests"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    }
                    item {
                        Surface(color = CardRaised, shape = RoundedCornerShape(23.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(ui("Серия:", "Streak:"), color = Ink, fontSize = 15.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("$streak " + ui("дня", "days"), color = Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(Modifier.width(7.dp))
                                        Text("🔥", fontSize = 25.sp)
                                    }
                                    Text(ui("Продолжай заниматься, чтобы не потерять серию!", "Keep learning to maintain your streak!"), color = Muted, fontSize = 12.sp)
                                }
                                Text("🔥", fontSize = 58.sp)
                            }
                        }
                    }
                    item { Text(ui("Активные квесты", "Active quests"), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    item { QuestCard(ui("Заверши 3 урока", "Complete 3 lessons"), ui("$completedLessons / 3 за эту сессию", "$completedLessons / 3 this session"), "$completedLessons / 3", Icons.Default.MenuBook) }
                    item { QuestCard(ui("Продвинься по курсу", "Make progress in your course"), ui("Этап $stage из ${course.units.size}", "Stage $stage of ${course.units.size}"), "$stage / ${course.units.size}", Icons.Default.School) }
                    item { QuestCard(ui("Заработай 50 XP", "Earn 50 XP"), "$xp / 50 XP", "$xp / 50", Icons.Default.Bolt) }
                }
                "Магазин" -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { tab = "Главная" }) { Icon(Icons.Default.ArrowBack, null, tint = Muted) }
                            Text(ui("Магазин", "Shop"), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Star, null, tint = Gold)
                            Spacer(Modifier.width(5.dp))
                            Text("120", color = Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(ui("Аксессуары", "Accessories"), ui("Эмоции", "Emotions"), ui("Темы", "Themes")).forEachIndexed { index, label ->
                                Column(Modifier.weight(1f).clickable { storeCategory = index }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, color = if (storeCategory == index) Ink else Muted, fontSize = 11.sp, fontWeight = if (storeCategory == index) FontWeight.Bold else FontWeight.Normal)
                                    if (storeCategory == index) Box(Modifier.padding(top = 6.dp).width(54.dp).height(3.dp).clip(CircleShape).background(Blue))
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StoreItemCard(ui("Наушники", "Headphones"), ui("Аксессуар", "Accessory"), "300", "♫", Modifier.weight(1f))
                            StoreItemCard(ui("Очки", "Glasses"), ui("Аксессуар", "Accessory"), "300", "◎", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StoreItemCard(ui("Шарф", "Scarf"), ui("Аксессуар", "Accessory"), "300", "▤", Modifier.weight(1f))
                            StoreItemCard(ui("Кепка", "Cap"), ui("Аксессуар", "Accessory"), "300", "◒", Modifier.weight(1f))
                        }
                    }
                }
                else -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { tab = "Главная" }) { Icon(Icons.Default.ArrowBack, null, tint = Muted) }
                            Text(ui("Профиль и настройки", "Profile & settings"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    item { AppLanguageSettingsCard(expanded = appLanguageMenuOpen, onExpandedChange = { appLanguageMenuOpen = it }, onSelect = { AppLanguage.changePreference(it); onSaveAppLanguage(it); appLanguageMenuOpen = false }) }
                    item { Text(ui("Мои курсы", "My courses"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
                    item { CoursePicker(course = course, expanded = languageMenuOpen, onExpandedChange = { languageMenuOpen = it }, onSelect = { courseId = it; stage = 1; onSaveSettings(it, dailyGoal); onSave(stage, xp) }) }
                    items(CourseCatalog.courses.size) { index ->
                        val item = CourseCatalog.courses[index]
                        Surface(color = Card, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { courseId = item.id; tab = "Главная"; onSaveSettings(item.id, dailyGoal) }) {
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
                    item { Text(ui("Словарь", "Vocabulary"), color = Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
                    items(unit.vocabulary.size) { index -> val word = unit.vocabulary[index]; WordCard(word.term, word.meaning, word.note) }
                    item { StatCard(ui("Всего опыта", "Total XP"), "$xp XP") }
                    item { StatCard(ui("Серия занятий", "Streak"), "$streak") }
                    item { StatCard(ui("Открытый этап", "Current stage"), "$stage") }
                }
            }
        }
    }

    if (lesson) {
        LessonFlow(
            course = course,
            unit = unit,
            placement = skipTest,
            onExit = { lesson = false; skipTest = false },
            onComplete = { score, total ->
                xp += score * 5
                completedLessons++
                if (score >= total * 0.7f) stage = (stage + 1).coerceAtMost(course.units.size)
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
        OutlinedButton(onClick = { onExpandedChange(true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Card)) {
            FlagBadge(course.id, Modifier.size(width = 32.dp, height = 22.dp))
            Spacer(Modifier.width(10.dp))
            Text(course.name + " · " + course.nativeName, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(onClick = { onExpandedChange(true) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardRaised, contentColor = Blue)) { Icon(Icons.Default.Add, contentDescription = ui("Добавить курс", "Add course")) }
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
    Surface(color = Card, shape = RoundedCornerShape(22.dp)) {
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
private fun StageCard(stage: Int, currentTitle: String, currentGoal: String, onStart: () -> Unit, onSkip: () -> Unit) {
    Surface(color = Card, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = CardRaised, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.School, null, tint = Blue, modifier = Modifier.padding(12.dp).size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(ui("ТЕКУЩИЙ ЭТАП", "CURRENT STAGE") + "  $stage", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(currentTitle, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(currentGoal, color = Muted, fontSize = 12.sp)
                }
                Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(25.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(color = Green, shape = CircleShape) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF102A36), modifier = Modifier.padding(9.dp).size(20.dp))
                    }
                    Box(Modifier.width(3.dp).height(17.dp).background(Blue.copy(alpha = 0.55f)))
                    Surface(color = Blue, shape = CircleShape) {
                        Icon(Icons.Default.MenuBook, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(20.dp))
                    }
                    Box(Modifier.width(3.dp).height(17.dp).background(Line))
                    Surface(color = CardRaised, shape = CircleShape) {
                        Icon(Icons.Default.Lock, null, tint = Muted, modifier = Modifier.padding(10.dp).size(20.dp))
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ui("Основы", "Basics"), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(ui("Пройдено", "Completed"), color = Green, fontSize = 11.sp)
                    }
                    Surface(color = CardRaised, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(currentTitle, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(ui("Текущий урок", "Current lesson"), color = Blue, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.Star, null, tint = Blue)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ui("Следующий урок", "Next lesson"), color = Muted, fontSize = 13.sp)
                        Text(ui("Следующий этап", "Next stage"), color = Muted.copy(alpha = 0.75f), fontSize = 10.sp)
                    }
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp).background(Brush.horizontalGradient(listOf(Color(0xFF8E6BFF), Color(0xFFB17CFF))), RoundedCornerShape(17.dp)), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Text(ui("Продолжить обучение", "Continue learning") + "  →", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Bolt, null, tint = Gold, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(ui("Проверить знания и пропустить этап", "Take a test to skip this stage"), color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AmbientSparkles(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val points = listOf(
            Offset(size.width * .16f, size.height * .18f) to 3.2f,
            Offset(size.width * .48f, size.height * .10f) to 2.0f,
            Offset(size.width * .78f, size.height * .24f) to 2.8f,
            Offset(size.width * .61f, size.height * .72f) to 1.8f,
            Offset(size.width * .91f, size.height * .62f) to 2.4f
        )
        points.forEachIndexed { index, (point, radius) ->
            drawCircle(Color(0xFF9D80FF).copy(alpha = if (index % 2 == 0) .78f else .45f), radius = radius.dp.toPx(), center = point)
            if (index == 0 || index == 2) {
                drawLine(Color(0xFF9D80FF).copy(alpha = .5f), Offset(point.x - 5.dp.toPx(), point.y), Offset(point.x + 5.dp.toPx(), point.y), 1.dp.toPx())
                drawLine(Color(0xFF9D80FF).copy(alpha = .5f), Offset(point.x, point.y - 5.dp.toPx()), Offset(point.x, point.y + 5.dp.toPx()), 1.dp.toPx())
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
    val current = progress.substringBefore("/").trim().toFloatOrNull() ?: 0f
    val total = progress.substringAfter("/", "").trim().substringBefore(" ").toFloatOrNull()?.coerceAtLeast(1f) ?: 1f
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CardRaised, shape = RoundedCornerShape(14.dp)) {
                Icon(icon, null, tint = Blue, modifier = Modifier.padding(12.dp).size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, color = Muted, fontSize = 11.sp, maxLines = 2)
                LinearProgressIndicator(progress = { (current / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Blue, trackColor = Line)
            }
            Spacer(Modifier.width(9.dp))
            Text(progress, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
private fun StoreItemCard(name: String, description: String, price: String, symbol: String, modifier: Modifier = Modifier) {
    val kind = when (symbol) { "♫" -> "headphones"; "◎" -> "glasses"; "▤" -> "scarf"; else -> "cap" }
    Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF302D70), Color(0xFF1D1B4B)))),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val glow = Color(0xFF9872FF)
                    drawCircle(glow.copy(alpha = 0.12f), radius = size.minDimension * 0.43f)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val stroke = size.minDimension * 0.055f
                    when (kind) {
                        "headphones" -> {
                            val arc = Path().apply {
                                moveTo(cx - size.width * .27f, cy + size.height * .13f)
                                cubicTo(cx - size.width * .30f, cy - size.height * .47f, cx + size.width * .30f, cy - size.height * .47f, cx + size.width * .27f, cy + size.height * .13f)
                            }
                            drawPath(arc, Color(0xFFB69AFF), style = Stroke(stroke * 1.7f, cap = StrokeCap.Round))
                            drawRoundRect(Color(0xFF8A63F5), Offset(cx - size.width * .34f, cy - size.height * .02f), Size(size.width * .17f, size.height * .38f), androidx.compose.ui.geometry.CornerRadius(stroke * 2))
                            drawRoundRect(Color(0xFF8A63F5), Offset(cx + size.width * .17f, cy - size.height * .02f), Size(size.width * .17f, size.height * .38f), androidx.compose.ui.geometry.CornerRadius(stroke * 2))
                            drawRoundRect(Color(0xFFD8C8FF), Offset(cx - size.width * .31f, cy + size.height * .04f), Size(size.width * .10f, size.height * .24f), androidx.compose.ui.geometry.CornerRadius(stroke))
                            drawRoundRect(Color(0xFFD8C8FF), Offset(cx + size.width * .21f, cy + size.height * .04f), Size(size.width * .10f, size.height * .24f), androidx.compose.ui.geometry.CornerRadius(stroke))
                        }
                        "glasses" -> {
                            drawRoundRect(Color(0xFFB99BFF), Offset(cx - size.width * .39f, cy - size.height * .18f), Size(size.width * .32f, size.height * .34f), androidx.compose.ui.geometry.CornerRadius(size.minDimension * .12f), style = Stroke(stroke))
                            drawRoundRect(Color(0xFFB99BFF), Offset(cx + size.width * .07f, cy - size.height * .18f), Size(size.width * .32f, size.height * .34f), androidx.compose.ui.geometry.CornerRadius(size.minDimension * .12f), style = Stroke(stroke))
                            drawLine(Color(0xFFD8C8FF), Offset(cx - size.width * .07f, cy - size.height * .04f), Offset(cx + size.width * .07f, cy - size.height * .04f), stroke)
                            drawLine(Color(0xFFB99BFF), Offset(cx - size.width * .39f, cy - size.height * .07f), Offset(cx - size.width * .48f, cy - size.height * .15f), stroke * .75f)
                            drawLine(Color(0xFFB99BFF), Offset(cx + size.width * .39f, cy - size.height * .07f), Offset(cx + size.width * .48f, cy - size.height * .15f), stroke * .75f)
                        }
                        "scarf" -> {
                            val scarf = Path().apply {
                                moveTo(cx - size.width * .30f, cy - size.height * .22f)
                                quadraticTo(cx, cy - size.height * .02f, cx + size.width * .30f, cy - size.height * .22f)
                                lineTo(cx + size.width * .23f, cy + size.height * .03f)
                                lineTo(cx + size.width * .17f, cy + size.height * .43f)
                                lineTo(cx - size.width * .01f, cy + size.height * .43f)
                                lineTo(cx - size.width * .02f, cy + size.height * .02f)
                                quadraticTo(cx - size.width * .24f, cy + size.height * .05f, cx - size.width * .30f, cy - size.height * .22f)
                                close()
                            }
                            drawPath(scarf, Brush.verticalGradient(listOf(Color(0xFFB08AFF), Color(0xFF6947D5))))
                            drawLine(Color(0xFFD8C8FF), Offset(cx - size.width * .18f, cy + size.height * .14f), Offset(cx - size.width * .18f, cy + size.height * .37f), stroke * .65f)
                            drawLine(Color(0xFFD8C8FF), Offset(cx + size.width * .09f, cy + size.height * .15f), Offset(cx + size.width * .09f, cy + size.height * .37f), stroke * .65f)
                        }
                        else -> {
                            val crown = Path().apply {
                                moveTo(cx - size.width * .32f, cy + size.height * .05f)
                                quadraticTo(cx - size.width * .30f, cy - size.height * .32f, cx, cy - size.height * .34f)
                                quadraticTo(cx + size.width * .31f, cy - size.height * .31f, cx + size.width * .32f, cy + size.height * .05f)
                                close()
                            }
                            drawPath(crown, Brush.verticalGradient(listOf(Color(0xFF9D79FF), Color(0xFF6546C8))))
                            drawArc(Color(0xFFB99BFF), 8f, 164f, false, Offset(cx - size.width * .43f, cy - size.height * .02f), Size(size.width * .86f, size.height * .40f), style = Stroke(stroke * 1.4f, cap = StrokeCap.Round))
                            drawLine(Color(0xFFD8C8FF), Offset(cx, cy - size.height * .27f), Offset(cx, cy + size.height * .02f), stroke * .45f)
                        }
                    }
                    drawCircle(Color.White.copy(alpha = .7f), radius = stroke * .32f, center = Offset(cx + size.width * .34f, cy - size.height * .34f))
                }
            }
            Text(name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text("★ $price", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
