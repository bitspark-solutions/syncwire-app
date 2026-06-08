# SyncWire – Tiltfile
# Orchestrates the ASP.NET Core SignalR backend and Next.js frontend.
#
# Prerequisites: Docker, Tilt (https://tilt.dev)
# Usage: tilt up

# ── Backend ──────────────────────────────────────────────────────────────────
docker_build(
    'syncwire-backend',
    context='./backend/SyncWire.API',
    dockerfile='./backend/SyncWire.API/Dockerfile',
    live_update=[
        # Sync source changes into the container and restart
        sync('./backend/SyncWire.API', '/src'),
        run('cd /src && dotnet publish -c Release -o /app/publish', trigger=['./backend/SyncWire.API']),
    ],
)

k8s_yaml(blob("""
apiVersion: v1
kind: Service
metadata:
  name: syncwire-backend
spec:
  selector:
    app: syncwire-backend
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: syncwire-backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: syncwire-backend
  template:
    metadata:
      labels:
        app: syncwire-backend
    spec:
      containers:
        - name: syncwire-backend
          image: syncwire-backend
          ports:
            - containerPort: 8080
          env:
            - name: ASPNETCORE_URLS
              value: "http://0.0.0.0:8080"
"""))

k8s_resource('syncwire-backend', port_forwards='8080:8080')

# ── Frontend ─────────────────────────────────────────────────────────────────
docker_build(
    'syncwire-frontend',
    context='./frontend',
    dockerfile='./frontend/Dockerfile',
    # NEXT_PUBLIC_* vars are baked in at build time; set the browser-reachable URL.
    build_args={'NEXT_PUBLIC_API_URL': 'http://localhost:8080'},
    live_update=[
        sync('./frontend/src', '/app/src'),
    ],
)

k8s_yaml(blob("""
apiVersion: v1
kind: Service
metadata:
  name: syncwire-frontend
spec:
  selector:
    app: syncwire-frontend
  ports:
    - protocol: TCP
      port: 3000
      targetPort: 3000
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: syncwire-frontend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: syncwire-frontend
  template:
    metadata:
      labels:
        app: syncwire-frontend
    spec:
      containers:
        - name: syncwire-frontend
          image: syncwire-frontend
          ports:
            - containerPort: 3000
          env:
            - name: PORT
              value: "3000"
"""))

k8s_resource('syncwire-frontend', port_forwards='3000:3000')
