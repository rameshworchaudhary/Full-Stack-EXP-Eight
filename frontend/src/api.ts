import { ApiResponse, DashboardStats, DlqRecord, EventLog, EventRecord, PublishEventPayload } from './types';

const API_BASE = '/api';

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    let errorDetail = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const errObj = await response.json();
      if (errObj.message) errorDetail = errObj.message;
    } catch {
      // ignore
    }
    throw new Error(errorDetail);
  }

  const json: ApiResponse<T> = await response.json();
  return json.data;
}

export const api = {
  async getDashboardStats(): Promise<DashboardStats> {
    return fetchJson<DashboardStats>(`${API_BASE}/dashboard/stats`);
  },

  async getAllEvents(): Promise<EventRecord[]> {
    return fetchJson<EventRecord[]>(`${API_BASE}/events`);
  },

  async getEventById(eventId: string): Promise<EventRecord> {
    return fetchJson<EventRecord>(`${API_BASE}/events/${encodeURIComponent(eventId)}`);
  },

  async getEventLogs(eventId: string): Promise<EventLog[]> {
    return fetchJson<EventLog[]>(`${API_BASE}/events/${encodeURIComponent(eventId)}/logs`);
  },

  async getDlqRecords(): Promise<DlqRecord[]> {
    return fetchJson<DlqRecord[]>(`${API_BASE}/events/dlq`);
  },

  async publishEvent(data: PublishEventPayload): Promise<EventRecord> {
    return fetchJson<EventRecord>(`${API_BASE}/events`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async replayDlqEvent(eventId: string): Promise<EventRecord> {
    return fetchJson<EventRecord>(`${API_BASE}/events/dlq/${encodeURIComponent(eventId)}/replay`, {
      method: 'POST',
    });
  },

  async checkHealth(): Promise<boolean> {
    try {
      const res = await fetch('/actuator/health');
      return res.ok;
    } catch {
      return false;
    }
  },
};
