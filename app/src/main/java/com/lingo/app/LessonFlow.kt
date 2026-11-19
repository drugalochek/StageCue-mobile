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
    val kind: String,
    val title: String,
    val prompt: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val explanation: String = ""
)

private fun lessonTasks(course: LanguageCourse, placement: Boolean): List<LessonTask> {
    val unit = course.firstUnit
    val words = unit.vocabulary
    val first = words[0]
    val second = words.getOrElse(1) { first }
    val third = words.getOrElse(2) { second }
    val quiz = unit.quiz.first()
    val readingQuestion = unit.reading.questions.first()
    val spoken = unit.dialogue.first().text
    val sample = first.example.replace(first.term, "_____ ", ignoreCase = true).replace("_____  ", "_____ ").trim()
    return if (placement) {
        unit.quiz.take(8).map { LessonTask("choice", "Проверка уровня", it.prompt, it.options[it.answer], it.options, it.explanation) }
    } else listOf(
        LessonTask("choice", "Узнай слово", "Как переводится «${first.term}»?", first.meaning,
            listOf(first.meaning, second.meaning, third.meaning).distinct(), first.note),
        LessonTask("type", "Вспомни написание", "Напиши на изучаемом языке: «${second.meaning}»", second.term,
            explanation = second.example),
        LessonTask("fill", "Заполни пропуск", if (sample.contains("_____")) sample else "Вставь слово «${third.term}» в пример: ${third.example}",
            if (sample.contains("_____")) first.term else third.term, explanation = first.example),
        LessonTask("choice", "Выбери правильный ответ", quiz.prompt, quiz.options[quiz.answer], quiz.options, quiz.explanation),
        LessonTask("reading", "Пойми текст", readingQuestion.first, readingQuestion.second,
            listOf(readingQuestion.second, "В тексте этого не сказано", unit.reading.questions.last().second).distinct(),
            "Ответ ищется в тексте «${unit.reading.title}»."),
        LessonTask("order", "Собери фразу", "Перепечатай фразу в правильном порядке:\n" + spoken.split(" ").shuffled().joinToString("  "), spoken,
            explanation = unit.dialogue.first().translation),
        LessonTask("speak", "Произнеси вслух", "Нажми на микрофон и произнеси:\n«$spoken»", spoken,
            explanation = unit.dialogue.first().translation)
    )
}

@Composable
internal fun LessonFlow(
    course: LanguageCourse,
    placement: Boolean,
    onExit: () -> Unit,
    onComplete: (Int, Int) -> Unit
) {
    val tasks = remember(course.id, placement) { lessonTasks(course, placement) }
    var index by remember(course.id, placement) { mutableIntStateOf(0) }
    var input by remember(course.id, placement) { mutableStateOf("") }
    var chosen by remember(course.id, placement) { mutableStateOf("") }
    var checked by remember(course.id, placement) { mutableStateOf(false) }
    var wasCorrect by remember(course.id, placement) { mutableStateOf(false) }
    var score by remember(course.id, placement) { mutableIntStateOf(0) }
    var finished by remember(course.id, placement) { mutableStateOf(false) }
    val context = LocalContext.current
    val task = tasks[index.coerceIn(0, tasks.lastIndex)]
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (spoken.isNotBlank()) input = spoken
    }
    fun normalize(value: String) = value.lowercase().trim().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ")
    fun checkAnswer() {
        if (checked) return
        val candidate = if (task.kind == "choice") chosen else input
        wasCorrect = normalize(candidate) == normalize(task.answer)
        if (wasCorrect) score++
        checked = true
    }
    Dialog(onDismissRequest = onExit) {
        Surface(color = Color.White, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (finished) {
                    Text("Урок завершён", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("$score из ${tasks.size} заданий правильно", color = Muted)
                    LinearProgressIndicator(progress = { score.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(9.dp))
                    Text(if (score >= tasks.size * 0.7f) "Хорошая работа. Материал закреплён." else "Часть заданий стоит повторить. Ошибки для этого и существуют.", color = Ink)
                    Button(onClick = { onComplete(score, tasks.size) }, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBack, contentDescription = "Закрыть урок") }
                        Column(Modifier.weight(1f)) {
                            Text(if (placement) "Проверка уровня" else course.name, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Задание ${index + 1} из ${tasks.size}", color = Muted, fontSize = 12.sp)
                        }
                        Text("${(index * 100 / tasks.size)}%", color = Blue, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { index.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Blue, trackColor = Line)
                    Surface(color = Pale, shape = RoundedCornerShape(12.dp)) {
                        Text(task.title.uppercase(), Modifier.fillMaxWidth().padding(11.dp), color = Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                    Text(task.prompt, fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    when (task.kind) {
                        "choice", "reading" -> task.options.forEach { option ->
                            val active = chosen == option
                            OutlinedButton(
                                onClick = { if (!checked) chosen = option },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (checked && option == task.answer) Color(0xFFE8F7EF) else if (active) Color(0xFFE9EDFF) else Color.White)
                            ) { Text(option, color = Ink, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
                        }
                        "speak" -> {
                            OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Распознанная речь") })
                            Button(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, course.id)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Произнеси фразу на ${course.nativeName}")
                                }
                                runCatching { speechLauncher.launch(intent) }
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("Распознать речь")
                            }
                            Text("Распознавание зависит от доступного на устройстве голосового сервиса.", color = Muted, fontSize = 11.sp)
                        }
                        else -> OutlinedTextField(value = input, onValueChange = { if (!checked) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (task.kind == "order") "Фраза целиком" else "Твой ответ") })
                    }
                    if (checked) {
                        Surface(color = if (wasCorrect) Color(0xFFE8F7EF) else Color(0xFFFFF0F0), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (wasCorrect) "Верно!" else "Правильный ответ: ${task.answer}", color = if (wasCorrect) Green else Color(0xFFC75B5B), fontWeight = FontWeight.Bold)
                                if (task.explanation.isNotBlank()) Text(task.explanation, color = Ink, fontSize = 12.sp)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!checked) checkAnswer()
                            else if (index == tasks.lastIndex) finished = true
                            else { index++; input = ""; chosen = ""; checked = false; wasCorrect = false }
                        },
                        enabled = checked || (if (task.kind == "choice" || task.kind == "reading") chosen.isNotBlank() else input.isNotBlank()),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (!checked) "Проверить" else if (index == tasks.lastIndex) "Завершить урок" else "Дальше") }
                    Text("За урок можно ошибаться. Важно понять объяснение и попробовать снова.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
