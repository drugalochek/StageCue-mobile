# Lingo

Lingo is a mobile-first language learning app built with Kotlin and Jetpack Compose.

## Current foundation
- Language-specific course catalog: English, Spanish, German, French, Italian, Portuguese, Turkish and Japanese
- Each starter course has its own authored dialogue, vocabulary, reading passage, comprehension prompts and quiz questions
- Linear learning path with stages and lessons
- Stage knowledge check to skip a stage
- Daily streak and XP foundation
- Daily quests foundation
- Expressive star-marked bear mascot
- GitHub Actions debug APK build

## Content architecture
Course content is modeled independently from the UI in `CourseCatalog.kt`. Each language supplies its own lesson units, dialogue turns, vocabulary entries, reading passages and assessment items. New languages can be added by creating a course entry with native examples and grammar notes. The current eight courses are starter content, not a claim of complete fluency coverage.

## Build
`gradle assembleDebug`

The Android application ID is `com.lingo.app`.
