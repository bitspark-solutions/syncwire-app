"use client";

import { useState } from "react";
import {
  Paper,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Box,
  Alert,
  Divider,
} from "@mui/material";
import SendIcon from "@mui/icons-material/Send";
import type { SendNotificationRequest } from "@/types/notification";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface Props {
  activeChannel: string;
  onChannelChange: (channel: string) => void;
}

const defaultForm: SendNotificationRequest = {
  title: "",
  body: "",
  channel: "",
  deviceId: "",
  priority: "Default",
};

export default function SendNotificationForm({
  activeChannel,
  onChannelChange,
}: Props) {
  const [form, setForm] = useState<SendNotificationRequest>({
    ...defaultForm,
    channel: activeChannel,
  });
  const [status, setStatus] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);
  const [loading, setLoading] = useState(false);

  const handleChange =
    (field: keyof SendNotificationRequest) =>
    (e: React.ChangeEvent<HTMLInputElement | { value: unknown }>) => {
      const value = (e.target as HTMLInputElement).value;
      setForm((prev) => ({ ...prev, [field]: value }));
      if (field === "channel") onChannelChange(value as string);
    };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setStatus(null);
    try {
      const payload: SendNotificationRequest = {
        ...form,
        channel: form.channel || undefined,
        deviceId: form.deviceId || undefined,
      };
      const res = await fetch(`${API_URL}/api/notifications`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || res.statusText);
      }
      setStatus({ type: "success", message: "Notification sent!" });
      setForm((prev) => ({ ...prev, title: "", body: "" }));
    } catch (err: unknown) {
      setStatus({
        type: "error",
        message: err instanceof Error ? err.message : "Unknown error",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}>
      <Typography variant="h6" fontWeight={600} gutterBottom>
        Send Notification
      </Typography>
      <Divider sx={{ mb: 2 }} />
      <Box component="form" onSubmit={handleSubmit} sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <TextField
          label="Title"
          required
          value={form.title}
          onChange={handleChange("title")}
          size="small"
          fullWidth
        />
        <TextField
          label="Body"
          value={form.body}
          onChange={handleChange("body")}
          size="small"
          fullWidth
          multiline
          rows={3}
        />
        <TextField
          label="Channel (optional)"
          value={form.channel}
          onChange={handleChange("channel")}
          size="small"
          fullWidth
          helperText="Leave blank to broadcast to all clients"
        />
        <TextField
          label="Device ID (optional)"
          value={form.deviceId}
          onChange={handleChange("deviceId")}
          size="small"
          fullWidth
        />
        <FormControl size="small" fullWidth>
          <InputLabel>Priority</InputLabel>
          <Select
            value={form.priority}
            label="Priority"
            onChange={(e) =>
              setForm((prev) => ({
                ...prev,
                priority: e.target.value as SendNotificationRequest["priority"],
              }))
            }
          >
            <MenuItem value="Low">Low</MenuItem>
            <MenuItem value="Default">Default</MenuItem>
            <MenuItem value="High">High</MenuItem>
          </Select>
        </FormControl>

        {status && (
          <Alert severity={status.type} onClose={() => setStatus(null)}>
            {status.message}
          </Alert>
        )}

        <Button
          type="submit"
          variant="contained"
          endIcon={<SendIcon />}
          disabled={loading}
          fullWidth
        >
          {loading ? "Sending…" : "Send"}
        </Button>
      </Box>
    </Paper>
  );
}
