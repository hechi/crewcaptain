'use client';

import { useState } from 'react';
import { TrendRadarInsight, AiTrendRadarResponse } from '@/types/settings';
import { generateTrendRadar } from '@/lib/api-client';
import { Radar, Heart, Scale, Award, MessageCircle } from 'lucide-react';

interface TrendRadarInsightsProps {
  token: string;
  personId: string;
  personName: string;
}

export default function TrendRadarInsights({ token, personId, personName }: TrendRadarInsightsProps) {
  const [loading, setLoading] = useState(false);
  const [insights, setInsights] = useState<TrendRadarInsight[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [insufficientData, setInsufficientData] = useState(false);
  const [meetingsNeeded, setMeetingsNeeded] = useState<number | null>(null);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setInsights(null);
    setInsufficientData(false);
    try {
      const result: AiTrendRadarResponse = await generateTrendRadar(token, personId);
      if (result.insufficientData) {
        setInsufficientData(true);
        setMeetingsNeeded(result.meetingsNeeded);
        setError(result.error);
      } else if (result.error) {
        setError(result.error);
      } else {
        setInsights(result.insights);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate insights');
    } finally {
      setLoading(false);
    }
  };

  const getConfidenceColor = (score: number): string => {
    if (score >= 75) return 'var(--color-success, #00ff88)';
    if (score >= 40) return 'var(--color-primary)';
    return 'var(--color-text-muted)';
  };

  const getConfidenceLabel = (score: number): string => {
    if (score >= 75) return 'High Signal';
    if (score >= 40) return 'Moderate Signal';
    return 'Low Signal';
  };

  const getDimensionIcon = (dimension: string, color: string) => {
    const iconProps = { size: 16, color, strokeWidth: 2 };
    switch (dimension) {
      case 'MORALE': return <Heart {...iconProps} />;
      case 'WORK_GROWTH_BALANCE': return <Scale {...iconProps} />;
      case 'RECOGNITION': return <Award {...iconProps} />;
      case 'MEETING_EFFICACY': return <MessageCircle {...iconProps} />;
      default: return <Radar {...iconProps} />;
    }
  };

  const getDimensionLabel = (dimension: string): string => {
    switch (dimension) {
      case 'MORALE': return 'Morale';
      case 'WORK_GROWTH_BALANCE': return 'Work/Growth';
      case 'RECOGNITION': return 'Recognition';
      case 'MEETING_EFFICACY': return 'Meeting Efficacy';
      default: return dimension;
    }
  };

  return (
    <div data-testid="trend-radar-insights">
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-4)' }}>
        <h3 style={{
          margin: 0,
          fontSize: 'var(--text-h3)',
          fontFamily: 'var(--font-heading)',
          fontWeight: 'var(--weight-semibold)',
          color: 'var(--color-primary)',
          letterSpacing: '0.5px',
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--space-2)',
        }}>
          <Radar size={20} />
          Strategic Trend Radar
        </h3>
        <button
          type="button"
          onClick={handleGenerate}
          disabled={loading}
          data-testid="generate-insights-button"
          style={{
            padding: '8px 16px',
            backgroundColor: loading ? 'var(--color-bg-elevated)' : 'var(--color-primary)',
            color: loading ? 'var(--color-text-muted)' : 'var(--color-bg-base)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-mono)',
            cursor: loading ? 'not-allowed' : 'pointer',
            boxShadow: loading ? 'none' : 'var(--glow-primary)',
            transition: 'all 0.2s',
          }}
        >
          {loading ? 'Scanning...' : insights ? 'Rescan' : 'Scan Radar'}
        </button>
      </div>

      {/* Loading state */}
      {loading && (
        <div
          data-testid="radar-loading"
          style={{
            padding: 'var(--space-6)',
            textAlign: 'center',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-large)',
            backgroundColor: 'var(--color-bg-surface)',
          }}
        >
          <div style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '2px solid var(--color-border)',
            borderTopColor: 'var(--color-primary)',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
          }} />
          <p style={{
            marginTop: 'var(--space-3)',
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-small)',
          }}>
            Analyzing 90-day data window...
          </p>
        </div>
      )}

      {/* Insufficient data state */}
      {!loading && insufficientData && (
        <div
          data-testid="radar-insufficient-data"
          style={{
            padding: 'var(--space-6)',
            textAlign: 'center',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-large)',
            backgroundColor: 'var(--color-bg-surface)',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          <div style={{
            position: 'absolute',
            inset: 0,
            background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.01) 2px, rgba(0,255,255,0.01) 4px)',
            pointerEvents: 'none',
          }} />
          <div style={{ position: 'relative', zIndex: 1 }}>
            <div style={{ marginBottom: 'var(--space-3)', color: 'var(--color-primary)', display: 'flex', justifyContent: 'center' }}>
              <Radar size={32} />
            </div>
            <p style={{
              color: 'var(--color-text-secondary)',
              fontFamily: 'var(--font-mono)',
              fontSize: 'var(--text-body)',
              margin: 0,
            }}>
              {error || `Scanning horizon... Need ${meetingsNeeded || 2} more 1:1(s) to establish a baseline.`}
            </p>
          </div>
        </div>
      )}

      {/* Error state */}
      {!loading && !insufficientData && error && (
        <div
          data-testid="radar-error"
          style={{
            padding: 'var(--space-4)',
            border: '1px solid var(--color-alert)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-body)',
          }}
        >
          {error}
        </div>
      )}

      {/* Insights cards */}
      {!loading && insights && insights.length > 0 && (
        <div
          data-testid="radar-insights-list"
          style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}
        >
          {insights.map((insight, index) => (
            <div
              key={index}
              data-testid={`insight-card-${index}`}
              style={{
                padding: 'var(--space-4)',
                borderRadius: 'var(--radius-large)',
                border: `1px solid ${getConfidenceColor(insight.confidenceScore)}33`,
                backgroundColor: 'var(--color-bg-surface)',
                backdropFilter: 'blur(8px)',
                position: 'relative',
                overflow: 'hidden',
                transition: 'border-color 0.3s, box-shadow 0.3s',
                boxShadow: `0 0 12px ${getConfidenceColor(insight.confidenceScore)}11`,
              }}
            >
              {/* Scan-line texture */}
              <div style={{
                position: 'absolute',
                inset: 0,
                background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.008) 2px, rgba(0,255,255,0.008) 4px)',
                pointerEvents: 'none',
                borderRadius: 'var(--radius-large)',
              }} />

              <div style={{ position: 'relative', zIndex: 1 }}>
                {/* Title row */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-2)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
                    <span style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: getConfidenceColor(insight.confidenceScore),
                    }}>
                      {getDimensionIcon(insight.dimension, getConfidenceColor(insight.confidenceScore))}
                    </span>
                    <h4 style={{
                      margin: 0,
                      fontSize: 'var(--text-body)',
                      fontWeight: 'var(--weight-semibold)',
                      fontFamily: 'var(--font-heading)',
                      color: 'var(--color-text-primary)',
                    }}>
                      {insight.title}
                    </h4>
                  </div>
                  <span
                    data-testid={`insight-dimension-${index}`}
                    style={{
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                      color: 'var(--color-text-muted)',
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px',
                    }}
                  >
                    {getDimensionLabel(insight.dimension)}
                  </span>
                </div>

                {/* Description */}
                <p style={{
                  margin: '0 0 var(--space-3) 0',
                  fontSize: 'var(--text-body)',
                  color: 'var(--color-text-secondary)',
                  lineHeight: 1.5,
                }}>
                  {insight.description}
                </p>

                {/* Confidence gauge */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
                  <div style={{
                    flex: 1,
                    height: '4px',
                    backgroundColor: 'var(--color-bg-elevated)',
                    borderRadius: '2px',
                    overflow: 'hidden',
                  }}>
                    <div
                      data-testid={`insight-confidence-bar-${index}`}
                      style={{
                        width: `${insight.confidenceScore}%`,
                        height: '100%',
                        backgroundColor: getConfidenceColor(insight.confidenceScore),
                        borderRadius: '2px',
                        transition: 'width 0.5s ease-out',
                        boxShadow: `0 0 6px ${getConfidenceColor(insight.confidenceScore)}66`,
                      }}
                    />
                  </div>
                  <span
                    data-testid={`insight-confidence-label-${index}`}
                    style={{
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                      color: getConfidenceColor(insight.confidenceScore),
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {insight.confidenceScore}% — {getConfidenceLabel(insight.confidenceScore)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Empty state (no scan yet) */}
      {!loading && !insights && !error && !insufficientData && (
        <div
          data-testid="radar-empty-state"
          style={{
            padding: 'var(--space-6)',
            textAlign: 'center',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-large)',
            backgroundColor: 'var(--color-bg-surface)',
          }}
        >
          <div style={{ marginBottom: 'var(--space-3)', color: 'var(--color-primary)', display: 'flex', justifyContent: 'center' }}>
            <Radar size={32} />
          </div>
          <p style={{
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-mono)',
            fontSize: 'var(--text-body)',
            margin: 0,
          }}>
            Click &quot;Scan Radar&quot; to analyze 90 days of data for {personName}.
          </p>
        </div>
      )}
    </div>
  );
}
