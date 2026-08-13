package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.db.TelemetryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TelemetryInfo(
    val androidVersion: String,
    val deviceModel: String,
    val firstLaunchDate: String,
    val appVersion: String,
    val publicIp: String,
    val ipLocation: String,
    val cpuArch: String,
    val networkType: String,
    val totalLecturesCount: Int,
    val totalStorageMb: Double,
    val proxyMode: String,
    val dohEnabled: Boolean
)

class TelemetryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_telemetry_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getFirstLaunchDate(): String {
        var dateStr = prefs.getString("first_launch_date", null)
        if (dateStr.isNullOrBlank()) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
            val formatted = sdf.format(Date())
            prefs.edit().putString("first_launch_date", formatted).apply()
            return formatted
        }
        return dateStr
    }

    suspend fun fetchPublicIp(): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "UniScribe-App/1.2")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val ip = json.optString("ip", "Скрыт")
                    val country = json.optString("country_name", "")
                    val city = json.optString("city", "")
                    val org = json.optString("org", "")
                    val locStr = listOf(country, city, org).filter { it.isNotBlank() }.joinToString(", ")
                    return@withContext Pair(ip, if (locStr.isBlank()) "Не определено" else locStr)
                }
            }
        } catch (e: Exception) {
            // Fallback to simple ipify
            try {
                val request = Request.Builder()
                    .url("https://api.ipify.org?format=json")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val ip = JSONObject(body).optString("ip", "Неизвестно")
                        return@withContext Pair(ip, "Локация не определена")
                    }
                }
            } catch (_: Exception) {}
        }
        return@withContext Pair("Офлайн / Защищено", "Локальное подключение")
    }

    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Неизвестно"
        val activeNetwork = cm.activeNetwork ?: return "Нет подключения"
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "Нет подключения"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Мобильная сеть (4G/5G)"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Подключено"
        }
    }

    suspend fun collectTelemetry(
        totalLectures: Int,
        storageMb: Double,
        customProxyUrl: String,
        useDoh: Boolean
    ): TelemetryInfo {
        val (ip, location) = fetchPublicIp()
        val firstLaunch = getFirstLaunchDate()
        val androidVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val model = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${Build.MODEL}"
        val cpu = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val proxyStatus = when {
            customProxyUrl.isNotBlank() -> "Свой прокси ($customProxyUrl)"
            useDoh -> "Cloudflare 1.1.1.1 DoH"
            else -> "Прямое подключение"
        }

        val telemetryInfo = TelemetryInfo(
            androidVersion = androidVer,
            deviceModel = model,
            firstLaunchDate = firstLaunch,
            appVersion = "1.2.0 (Build 2026)",
            publicIp = ip,
            ipLocation = location,
            cpuArch = cpu,
            networkType = getNetworkType(),
            totalLecturesCount = totalLectures,
            totalStorageMb = storageMb,
            proxyMode = proxyStatus,
            dohEnabled = useDoh
        )

        // Save to Room Database
        withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).telemetryDao()
                dao.insertTelemetry(
                    TelemetryEntity(
                        androidVersion = androidVer,
                        deviceModel = model,
                        firstLaunchDate = firstLaunch,
                        appVersion = "1.2.0",
                        publicIp = ip,
                        totalLecturesCount = totalLectures,
                        proxyMode = proxyStatus
                    )
                )
            } catch (_: Exception) {}
        }

        return telemetryInfo
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://script.google.com/macros/s/AKfycbz3hFcyzhVydGo0qC2VXue4cdqF_005v1MKc4NPW4NCdRvCuZA4mmDwFkXJ4SpNA19q/exec"
    }

    suspend fun sendToGoogleSheets(
        info: TelemetryInfo,
        webhookUrl: String = DEFAULT_ENDPOINT
    ): Boolean = withContext(Dispatchers.IO) {
        val targetUrl = webhookUrl.ifBlank { DEFAULT_ENDPOINT }
        try {
            val json = JSONObject().apply {
                put("androidVersion", info.androidVersion)
                put("deviceModel", info.deviceModel)
                put("publicIp", info.publicIp)
                put("ipLocation", info.ipLocation)
                put("networkType", info.networkType)
                put("totalLecturesCount", info.totalLecturesCount)
                put("totalStorageMb", "%.1f MB".format(info.totalStorageMb))
                put("proxyMode", info.proxyMode)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(targetUrl)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful || response.code == 302 || response.code == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
