import React from 'react';
import { EventLog } from '../types';
import { Terminal, Clock, RefreshCw } from 'lucide-react';

interface RetryLogsViewerProps {
  eventId: string | null;
  logs: EventLog[];
  loading: boolean;
  onRefresh: () => void;
}

export const RetryLogsViewer: React.FC<RetryLogsViewerProps> = ({
  eventId,
  logs,
  loading,
  onRefresh,
}) => {
  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2 className="card-title">
            <Terminal size={18} /> Retry Logs & Processing Timeline
          </h2>
          <p className="card-desc">
            {eventId
              ? `Chronological attempt log and exponential backoff telemetry for event: ${eventId}`
              : 'Select an event from the Event History table to inspect its attempt progression.'}
          </p>
        </div>
        {eventId && (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onRefresh}
            disabled={loading}
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Refresh Logs
          </button>
        )}
      </div>

      {!eventId ? (
        <div className="empty-state">
          <Terminal size={32} style={{ margin: '0 auto', opacity: 0.3 }} />
          <p>No event selected. Click "Logs" on any row in the Event History table above.</p>
        </div>
      ) : logs.length === 0 ? (
        <div className="empty-state">
          <Clock size={32} style={{ margin: '0 auto', opacity: 0.3 }} />
          <p>{loading ? 'Loading processing logs...' : `No logs recorded yet for event ${eventId}.`}</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Attempt #</th>
                <th>Backoff Delay</th>
                <th>Status</th>
                <th>Timestamp</th>
                <th>Diagnostic Message</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td className="mono" style={{ fontWeight: 600 }}>
                    #{log.attemptNumber}
                  </td>
                  <td className="mono">
                    {log.backoffDelayMs > 0 ? `${log.backoffDelayMs} ms` : '0 ms (Initial)'}
                  </td>
                  <td>
                    <span className="badge">{log.status}</span>
                  </td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                    {new Date(log.timestamp).toLocaleTimeString()}
                  </td>
                  <td style={{ fontSize: '0.82rem', fontFamily: 'var(--font-mono)' }}>
                    {log.message}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
