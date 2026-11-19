package com.lingo.app

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class LessonTask(
    val kind: String, val title: String, val prompt: String,
    val answer: String = "", val options: List<String> = emptyList(),
    val explanation: String = "", val pairs: List<Pair<String, String>> = emptyList()
)

private fun lessonTasks(course: LanguageCourse, unit: LessonUnit, placement: Boolean): List<LessonTask> {
    val u = unit
    if (placement) return u.quiz.take(8).map {
        LessonTask("choice", "Проверка уровня", it.prompt, it.options[it.answer], it.options, it.explanation)
    }
    val v = u.vocabulary
    val a = v.first()
    val b = v.getOrElse(1) { a }
    val firstLine = u.dialogue.first()
    val blank = a.example.replace(a.term.substringBefore(" / "), "_____", ignoreCase = true)
    val fillAnswer = a.term.substringBefore(" / ")
    val fillPrompt = if (blank != a.example) blank else {
        val firstWord = firstLine.text.substringBefore(" ")
        firstLine.text.replaceFirst(firstWord, "_____", ignoreCase = true)
    }
    val actualFillAnswer = if (blank != a.example) fillAnswer else firstLine.text.substringBefore(" ")
    val quizTasks = u.quiz.map { quiz ->
        LessonTask("choice", "Проверь себя", quiz.prompt, quiz.options[quiz.answer], quiz.options, quiz.explanation)
    }
    return listOf(
        LessonTask("intro", "Новая тема", u.title, explanation = u.goal),
        LessonTask("learn", "Сначала запомни слова", "Прочитай новые слова и примеры. Не торопись — они понадобятся дальше."),
        LessonTask("match", "Соедини пары", "Найди перевод каждого нового слова.",
            pairs = v.map { it.term to it.meaning }, explanation = "Отлично! Эти слова встретятся в следующих заданиях.")
    ) + quizTasks + listOf(
        LessonTask("listen", "Слушай и собирай фразу", "Собери фразу из слов в правильном порядке.",
            firstLine.text, firstLine.text.split(Regex("\\s+")).shuffled(), "Перевод: " + firstLine.translation),
        LessonTask("type", "Переведи самостоятельно", "Напиши на изучаемом языке:\n«" + b.meaning + "»",
            b.term, explanation = "Пример: " + b.example),
        LessonTask("fill", "Восстанови предложение", "Вставь пропущенное слово:\n" + fillPrompt,
            actualFillAnswer, explanation = a.meaning + ". " + a.note),
        LessonTask("reading", "Прочитай и пойми", u.reading.title + "\n\n" + u.reading.body,
            u.reading.questions.first().second,
            listOf(u.reading.questions.first().second, u.reading.questions.last().second).distinct(),
            u.reading.translation),
        LessonTask("speak", "Произнеси фразу", "Произнеси вслух:\n«" + u.dialogue.last().text + "»",
            u.dialogue.last().text, explanation = u.dialogue.last().translation)
    )
}
@Composable
internal fun LessonFlow(course: LanguageCourse, unit: LessonUnit, placement: Boolean, onExit: () -> Unit, onComplete: (Int, Int) -> Unit) {
    val tasks = remember(course.id, unit.title, placement) { lessonTasks(course, unit, placement) }
    val graded = tasks.count { it.kind != "intro" && it.kind != "learn" }
    var index by remember(course.id, placement) { mutableIntStateOf(0) }
    var input by remember(course.id, placement) { mutableStateOf("") }
    var choice by remember(course.id, placement) { mutableStateOf("") }
    var tiles by remember(course.id, placement) { mutableStateOf(listOf<Int>()) }
    var matches by remember(course.id, placement) { mutableStateOf(setOf<Int>()) }
    var left by remember(course.id, placement) { mutableIntStateOf(-1) }
    var checked by remember(course.id, placement) { mutableStateOf(false) }
    var correct by remember(course.id, placement) { mutableStateOf(false) }
    var score by remember(course.id, placement) { mutableIntStateOf(0) }
    var finished by remember(course.id, placement) { mutableStateOf(false) }
    val task = tasks[index.coerceIn(0, tasks.lastIndex)]
    val rightPairOrder = remember(index, task.kind) { if (task.kind == "match") task.pairs.indices.shuffled() else emptyList() }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { input = it }
    }
    fun norm(s: String) = s.lowercase().trim().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ")
    fun candidate() = when (task.kind) {
        "listen" -> tiles.joinToString(" ") { task.options[it] }
        "choice", "reading" -> choice
        else -> input
    }
    fun check() {
        if (checked) return
        correct = if (task.kind == "match") matches.size == task.pairs.size else norm(candidate()) == norm(task.answer)
        if (correct) score++
        checked = true
    }
    fun next() {
        if (index == tasks.lastIndex) finished = true else {
            index++; input = ""; choice = ""; tiles = emptyList(); matches = emptySet()
            left = -1; checked = false; correct = false
        }
    }

    Dialog(onDismissRequest = onExit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Pale, shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().background(Pale).verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (finished) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BearMascot(Modifier.size(220.dp), emotion = if (score >= graded * 0.7f) 8 else 13) }
                    Text(ui("Урок пройден!", "Lesson complete!"), fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Surface(color = CardRaised, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(score.toString() + " / " + graded + " " + ui("правильных ответов", "correct answers"), color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            LinearProgressIndicator(progress = { score.toFloat() / graded.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(20.dp)), color = Blue, trackColor = Line)
                            Text("+${score * 5} XP", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Text(ui("Ты изучал слова в контексте, строил предложения, читал и тренировался говорить.", "You learned words in context, built sentences, read, and practiced speaking."), color = Muted, textAlign = TextAlign.Center)
                    Button(onClick = { onComplete(score, graded) }, modifier = Modifier.fillMaxWidth().height(54.dp).background(Brush.horizontalGradient(listOf(Color(0xFF8E6BFF), Color(0xFFB17CFF))), RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) { Text(ui("Продолжить", "Continue"), fontWeight = FontWeight.Bold) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExit, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Close, contentDescription = ui("Закрыть урок", "Close lesson"), tint = Muted, modifier = Modifier.size(27.dp)) }
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(progress = { (index + 1).toFloat() / tasks.size }, modifier = Modifier.weight(1f).height(11.dp).clip(CircleShape), color = Blue, trackColor = Line, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                        Spacer(Modifier.width(12.dp))
                        Text("${index + 1} / ${tasks.size}", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Text(if (placement) ui("Проверка уровня", "Placement check") else task.title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    when (task.kind) {
                        "intro" -> {
                            Text(task.prompt, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(task.explanation, fontSize = 17.sp, lineHeight = 25.sp, color = Muted)
                            Surface(color = CardRaised, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    ui("Сначала разберём тему простыми словами, затем выучим несколько слов и только после этого перейдём к заданиям.",
                                        "First, understand the topic. Then learn a few words before you try the exercises."),
                                    modifier = Modifier.padding(16.dp), color = Ink, fontSize = 14.sp, lineHeight = 21.sp
                                )
                            }
                        }
                        "learn" -> {
                            Text(task.prompt, fontSize = 18.sp, lineHeight = 25.sp, color = Muted)
                            unit.vocabulary.forEach { word ->
                                Surface(color = CardRaised, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(word.term, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text(word.meaning, color = Blue, fontWeight = FontWeight.SemiBold)
                                        Text(word.example, color = Ink, fontSize = 14.sp)
                                        if (word.note.isNotBlank()) Text(word.note, color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        "listen" -> {
                            Text(task.prompt, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Surface(color = CardRaised, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Blue)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(ui("Фраза для восприятия на слух", "Listen to the phrase"), color = Ink, fontWeight = FontWeight.Bold)
                                        Text(task.answer, color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            Text(ui("Твоя фраза", "Your phrase"), color = Muted, fontSize = 12.sp)
                            Surface(color = CardRaised, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (tiles.isEmpty()) Text(ui("Выбери слова ниже", "Choose the words below"), color = Muted)
                                    tiles.forEach { i ->
                                        Surface(color = Blue, shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { if (!checked) tiles = tiles - i }) {
                                            Text(task.options[i], Modifier.padding(horizontal = 8.dp, vertical = 7.dp), color = Color.White)
                                        }
                                    }
                                }
                            }
                            task.options.indices.toList().chunked(3).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    rowItems.filter { it !in tiles }.forEach { i ->
                                        OutlinedButton(onClick = { if (!checked) tiles = tiles + i }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) {
                                            Text(task.options[i], color = Ink)
                                        }
                                    }
                                }
                            }
                        }
                        "type", "fill" -> {
                            Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text(ui("Твой ответ", "Your answer")) }, minLines = 2)
                        }
                        "match" -> {
                            Text(task.prompt, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    task.pairs.forEachIndexed { i, pair ->
                                        OutlinedButton(onClick = { if (!checked && i !in matches) left = i }, modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (left == i) CardRaised else Card)) {
                                            Text(pair.first, color = Ink, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    rightPairOrder.forEach { i ->
                                        OutlinedButton(onClick = {
                                            if (!checked && left >= 0 && i !in matches) {
                                                if (left == i) matches = matches + i
                                                left = -1
                                            }
                                        }, modifier = Modifier.fillMaxWidth(), enabled = !checked && i !in matches) {
                                            Text(task.pairs[i].second, color = Ink, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                            Text(ui("Найдено пар: ", "Matched pairs: ") + matches.size + " / " + task.pairs.size, color = Muted, fontSize = 12.sp)
                        }
                        "choice", "reading" -> {
                            if (task.kind == "choice") {
                                Surface(color = CardRaised, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 18.dp, vertical = 25.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(task.prompt, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.VolumeUp, null, tint = Blue, modifier = Modifier.size(29.dp))
                                    }
                                }
                            } else {
                                Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            }
                            task.options.forEach { option ->
                                OutlinedButton(onClick = { if (!checked) choice = option }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    border = androidx.compose.foundation.BorderStroke(if (choice == option) 2.dp else 1.dp, if (choice == option) Blue else Line),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (choice == option) CardRaised else Card)) {
                                    Text(option, color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 16.sp)
                                    if (choice == option) Icon(Icons.Default.Check, null, tint = Blue)
                                }
                            }
                        }
                        "speak" -> {
                            Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text(ui("Распознанная речь", "Recognized speech")) })
                            Button(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (course.id) { "en" -> "en-US"; "ru" -> "ru-RU"; "es" -> "es-ES"; "de" -> "de-DE"; "fr" -> "fr-FR"; "it" -> "it-IT"; "pt" -> "pt-PT"; "tr" -> "tr-TR"; "ja" -> "ja-JP"; else -> course.id })
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Произнеси фразу на " + course.nativeName)
                                }
                                runCatching { speech.launch(intent) }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Mic, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(ui("Распознать речь", "Recognize speech"))
                            }
                        }
                    }
                    if (checked) {
                        Surface(color = CardRaised, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (correct) ui("Верно!", "Correct!") else ui("Разберём ответ", "Review your answer"), color = if (correct) Green else Gold, fontWeight = FontWeight.Bold)
                                if (!correct && task.answer.isNotBlank()) Text(ui("Правильный вариант: ", "Correct answer: ") + task.answer, color = Ink)
                                if (task.explanation.isNotBlank()) Text(task.explanation, color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            when {
                                task.kind == "intro" || task.kind == "learn" -> next()
                                !checked -> check()
                                !correct -> { checked = false; input = ""; choice = ""; tiles = emptyList() }
                                else -> next()
                            }
                        },
                        enabled = task.kind == "intro" || task.kind == "learn" || checked || when (task.kind) {
                            "listen" -> tiles.isNotEmpty()
                            "match" -> matches.size == task.pairs.size
                            "choice", "reading" -> choice.isNotBlank()
                            else -> input.isNotBlank()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            task.kind == "intro" -> "Изучить слова"
                            task.kind == "learn" -> "Начать практику"
                            !checked -> "Проверить ответ"
                            !correct -> "Исправить и повторить"
                            index == tasks.lastIndex -> "Завершить урок"
                            else -> "Следующий этап"
                        })
                    }
                    Text(ui("Lingo · маленькие шаги каждый день", "Lingo · little steps every day"), color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
internal fun OnboardingScreen(
    initialCourse: String,
    onFinish: (String, Int, String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var courseId by remember { mutableStateOf(initialCourse) }
    var goal by remember { mutableIntStateOf(10) }
    var level by remember { mutableStateOf("beginner") }
    var reason by remember { mutableStateOf("Для общения") }
    val selected = CourseCatalog.find(courseId)
    Column(Modifier.fillMaxSize().background(Pale).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BearMascot(Modifier.size(86.dp), emotion = 1)
            Column(Modifier.weight(1f)) {
                Text("LINGO", color = Blue, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                Text(ui("Я помогу тебе освоить язык. Без скучных таблиц на 40 страниц.", "I’ll help you learn a language without boring 40-page tables."), color = Ink, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().height(7.dp))
        Spacer(Modifier.height(22.dp))
        when (step) {
            0 -> {
                Text(ui("Какой язык изучаем?", "Which language are you learning?"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(ui("Выбери основной курс. Дополнительные можно будет добавить позже.", "Choose your main course. You can add more later."), color = Muted)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseCatalog.courses.forEach { item ->
                        Surface(color = if (courseId == item.id) CardRaised else Card, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { courseId = item.id }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                FlagBadge(item.id, Modifier.size(width = 42.dp, height = 28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) { Text(item.name, color = Ink, fontWeight = FontWeight.Bold); Text(item.nativeName, color = Muted, fontSize = 12.sp) }
                                if (courseId == item.id) Icon(Icons.Default.Check, null, tint = Blue)
                            }
                        }
                    }
                }
            }
            1 -> {
                Text(ui("Зачем тебе язык?", "Why are you learning?"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                listOf("Для путешествий" to "For travel", "Для общения" to "For conversation", "Для учёбы" to "For studies", "Для себя и интереса" to "Personal interest").forEach { (choice, choiceEn) ->
                    Surface(color = if (reason == choice) CardRaised else Card, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { reason = choice }) {
                        Text(ui(choice, choiceEn), Modifier.padding(16.dp), color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            2 -> {
                Text(ui("Сколько времени уделять?", "How much time per day?"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(ui("Выбирай комфортный темп. Его можно поменять в профиле.", "Choose a comfortable pace. You can change it in your profile."), color = Muted)
                listOf(5, 10, 15, 20).forEach { minutes ->
                    Surface(color = if (goal == minutes) CardRaised else Card, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { goal = minutes }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ui("$minutes минут в день", "$minutes minutes a day"), modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                            if (goal == minutes) Icon(Icons.Default.Check, null, tint = Blue)
                        }
                    }
                }
            }
            else -> {
                Text(ui("С чего начнём?", "Where should we start?"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(ui("Можно начать с основ или пройти короткую проверку уровня.", "Start with the basics or take a short placement check."), color = Muted)
                listOf("beginner" to ui("Я начинаю с нуля", "I am a complete beginner"), "placement" to ui("Я уже кое-что знаю", "I already know some of it")).forEach { (id, label) ->
                    Surface(color = if (level == id) CardRaised else Card, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { level = id }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); if (level == id) Icon(Icons.Default.Check, null, tint = Blue) }
                    }
                }
                Text(ui("Основной курс: ${selected.name}", "Main course: ${selected.name}"), color = Muted, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text(ui("Назад", "Back")) }
            Button(onClick = { if (step < 3) step++ else onFinish(courseId, goal, level) }, modifier = Modifier.weight(2f).height(52.dp).background(Brush.horizontalGradient(listOf(Color(0xFF8E6BFF), Color(0xFFB17CFF))), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) { Text(if (step < 3) ui("Продолжить", "Continue") else ui("Начать обучение", "Start learning")) }
        }
    }
}
