import React, { useState } from 'react';
import { DlqRecord } from '../types';
import { Skull, ChevronDown, ChevronRight, RefreshCw } from 'lucide-react';

interface DlqViewerProps {
  dlqRecords: DlqRecord[];
  loading: boolean;
  onRefresh: () => void;
  onReplay: (eventId: string) => Promise<void>;
}

export const DlqViewer: React.FC<DlqViewerProps> = ({ dlqRecords, loading, onRefresh, onReplay }) => {
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [replayingId, setReplayingId] = useState<string | null>(null);

  const toggleExpand = (id: number) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  const handleReplayClick = async (eventId: string) => {
    try {
      setReplayingId(eventId);
      await onReplay(eventId);
    } finally {
      setReplayingId(null);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2 className="card-title">
            <Skull size={18} /> Dead Letter Queue (DLQ) Inspector &amp; Replay Lab
          </h2>
          <p className="card-desc">
            Failed records routed to <code className="mono">events-dlq</code> by Spring Kafka's{' '}
            <code className="mono">DeadLetterPublishingRecoverer</code> after retries exhaustion.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onRefresh}
          disabled={loading}
        >
          <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Refresh DLQ
        </button>
      </div>

      <div className="table-responsive">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: '32px' }}></th>
              <th>Event ID</th>
              <th>Original Topic</th>
              <th>Partition / Offset</th>
              <th>Failure Reason</th>
              <th>Attempts</th>
              <th>Received At</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {dlqRecords.length === 0 ? (
              <tr>
                <td colSpan={8} className="empty-state">
                  No records in Dead Letter Queue. Submit a PERMANENT failure event to observe DLQ routing.
                </td>
              </tr>
            ) : (
              dlqRecords.map((dlq) => {
                const isExpanded = expandedId === dlq.id;
                const isReplaying = replayingId === dlq.eventId;
                return (
                  <React.Fragment key={dlq.id}>
                    <tr
                      onClick={() => toggleExpand(dlq.id)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td>
                        {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </td>
                      <td className="mono" style={{ fontWeight: 600 }}>
                        {dlq.eventId}
                      </td>
                      <td className="mono">{dlq.originalTopic || 'events-topic'}</td>
                      <td className="mono">
                        P{dlq.originalPartition ?? 0} : O{dlq.originalOffset ?? '—'}
                      </td>
                      <td style={{ color: 'var(--text-primary)', maxWidth: '260px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {dlq.failureReason}
                      </td>
                      <td>{dlq.attemptsMade}</td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                        {new Date(dlq.receivedAt).toLocaleTimeString()}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          disabled={isReplaying}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleReplayClick(dlq.eventId);
                          }}
                          title="Replay this DLQ event into Kafka events-topic"
                        >
                          <RefreshCw size={12} className={isReplaying ? 'animate-spin' : ''} />{' '}
                          {isReplaying ? 'Replaying...' : 'Replay'}
                        </button>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={7} style={{ backgroundColor: 'var(--bg-secondary)', padding: '1rem' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                            <div>
                              <div className="form-label">Failure Reason Details</div>
                              <div style={{ color: '#b91c1c', fontFamily: 'var(--font-mono)', fontSize: '0.82rem' }}>
                                {dlq.failureReason}
                              </div>
                            </div>
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              disabled={isReplaying}
                              onClick={() => handleReplayClick(dlq.eventId)}
                            >
                              <RefreshCw size={13} className={isReplaying ? 'animate-spin' : ''} /> Replay Dead Letter Event
                            </button>
                          </div>

                          <div className="grid-2">
                            <div>
                              <div className="form-label">Original Kafka Payload</div>
                              <pre className="log-console" style={{ maxHeight: '160px', padding: '0.75rem' }}>
                                {dlq.payload}
                              </pre>
                            </div>

                            <div>
                              <div className="form-label">DLT Exception & Kafka Headers</div>
                              <pre className="log-console" style={{ maxHeight: '160px', padding: '0.75rem', fontSize: '0.72rem' }}>
                                {dlq.headers || 'No headers captured'}
                              </pre>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
