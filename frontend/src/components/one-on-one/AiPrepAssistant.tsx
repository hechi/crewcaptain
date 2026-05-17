'use client';

import { useState } from 'react';
import { generateAiAgendaSuggestions } from '@/lib/api-client';

interface AiPrepAssistantProps {
  token: string;
  personId: string;
  onAddSuggestion: (text: string) => void;
}

export default function AiPrepAssistant({ token, personId, onAddSuggestion }: AiPrepAssistantProps) {
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addedIndexes, setAddedIndexes] = useState<Set<number>>(new Set());
  const [glowActive, setGlowActive] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setSuggestions([]);
    setAddedIndexes(new Set());
    setGlowActive(false);

    try {
      const result = await generateAiAgendaSuggestions(token, personId);
      if (result.error) {
        setError(result.error);
      } else {
        setSuggestions(result.suggestions);
        setGlowActive(true);
        setTimeout(() => setGlowActive(false), 1500);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate suggestions');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = (index: number) => {
    onAddSuggestion(suggestions[index]);
    setAddedIndexes((prev) => new Set([...prev, index]));
  };

  return (
    <div
      data-testid="ai-prep-assistant"
      style={{
        marginBottom: 'var(--space-5)',
        padding: 'var(--space-4)',
        borderRadius: 'var(--radius-large)',
        border: '1px solid var(--color-border)',
        backgroundColor: 'var(--color-bg-surface)',
        backdropFilter: 'blur(12px)',
        position: 'relative',
        overflow: 'hidden',
        transition: 'box-shadow 0.3s ease',
        boxShadow: glowActive ? '0 0 20px var(--color-primary), 0 0 40px var(--color-primary-muted)' : 'none',
      }}
    >
      {/* Scan-line texture overlay */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.015) 2px, rgba(0,255,255,0.015) 4px)',
          pointerEvents: 'none',
          borderRadius: 'var(--radius-large)',
        }}
      />

      <div style={{ position: 'relative', zIndex: 1 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-2)' }}>
          <h3
            style={{
              margin: 0,
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-heading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-primary)',
              letterSpacing: '0.5px',
            }}
          >
            ✦ AI Prep Assistant
          </h3>
          <button
            type="button"
            data-testid="ai-generate-btn"
            onClick={handleGenerate}
            disabled={loading}
            aria-label="Generate suggested agenda items"
            style={{
              padding: '6px 14px',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border-glow)',
              backgroundColor: loading ? 'var(--color-bg-elevated)' : 'var(--color-primary-muted)',
              color: 'var(--color-primary)',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontWeight: 'var(--weight-medium)',
              fontSize: 'var(--text-small)',
              fontFamily: 'var(--font-mono)',
              opacity: loading ? 0.6 : 1,
              transition: 'all 0.2s',
              boxShadow: loading ? 'none' : '0 0 8px var(--color-primary-muted)',
            }}
          >
            {loading ? 'Generating...' : 'Generate Agenda'}
          </button>
        </div>
        <p
          data-testid="ai-description"
          style={{
            margin: '0 0 var(--space-3) 0',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            lineHeight: 1.4,
          }}
        >
          Uses past 1:1 notes, open action items, active goals, and recent kudos to suggest agenda items for this meeting.
        </p>

        {/* Loading state with pulse animation */}
        {loading && (
          <div
            data-testid="ai-loading"
            style={{
              padding: 'var(--space-4)',
              textAlign: 'center',
              color: 'var(--color-text-secondary)',
              fontSize: 'var(--text-small)',
              animation: 'pulse 1.5s ease-in-out infinite',
            }}
          >
            <span style={{ fontFamily: 'var(--font-mono)' }}>Synthesizing context...</span>
          </div>
        )}

        {/* Error state */}
        {error && (
          <div
            data-testid="ai-error"
            style={{
              padding: 'var(--space-3)',
              borderRadius: 'var(--radius-small)',
              border: '1px solid var(--color-alert-muted)',
              backgroundColor: 'rgba(255, 50, 50, 0.05)',
              color: 'var(--color-alert)',
              fontSize: 'var(--text-small)',
            }}
          >
            {error}
          </div>
        )}

        {/* Suggestions list */}
        {suggestions.length > 0 && (
          <div data-testid="ai-suggestions" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
            <span style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', marginBottom: '4px' }}>
              Suggested agenda items:
            </span>
            {suggestions.map((suggestion, index) => (
              <div
                key={index}
                data-testid={`ai-suggestion-${index}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 'var(--space-2)',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-small)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: addedIndexes.has(index) ? 'rgba(0, 255, 100, 0.05)' : 'var(--color-bg-elevated)',
                  transition: 'all 0.2s',
                }}
              >
                <span style={{ flex: 1, fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>
                  {suggestion}
                </span>
                <button
                  type="button"
                  data-testid={`ai-add-suggestion-${index}`}
                  onClick={() => handleAdd(index)}
                  disabled={addedIndexes.has(index)}
                  aria-label={`Add suggestion: ${suggestion}`}
                  style={{
                    padding: '4px 10px',
                    borderRadius: 'var(--radius-small)',
                    border: addedIndexes.has(index) ? '1px solid var(--color-success)' : '1px solid var(--color-primary)',
                    backgroundColor: 'transparent',
                    color: addedIndexes.has(index) ? 'var(--color-success)' : 'var(--color-primary)',
                    cursor: addedIndexes.has(index) ? 'default' : 'pointer',
                    fontSize: 'var(--text-small)',
                    fontFamily: 'var(--font-mono)',
                    whiteSpace: 'nowrap',
                    transition: 'all 0.2s',
                  }}
                >
                  {addedIndexes.has(index) ? '✓ Added' : '+ Add'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* CSS animation for pulse */}
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
        @media (prefers-reduced-motion: reduce) {
          [data-testid="ai-loading"] {
            animation: none !important;
          }
          [data-testid="ai-prep-assistant"] {
            transition: none !important;
          }
        }
      `}</style>
    </div>
  );
}
