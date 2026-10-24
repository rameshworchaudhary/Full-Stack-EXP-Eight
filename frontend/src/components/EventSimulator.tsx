import React, { useState } from 'react';
import { FailureMode, PublishEventPayload } from '../types';
import { Play, RotateCcw, Sparkles } from 'lucide-react';

interface EventSimulatorProps {
  onPublish: (payload: PublishEventPayload) => Promise<void>;
  loading: boolean;
}

const TEMPLATES: Record<string, { type: string; payload: string }> = {
  ORDER: {
    type: 'ORDER_CREATED',
    payload: JSON.stringify({ orderId: 'ORD-5401', customerId: 'CUST-88', total: 249.99, items: 3 }, null, 2),
  },
  PAYMENT: {
    type: 'PAYMENT_PROCESSED',
    payload: JSON.stringify({ txnId: 'TXN-9021', cardLast4: '4242', amount: 89.5, currency: 'USD' }, null, 2),
  },
  INVENTORY: {
    type: 'INVENTORY_RESERVED',
    payload: JSON.stringify({ sku: 'PROD-SKU-77', quantity: 2, warehouse: 'US-EAST-1' }, null, 2),
  },
};

export const EventSimulator: React.FC<EventSimulatorProps> = ({ onPublish, loading }) => {
  const generateId = () => `EVT-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;

  const [eventId, setEventId] = useState(generateId());
  const [eventType, setEventType] = useState(TEMPLATES.ORDER.type);
  const [payload, setPayload] = useState(TEMPLATES.ORDER.payload);
  const [failureMode, setFailureMode] = useState<FailureMode>('NONE');
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const handleApplyTemplate = (key: string) => {
    const tmpl = TEMPLATES[key];
    if (tmpl) {
      setEventType(tmpl.type);
      setPayload(tmpl.payload);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    // Validate JSON
    try {
      JSON.parse(payload);
    } catch {
      setFeedback({ type: 'error', message: 'Payload must be valid JSON.' });
      return;
    }

    try {
      setSubmitting(true);
      await onPublish({
        eventId,
        eventType,
        payload,
        failureMode,
      });
      setFeedback({
        type: 'success',
        message: `Event '${eventId}' successfully submitted to Kafka topic 'events-topic'. Mode: ${failureMode}`,
      });
      // Generate new eventId ready for next test
      setEventId(generateId());
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err.message || 'Failed to submit event to backend.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2 className="card-title">
            <Play size={18} /> Event Simulator
          </h2>
          <p className="card-desc">
            Publish simulated events to Kafka with selectable reliability and failure injection scenarios.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.4rem' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleApplyTemplate('ORDER')}
          >
            Order
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleApplyTemplate('PAYMENT')}
          >
            Payment
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleApplyTemplate('INVENTORY')}
          >
            Inventory
          </button>
        </div>
      </div>

      {feedback && (
        <div className={`alert ${feedback.type === 'success' ? 'alert-success' : 'alert-error'}`}>
          {feedback.message}
        </div>
      )}

      {/* Experiment 3.1.2 Guided Scenario Selectors */}
      <div style={{ display: 'flex', gap: '0.4rem', marginBottom: '1.2rem', flexWrap: 'wrap', padding: '0.5rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '4px' }}>
        <span style={{ fontSize: '0.75rem', fontWeight: 700, alignSelf: 'center', marginRight: '0.25rem', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
          Quick Scenario:
        </span>
        <button
          type="button"
          className={`btn btn-sm ${failureMode === 'NONE' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setFailureMode('NONE')}
        >
          1. Normal Event
        </button>
        <button
          type="button"
          className={`btn btn-sm ${failureMode === 'TRANSIENT' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setFailureMode('TRANSIENT')}
        >
          2. Transient Failure (Retry &amp; Recover)
        </button>
        <button
          type="button"
          className={`btn btn-sm ${failureMode === 'PERMANENT' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setFailureMode('PERMANENT')}
        >
          3. Permanent Failure (DLQ Routing)
        </button>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">Event ID (Unique Correlation Key)</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                type="text"
                className="form-input mono"
                value={eventId}
                onChange={(e) => setEventId(e.target.value)}
                required
                placeholder="e.g. EVT-ABCD12"
              />
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setEventId(generateId())}
                title="Generate new Event ID"
              >
                <Sparkles size={14} /> New
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Event Type</label>
            <input
              type="text"
              className="form-input"
              value={eventType}
              onChange={(e) => setEventType(e.target.value)}
              required
              placeholder="e.g. ORDER_CREATED"
            />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Failure Mode Selector</label>
          <select
            className="form-select"
            value={failureMode}
            onChange={(e) => setFailureMode(e.target.value as FailureMode)}
          >
            <option value="NONE">NONE — Process successfully on Attempt 1 (Happy Path)</option>
            <option value="TRANSIENT">
              TRANSIENT — Fail temporarily on attempts 1 & 2, then succeed on retry #3 (Self-Healing)
            </option>
            <option value="PERMANENT">
              PERMANENT — Fail repeatedly through all 4 attempts until retry exhaustion &rarr; Route to DLQ
            </option>
          </select>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
            {failureMode === 'NONE' && '• Event executes business logic immediately and stores idempotency record.'}
            {failureMode === 'TRANSIENT' &&
              '• Consumer throws TransientProcessingException on attempts 1 & 2. Exponential backoff delays each retry before attempt 3 succeeds.'}
            {failureMode === 'PERMANENT' &&
              '• Consumer fails all retries. DefaultErrorHandler activates DeadLetterPublishingRecoverer to route message to events-dlq.'}
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">JSON Payload</label>
          <textarea
            className="form-textarea"
            rows={5}
            value={payload}
            onChange={(e) => setPayload(e.target.value)}
            required
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setEventId(generateId());
              setFeedback(null);
            }}
          >
            <RotateCcw size={15} /> Reset
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || submitting}
          >
            <Play size={15} /> {submitting ? 'Publishing...' : 'Publish Test Event'}
          </button>
        </div>
      </form>
    </div>
  );
};
