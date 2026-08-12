# UniScribe — Умный Конспект Лекций 🎓📱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Gemini API](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-orange.svg)](https://ai.google.dev/)
[![GitHub Pages](https://img.shields.io/badge/Website-GitHub%20Pages-brightgreen.svg)](https://san5ezez.github.io/UniScribe/)

**UniScribe (Умный конспект)** — это современное Android-приложение для студентов и преподавателей, превращающее аудиозаписи лекций в структурированные текстовые конспекты с помощью моделей искусственного интеллекта Google Gemini 2.5 Flash.

🌐 **Официальный сайт приложения:** [san5ezez.github.io/UniScribe](https://san5ezez.github.io/UniScribe/)

---

## 🌟 Ключевые возможности

- 🎙️ **Запись аудио лекций:** Встроенный аудиорекордер с таймером, индикатором громкости и автоматическим сохранением файла в локальное хранилище.
- ⚡ **AI-Расшифровка речи (Gemini 2.5 Flash):** Перевод записанного аудио в структурированный текст с разбивкой по смысловым абзацам.
- 📡 **Офлайн-режим и очередь (Offline First):** Если нет интернета, запись сохраняется локально и автоматически отправляется на расшифровку при появлении сети.
- 📊 **Детальная аналитика речи:** Расчет количества слов, символов, времени чтения, темпа речи диктора (слов в минуту) и автоматическое выделение ключевых понятий лекции.
- 🎧 **Встроенный аудиоплеер:** Прослушивание записей с регулировкой скорости (1.0x, 1.25x, 1.5x, 2.0x) и точным скроллингом по дорожке.
- 🏷️ **Тегирование по предметам:** Привязка лекций к учебным дисциплинам и удобная фильтрация.
- 🔍 **Поиск по тексту и настройка шрифта:** Полнотекстовый поиск внутри конспекта и изменение размера текста для удобного чтения.
- 📄 **Экспорт и шеринг:** Экспорт конспекта в PDF, копирование в буфер обмена и отправка через мессенджеры.
- 📚 **Агрегированные заметки:** Быстрый просмотр всех конспектов, сгруппированных по предметам.
- ⚙️ **Гибкие настройки:** Поддержка пользовательского Gemini API ключа, очистка кэша и темы оформления (Светлая, Темная, Системная).

---

## 🏗️ Архитектура и стек технологий

Приложение создано с соблюдением совремных стандартов Android-разработки (**Clean Architecture + MVVM**):

- **Язык:** Kotlin 1.9+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **State Management:** `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`
- **Локальная база данных:** Room ORM + KSP
- **Сеть и ИИ:** OkHttp3 + Gemini REST API (Multimodal Audio Input)
- **Аудио:** Android `MediaRecorder` & `MediaPlayer` API
- **Экспорт:** Android `PdfDocument` API

---

## 🚀 Быстрый старт и сборка

### Требования
- Android Studio Jellyfish / Ladybug or newer
- JDK 17
- Android SDK 34 (Android 14)
- Android устройство или эмулятор с Android 8.0+ (API level 26)

### Сборка из исходников

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/san5ezez/UniScribe.git
   cd UniScribe
   ```

2. (Опционально) Укажите ваш API ключ Gemini в переменной окружения или `app/build.gradle.kts`:
   ```bash
   export GEMINI_API_KEY="ваш_api_ключ"
   ```
   *Вы также можете ввести свой ключ напрямую в настройках приложения после запуска!*

3. Соберите проект через Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📱 Скриншоты и интерфейс

Посетите наш промо-сайт [https://san5ezez.github.io/UniScribe/](https://san5ezez.github.io/UniScribe/) для просмотра интерактивного демо и интерфейса.

---

## 📄 Лицензия

Проект распространяется под лицензией MIT. Подробнее см. в файле `LICENSE`.
