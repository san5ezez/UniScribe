package com.example.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiTranscriptionService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiTranscription"
        private val MODELS_TO_TRY = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
    }

    suspend fun transcribeAudioFile(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", null)?.trim()
        val apiKey = when {
            !savedKey.isNullOrBlank() -> savedKey
            !BuildConfig.GEMINI_API_KEY.isNullOrBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("AQ.")) {
            return@withContext Result.failure(
                IllegalStateException("Не указан Gemini API ключ! Добавьте ваш API ключ (начинается с AIzaSy...) в Настройках приложения ⚙️.")
            )
        }

        val customProxy = prefs.getString("gemini_proxy_url", null)?.trim()?.removeSuffix("/")
        val useCloudflareDoh = prefs.getBoolean("use_cloudflare_doh", true)

        val baseUrls = when {
            !customProxy.isNullOrBlank() -> listOf(customProxy)
            useCloudflareDoh -> listOf(
                "https://generativelanguage.googleapis.com",
                "https://proxy.vortim.ru"
            )
            else -> listOf("https://generativelanguage.googleapis.com")
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Аудиофайл не существует или имеет нулевой размер.")
            )
        }

        var lastException: Exception? = null

        for (baseUrl in baseUrls) {
            for (modelName in MODELS_TO_TRY) {
                try {
                    Log.d(TAG, "Attempting transcription using $baseUrl with model $modelName for file: ${audioFile.absolutePath}")
                    val bytes = audioFile.readBytes()
                    val base64Audio = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    val mimeType = when {
                        audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                        audioFile.name.endsWith(".mp3", ignoreCase = true) -> "audio/mp3"
                        audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                        audioFile.name.endsWith(".aac", ignoreCase = true) -> "audio/aac"
                        audioFile.name.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                        else -> "audio/mp4"
                    }

                    val systemInstruction = """
                        Ты — профессиональный стенографист лекций высокой точности.
                        Твоя задача — перевести аудиозапись речи преподавателя в текст.
                        
                        ТРЕБОВАНИЯ К РАСШИФРОВКЕ:
                        1. МАКСИМАЛЬНО ДОСЛОВНО (слово в слово, verbatim), без сокращений, без адаптации, без пересказа и без исправления устной речи преподавателя.
                        2. Разбивай текст на четкие логические абзацы по смысловым паузам и переходам мысли преподавателя.
                        3. ПОДДЕРЖКА КОД-СВИТЧИНГА (ДВУЯЗЫЧИЯ): Речь может содержать одновременно русский и английский языки (термины, названия, фразы на английском). Распознавай оба языка корректно без потерь.
                        4. Не добавляй никаких собственных комментариев, вступлений или заключений типа "Вот ваша расшифровка:". Выводи ТОЛЬКО полученный текст лекции.
                    """.trimIndent()

                    val jsonBody = JSONObject().apply {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", systemInstruction)
                                })
                            })
                        })
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", mimeType)
                                            put("data", base64Audio)
                                        })
                                    })
                                    put(JSONObject().apply {
                                        put("text", "Расшифруй эту аудиозапись лекции дословно слово в слово.")
                                    })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.1)
                        })
                    }

                    val url = "$baseUrl/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Gemini API Error for $modelName: Code ${response.code}, body: $responseBody")
                        val jsonErr = try { JSONObject(responseBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                        val errMsg = jsonErr ?: "Код ответа ${response.code}"

                        if (response.code == 403 || errMsg.contains("location", ignoreCase = true) || errMsg.contains("country", ignoreCase = true)) {
                            lastException = Exception("Запрос заблокирован по геолокации (403). Настройте прокси-сервер в Настройках приложения для работы в РФ.")
                        } else {
                            lastException = Exception("Ошибка сервиса Gemini API ($modelName: $errMsg).")
                        }
                        continue
                    }

                    val responseJson = JSONObject(responseBody)
                    if (responseJson.has("error")) {
                        val errorObj = responseJson.optJSONObject("error")
                        val errorMsg = errorObj?.optString("message", "Unknown error") ?: "Unknown API error"
                        lastException = Exception("Gemini API Error ($modelName): $errorMsg")
                        continue
                    }

                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        val promptFeedback = responseJson.optJSONObject("promptFeedback")
                        val blockReason = promptFeedback?.optString("blockReason")
                        val msg = if (!blockReason.isNullOrEmpty()) "Запрос заблокирован ($modelName): $blockReason" else "Пустой ответ от модели $modelName."
                        lastException = Exception(msg)
                        continue
                    }

                    val firstCandidate = candidates.getJSONObject(0)
                    val finishReason = firstCandidate.optString("finishReason", "")
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")

                    if (parts != null && parts.length() > 0) {
                        val resultText = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                resultText.append(part.getString("text"))
                            }
                        }

                        val transcribedText = resultText.toString().trim()
                        if (transcribedText.isNotEmpty()) {
                            Log.d(TAG, "Transcription successful using $modelName! Length: ${transcribedText.length} characters")
                            return@withContext Result.success(transcribedText)
                        }
                    }

                    val reasonMsg = if (finishReason.isNotEmpty()) " (Причина завершения: $finishReason)" else ""
                    lastException = Exception("Не удалось извлечь текст расшифровки из ответа $modelName$reasonMsg.")

                } catch (e: Exception) {
                    Log.e(TAG, "Transcription failed with exception on model $modelName via $baseUrl", e)
                    lastException = e
                }
            }
        }

        Result.failure(lastException ?: Exception("Не удалось выполнить расшифровку речи."))
    }
}

