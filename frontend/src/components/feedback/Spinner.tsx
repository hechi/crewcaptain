'use client';

import { Loader2 } from 'lucide-react';

interface SpinnerProps {
  /** Icon size in px. */
  size?: number;
  /** Accessible label for screen readers. */
  label?: string;
}

/**
 * Small inline spinner used to signal in-flight AI work on the feedback screens.
 * Uses lucide's Loader2 with a CSS spin animation. Respects prefers-reduced-motion:
 * users who prefer reduced motion see a static icon instead of a spinning one.
 */
export default function Spinner({ size = 14, label = 'Working…' }: SpinnerProps) {
  return (
    <span
      data-testid="ai-spinner"
      role="status"
      aria-live="polite"
      style={{ display: 'inline-flex', alignItems: 'center' }}
    >
      <Loader2 size={size} className="feedback-spinner-icon" aria-hidden="true" />
      <span style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)', whiteSpace: 'nowrap' }}>
        {label}
      </span>
      <style jsx>{`
        :global(.feedback-spinner-icon) {
          animation: feedback-spin 1s linear infinite;
        }
        @keyframes feedback-spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        @media (prefers-reduced-motion: reduce) {
          :global(.feedback-spinner-icon) {
            animation: none;
          }
        }
      `}</style>
    </span>
  );
}
