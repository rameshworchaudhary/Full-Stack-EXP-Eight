import React, { useState } from 'react';
import { EventRecord, PublishEventPayload } from '../types';
import { ShieldCheck, CheckCircle2 } from 'lucide-react';

interface IdempotencyDemoProps {
  events: EventRecord[];
  onPublish: (payload: PublishEventPayload) => Promise<void>;
  loading: boolean;
}

export const IdempotencyDemo: React.FC<IdempotencyDemoProps> = ({
  events,
  onPublish,
  loading,
}) => {
  const successfulEvents = events.filter((e) => e.status === 'SUCCESS' || e.status === 'DUPLICATE_IGNORED');
  const [selectedEventId, setSelectedEventId] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  const handleTestDuplicate = async () => {
    if (!selectedEventId) return;
    const target = events.find((e) => e.eventId === selectedEventId);
    if (!target) return;

    try {
      setSubmitting(true);
      setResult(null);
      await onPublish({
        eventId: target.eventId,
        eventType: target.eventType,
        payload: target.payload,
        failureMode: 'NONE', // Try to process
      });
      setResult(
        `Duplicate submission initiated for '${target.eventId}'. Check the Event History and Logs to observe the DUPLICATE_SKIPPED action!`
      );
    } catch (err: any) {
      setResult(`Error submitting duplicate: ${err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2 className="card-title">
            <ShieldCheck size={18} /> Idempotency & Duplicate Prevention Lab
          </h2>
          <p className="card-desc">
            Verify transactional deduplication using PostgreSQL unique constraints under Kafka at-least-once delivery.
          </p>
        </div>
      </div>

      <div className="grid-2" style={{ marginBottom: '1.25rem' }}>
        <div style={{ padding: '1rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '6px' }}>
          <h3 style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.5rem', textTransform: 'uppercase' }}>
            1. Select an Existing Processed Event
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
            Choose an event that has already been successfully committed to the database.
          </p>

          <select
            className="form-select mono"
            value={selectedEventId}
            onChange={(e) => setSelectedEventId(e.target.value)}
          >
            <option value="">-- Choose an Event ID --</option>
            {successfulEvents.map((e) => (
              <option key={e.eventId} value={e.eventId}>
                {e.eventId} ({e.eventType} - {e.status})
              </option>
            ))}
          </select>

          <button
            type="button"
            className="btn btn-primary"
            style={{ marginTop: '0.75rem', width: '100%' }}
            onClick={handleTestDuplicate}
            disabled={!selectedEventId || submitting || loading}
          >
            {submitting ? 'Submitting Duplicate...' : 'Trigger Duplicate Submission'}
          </button>
        </div>

        <div style={{ padding: '1rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '6px' }}>
          <h3 style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.5rem', textTransform: 'uppercase' }}>
            2. Idempotency Architecture Principle
          </h3>
          <ul style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', paddingLeft: '1.2rem', lineHeight: '1.6' }}>
            <li>
              <strong>Uniqueness Constraint:</strong> Table <code className="mono">processed_events</code> enforces a unique constraint on <code className="mono">event_id</code>.
            </li>
            <li>
              <strong>Atomic Transaction:</strong> The core business mutation and the idempotency record insertion execute in a single <code className="mono">@Transactional</code> boundary.
            </li>
            <li>
              <strong>Zero Duplicate Side Effects:</strong> When a duplicate delivery occurs, the existence check detects the record and exits cleanly without re-executing business operations.
            </li>
          </ul>
        </div>
      </div>

      {result && (
        <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <CheckCircle2 size={16} />
          <span>{result}</span>
        </div>
      )}
    </div>
  );
};
