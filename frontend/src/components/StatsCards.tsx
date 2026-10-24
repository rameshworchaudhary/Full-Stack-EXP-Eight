import React from 'react';
import { DashboardStats } from '../types';
import { CheckCircle, AlertTriangle, RefreshCw, Copy, Skull, Send } from 'lucide-react';

interface StatsCardsProps {
  stats: DashboardStats | null;
  loading: boolean;
}

export const StatsCards: React.FC<StatsCardsProps> = ({ stats, loading }) => {
  const items = [
    {
      label: 'Total Submitted',
      value: stats?.totalSubmitted ?? 0,
      desc: 'All events sent to Kafka',
      icon: <Send size={18} />,
    },
    {
      label: 'Successfully Processed',
      value: stats?.successfullyProcessed ?? 0,
      desc: 'Committed to DB with idempotency',
      icon: <CheckCircle size={18} />,
    },
    {
      label: 'Retrying',
      value: stats?.retrying ?? 0,
      desc: 'In exponential backoff retry loop',
      icon: <RefreshCw size={18} />,
    },
    {
      label: 'Failed',
      value: stats?.failed ?? 0,
      desc: 'Encountered error during processing',
      icon: <AlertTriangle size={18} />,
    },
    {
      label: 'Duplicate Ignored',
      value: stats?.duplicateIgnored ?? 0,
      desc: 'Idempotency prevented re-execution',
      icon: <Copy size={18} />,
    },
    {
      label: 'DLQ Events',
      value: stats?.dlqEvents ?? 0,
      desc: 'Exhausted retries -> events-dlq',
      icon: <Skull size={18} />,
    },
  ];

  return (
    <div className="stats-grid">
      {items.map((item) => (
        <div key={item.label} className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">{item.label}</span>
            <span style={{ color: 'var(--text-muted)' }}>{item.icon}</span>
          </div>
          <div className="stat-value">{loading && !stats ? '—' : item.value}</div>
          <div className="stat-desc">{item.desc}</div>
        </div>
      ))}
    </div>
  );
};
