export type FailureMode = 'NONE' | 'TRANSIENT' | 'PERMANENT';

export type EventStatus =
  | 'SUBMITTED'
  | 'PROCESSING'
  | 'RETRYING'
  | 'SUCCESS'
  | 'FAILED'
  | 'DUPLICATE_IGNORED'
  | 'DLQ';

export interface EventRecord {
  id: number;
  eventId: string;
  eventType: string;
  payload: string;
  failureMode: FailureMode;
  status: EventStatus;
  attemptCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface EventLog {
  id: number;
  eventId: string;
  attemptNumber: number;
  status: string;
  backoffDelayMs: number;
  message: string;
  timestamp: string;
}

export interface DlqRecord {
  id: number;
  eventId: string;
  originalTopic: string;
  originalPartition: number;
  originalOffset: number;
  payload: string;
  failureReason: string;
  headers: string;
  attemptsMade: number;
  receivedAt: string;
}

export interface DashboardStats {
  totalSubmitted: number;
  successfullyProcessed: number;
  failed: number;
  retrying: number;
  duplicateIgnored: number;
  dlqEvents: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PublishEventPayload {
  eventId: string;
  eventType: string;
  payload: string;
  failureMode: FailureMode;
}
