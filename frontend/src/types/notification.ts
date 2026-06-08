export interface Notification {
  id: string;
  title: string;
  body: string;
  channel?: string;
  deviceId?: string;
  timestamp: string;
  priority: "Low" | "Default" | "High";
}

export interface SendNotificationRequest {
  title: string;
  body: string;
  channel?: string;
  deviceId?: string;
  priority: "Low" | "Default" | "High";
}
