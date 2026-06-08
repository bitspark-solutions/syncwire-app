using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.SignalR;
using SyncWire.API.Hubs;
using SyncWire.API.Models;
using SyncWire.API.Services;

namespace SyncWire.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class NotificationsController : ControllerBase
{
    private readonly NotificationService _notificationService;
    private readonly IHubContext<NotificationHub> _hubContext;
    private readonly ILogger<NotificationsController> _logger;

    public NotificationsController(
        NotificationService notificationService,
        IHubContext<NotificationHub> hubContext,
        ILogger<NotificationsController> logger)
    {
        _notificationService = notificationService;
        _hubContext = hubContext;
        _logger = logger;
    }

    /// <summary>Get recent notifications.</summary>
    [HttpGet]
    public ActionResult<IEnumerable<Notification>> GetAll([FromQuery] int limit = 50)
    {
        return Ok(_notificationService.GetRecent(Math.Clamp(limit, 1, 200)));
    }

    /// <summary>Send a notification to all clients or a specific channel.</summary>
    [HttpPost]
    public async Task<ActionResult<Notification>> Send([FromBody] SendNotificationRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Title))
            return BadRequest("Title is required.");

        var notification = _notificationService.Add(request);
        _logger.LogInformation("Sending notification {Id}: {Title}", notification.Id, notification.Title);

        if (!string.IsNullOrWhiteSpace(request.Channel))
        {
            await _hubContext.Clients.Group(request.Channel)
                .SendAsync("ReceiveNotification", notification);
        }
        else
        {
            await _hubContext.Clients.All
                .SendAsync("ReceiveNotification", notification);
        }

        return CreatedAtAction(nameof(GetAll), notification);
    }
}
