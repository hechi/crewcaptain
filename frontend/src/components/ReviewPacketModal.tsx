'use client';

import { useState } from 'react';

interface ReviewPacketModalProps {
  personName: string;
  onGenerate: (dateFrom: string, dateTo: string) => void;
  onClose: () => void;
  generating: boolean;
}

export default function ReviewPacketModal({
  personName,
  onGenerate,
  onClose,
  generating,
}: ReviewPacketModalProps) {
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

  return (
    <div
      data-testid="review-packet-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="review-packet-title"
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
        data-testid="review-packet-backdrop"
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
          maxWidth: '440px',
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
        }}
      >
        <h2
          id="review-packet-title"
          style={{
            margin: '0 0 var(--space-2) 0',
            fontSize: 'var(--text-h3)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
          }}
        >
          Generate Review Packet
        </h2>
        <p
          style={{
            margin: '0 0 var(--space-5) 0',
            fontSize: 'var(--text-body)',
            color: 'var(--color-text-secondary)',
          }}
        >
          Create a summary document for <strong>{personName}</strong> over a date range.
        </p>

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', gap: 'var(--space-4)', marginBottom: 'var(--space-4)' }}>
            <div style={{ flex: 1 }}>
              <label
                htmlFor="review-date-from"
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
                id="review-date-from"
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                data-testid="review-date-from"
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
                htmlFor="review-date-to"
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
                id="review-date-to"
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                data-testid="review-date-to"
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
              data-testid="review-packet-validation-error"
              style={{
                color: 'var(--color-alert)',
                fontSize: 'var(--text-small)',
                margin: '0 0 var(--space-4) 0',
              }}
            >
              {validationError}
            </p>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
            <button
              type="button"
              onClick={onClose}
              data-testid="review-packet-cancel"
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
              data-testid="review-packet-generate"
              disabled={generating}
              style={{
                padding: '8px 16px',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                background: 'var(--color-primary)',
                color: 'var(--color-bg-base)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-semibold)' as any,
                cursor: generating ? 'not-allowed' : 'pointer',
                opacity: generating ? 0.6 : 1,
              }}
            >
              {generating ? 'Generating...' : 'Generate'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
