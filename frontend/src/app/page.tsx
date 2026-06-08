"use client";

import { useState } from "react";
import {
  Box,
  Container,
  Typography,
  AppBar,
  Toolbar,
  Chip,
} from "@mui/material";
import WifiIcon from "@mui/icons-material/Wifi";
import WifiOffIcon from "@mui/icons-material/WifiOff";
import { HubConnectionState } from "@microsoft/signalr";
import { useNotifications } from "@/hooks/useNotifications";
import NotificationList from "@/components/NotificationList";
import SendNotificationForm from "@/components/SendNotificationForm";

export default function HomePage() {
  const { notifications, connectionState } = useNotifications();
  const [activeChannel, setActiveChannel] = useState<string>("");

  const isConnected = connectionState === HubConnectionState.Connected;

  const filtered = activeChannel
    ? notifications.filter(
        (n) => !n.channel || n.channel === activeChannel
      )
    : notifications;

  return (
    <>
      <AppBar position="static" color="primary" elevation={2}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            SyncWire Dashboard
          </Typography>
          <Chip
            icon={isConnected ? <WifiIcon /> : <WifiOffIcon />}
            label={isConnected ? "Connected" : connectionState}
            color={isConnected ? "success" : "default"}
            variant="outlined"
            sx={{ color: "white", borderColor: "white" }}
          />
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ mt: 4, mb: 6 }}>
        <Box sx={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
          <Box sx={{ flex: "1 1 340px", minWidth: 300 }}>
            <SendNotificationForm
              activeChannel={activeChannel}
              onChannelChange={setActiveChannel}
            />
          </Box>
          <Box sx={{ flex: "2 1 500px" }}>
            <NotificationList notifications={filtered} />
          </Box>
        </Box>
      </Container>
    </>
  );
}
