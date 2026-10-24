import React, { useState, useEffect, useCallback } from 'react';
import { api } from './api';
import { DashboardStats, DlqRecord, EventLog, EventRecord, PublishEventPayload } from './types';
import { StatsCards } from './components/StatsCards';
import { EventSimulator } from './components/EventSimulator';
import { EventHistoryTable } from './components/EventHistoryTable';
import { RetryLogsViewer } from './components/RetryLogsViewer';
import { DlqViewer } from './components/DlqViewer';
import { IdempotencyDemo } from './components/IdempotencyDemo';
import {
  Layers,
  Activity,
  Terminal,
  Skull,
  ShieldCheck,
  RefreshCw,
  Server,
} from 'lucide-react';

export const App: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [events, setEvents] = useState<EventRecord[]>([]);
  const [dlqRecords, setDlqRecords] = useState<DlqRecord[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [selectedLogs, setSelectedLogs] = useState<EventLog[]>([]);

  const [activeTab, setActiveTab] = useState<'simulator' | 'logs' | 'dlq' | 'idempotency'>('simulator');
  const [loading, setLoading] = useState(false);
  const [logsLoading, setLogsLoading] = useState(false);
  const [backendHealthy, setBackendHealthy] = useState<boolean | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  // Load overall system state
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [statsData, eventsData, dlqData, health] = await Promise.all([
        api.getDashboardStats().catch(() => null),
        api.getAllEvents().catch(() => []),
        api.getDlqRecords().catch(() => []),
        api.checkHealth(),
      ]);

      if (statsData) setStats(statsData);
      setEvents(eventsData);
      setDlqRecords(dlqData);
      setBackendHealthy(health);

      // If an event is selected, refresh its logs
      if (selectedEventId) {
        api.getEventLogs(selectedEventId).then(setSelectedLogs).catch(() => {});
      }
    } catch (err) {
      console.error('Error loading lab data:', err);
    } finally {
      setLoading(false);
    }
  }, [selectedEventId]);

  // Initial load
  useEffect(() => {
    loadData();
  }, [loadData]);

  // Auto-refresh interval
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => {
      loadData();
    }, 3000);
    return () => clearInterval(interval);
  }, [autoRefresh, loadData]);

  // Handle selecting an event to inspect its logs
  const handleSelectEvent = async (eventId: string) => {
    setSelectedEventId(eventId);
    setActiveTab('logs');
    try {
      setLogsLoading(true);
      const logs = await api.getEventLogs(eventId);
      setSelectedLogs(logs);
    } catch (err) {
      console.error('Failed to load event logs:', err);
    } finally {
      setLogsLoading(false);
    }
  };

  // Publish event handler
  const handlePublish = async (payload: PublishEventPayload) => {
    await api.publishEvent(payload);
    setSelectedEventId(payload.eventId);
    await loadData();
    // After publishing, fetch initial logs
    api.getEventLogs(payload.eventId).then(setSelectedLogs).catch(() => {});
  };

  // Quick duplicate submit from history table
  const handleDuplicateSubmit = async (record: EventRecord) => {
    await handlePublish({
      eventId: record.eventId,
      eventType: record.eventType,
      payload: record.payload,
      failureMode: 'NONE',
    });
  };

  // Replay dead-letter queue event
  const handleDlqReplay = async (eventId: string) => {
    await api.replayDlqEvent(eventId);
    setSelectedEventId(eventId);
    await loadData();
    api.getEventLogs(eventId).then(setSelectedLogs).catch(() => {});
  };

  return (
    <div>
      {/* Top Navigation & Status Header */}
      <header className="app-header">
        <div className="header-title-wrap">
          <Layers size={24} />
          <div>
            <h1 className="header-title">Rameshwor Chaudhary (24BAI71012)</h1>
            <p className="header-subtitle">
              Kafka Reliability Lab — Event-Driven Architecture: Exponential Backoff Retries, Dead-Letter Queues &amp; Idempotency
            </p>
          </div>
        </div>

        <div className="header-actions">
          {/* Health indicator */}
          <div className="badge" style={{ textTransform: 'none' }}>
            <Server size={13} />
            <span>Backend:</span>
            {backendHealthy === null ? (
              <span style={{ color: 'var(--text-muted)' }}>Checking...</span>
            ) : backendHealthy ? (
              <span style={{ fontWeight: 700 }}>ONLINE</span>
            ) : (
              <span style={{ fontWeight: 700, color: '#dc2626' }}>OFFLINE</span>
            )}
          </div>

          {/* Auto refresh toggle */}
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Auto-refresh (3s)
          </label>

          {/* Manual refresh button */}
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => loadData()}
            disabled={loading}
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
        </div>
      </header>

      <main className="container">
        {/* Metric Counters */}
        <StatsCards stats={stats} loading={loading} />

        {/* Section Tabs */}
        <nav className="tabs-nav">
          <button
            className={`tab-btn ${activeTab === 'simulator' ? 'active' : ''}`}
            onClick={() => setActiveTab('simulator')}
          >
            <Activity size={16} /> Simulator &amp; History
          </button>
          <button
            className={`tab-btn ${activeTab === 'logs' ? 'active' : ''}`}
            onClick={() => setActiveTab('logs')}
          >
            <Terminal size={16} /> Retry Logs
            {selectedEventId && (
              <span className="badge badge-sm mono" style={{ fontSize: '0.7rem' }}>
                {selectedEventId}
              </span>
            )}
          </button>
          <button
            className={`tab-btn ${activeTab === 'dlq' ? 'active' : ''}`}
            onClick={() => setActiveTab('dlq')}
          >
            <Skull size={16} /> Dead Letter Queue
            {dlqRecords.length > 0 && (
              <span className="badge badge-dlq" style={{ fontSize: '0.7rem' }}>
                {dlqRecords.length}
              </span>
            )}
          </button>
          <button
            className={`tab-btn ${activeTab === 'idempotency' ? 'active' : ''}`}
            onClick={() => setActiveTab('idempotency')}
          >
            <ShieldCheck size={16} /> Idempotency Demo
          </button>
        </nav>

        {/* Tab 1: Simulator & History */}
        {activeTab === 'simulator' && (
          <div>
            <EventSimulator onPublish={handlePublish} loading={loading} />
            <EventHistoryTable
              events={events}
              onSelectEvent={handleSelectEvent}
              onDuplicateSubmit={handleDuplicateSubmit}
              selectedEventId={selectedEventId}
            />
          </div>
        )}

        {/* Tab 2: Retry Logs */}
        {activeTab === 'logs' && (
          <RetryLogsViewer
            eventId={selectedEventId}
            logs={selectedLogs}
            loading={logsLoading}
            onRefresh={() => selectedEventId && api.getEventLogs(selectedEventId).then(setSelectedLogs)}
          />
        )}

        {/* Tab 3: Dead Letter Queue */}
        {activeTab === 'dlq' && (
          <DlqViewer
            dlqRecords={dlqRecords}
            loading={loading}
            onRefresh={() => api.getDlqRecords().then(setDlqRecords)}
            onReplay={handleDlqReplay}
          />
        )}

        {/* Tab 4: Idempotency Lab */}
        {activeTab === 'idempotency' && (
          <IdempotencyDemo
            events={events}
            onPublish={handlePublish}
            loading={loading}
          />
        )}

        {/* Footer */}
        <footer style={{ marginTop: '3rem', padding: '1.5rem 0', borderTop: '1px solid var(--border-light)', textAlign: 'center', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
          <p><strong>Rameshwor Chaudhary (24BAI71012)</strong> — Full Stack-II Laboratory Experiment 8 — Kafka Reliability, Exponential Retries, DLQ &amp; Idempotency</p>
          <p style={{ marginTop: '0.25rem' }}>Engineered with Java 17+, Spring Boot 3.x, Spring Kafka, PostgreSQL, React, and TypeScript.</p>
        </footer>
      </main>
    </div>
  );
};

export default App;
