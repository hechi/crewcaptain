'use client';

import { useCallback, useEffect, useState } from 'react';
import { FeedbackAnalytics, RatingPeriod } from '@/types/feedback';
import { getFeedbackAnalytics } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface FeedbackAnalyticsPanelProps {
  personId: string;
  refreshKey?: number;
}

type Bucket = 'MONTH' | 'QUARTER';

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const CHART_WIDTH = 480;
const CHART_HEIGHT = 160;
const PAD_LEFT = 32;
const PAD_BOTTOM = 28;
const PAD_TOP = 12;
const PAD_RIGHT = 12;
const MAX_RATING = 5;

/** Renders a simple line chart of average rating over time as inline SVG. */
function RatingChart({ periods }: { periods: RatingPeriod[] }) {
  const innerW = CHART_WIDTH - PAD_LEFT - PAD_RIGHT;
  const innerH = CHART_HEIGHT - PAD_TOP - PAD_BOTTOM;

  const points = periods.map((p, i) => {
    const x = periods.length === 1 ? PAD_LEFT + innerW / 2 : PAD_LEFT + (innerW * i) / (periods.length - 1);
    const y = PAD_TOP + innerH * (1 - p.averageRating / MAX_RATING);
    return { x, y, period: p };
  });

  const linePath = points.map((pt, i) => `${i === 0 ? 'M' : 'L'} ${pt.x.toFixed(1)} ${pt.y.toFixed(1)}`).join(' ');

  return (
    <svg
      data-testid="feedback-analytics-chart"
      viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
      role="img"
      aria-label="Average rating over time"
      style={{ width: '100%', maxWidth: `${CHART_WIDTH}px`, height: 'auto' }}
    >
      {/* Y axis gridlines at 0..5 */}
      {[0, 1, 2, 3, 4, 5].map((v) => {
        const y = PAD_TOP + innerH * (1 - v / MAX_RATING);
        return (
          <g key={v}>
            <line
              x1={PAD_LEFT}
              y1={y}
              x2={CHART_WIDTH - PAD_RIGHT}
              y2={y}
              stroke="var(--color-border)"
              strokeWidth={1}
            />
            <text x={PAD_LEFT - 6} y={y + 3} textAnchor="end" fontSize="9" fill="var(--color-text-muted)">
              {v}
            </text>
          </g>
        );
      })}

      {/* Line connecting periods */}
      {points.length > 1 && (
        <path d={linePath} fill="none" stroke="var(--color-primary)" strokeWidth={2} />
      )}

      {/* Points + x labels */}
      {points.map((pt, i) => (
        <g key={i} data-testid={`feedback-analytics-point-${i}`}>
          <circle cx={pt.x} cy={pt.y} r={3.5} fill="var(--color-primary)" />
          <text
            x={pt.x}
            y={CHART_HEIGHT - PAD_BOTTOM + 16}
            textAnchor="middle"
            fontSize="9"
            fill="var(--color-text-muted)"
          >
            {pt.period.label}
          </text>
        </g>
      ))}
    </svg>
  );
}

/**
 * Feedback analytics for a person: overall average rating, total responses,
 * top themes as chips, and an inline-SVG average-rating-over-time chart.
 */
export default function FeedbackAnalyticsPanel({ personId, refreshKey = 0 }: FeedbackAnalyticsPanelProps) {
  const { getToken } = useStableToken();
  const [analytics, setAnalytics] = useState<FeedbackAnalytics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [bucket, setBucket] = useState<Bucket>('MONTH');

  const fetchAnalytics = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await getFeedbackAnalytics(token, personId, { bucket });
      setAnalytics(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  }, [getToken, personId, bucket]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics, refreshKey]);

  const empty = !loading && analytics != null && analytics.totalResponses === 0;

  return (
    <div data-testid="feedback-analytics-panel">
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
        <span style={labelStyle}>Bucket</span>
        <div style={{ display: 'inline-flex', gap: '4px' }}>
          {(['MONTH', 'QUARTER'] as Bucket[]).map((b) => (
            <button
              key={b}
              type="button"
              onClick={() => setBucket(b)}
              data-testid={`feedback-analytics-bucket-${b.toLowerCase()}`}
              aria-pressed={bucket === b}
              style={{
                padding: '4px 10px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: `1px solid ${bucket === b ? 'var(--color-primary)' : 'var(--color-border)'}`,
                borderRadius: 'var(--radius-medium)',
                backgroundColor: bucket === b ? 'var(--color-primary)' : 'transparent',
                color: bucket === b ? 'var(--color-bg-base)' : 'var(--color-text-secondary)',
                cursor: 'pointer',
              }}
            >
              {b === 'MONTH' ? 'Monthly' : 'Quarterly'}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div
          data-testid="feedback-analytics-error"
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

      {loading ? (
        <div data-testid="feedback-analytics-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading analytics...
        </div>
      ) : empty ? (
        <div data-testid="feedback-analytics-empty" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          No feedback responses yet — analytics will appear once responses come in.
        </div>
      ) : analytics ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          {/* Summary stats */}
          <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap' }}>
            <div
              data-testid="feedback-analytics-average"
              style={{
                padding: 'var(--space-3)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                minWidth: '120px',
              }}
            >
              <div style={labelStyle}>Avg rating</div>
              <div style={{ fontSize: 'var(--text-h2)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>
                {analytics.overallAverageRating != null ? `${analytics.overallAverageRating.toFixed(1)}/5` : '—'}
              </div>
            </div>
            <div
              data-testid="feedback-analytics-total"
              style={{
                padding: 'var(--space-3)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                minWidth: '120px',
              }}
            >
              <div style={labelStyle}>Responses</div>
              <div style={{ fontSize: 'var(--text-h2)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>
                {analytics.totalResponses}
              </div>
            </div>
          </div>

          {/* Top themes */}
          {analytics.topThemes.length > 0 && (
            <div data-testid="feedback-analytics-themes">
              <div style={{ ...labelStyle, marginBottom: '6px' }}>Top themes</div>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {analytics.topThemes.map((t) => (
                  <span
                    key={t.theme}
                    data-testid={`feedback-analytics-theme-${t.theme}`}
                    style={{
                      padding: '3px 10px',
                      borderRadius: 'var(--radius-full)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-secondary)',
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    {t.theme} · {t.count}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Chart */}
          {analytics.periods.length > 0 && (
            <div>
              <div style={{ ...labelStyle, marginBottom: '6px' }}>Average rating over time</div>
              <RatingChart periods={analytics.periods} />
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}
