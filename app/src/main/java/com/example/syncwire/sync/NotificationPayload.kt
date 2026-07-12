package com.example.syncwire.sync

import kotlinx.serialization.Serializable

/**
 * Payload sent to POST {serverUrl}/api/notifications.
 * Mirrors the server's CreateNotificationDto:
 *   - id           client-generated UUID; used for server-side dedupe
 *   - deviceId     opaque device identifier (M1: stored locally; M2: server-issued)
 *   - sourceType   free-form tag, e.g. "NOTIFICATION" (server is content-agnostic)
 *   - sender       app package name or phone number
 *   - content      notification body
 *   - packageName  source app package
 *   - timestamp    millis since epoch when the notification fired on this device
 */
@Serializable
data class NotificationPayload(
    val id: String,
    val deviceId: String,
    val sourceType: String,
    val sender: String,
    val content: String,
    val packageName: String,
    val timestamp: Long,
)

@Serializable
data class NotificationRecord(
    val id: String,
    val deviceId: String,
    val sourceType: String,
    val sender: String,
    val content: String,
    val packageName: String,
    val timestamp: Long,
    val receivedAt: String,
)
