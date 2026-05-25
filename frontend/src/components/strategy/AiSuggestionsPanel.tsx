'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { Sparkles, Loader2, Target, Users, Check, X, Wand2 } from 'lucide-react';
import { useStableToken } from '@/lib/useStableToken';
import { generateAiLinkSuggestions, linkPdpGoalToStrategyGoal } from '@/lib/api-client';
import { LinkSuggestion } from '@/types/strategy-goal';

interface AiSuggestionsPanelProps {
  onSuggestionApplied?: () => void;
}

export default function AiSuggestionsPanel({ onSuggestionApplied }: AiSuggestionsPanelProps) {
  const { status } = useSession();
  const { getToken } = useStableToken();
  const [suggestions, setSuggestions] = useState<LinkSuggestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasGenerated, setHasGenerated] = useState(false);
  const [applyingId, setApplyingId] = useState<string | null>(null);
  const [dismissedIds, setDismissedIds] = useState<Set<string>>(new Set());

  const handleGenerate = async () => {
    if (status !== 'authenticated') return;

    const token = getToken();
    if (!token) return;

    try {
      setLoading(true);
      setError(null);
      setSuggestions([]);

      const response = await generateAiLinkSuggestions(token);

      if (response.error) {
        setError(response.error);
        setSuggestions([]);
      } else if (response.suggestions) {
        setSuggestions(response.suggestions);
        setError(null);
      } else {
        setSuggestions([]);
        setError('No suggestions returned from AI');
      }
      setHasGenerated(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate suggestions');
      setSuggestions([]);
      setHasGenerated(true);
    } finally {
      setLoading(false);
    }
  };

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

  // Initial state - show Generate button
  if (!hasGenerated && !loading) {
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
          <div>
            <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
              AI Link Suggestions
            </h3>
            <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
              Use AI to analyze your goals and suggest meaningful connections
            </p>
          </div>
        </div>

        <button
          onClick={handleGenerate}
          disabled={status !== 'authenticated'}
          style={{
            width: '100%',
            padding: '12px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-semibold)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: status === 'authenticated' ? 'pointer' : 'not-allowed',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            boxShadow: 'var(--glow-primary)',
            transition: 'all 0.2s ease',
          }}
        >
          <Wand2 size={18} />
          Generate Suggestions
        </button>

        {status !== 'authenticated' && (
          <p style={{ margin: '8px 0 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-caption)', textAlign: 'center' }}>
            Please sign in to use AI features
          </p>
        )}
      </div>
    );
  }

  // Loading state with cyberpunk animation
  if (loading) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Animated gradient background */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.05), transparent)',
            animation: 'shimmer 2s infinite',
          }}
        />

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px', position: 'relative' }}>
          <div
            style={{
              animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            }}
          >
            <Sparkles size={20} color="var(--color-primary)" />
          </div>
          <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
            AI Link Suggestions
          </h3>
        </div>

        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '16px',
            padding: '32px',
            position: 'relative',
          }}
        >
          {/* Animated spinner with glow */}
          <div
            style={{
              position: 'relative',
              width: '48px',
              height: '48px',
            }}
          >
            <Loader2
              size={48}
              color="var(--color-primary)"
              style={{
                animation: 'spin 1s linear infinite',
                filter: 'drop-shadow(0 0 8px rgba(6, 182, 212, 0.5))',
              }}
            />
          </div>

          <div style={{ textAlign: 'center' }}>
            <p
              style={{
                margin: '0 0 4px',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-medium)',
              }}
            >
              Analyzing your goals...
            </p>
            <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
              AI is comparing strategy goals with PDP goals to find alignments
            </p>
          </div>
        </div>

        <style jsx>{`
          @keyframes shimmer {
            0% { transform: translateX(-100%); }
            100% { transform: translateX(100%); }
          }
          @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
          }
          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  // Error state
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
        <p style={{ margin: '0 0 16px', color: 'var(--color-alert)', fontSize: 'var(--text-small)' }}>{error}</p>
        <button
          onClick={handleGenerate}
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-small)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          Try Again
        </button>
      </div>
    );
  }

  // Empty state - no suggestions
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <Sparkles size={20} color="var(--color-morale-green)" />
          <div style={{ flex: 1 }}>
            <h3 style={{ margin: 0, fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)' }}>
              AI Link Suggestions
            </h3>
            <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
              Analysis complete
            </p>
          </div>
          <button
            onClick={handleGenerate}
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
            Regenerate
          </button>
        </div>

        <div
          style={{
            padding: '24px',
            textAlign: 'center',
            backgroundColor: 'var(--color-bg-elevated)',
            borderRadius: 'var(--radius-medium)',
          }}
        >
          <p style={{ margin: '0 0 8px', color: 'var(--color-text-primary)', fontSize: 'var(--text-body)' }}>
            No suggestions found
          </p>
          <p style={{ margin: 0, color: 'var(--color-text-muted)', fontSize: 'var(--text-small)' }}>
            Your goals look well-aligned! The AI didn&apos;t find any strong new connections to suggest.
          </p>
        </div>
      </div>
    );
  }

  // Success state with suggestions
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
            {visibleSuggestions.length} suggestion{visibleSuggestions.length !== 1 ? 's' : ''} found
          </p>
        </div>
        <button
          onClick={handleGenerate}
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
          Regenerate
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
