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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private data class LessonTask(
    val kind: String, val title: String, val prompt: String,
    val answer: String = "", val options: List<String> = emptyList(),
    val explanation: String = "", val pairs: List<Pair<String, String>> = emptyList()
)

private fun lessonTasks(course: LanguageCourse, placement: Boolean): List<LessonTask> {
    val u = course.firstUnit
    if (placement) return u.quiz.take(8).map {
        LessonTask("choice", "Проверка уровня", it.prompt, it.options[it.answer], it.options, it.explanation)
    }
    val v = u.vocabulary
    val a = v[0]
    val b = v.getOrElse(1) { a }
    val c = v.getOrElse(2) { a }
    val firstLine = u.dialogue.first()
    val blank = a.example.replace(a.term, "_____", ignoreCase = true)
    val fillAnswer = if (blank != a.example) a.term else c.term
    val fillPrompt = if (blank != a.example) blank else c.example
    return listOf(
        LessonTask("intro", "Знакомство с темой", u.title, explanation = u.goal),
        LessonTask("listen", "Слушай и собирай фразу", "Собери фразу из слов в правильном порядке.",
            firstLine.text, firstLine.text.split(Regex("\\s+")).shuffled(), "Перевод: " + firstLine.translation),
        LessonTask("type", "Переведи самостоятельно", "Напиши на изучаемом языке:\n«" + b.meaning + "»",
            b.term, explanation = "Пример: " + b.example),
        LessonTask("fill", "Восстанови предложение", "Вставь пропущенное слово:\n" + fillPrompt,
            fillAnswer, explanation = a.meaning + ". " + a.note),
        LessonTask("match", "Соедини пары", "Найди перевод каждого слова.",
            pairs = v.take(4).map { it.term to it.meaning }, explanation = "Повтори эти пары: они встретятся дальше."),
        LessonTask("reading", "Прочитай и пойми", u.reading.title + "\n\n" + u.reading.body,
            u.reading.questions.first().second,
            listOf(u.reading.questions.first().second, u.reading.questions.last().second).distinct(),
            u.reading.translation),
        LessonTask("speak", "Произнеси фразу", "Произнеси вслух:\n«" + u.dialogue.last().text + "»",
            u.dialogue.last().text, explanation = u.dialogue.last().translation)
    )
}

@Composable
internal fun LessonFlow(course: LanguageCourse, placement: Boolean, onExit: () -> Unit, onComplete: (Int, Int) -> Unit) {
    val tasks = remember(course.id, placement) { lessonTasks(course, placement) }
    val graded = tasks.count { it.kind != "intro" }
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

    Dialog(onDismissRequest = onExit) {
        Surface(color = Card, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(max = 710.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                if (finished) {
                    Text("Урок завершён", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text(score.toString() + " из " + graded + " упражнений выполнено правильно", color = Muted)
                    LinearProgressIndicator(progress = { score.toFloat() / graded.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(9.dp))
                    Text("Ты изучал слова в контексте, строил предложения, читал и тренировался говорить.", color = Ink)
                    Button(onClick = { onComplete(score, graded) }, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBack, contentDescription = "Закрыть урок", tint = Ink) }
                        Column(Modifier.weight(1f)) {
                            Text(if (placement) "Проверка уровня" else course.name, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Шаг " + (index + 1) + " из " + tasks.size, color = Muted, fontSize = 12.sp)
                        }
                        Text((index * 100 / tasks.size).toString() + "%", color = Blue, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { index.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Blue, trackColor = Line)
                    Surface(color = CardRaised, shape = RoundedCornerShape(12.dp)) {
                        Text(task.title.uppercase(), Modifier.fillMaxWidth().padding(11.dp), color = Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    when (task.kind) {
                        "intro" -> {
                            Text(task.prompt, fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(task.explanation, color = Muted)
                            Text("Новые выражения", color = Ink, fontWeight = FontWeight.Bold)
                            course.firstUnit.vocabulary.take(4).forEach { w ->
                                Surface(color = CardRaised, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(13.dp)) {
                                        Text(w.term, color = Ink, fontWeight = FontWeight.Bold)
                                        Text(w.meaning, color = Blue)
                                        if (w.note.isNotBlank()) Text(w.note, color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            Text("Короткий диалог", color = Ink, fontWeight = FontWeight.Bold)
                            course.firstUnit.dialogue.take(2).forEach { d ->
                                Surface(color = CardRaised, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(d.speaker, color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(d.text, color = Ink)
                                        Text(d.translation, color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        "listen" -> {
                            Text(task.prompt, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Surface(color = CardRaised, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Blue)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Фраза для восприятия на слух", color = Ink, fontWeight = FontWeight.Bold)
                                        Text(task.answer, color = Muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            Text("Твоя фраза", color = Muted, fontSize = 12.sp)
                            Surface(color = CardRaised, shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (tiles.isEmpty()) Text("Выбери слова ниже", color = Muted)
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
                            OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Твой ответ") }, minLines = 2)
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
                                    task.pairs.indices.reversed().forEach { i ->
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
                            Text("Найдено пар: " + matches.size + " / " + task.pairs.size, color = Muted, fontSize = 12.sp)
                        }
                        "choice", "reading" -> {
                            Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            task.options.forEach { option ->
                                OutlinedButton(onClick = { if (!checked) choice = option }, modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (choice == option) CardRaised else Card)) {
                                    Text(option, color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                }
                            }
                        }
                        "speak" -> {
                            Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Распознанная речь") })
                            Button(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, course.id)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Произнеси фразу на " + course.nativeName)
                                }
                                runCatching { speech.launch(intent) }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Mic, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Распознать речь")
                            }
                        }
                    }
                    if (checked) {
                        Surface(color = CardRaised, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(if (correct) "Верно!" else "Разберём ответ", color = if (correct) Green else Gold, fontWeight = FontWeight.Bold)
                                if (!correct && task.answer.isNotBlank()) Text("Правильный вариант: " + task.answer, color = Ink)
                                if (task.explanation.isNotBlank()) Text(task.explanation, color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            when {
                                task.kind == "intro" -> next()
                                !checked -> check()
                                !correct -> { checked = false; input = ""; choice = ""; tiles = emptyList() }
                                else -> next()
                            }
                        },
                        enabled = task.kind == "intro" || checked || when (task.kind) {
                            "listen" -> tiles.isNotEmpty()
                            "match" -> matches.size == task.pairs.size
                            "choice", "reading" -> choice.isNotBlank()
                            else -> input.isNotBlank()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            task.kind == "intro" -> "Перейти к практике"
                            !checked -> "Проверить ответ"
                            !correct -> "Исправить и повторить"
                            index == tasks.lastIndex -> "Завершить урок"
                            else -> "Следующий этап"
                        })
                    }
                    Text("После каждого упражнения разбираем результат и закрепляем материал.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
                Text("Я помогу тебе освоить язык. Без скучных таблиц на 40 страниц.", color = Ink, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().height(7.dp))
        Spacer(Modifier.height(22.dp))
        when (step) {
            0 -> {
                Text("Какой язык изучаем?", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Выбери основной курс. Дополнительные можно будет добавить позже.", color = Muted)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseCatalog.courses.forEach { item ->
                        Surface(color = if (courseId == item.id) Color(0xFFE9EDFF) else Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { courseId = item.id }) {
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
                Text("Зачем тебе язык?", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                listOf("Для путешествий", "Для общения", "Для учёбы", "Для себя и интереса").forEach { choice ->
                    Surface(color = if (reason == choice) Color(0xFFE9EDFF) else Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { reason = choice }) {
                        Text(choice, Modifier.padding(16.dp), color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            2 -> {
                Text("Сколько времени уделять?", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Выбирай комфортный темп. Его можно поменять в профиле.", color = Muted)
                listOf(5, 10, 15, 20).forEach { minutes ->
                    Surface(color = if (goal == minutes) Color(0xFFE9EDFF) else Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { goal = minutes }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$minutes минут в день", modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                            if (goal == minutes) Icon(Icons.Default.Check, null, tint = Blue)
                        }
                    }
                }
            }
            else -> {
                Text("С чего начнём?", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Можно начать с основ или пройти короткую проверку уровня.", color = Muted)
                listOf("beginner" to "Я начинаю с нуля", "placement" to "Я уже кое-что знаю").forEach { (id, label) ->
                    Surface(color = if (level == id) Color(0xFFE9EDFF) else Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clickable { level = id }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); if (level == id) Icon(Icons.Default.Check, null, tint = Blue) }
                    }
                }
                Text("Основной курс: ${selected.name}", color = Muted, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Назад") }
            Button(onClick = { if (step < 3) step++ else onFinish(courseId, goal, level) }, modifier = Modifier.weight(2f)) { Text(if (step < 3) "Продолжить" else "Начать обучение") }
        }
    }
}

@Composable
internal fun FlagBadge(code: String, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(5.dp))) {
        val w = size.width
        val h = size.height
        fun rect(color: Color, x: Float, y: Float, rw: Float, rh: Float) = drawRect(color, Offset(x, y), Size(rw, rh))
        when (code) {
            "en" -> {
                rect(Color(0xFF233B78),0f,0f,w,h); rect(Color.White,0f,h*.40f,w,h*.20f); rect(Color(0xFFC83D50),0f,h*.45f,w,h*.10f)
                rect(Color.White,w*.43f,0f,w*.14f,h); rect(Color(0xFFC83D50),w*.465f,0f,w*.07f,h)
            }
            "es" -> { rect(Color(0xFFAA2638),0f,0f,w,h*.25f); rect(Color(0xFFFFC928),0f,h*.25f,w,h*.5f); rect(Color(0xFFAA2638),0f,h*.75f,w,h*.25f) }
            "de" -> { rect(Color(0xFF222222),0f,0f,w,h/3); rect(Color(0xFFD43B35),0f,h/3,w,h/3); rect(Color(0xFFFFD34D),0f,h*2/3,w,h/3) }
            "fr" -> { rect(Color(0xFF294A9B),0f,0f,w/3,h); rect(Color.White,w/3,0f,w/3,h); rect(Color(0xFFE34C55),w*2/3,0f,w/3,h) }
            "it" -> { rect(Color(0xFF26965A),0f,0f,w/3,h); rect(Color.White,w/3,0f,w/3,h); rect(Color(0xFFE34C55),w*2/3,0f,w/3,h) }
            "pt" -> { rect(Color(0xFF16824B),0f,0f,w*.4f,h); rect(Color(0xFFD53B43),w*.4f,0f,w*.6f,h) }
            "tr" -> { rect(Color(0xFFD83A48),0f,0f,w,h); drawCircle(Color.White,h*.25f,Offset(w*.43f,h*.5f)); drawCircle(Color(0xFFD83A48),h*.20f,Offset(w*.47f,h*.47f)); drawCircle(Color.White,h*.08f,Offset(w*.69f,h*.5f)) }
            "ja" -> { rect(Color.White,0f,0f,w,h); drawCircle(Color(0xFFBC3344),h*.27f,Offset(w*.5f,h*.5f)) }
        }
        drawRoundRect(Color(0x22000000), style = Stroke(width = 1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
    }
}
