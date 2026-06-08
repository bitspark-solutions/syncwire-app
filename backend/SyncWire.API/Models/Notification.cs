namespace SyncWire.API.Models;

public class Notification
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string? Channel { get; set; }
    public string? DeviceId { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    public NotificationPriority Priority { get; set; } = NotificationPriority.Default;
}

public enum NotificationPriority
{
    Low,
    Default,
    High
}

public class SendNotificationRequest
{
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string? Channel { get; set; }
    public string? DeviceId { get; set; }
    public NotificationPriority Priority { get; set; } = NotificationPriority.Default;
}
