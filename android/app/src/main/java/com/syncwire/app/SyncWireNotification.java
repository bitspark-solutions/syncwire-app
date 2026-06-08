package com.syncwire.app;

/**
 * Plain-old Java object that mirrors the Notification model from the backend.
 * Used for SignalR deserialization in the foreground service.
 */
public class SyncWireNotification {
    public String id;
    public String title;
    public String body;
    public String channel;
    public String deviceId;
    public String timestamp;
    public String priority; // "Low" | "Default" | "High"

    @Override
    public String toString() {
        return "SyncWireNotification{id='" + id + "', title='" + title + "', priority='" + priority + "'}";
    }
}
