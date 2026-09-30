'use client';

import { useState } from 'react';
import FeedbackResponsesPanel from '@/components/feedback/FeedbackResponsesPanel';
import FeedbackLinksPanel from '@/components/feedback/FeedbackLinksPanel';
import FeedbackAnalyticsPanel from '@/components/feedback/FeedbackAnalyticsPanel';
import FeedbackSummaryPanel from '@/components/feedback/FeedbackSummaryPanel';

interface FeedbackTabPanelProps {
  personId: string;
  aiAvailable?: boolean;
  /** Bumping this refreshes the links/analytics sub-panels (e.g. after creating a link). */
  refreshKey?: number;
  /** When provided, the Links sub-panel shows a "Create feedback link" button. */
  onCreateLink?: () => void;
}

type SubTab = 'responses' | 'links' | 'analytics' | 'summary';

const SUBTABS: { key: SubTab; label: string }[] = [
  { key: 'responses', label: 'Responses' },
  { key: 'links', label: 'Links' },
  { key: 'analytics', label: 'Analytics' },
  { key: 'summary', label: 'Summary' },
];

/**
 * Container for the person-detail "Feedback" tab. Presents four sub-sections
 * (responses, links, analytics, summary) as in-panel sub-tabs.
 */
export default function FeedbackTabPanel({ personId, aiAvailable = false, refreshKey = 0, onCreateLink }: FeedbackTabPanelProps) {
  const [subTab, setSubTab] = useState<SubTab>('responses');

  return (
    <div data-testid="feedback-tab-panel">
      {/* Header: a segmented control for the sub-sections on the left (matching the
          app's in-panel filter/scope toggles), the primary "Create feedback link"
          action on the right. */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 'var(--space-3)',
          marginBottom: 'var(--space-4)',
          flexWrap: 'wrap',
        }}
      >
        <div
          role="tablist"
          aria-label="Feedback sections"
          style={{
            display: 'inline-flex',
            borderRadius: 'var(--radius-medium)',
            overflow: 'hidden',
            border: '1px solid var(--color-border)',
          }}
        >
          {SUBTABS.map((t, i) => {
            const selected = subTab === t.key;
            return (
              <button
                key={t.key}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setSubTab(t.key)}
                data-testid={`feedback-subtab-${t.key}`}
                style={{
                  padding: 'var(--space-2) var(--space-4)',
                  border: 'none',
                  borderLeft: i === 0 ? 'none' : '1px solid var(--color-border)',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  fontWeight: selected ? 'var(--weight-medium)' : 'var(--weight-regular)',
                  backgroundColor: selected ? 'var(--color-primary-muted)' : 'transparent',
                  color: selected ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                  cursor: 'pointer',
                  transition: 'background-color 0.15s, color 0.15s',
                }}
              >
                {t.label}
              </button>
            );
          })}
        </div>

        {onCreateLink && (
          <button
            type="button"
            onClick={onCreateLink}
            data-testid="feedback-create-link-button"
            title="Create a shareable feedback link for this person"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 16px',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              background: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              fontFamily: 'var(--font-mono)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-medium)',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              transition: 'opacity 0.2s',
            }}
          >
            + Create feedback link
          </button>
        )}
      </div>

      {subTab === 'responses' && <FeedbackResponsesPanel personId={personId} />}
      {subTab === 'links' && <FeedbackLinksPanel personId={personId} refreshKey={refreshKey} />}
      {subTab === 'analytics' && <FeedbackAnalyticsPanel personId={personId} refreshKey={refreshKey} />}
      {subTab === 'summary' && <FeedbackSummaryPanel personId={personId} aiAvailable={aiAvailable} refreshKey={refreshKey} />}
    </div>
  );
}
