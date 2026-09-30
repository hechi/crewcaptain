'use client';

import { useCallback, useEffect, useState } from 'react';
import { FeedbackSummary } from '@/types/feedback';
import {
  listFeedbackSummaries,
  generateFeedbackSummary,
  saveFeedbackSummary,
  getFeedbackAnalytics,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import Spinner from '@/components/feedback/Spinner';

interface FeedbackSummaryPanelProps {
  personId: string;
  /** Whether AI features are available (gates the "Summarize feedback" button). */
  aiAvailable?: boolean;
  refreshKey?: number;
}

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 12px',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-medium)',
  fontSize: 'var(--text-body)',
  backgroundColor: 'var(--color-bg-elevated)',
  color: 'var(--color-text-primary)',
  boxSizing: 'border-box',
};

/** Returns the ISO date (yyyy-mm-dd) `months` before today. */
function monthsAgo(months: number): string {
  const d = new Date();
  d.setMonth(d.getMonth() - months);
  return d.toISOString().split('T')[0];
}

function today(): string {
  return new Date().toISOString().split('T')[0];
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return iso;
  }
}

/**
 * AI-assisted feedback summaries for a person. Generates a draft summary over a
 * recent period, lets the manager edit it, saves it, and lists prior summaries.
 */
export default function FeedbackSummaryPanel({ personId, aiAvailable = false, refreshKey = 0 }: FeedbackSummaryPanelProps) {
  const { getToken } = useStableToken();
  const [summaries, setSummaries] = useState<FeedbackSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [draft, setDraft] = useState<string | null>(null);
  const [responseCount, setResponseCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [genError, setGenError] = useState<string | null>(null);

  const periodFrom = monthsAgo(6);
  const periodTo = today();

  const fetchSummaries = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await listFeedbackSummaries(token, personId);
      setSummaries(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load summaries');
    } finally {
      setLoading(false);
    }
  }, [getToken, personId]);

  useEffect(() => {
    fetchSummaries();
  }, [fetchSummaries, refreshKey]);

  const handleGenerate = async () => {
    const token = getToken();
    if (!token) return;
    setGenerating(true);
    setGenError(null);
    try {
      // Best-effort response count from analytics for the same window.
      try {
        const analytics = await getFeedbackAnalytics(token, personId, { from: periodFrom, to: periodTo });
        setResponseCount(analytics.totalResponses);
      } catch {
        // Non-critical; leave responseCount as-is.
      }
      const result = await generateFeedbackSummary(token, personId, { from: periodFrom, to: periodTo });
      if (result.content) {
        setDraft(result.content);
      } else if (result.error) {
        setGenError(result.error);
      }
    } catch (err) {
      setGenError(err instanceof Error ? err.message : 'Failed to generate summary');
    } finally {
      setGenerating(false);
    }
  };

  const handleSave = async () => {
    const token = getToken();
    if (!token || draft == null || !draft.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await saveFeedbackSummary(token, personId, {
        periodFrom,
        periodTo,
        content: draft.trim(),
        responseCount,
      });
      setDraft(null);
      await fetchSummaries();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save summary');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div data-testid="feedback-summary-panel">
      {aiAvailable && (
        <button
          type="button"
          onClick={handleGenerate}
          disabled={generating}
          data-testid="feedback-summary-generate"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: generating ? 'var(--color-primary-muted)' : 'transparent',
            color: 'var(--color-primary)',
            cursor: generating ? 'not-allowed' : 'pointer',
            opacity: generating ? 0.6 : 1,
            marginBottom: 'var(--space-4)',
          }}
        >
          {generating ? (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <Spinner size={14} label="Summarizing feedback" /> Summarizing...
            </span>
          ) : '✦ Summarize feedback'}
        </button>
      )}

      {genError && (
        <div
          data-testid="feedback-summary-gen-error"
          style={{
            marginBottom: 'var(--space-3)',
            padding: '8px 12px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {genError}
        </div>
      )}

      {draft != null && (
        <div
          data-testid="feedback-summary-draft"
          style={{
            marginBottom: 'var(--space-4)',
            padding: 'var(--space-3)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary-muted)',
          }}
        >
          <div style={{ ...labelStyle, marginBottom: '6px' }}>
            Draft summary · {formatDate(periodFrom)} – {formatDate(periodTo)}
          </div>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={6}
            data-testid="feedback-summary-textarea"
            style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }}
          />
          <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !draft.trim()}
              data-testid="feedback-summary-save"
              style={{
                padding: '6px 14px',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-mono)',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-bg-base)',
                cursor: saving || !draft.trim() ? 'not-allowed' : 'pointer',
                fontWeight: 'var(--weight-medium)',
                opacity: saving || !draft.trim() ? 0.6 : 1,
              }}
            >
              {saving ? 'Saving...' : 'Save summary'}
            </button>
            <button
              type="button"
              onClick={() => setDraft(null)}
              data-testid="feedback-summary-discard"
              style={{
                padding: '6px 14px',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'transparent',
                color: 'var(--color-text-secondary)',
                cursor: 'pointer',
              }}
            >
              Discard
            </button>
          </div>
        </div>
      )}

      {error && (
        <div
          data-testid="feedback-summary-error"
          style={{
            marginBottom: 'var(--space-3)',
            padding: '8px 12px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {error}
        </div>
      )}

      {/* Saved summaries */}
      <div style={{ ...labelStyle, marginBottom: '6px' }}>Saved summaries</div>
      {loading ? (
        <div data-testid="feedback-summary-loading" style={{ textAlign: 'center', padding: 'var(--space-4)', color: 'var(--color-text-muted)' }}>
          Loading summaries...
        </div>
      ) : summaries.length === 0 ? (
        <div data-testid="feedback-summary-empty" style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
          No saved summaries yet.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          {summaries.map((s) => (
            <div
              key={s.id}
              data-testid={`feedback-summary-${s.id}`}
              style={{
                padding: 'var(--space-3)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
              }}
            >
              <div style={labelStyle}>
                {formatDate(s.periodFrom)} – {formatDate(s.periodTo)} · {s.responseCount} response{s.responseCount === 1 ? '' : 's'}
              </div>
              <p style={{ margin: '6px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                {s.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
