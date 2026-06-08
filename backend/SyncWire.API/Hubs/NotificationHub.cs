using Microsoft.AspNetCore.SignalR;
using SyncWire.API.Models;
using SyncWire.API.Services;

namespace SyncWire.API.Hubs;

public class NotificationHub : Hub
{
    private readonly NotificationService _notificationService;
    private readonly ILogger<NotificationHub> _logger;

    public NotificationHub(NotificationService notificationService, ILogger<NotificationHub> logger)
    {
        _notificationService = notificationService;
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        _logger.LogInformation("Client connected: {ConnectionId}", Context.ConnectionId);
        // Send recent notifications to newly connected clients
        var recent = _notificationService.GetRecent(20);
        await Clients.Caller.SendAsync("InitialNotifications", recent);
        await base.OnConnectedAsync();
    }

    public override Task OnDisconnectedAsync(Exception? exception)
    {
        _logger.LogInformation("Client disconnected: {ConnectionId}", Context.ConnectionId);
        return base.OnDisconnectedAsync(exception);
    }

    /// <summary>Called by Android clients to register themselves to a named channel.</summary>
    public async Task JoinChannel(string channel)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, channel);
        _logger.LogInformation("Connection {ConnectionId} joined channel '{Channel}'", Context.ConnectionId, channel);
    }

    public async Task LeaveChannel(string channel)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, channel);
    }

    /// <summary>Android foreground service calls this to acknowledge receipt.</summary>
    public Task Acknowledge(string notificationId)
    {
        _logger.LogInformation("Notification {NotificationId} acknowledged by {ConnectionId}", notificationId, Context.ConnectionId);
        return Task.CompletedTask;
    }
}
