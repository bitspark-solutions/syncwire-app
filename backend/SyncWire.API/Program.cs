using SyncWire.API.Hubs;
using SyncWire.API.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "SyncWire API", Version = "v1" });
});

builder.Services.AddSignalR();
builder.Services.AddSingleton<NotificationService>();

var allowedOrigins = builder.Configuration
    .GetSection("Cors:AllowedOrigins")
    .Get<string[]>() ?? [];

builder.Services.AddCors(options =>
{
    options.AddPolicy("SignalRCors", policy =>
    {
        if (allowedOrigins.Length == 0 || allowedOrigins.Contains("*"))
        {
            // Development convenience – allow any origin.
            // Set Cors:AllowedOrigins in appsettings to restrict in production.
            policy.SetIsOriginAllowed(_ => true);
        }
        else
        {
            policy.WithOrigins(allowedOrigins);
        }
        policy.AllowAnyHeader().AllowAnyMethod().AllowCredentials();
    });
});

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

app.UseCors("SignalRCors");
app.UseRouting();
app.UseAuthorization();

app.MapControllers();
app.MapHub<NotificationHub>("/hubs/notifications");

app.Run();
