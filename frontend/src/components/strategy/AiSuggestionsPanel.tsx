'use client';

import { useState, useEffect } from 'react';
import { useSession } from 'next-auth/react';
import { Sparkles, Loader2, Target, Users, Link2, Check, X } from 'lucide-react';
import { useStableToken } from '@/lib/useStableToken';
import { getAiLinkSuggestions, linkPdpGoalToStrategyGoal } from '@/lib/api-client';
import { LinkSuggestion } from '@/types/strategy-goal';

interface AiSuggestionsPanelProps {
  onSuggestionApplied?: () => void;
}

export default function AiSuggestionsPanel({ onSuggestionApplied }: AiSuggestionsPanelProps) {
  const { status } = useSession();
  const getToken = useStableToken();
  const [suggestions, setSuggestions] = useState<LinkSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [applyingId, setApplyingId] = useState<string | null>(null);
  const [dismissedIds, setDismissedIds] = useState<Set<string>>(new Set());

  const fetchSuggestions = async () => {
    if (status !== 'authenticated') return;

    const token = getToken();
    if (!token) return;

    try {
      setLoading(true);
      setError(null);
      const data = await getAiLinkSuggestions(token);
      setSuggestions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load suggestions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSuggestions();
  }, [status, getToken]);

  const handleApply = async (suggestion: LinkSuggestion) => {
    const token = getToken();
    if (!token) return;

    setApplyingId(suggestion.pdpGoalId);
    try {
      await linkPdpGoalToStrategyGoal(token, suggestion.strategyGoalId, {
        pdpGoalId: suggestion.pdpGoalId,
        personId: suggestion.personId,
      });
      setDismissedIds((prev) => new Set(prev).add(suggestion.pdpGoalId + suggestion.strategyGoalId));
      onSuggestionApplied?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to apply suggestion');
    } finally {
      setApplyingId(null);
    }
  };

  const handleDismiss = (suggestion: LinkSuggestion) => {
    setDismissedIds((prev) => new Set(prev).add(suggestion.pdpGoalId + suggestion.strategyGoalId));
  };

  const visibleSuggestions = suggestions.filter(
    (s) => !dismissedIds.has(s.pdpGoalId + s.strategyGoalId)
  );

  if (loading) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <Sparkles size={20} color="var(--color-primary)" />
          <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
            AI Link Suggestions
          </h3>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--color-text-muted)' }}>
          <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />
          <span>Analyzing your goals...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-alert)',
          borderRadius: 'var(--radius-medium)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
          <Sparkles size={20} color="var(--color-alert)" />
          <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
            AI Link Suggestions
          </h3>
        </div>
        <p style={{ margin: 0, color: 'var(--color-alert)', fontSize: 'var(--text-small)' }}>{error}</p>
      </div>
    );
  }

  if (visibleSuggestions.length === 0) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Sparkles size={20} color="var(--color-morale-green)" />
          <div>
            <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
              AI Link Suggestions
            </h3>
            <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
              No suggestions right now. Your goals look well-aligned!
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        padding: 'var(--space-4)',
        backgroundColor: 'var(--color-bg-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
        <Sparkles size={20} color="var(--color-primary)" />
        <div style={{ flex: 1 }}>
          <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
            AI Link Suggestions
          </h3>
          <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
            Based on keyword analysis
          </p>
        </div>
        <button
          onClick={fetchSuggestions}
          style={{
            padding: '4px 8px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-small)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          Refresh
        </button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {visibleSuggestions.slice(0, 5).map((suggestion) => (
          <div
            key={suggestion.pdpGoalId + suggestion.strategyGoalId}
            style={{
              padding: '12px',
              backgroundColor: 'var(--color-bg-elevated)',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-primary-muted)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', marginBottom: '8px' }}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: '32px',
                  height: '32px',
                  borderRadius: 'var(--radius-full)',
                  backgroundColor:
                    suggestion.matchScore >= 70
                      ? 'var(--color-morale-green-muted)'
                      : suggestion.matchScore >= 40
                        ? 'var(--color-morale-yellow-muted)'
                        : 'var(--color-bg-surface)',
                  flexShrink: 0,
                }}
              >
                <span
                  style={{
                    fontSize: 'var(--text-caption)',
                    fontWeight: 'var(--weight-bold)',
                    color:
                      suggestion.matchScore >= 70
                        ? 'var(--color-morale-green)'
                        : suggestion.matchScore >= 40
                          ? 'var(--color-morale-yellow)'
                          : 'var(--color-text-muted)',
                  }}
                >
                  {suggestion.matchScore}%
                </span>
              </div>

              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    marginBottom: '4px',
                  }}
                >
                  <Target size={12} color="var(--color-primary)" />
                  <span
                    style={{
                      fontSize: 'var(--text-small)',
                      fontWeight: 'var(--weight-medium)',
                      color: 'var(--color-text-primary)',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    title={suggestion.strategyGoalTitle}
                  >
                    {suggestion.strategyGoalTitle}
                  </span>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    marginBottom: '4px',
                  }}
                >
                  <Users size={12} color="var(--color-text-secondary)" />
                  <span
                    style={{
                      fontSize: 'var(--text-small)',
                      color: 'var(--color-text-secondary)',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    title={`${suggestion.personName}: ${suggestion.pdpGoalTitle}`}
                  >
                    {suggestion.personName}: {suggestion.pdpGoalTitle}
                  </span>
                </div>

                <p
                  style={{
                    margin: 0,
                    fontSize: 'var(--text-caption)',
                    color: 'var(--color-text-muted)',
                    fontStyle: 'italic',
                  }}
                >
                  {suggestion.reasoning}
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button
                onClick={() => handleDismiss(suggestion)}
                disabled={applyingId === suggestion.pdpGoalId}
                style={{
                  padding: '4px 8px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-small)',
                  backgroundColor: 'var(--color-bg-surface)',
                  color: 'var(--color-text-muted)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <X size={12} />
                Dismiss
              </button>
              <button
                onClick={() => handleApply(suggestion)}
                disabled={applyingId === suggestion.pdpGoalId}
                style={{
                  padding: '4px 8px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: 'none',
                  borderRadius: 'var(--radius-small)',
                  backgroundColor: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                {applyingId === suggestion.pdpGoalId ? (
                  <Loader2 size={12} style={{ animation: 'spin 1s linear infinite' }} />
                ) : (
                  <Check size={12} />
                )}
                Link
              </button>
            </div>
          </div>
        ))}

        {visibleSuggestions.length > 5 && (
          <p
            style={{
              margin: '8px 0 0',
              textAlign: 'center',
              fontSize: 'var(--text-small)',
              color: 'var(--color-text-muted)',
            }}
          >
            +{visibleSuggestions.length - 5} more suggestions
          </p>
        )}
      </div>
    </div>
  );
}
