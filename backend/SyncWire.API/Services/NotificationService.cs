using SyncWire.API.Models;

namespace SyncWire.API.Services;

/// <summary>In-memory store for recent notifications (replace with a real DB for production).</summary>
public class NotificationService
{
    private readonly List<Notification> _notifications = new();
    private readonly object _lock = new();
    private const int MaxNotifications = 200;

    public Notification Add(SendNotificationRequest request)
    {
        var notification = new Notification
        {
            Title = request.Title,
            Body = request.Body,
            Channel = request.Channel,
            DeviceId = request.DeviceId,
            Priority = request.Priority,
            Timestamp = DateTime.UtcNow
        };

        lock (_lock)
        {
            _notifications.Insert(0, notification);
            if (_notifications.Count > MaxNotifications)
                _notifications.RemoveRange(MaxNotifications, _notifications.Count - MaxNotifications);
        }

        return notification;
    }

    public IReadOnlyList<Notification> GetRecent(int count = 50)
    {
        lock (_lock)
        {
            return _notifications.Take(count).ToList();
        }
    }
}
