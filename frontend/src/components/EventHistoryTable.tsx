import React, { useState } from 'react';
import { EventRecord, EventStatus } from '../types';
import { History, Search, FileText, Repeat } from 'lucide-react';

interface EventHistoryTableProps {
  events: EventRecord[];
  onSelectEvent: (eventId: string) => void;
  onDuplicateSubmit: (record: EventRecord) => void;
  selectedEventId: string | null;
}

export const EventHistoryTable: React.FC<EventHistoryTableProps> = ({
  events,
  onSelectEvent,
  onDuplicateSubmit,
  selectedEventId,
}) => {
  const [filter, setFilter] = useState('');

  const getStatusBadge = (status: EventStatus) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="badge badge-success">SUCCESS</span>;
      case 'SUBMITTED':
        return <span className="badge badge-submitted">SUBMITTED</span>;
      case 'PROCESSING':
        return <span className="badge badge-processing">PROCESSING</span>;
      case 'RETRYING':
        return <span className="badge badge-retrying">RETRYING</span>;
      case 'FAILED':
        return <span className="badge badge-failed">FAILED</span>;
      case 'DUPLICATE_IGNORED':
        return <span className="badge badge-duplicate">DUPLICATE IGNORED</span>;
      case 'DLQ':
        return <span className="badge badge-dlq">DLQ</span>;
      default:
        return <span className="badge">{status}</span>;
    }
  };

  const filteredEvents = events.filter(
    (e) =>
      e.eventId.toLowerCase().includes(filter.toLowerCase()) ||
      e.eventType.toLowerCase().includes(filter.toLowerCase()) ||
      e.status.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2 className="card-title">
            <History size={18} /> Event History
          </h2>
          <p className="card-desc">
            Persisted application records reflecting Kafka ingestion, state transitions, and idempotency status.
          </p>
        </div>
        <div style={{ position: 'relative', width: '220px' }}>
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '1.8rem', fontSize: '0.8rem' }}
            placeholder="Search events..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
          <Search
            size={14}
            style={{ position: 'absolute', left: '0.6rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}
          />
        </div>
      </div>

      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th>Event ID</th>
              <th>Type</th>
              <th>Mode</th>
              <th>Status</th>
              <th>Attempts</th>
              <th>Created At</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredEvents.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-state">
                  No events found. Publish an event from the simulator above.
                </td>
              </tr>
            ) : (
              filteredEvents.map((evt) => {
                const isSelected = evt.eventId === selectedEventId;
                return (
                  <tr
                    key={evt.id}
                    style={{
                      backgroundColor: isSelected ? 'var(--bg-secondary)' : undefined,
                      fontWeight: isSelected ? 600 : undefined,
                    }}
                  >
                    <td className="mono">{evt.eventId}</td>
                    <td>{evt.eventType}</td>
                    <td>
                      <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}>
                        {evt.failureMode}
                      </span>
                    </td>
                    <td>{getStatusBadge(evt.status)}</td>
                    <td className="mono">{evt.attemptCount}</td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                      {new Date(evt.createdAt).toLocaleTimeString()}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => onSelectEvent(evt.eventId)}
                          title="View retry logs and attempt timings"
                        >
                          <FileText size={13} /> Logs
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => onDuplicateSubmit(evt)}
                          title="Submit this exact event ID again to test duplicate prevention"
                        >
                          <Repeat size={13} /> Re-submit
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
