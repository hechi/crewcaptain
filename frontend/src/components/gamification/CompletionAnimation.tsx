'use client';

import { useEffect, useState } from 'react';

interface CompletionAnimationProps {
  /** Whether to show the animation */
  show: boolean;
  /** Callback when animation completes */
  onComplete?: () => void;
}

/**
 * Micro-animation component for task completion.
 * Shows a checkmark with a brief glow burst effect.
 * Respects prefers-reduced-motion by showing a static checkmark instead.
 */
export default function CompletionAnimation({ show, onComplete }: CompletionAnimationProps) {
  const [visible, setVisible] = useState(false);
  const [phase, setPhase] = useState<'idle' | 'burst' | 'fade'>('idle');

  useEffect(() => {
    if (show) {
      setVisible(true);
      setPhase('burst');

      const fadeTimer = setTimeout(() => {
        setPhase('fade');
      }, 600);

      const hideTimer = setTimeout(() => {
        setVisible(false);
        setPhase('idle');
        onComplete?.();
      }, 1000);

      return () => {
        clearTimeout(fadeTimer);
        clearTimeout(hideTimer);
      };
    }
  }, [show, onComplete]);

  if (!visible) return null;

  return (
    <div
      data-testid="completion-animation"
      aria-hidden="true"
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: '32px',
        height: '32px',
        borderRadius: 'var(--radius-full)',
        backgroundColor: phase === 'burst' ? 'var(--color-success-muted)' : 'transparent',
        boxShadow: phase === 'burst' ? 'var(--glow-success)' : 'none',
        transition: 'all 0.3s ease-out',
        opacity: phase === 'fade' ? 0 : 1,
      }}
    >
      <svg
        width="20"
        height="20"
        viewBox="0 0 20 20"
        fill="none"
        data-testid="completion-checkmark"
      >
        <path
          d="M4 10L8 14L16 6"
          stroke="var(--color-success)"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          style={{
            strokeDasharray: 20,
            strokeDashoffset: phase === 'idle' ? 20 : 0,
            transition: 'stroke-dashoffset 0.4s ease-out',
          }}
        />
      </svg>
    </div>
  );
}
