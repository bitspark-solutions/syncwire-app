"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { HubConnectionState } from "@microsoft/signalr";
import { startConnection, getConnection } from "@/lib/signalr";
import type { Notification } from "@/types/notification";

export function useNotifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [connectionState, setConnectionState] = useState<HubConnectionState>(
    HubConnectionState.Disconnected
  );
  const mountedRef = useRef(true);

  const updateState = useCallback(() => {
    if (mountedRef.current) {
      setConnectionState(getConnection().state);
    }
  }, []);

  useEffect(() => {
    mountedRef.current = true;

    const init = async () => {
      try {
        const conn = await startConnection();
        updateState();

        conn.on("ReceiveNotification", (notification: Notification) => {
          if (mountedRef.current) {
            setNotifications((prev) => [notification, ...prev].slice(0, 200));
          }
        });

        conn.on("InitialNotifications", (initial: Notification[]) => {
          if (mountedRef.current) {
            setNotifications(initial);
          }
        });

        conn.onreconnecting(() => updateState());
        conn.onreconnected(() => updateState());
        conn.onclose(() => updateState());
      } catch (err) {
        console.error("SignalR connection error:", err);
        updateState();
      }
    };

    init();

    return () => {
      mountedRef.current = false;
    };
  }, [updateState]);

  return { notifications, connectionState };
}
