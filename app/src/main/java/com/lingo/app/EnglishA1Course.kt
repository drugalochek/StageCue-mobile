package com.lingo.app

/**
 * Beginner-first American English course.
 * Every unit introduces a small amount of language before asking the learner to use it.
 */
object EnglishA1Course {
    val units = listOf(
        LessonUnit(
            "Первые слова", "Научись здороваться, прощаться и отвечать «да» или «нет».",
            listOf(
                DialogueLine("Mia", "Hello!", "Привет! / Здравствуйте!"),
                DialogueLine("Leo", "Hi!", "Привет!"),
                DialogueLine("Mia", "Bye!", "Пока!"),
                DialogueLine("Leo", "See you!", "До встречи!")
            ),
            listOf(
                VocabEntry("hello", "здравствуйте; привет", "Hello, Mia!", "Нейтральное приветствие."),
                VocabEntry("hi", "привет", "Hi, Leo!", "Короткое неформальное приветствие."),
                VocabEntry("bye", "пока; до свидания", "Bye, Mom!", "Обычное прощание."),
                VocabEntry("yes / no", "да / нет", "Yes, please. / No, thanks.", "Два базовых ответа.")
            ),
            ReadingText("At the door", "Mia: Hello!\nLeo: Hi!\nMia: Bye, Leo!\nLeo: Bye!", "Мия: Привет!\nЛео: Привет!\nМия: Пока, Лео!\nЛео: Пока!",
                listOf("What does “Hello” mean?" to "Привет! / Здравствуйте.", "What does “Bye” mean?" to "Пока.")),
            listOf(
                QuizItem("Как переводится “Hello”?", listOf("Привет", "Спасибо", "Пожалуйста"), 0, "Hello — приветствие."),
                QuizItem("Как сказать «пока»?", listOf("Yes", "Bye", "No"), 1, "Bye используют при прощании."),
                QuizItem("Что означает “No”?", listOf("Да", "Нет", "Привет"), 1, "No — отрицательный ответ.")
            )
        ),
        LessonUnit(
            "Знакомство", "Научись назвать своё имя и спросить, как зовут другого человека.",
            listOf(
                DialogueLine("Mia", "What's your name?", "Как тебя зовут?"),
                DialogueLine("Leo", "My name is Leo.", "Меня зовут Лео."),
                DialogueLine("Mia", "I'm Mia.", "Я Мия."),
                DialogueLine("Leo", "Nice to meet you!", "Приятно познакомиться!")
            ),
            listOf(
                VocabEntry("name", "имя", "My name is Leo.", "Слово для имени человека."),
                VocabEntry("my", "мой; моя; моё", "My name is Mia.", "Показывает, что что-то принадлежит говорящему."),
                VocabEntry("your", "твой; ваш", "What's your name?", "Обращение к собеседнику."),
                VocabEntry("nice to meet you", "приятно познакомиться", "Nice to meet you, Alex.", "Вежливая фраза при знакомстве.")
            ),
            ReadingText("Two new friends", "Hi! My name is Ben. This is my friend, Anna. Anna says, “Nice to meet you!”", "Привет! Меня зовут Бен. Это моя подруга Анна. Анна говорит: «Приятно познакомиться!»",
                listOf("What is the boy's name?" to "Ben.", "Who is Anna?" to "Ben's friend.")),
            listOf(
                QuizItem("Как спросить «Как тебя зовут?»", listOf("What's your name?", "How old are you?", "Where are you?"), 0, "What's your name? — Как тебя зовут?"),
                QuizItem("Выбери перевод “My name is Sam.”", listOf("Твоё имя Сэм.", "Меня зовут Сэм.", "Я знаю Сэма."), 1, "My name is… — Меня зовут…"),
                QuizItem("Что говорят при знакомстве?", listOf("Nice to meet you.", "Good night.", "I'm sorry."), 0, "Nice to meet you — приятно познакомиться.")
            )
        ),
        LessonUnit(
            "Кто есть кто", "Познакомься с местоимениями I, you, he, she, we и they.",
            listOf(
                DialogueLine("Mia", "I am Mia.", "Я Мия."),
                DialogueLine("Leo", "You are Leo.", "Ты Лео."),
                DialogueLine("Mia", "He is Ben. She is Anna.", "Он Бен. Она Анна."),
                DialogueLine("Leo", "We are friends. They are here.", "Мы друзья. Они здесь.")
            ),
            listOf(
                VocabEntry("I", "я", "I am Mia.", "Говорящий говорит о себе."),
                VocabEntry("you", "ты; вы", "You are here.", "Человек или люди, к которым обращаются."),
                VocabEntry("he / she", "он / она", "He is Ben. She is Anna.", "He — о мужчине или мальчике; she — о женщине или девочке."),
                VocabEntry("we / they", "мы / они", "We are friends. They are friends.", "We включает говорящего; they — другие люди или предметы.")
            ),
            ReadingText("Our group", "I am Alex. You are Sam. He is Ben. She is Mia. We are a team. They are our friends.", "Я Алекс. Ты Сэм. Он Бен. Она Мия. Мы команда. Они наши друзья.",
                listOf("Which word means «она»?" to "She.", "Which word means «мы»?" to "We.")),
            listOf(
                QuizItem("Как сказать «я»?", listOf("I", "He", "They"), 0, "I означает «я»."),
                QuizItem("Какое местоимение означает «она»?", listOf("We", "She", "You"), 1, "She — она."),
                QuizItem("Как перевести “They are friends”?", listOf("Мы друзья.", "Он друг.", "Они друзья."), 2, "They — они.")
            )
        ),
        LessonUnit(
            "Глагол to be: am / is / are", "Научись говорить, кто ты и кто другие люди, используя am, is и are.",
            listOf(
                DialogueLine("Mia", "I am happy.", "Я счастлива."),
                DialogueLine("Leo", "You are kind.", "Ты добрый."),
                DialogueLine("Mia", "He is my brother.", "Он мой брат."),
                DialogueLine("Leo", "We are ready.", "Мы готовы.")
            ),
            listOf(
                VocabEntry("am", "форма глагола be с I", "I am Alex.", "После I используется am."),
                VocabEntry("is", "форма глагола be с he / she / it", "She is happy.", "С he, she и it используется is."),
                VocabEntry("are", "форма глагола be с you / we / they", "They are here.", "С you, we и they используется are."),
                VocabEntry("happy", "счастливый; довольный", "I am happy.", "Прилагательное описывает состояние.")
            ),
            ReadingText("In class", "I am Nina. My teacher is Mark. He is nice. We are in class. They are my friends.", "Я Нина. Мой учитель — Марк. Он хороший. Мы на уроке. Они мои друзья.",
                listOf("Who is Mark?" to "Nina's teacher.", "Which form is used with “we”?" to "Are.")),
            listOf(
                QuizItem("Complete: “I ___ Alex.”", listOf("is", "am", "are"), 1, "С I используется am."),
                QuizItem("Complete: “She ___ nice.”", listOf("is", "am", "are"), 0, "С she используется is."),
                QuizItem("Complete: “We ___ ready.”", listOf("is", "am", "are"), 2, "С we используется are.")
            )
        ),
        LessonUnit(
            "Простые предложения", "Собирай короткие предложения о себе, людях и предметах.",
            listOf(
                DialogueLine("Leo", "I am a student.", "Я студент."),
                DialogueLine("Mia", "You are my friend.", "Ты мой друг."),
                DialogueLine("Leo", "This is a book.", "Это книга."),
                DialogueLine("Mia", "It is new.", "Она новая.")
            ),
            listOf(
                VocabEntry("this", "это; этот; эта", "This is a book.", "Указывает на предмет рядом."),
                VocabEntry("a", "неопределённый артикль", "a book", "Ставится перед одним исчисляемым предметом, когда говорим о нём впервые."),
                VocabEntry("student", "ученик; студент", "I am a student.", "Человек, который учится."),
                VocabEntry("new", "новый", "It is new.", "Описание предмета или человека.")
            ),
            ReadingText("A new book", "This is a book. It is new. I am a student. The book is for my class.", "Это книга. Она новая. Я студент. Книга нужна мне для занятий.",
                listOf("What is new?" to "The book.", "Who is the speaker?" to "A student.")),
            listOf(
                QuizItem("Выбери правильный перевод “This is a book.”", listOf("Это книга.", "Я читаю книгу.", "Книга старая."), 0, "This is… — Это…"),
                QuizItem("Что означает “student”?", listOf("Учитель", "Студент", "Друг"), 1, "Student — ученик или студент."),
                QuizItem("Выбери правильную фразу:", listOf("I is a student.", "I am a student.", "I are a student."), 1, "С I используется am.")
            )
        ),
        LessonUnit(
            "Числа и возраст", "Выучи числа от одного до двадцати и научись говорить о возрасте.",
            listOf(
                DialogueLine("Mia", "How old are you?", "Сколько тебе лет?"),
                DialogueLine("Leo", "I am ten years old.", "Мне десять лет."),
                DialogueLine("Mia", "I am eleven.", "Мне одиннадцать."),
                DialogueLine("Leo", "Great!", "Здорово!")
            ),
            listOf(
                VocabEntry("one – five", "один – пять", "One, two, three, four, five.", "Начни с первых пяти чисел."),
                VocabEntry("six – ten", "шесть – десять", "Six, seven, eight, nine, ten.", "Ten — десять."),
                VocabEntry("eleven – twenty", "одиннадцать – двадцать", "Eleven, twelve, thirteen… twenty.", "Обрати внимание на формы thirteen и fifteen."),
                VocabEntry("How old are you?", "Сколько тебе лет?", "How old are you? — I am ten.", "Для ответа используй I am + число.")
            ),
            ReadingText("Birthday", "Today is Ben's birthday. He is ten years old. His sister Anna is twelve. They are happy.", "Сегодня день рождения Бена. Ему десять лет. Его сестре Анне двенадцать. Они счастливы.",
                listOf("How old is Ben?" to "Ten.", "How old is Anna?" to "Twelve.")),
            listOf(
                QuizItem("Как будет «семь»?", listOf("Seven", "Eleven", "Seventeen"), 0, "Seven — семь."),
                QuizItem("Как спросить «Сколько тебе лет?»", listOf("How are you?", "How old are you?", "What is your name?"), 1, "How old are you? — Сколько тебе лет?"),
                QuizItem("Complete: “I am ___ years old.” (15)", listOf("fifty", "five", "fifteen"), 2, "Fifteen — пятнадцать.")
            )
        ),
        LessonUnit(
            "Предметы вокруг нас", "Назови знакомые вещи и используй их в простых предложениях.",
            listOf(
                DialogueLine("Mia", "What is this?", "Что это?"),
                DialogueLine("Leo", "It is a phone.", "Это телефон."),
                DialogueLine("Mia", "Is it your book?", "Это твоя книга?"),
                DialogueLine("Leo", "No, it is my bag.", "Нет, это моя сумка.")
            ),
            listOf(
                VocabEntry("book", "книга", "This is a book.", "Предмет для чтения."),
                VocabEntry("phone", "телефон", "It is my phone.", "Устройство для звонков."),
                VocabEntry("table", "стол", "The book is on the table.", "Предмет мебели."),
                VocabEntry("water", "вода", "I drink water.", "Обычно неисчисляемое существительное.")
            ),
            ReadingText("In my room", "This is my room. I have a bed, a table and a chair. My phone is on the table. My book is in my bag.", "Это моя комната. У меня есть кровать, стол и стул. Мой телефон на столе. Моя книга в сумке.",
                listOf("Where is the phone?" to "On the table.", "Where is the book?" to "In the bag.")),
            listOf(
                QuizItem("Как переводится “phone”?", listOf("Телефон", "Стол", "Книга"), 0, "Phone — телефон."),
                QuizItem("Что означает “water”?", listOf("Еда", "Вода", "Комната"), 1, "Water — вода."),
                QuizItem("Complete: “The book is on the ___.”", listOf("table", "water", "phone"), 0, "On the table — на столе.")
            )
        ),
        LessonUnit(
            "Повторение: мой первый разговор", "Объедини приветствие, знакомство, простые предложения, числа и знакомые слова.",
            listOf(
                DialogueLine("Mia", "Hello! What's your name?", "Привет! Как тебя зовут?"),
                DialogueLine("Leo", "Hi! My name is Leo. I am twelve.", "Привет! Меня зовут Лео. Мне двенадцать."),
                DialogueLine("Mia", "Nice to meet you, Leo.", "Приятно познакомиться, Лео."),
                DialogueLine("Leo", "Nice to meet you, too!", "Мне тоже приятно познакомиться!")
            ),
            listOf(
                VocabEntry("Hello! What's your name?", "Привет! Как тебя зовут?", "Hello! What's your name?", "Объединяет приветствие и вопрос."),
                VocabEntry("My name is…", "Меня зовут…", "My name is Leo.", "Подставь своё имя."),
                VocabEntry("I am … years old.", "Мне … лет.", "I am twelve years old.", "После am укажи возраст."),
                VocabEntry("Nice to meet you, too.", "Мне тоже приятно познакомиться.", "Nice to meet you, too!", "Естественный ответ на знакомство.")
            ),
            ReadingText("A first conversation", "Hello! My name is Emma. I am eleven years old. This is my friend, Tom. He is twelve. We are students. Nice to meet you!", "Привет! Меня зовут Эмма. Мне одиннадцать лет. Это мой друг Том. Ему двенадцать. Мы ученики. Приятно познакомиться!",
                listOf("How old is Emma?" to "Eleven.", "Are Emma and Tom students?" to "Yes, they are.")),
            listOf(
                QuizItem("Как правильно представиться?", listOf("My name is Anna.", "I name Anna.", "Me is Anna."), 0, "My name is… — Меня зовут…"),
                QuizItem("Complete: “He ___ my friend.”", listOf("am", "are", "is"), 2, "С he используется is."),
                QuizItem("Как сказать «Мне двенадцать лет»?", listOf("I have twelve.", "I am twelve years old.", "I is twelve."), 1, "В английском возраст выражается с be: I am twelve.")
            )
        )
    )
}
