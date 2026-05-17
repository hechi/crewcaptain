'use client';

import { useState } from 'react';

interface AiNarrativeModalProps {
  personName: string;
  onGenerate: (dateFrom: string, dateTo: string) => void;
  onClose: () => void;
  generating: boolean;
  narrative: string | null;
  error: string | null;
}

export default function AiNarrativeModal({
  personName,
  onGenerate,
  onClose,
  generating,
  narrative,
  error,
}: AiNarrativeModalProps) {
  const today = new Date().toISOString().split('T')[0];
  const sixMonthsAgo = new Date(Date.now() - 180 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

  const [dateFrom, setDateFrom] = useState(sixMonthsAgo);
  const [dateTo, setDateTo] = useState(today);
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);

    if (!dateFrom || !dateTo) {
      setValidationError('Both dates are required');
      return;
    }

    if (dateTo < dateFrom) {
      setValidationError('End date must not be before start date');
      return;
    }

    onGenerate(dateFrom, dateTo);
  };

  const handleCopyToClipboard = () => {
    if (narrative) {
      navigator.clipboard.writeText(narrative);
    }
  };

  return (
    <div
      data-testid="ai-narrative-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="ai-narrative-title"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
    >
      {/* Backdrop */}
      <div
        data-testid="ai-narrative-backdrop"
        onClick={onClose}
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)',
        }}
      />

      {/* Modal content */}
      <div
        style={{
          position: 'relative',
          background: 'var(--color-bg-elevated)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-large)',
          padding: 'var(--space-6)',
          width: '100%',
          maxWidth: narrative ? '640px' : '440px',
          maxHeight: '80vh',
          overflow: 'auto',
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
          transition: 'max-width 0.3s ease',
        }}
      >
        <h2
          id="ai-narrative-title"
          style={{
            margin: '0 0 var(--space-2) 0',
            fontSize: 'var(--text-h3)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
          }}
        >
          Generate AI Narrative
        </h2>
        <p
          style={{
            margin: '0 0 var(--space-5) 0',
            fontSize: 'var(--text-body)',
            color: 'var(--color-text-secondary)',
          }}
        >
          Generate a performance review narrative for <strong>{personName}</strong> using AI.
        </p>

        {!narrative && (
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', gap: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
              <div style={{ flex: 1 }}>
                <label
                  htmlFor="narrative-date-from"
                  style={{
                    display: 'block',
                    marginBottom: 'var(--space-1)',
                    fontSize: 'var(--text-small)',
                    color: 'var(--color-text-secondary)',
                    fontFamily: 'var(--font-mono)',
                  }}
                >
                  From
                </label>
                <input
                  id="narrative-date-from"
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  data-testid="narrative-date-from"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-medium)',
                    background: 'var(--color-bg-surface)',
                    color: 'var(--color-text-primary)',
                    fontSize: 'var(--text-body)',
                  }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label
                  htmlFor="narrative-date-to"
                  style={{
                    display: 'block',
                    marginBottom: 'var(--space-1)',
                    fontSize: 'var(--text-small)',
                    color: 'var(--color-text-secondary)',
                    fontFamily: 'var(--font-mono)',
                  }}
                >
                  To
                </label>
                <input
                  id="narrative-date-to"
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  data-testid="narrative-date-to"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-medium)',
                    background: 'var(--color-bg-surface)',
                    color: 'var(--color-text-primary)',
                    fontSize: 'var(--text-body)',
                  }}
                />
              </div>
            </div>

            {validationError && (
              <p
                data-testid="ai-narrative-validation-error"
                style={{
                  color: 'var(--color-alert)',
                  fontSize: 'var(--text-small)',
                  margin: '0 0 var(--space-4) 0',
                }}
              >
                {validationError}
              </p>
            )}

            {error && (
              <p
                data-testid="ai-narrative-error"
                style={{
                  color: 'var(--color-alert)',
                  fontSize: 'var(--text-small)',
                  margin: '0 0 var(--space-4) 0',
                }}
              >
                {error}
              </p>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
              <button
                type="button"
                onClick={onClose}
                data-testid="ai-narrative-cancel"
                disabled={generating}
                style={{
                  padding: '8px 16px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  background: 'var(--color-bg-elevated)',
                  color: 'var(--color-text-secondary)',
                  fontSize: 'var(--text-body)',
                  cursor: generating ? 'not-allowed' : 'pointer',
                }}
              >
                Cancel
              </button>
              <button
                type="submit"
                data-testid="ai-narrative-generate"
                disabled={generating}
                style={{
                  padding: '8px 16px',
                  border: 'none',
                  borderRadius: 'var(--radius-medium)',
                  background: generating ? 'var(--color-primary)' : 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  fontSize: 'var(--text-body)',
                  fontWeight: 'var(--weight-semibold)' as any,
                  cursor: generating ? 'not-allowed' : 'pointer',
                  opacity: generating ? 0.6 : 1,
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                {generating && (
                  <span
                    data-testid="ai-narrative-pulse"
                    style={{
                      position: 'absolute',
                      inset: 0,
                      background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)',
                      animation: 'pulse-sweep 1.5s infinite',
                    }}
                  />
                )}
                <span style={{ position: 'relative' }}>
                  {generating ? 'Generating...' : 'Generate Narrative'}
                </span>
              </button>
            </div>
          </form>
        )}

        {narrative && (
          <div data-testid="ai-narrative-result">
            <textarea
              data-testid="ai-narrative-textarea"
              value={narrative}
              readOnly
              aria-label="Generated narrative"
              style={{
                width: '100%',
                minHeight: '250px',
                padding: 'var(--space-4)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                background: 'var(--color-bg-surface)',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-body)',
                lineHeight: '1.6',
                resize: 'vertical',
                marginBottom: 'var(--space-4)',
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
              <button
                type="button"
                onClick={handleCopyToClipboard}
                data-testid="ai-narrative-copy"
                style={{
                  padding: '8px 16px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  background: 'var(--color-bg-elevated)',
                  color: 'var(--color-text-secondary)',
                  fontSize: 'var(--text-body)',
                  cursor: 'pointer',
                }}
              >
                Copy to Clipboard
              </button>
              <button
                type="button"
                onClick={onClose}
                data-testid="ai-narrative-done"
                style={{
                  padding: '8px 16px',
                  border: 'none',
                  borderRadius: 'var(--radius-medium)',
                  background: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  fontSize: 'var(--text-body)',
                  fontWeight: 'var(--weight-semibold)' as any,
                  cursor: 'pointer',
                }}
              >
                Done
              </button>
            </div>
          </div>
        )}
      </div>

      <style>{`
        @keyframes pulse-sweep {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(100%); }
        }
      `}</style>
    </div>
  );
}
