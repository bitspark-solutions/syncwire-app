"use client";

import {
  List,
  ListItem,
  ListItemText,
  Paper,
  Typography,
  Chip,
  Box,
  Divider,
} from "@mui/material";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import type { Notification } from "@/types/notification";

const priorityColor: Record<
  Notification["priority"],
  "default" | "primary" | "error"
> = {
  Low: "default",
  Default: "primary",
  High: "error",
};

interface Props {
  notifications: Notification[];
}

export default function NotificationList({ notifications }: Props) {
  return (
    <Paper elevation={2} sx={{ borderRadius: 2 }}>
      <Box sx={{ p: 2, display: "flex", alignItems: "center", gap: 1 }}>
        <NotificationsNoneIcon color="action" />
        <Typography variant="h6" fontWeight={600}>
          Notifications
        </Typography>
        <Chip label={notifications.length} size="small" sx={{ ml: "auto" }} />
      </Box>
      <Divider />
      {notifications.length === 0 ? (
        <Box sx={{ p: 4, textAlign: "center" }}>
          <Typography color="text.secondary">
            No notifications yet. Send one or connect an Android device.
          </Typography>
        </Box>
      ) : (
        <List dense disablePadding>
          {notifications.map((n, idx) => (
            <Box key={n.id}>
              <ListItem alignItems="flex-start" sx={{ py: 1.5, px: 2 }}>
                <ListItemText
                  primary={
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography fontWeight={600} component="span">
                        {n.title}
                      </Typography>
                      <Chip
                        label={n.priority}
                        size="small"
                        color={priorityColor[n.priority]}
                        variant="outlined"
                      />
                      {n.channel && (
                        <Chip
                          label={`#${n.channel}`}
                          size="small"
                          variant="outlined"
                        />
                      )}
                    </Box>
                  }
                  secondary={
                    <>
                      <Typography
                        component="span"
                        variant="body2"
                        display="block"
                        sx={{ mt: 0.5 }}
                      >
                        {n.body}
                      </Typography>
                      <Typography
                        component="span"
                        variant="caption"
                        color="text.secondary"
                      >
                        {new Date(n.timestamp).toLocaleString()}
                        {n.deviceId && ` · device: ${n.deviceId}`}
                      </Typography>
                    </>
                  }
                />
              </ListItem>
              {idx < notifications.length - 1 && (
                <Divider component="li" variant="inset" />
              )}
            </Box>
          ))}
        </List>
      )}
    </Paper>
  );
}
