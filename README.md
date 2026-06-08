# syncwire-app

Real-time notification routing ecosystem: Android foreground service + ASP.NET Core SignalR backend + Next.js (MUI) dashboard, orchestrated with Tilt.

---

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                        SyncWire Ecosystem                       │
│                                                                  │
│  ┌────────────────┐   SignalR    ┌──────────────────────────┐  │
│  │  Android App   │◄────────────►│  ASP.NET Core Backend    │  │
│  │  (Foreground   │   WebSocket  │  - NotificationHub       │  │
│  │   Service)     │              │  - REST POST /api/       │  │
│  └────────────────┘              │    notifications         │  │
│                                  └────────────┬─────────────┘  │
│  ┌────────────────┐   SignalR              SignalR              │
│  │ Next.js / MUI  │◄───────────────────────────┘              │
│  │  Dashboard     │   REST API                                  │
│  └────────────────┘                                             │
└────────────────────────────────────────────────────────────────┘
```

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| **Backend** | `backend/SyncWire.API/` | ASP.NET Core 8 Web API with SignalR hub (`/hubs/notifications`) and REST endpoint (`POST /api/notifications`) |
| **Frontend** | `frontend/` | Next.js 14 dashboard with Material UI. Shows live notifications and lets you send test messages |
| **Android** | `android/` | Android foreground service using the SignalR Java client. Receives notifications and displays them as system alerts |
| **Tiltfile** | `Tiltfile` | Tilt configuration for local Kubernetes orchestration |
| **docker-compose** | `docker-compose.yml` | Docker Compose configuration for running backend + frontend without Kubernetes |

---

## Getting Started

### Option A – Docker Compose (quickest)

```bash
docker compose up --build
```

- Dashboard: http://localhost:3000
- API / Swagger: http://localhost:8080/swagger

### Option B – Tilt (Kubernetes)

Requires a local Kubernetes cluster (e.g., `kind`, `minikube`, or Docker Desktop).

```bash
tilt up
```

Tilt will build the Docker images, deploy them to the cluster, and set up port forwards:

- Dashboard → http://localhost:3000
- Backend → http://localhost:8080

Press `Ctrl+C` then `tilt down` to stop.

### Option C – Run services individually

**Backend:**
```bash
cd backend/SyncWire.API
dotnet run
```

**Frontend:**
```bash
cd frontend
npm install
NEXT_PUBLIC_API_URL=http://localhost:8080 npm run dev
```

---

## Android App

The Android project is in `android/`. Open it in Android Studio or build via the command line:

```bash
cd android
./gradlew assembleDebug
```

The `SIGNALR_HUB_URL` build config field in `app/build.gradle` points to `http://10.0.2.2:8080/hubs/notifications` by default (the Android emulator address for `localhost`). Update it in `android/app/build.gradle` or override via `local.properties` for a real device:

```
# android/local.properties
signalr.hub.url=http://192.168.1.100:8080/hubs/notifications
```

**Permissions required:**
- `INTERNET` – SignalR connection
- `FOREGROUND_SERVICE` – persistent background connection
- `POST_NOTIFICATIONS` – Android 13+ system notifications

---

## API Reference

### `POST /api/notifications`

Send a notification to all clients or a specific channel/device.

```json
{
  "title": "Hello World",
  "body": "This is a test notification",
  "channel": "my-device-001",
  "priority": "High"
}
```

`channel` is optional. Omit it to broadcast to all connected clients.

### SignalR Hub: `/hubs/notifications`

| Client → Server | Description |
|-----------------|-------------|
| `JoinChannel(channel)` | Subscribe to a named channel |
| `LeaveChannel(channel)` | Unsubscribe from a channel |
| `Acknowledge(notificationId)` | Confirm receipt |

| Server → Client | Description |
|-----------------|-------------|
| `ReceiveNotification(notification)` | New notification |
| `InitialNotifications(list)` | Sent on connect with recent history |

---

## Project Structure

```
syncwire-app/
├── Tiltfile                          # Tilt orchestration
├── docker-compose.yml                # Docker Compose (backend + frontend)
├── backend/
│   └── SyncWire.API/
│       ├── Controllers/
│       │   └── NotificationsController.cs
│       ├── Hubs/
│       │   └── NotificationHub.cs
│       ├── Models/
│       │   └── Notification.cs
│       ├── Services/
│       │   └── NotificationService.cs
│       ├── Program.cs
│       ├── Dockerfile
│       └── SyncWire.API.csproj
├── frontend/
│   ├── src/
│   │   ├── app/          # Next.js App Router pages
│   │   ├── components/   # MUI components
│   │   ├── hooks/        # useNotifications SignalR hook
│   │   ├── lib/          # SignalR connection factory
│   │   └── types/        # Shared TypeScript types
│   ├── Dockerfile
│   └── package.json
└── android/
    └── app/src/main/java/com/syncwire/app/
        ├── MainActivity.java
        ├── NotificationForegroundService.java
        └── SyncWireNotification.java
```