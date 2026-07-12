package com.example.syncwire.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP client for the SyncWire REST API. One method per endpoint we use
 * in M1. Network calls run on Dispatchers.IO.
 */
class SyncwireApi(
    private val baseUrlProvider: suspend () -> String,
    private val deviceIdProvider: suspend () -> String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun postNotification(payload: NotificationPayload): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = baseUrlProvider().trimEnd('/')
                val body = json.encodeToString(NotificationPayload.serializer(), payload)
                    .toRequestBody(jsonMedia)
                val req = Request.Builder()
                    .url("$baseUrl/notifications")
                    .post(body)
                    .header("X-Device-Id", deviceIdProvider())
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error("HTTP ${resp.code}: ${resp.body?.string().orEmpty()}")
                    }
                }
            }
        }

    suspend fun listNotifications(limit: Int = 50): Result<List<NotificationRecord>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = baseUrlProvider().trimEnd('/')
                val req = Request.Builder()
                    .url("$baseUrl/notifications?deviceId=${deviceIdProvider()}&limit=$limit")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error("HTTP ${resp.code}: ${resp.body?.string().orEmpty()}")
                    }
                    val text = resp.body?.string().orEmpty()
                    json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(NotificationRecord.serializer()),
                        text,
                    )
                }
            }
        }
}
