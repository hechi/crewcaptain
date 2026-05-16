'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { createQuickNote } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

/**
 * Global Quick Note overlay — accessible from any page via Ctrl+Shift+Q
 * or the floating action button. Cyberpunk-lite glassmorphism design with
 * neon glow fade-in animation.
 */
export default function QuickNoteOverlay() {
  const [isOpen, setIsOpen] = useState(false);
  const [text, setText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const { getToken, isAuthenticated } = useStableToken();

  const open = useCallback(() => {
    if (!isAuthenticated) return;
    setIsOpen(true);
    setText('');
    setError(null);
    setSuccess(false);
  }, [isAuthenticated]);

  const close = useCallback(() => {
    setIsOpen(false);
    setText('');
    setError(null);
    setSuccess(false);
  }, []);

  // Keyboard shortcut: Ctrl+Shift+Q
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'Q') {
        e.preventDefault();
        if (isOpen) {
          close();
        } else {
          open();
        }
      }
      // Escape to close
      if (e.key === 'Escape' && isOpen) {
        e.preventDefault();
        close();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, open, close]);

  // Auto-focus textarea when overlay opens
  useEffect(() => {
    if (isOpen && textareaRef.current) {
      // Small delay to allow animation to start
      const timer = setTimeout(() => textareaRef.current?.focus(), 50);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    const token = getToken();
    if (!token) return;

    setSubmitting(true);
    setError(null);

    try {
      await createQuickNote(token, { text: text.trim() });
      setSuccess(true);
      // Auto-close after brief success feedback
      setTimeout(() => {
        close();
      }, 800);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save note');
    } finally {
      setSubmitting(false);
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      close();
    }
  };

  // Don't render anything if not authenticated
  if (!isAuthenticated) return null;

  return (
    <>
      {/* Floating Action Button */}
      <button
        type="button"
        onClick={open}
        data-testid="quick-note-fab"
        aria-label="Quick note (Ctrl+Shift+Q)"
        title="Quick note (Ctrl+Shift+Q)"
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          border: '1px solid var(--color-border-glow)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-primary)',
          fontSize: '22px',
          fontFamily: 'var(--font-mono)',
          fontWeight: '700',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--glow-primary)',
          transition: 'box-shadow 0.2s, border-color 0.2s, transform 0.2s',
          zIndex: 900,
        }}
      >
        ✎
      </button>

      {/* Overlay */}
      {isOpen && (
        <div
          data-testid="quick-note-overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby="quick-note-overlay-title"
          onClick={handleBackdropClick}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1100,
            animation: 'qn-overlay-fade-in 0.25s ease-out forwards',
          }}
        >
          {/* Backdrop */}
          <div
            data-testid="quick-note-overlay-backdrop"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: 'rgba(0, 0, 0, 0.7)',
              backdropFilter: 'blur(4px)',
            }}
          />

          {/* Modal panel */}
          <div
            data-testid="quick-note-overlay-panel"
            style={{
              position: 'relative',
              width: '100%',
              maxWidth: '480px',
              margin: '0 16px',
              padding: 'var(--space-6, 24px)',
              backgroundColor: 'var(--glass-elevated-bg, rgba(30, 35, 48, 0.9))',
              border: '1px solid rgba(0, 240, 255, 0.2)',
              borderRadius: 'var(--radius-large, 12px)',
              backdropFilter: 'var(--glass-elevated-blur, blur(16px))',
              boxShadow: '0 0 30px rgba(0, 240, 255, 0.1), 0 20px 60px rgba(0, 0, 0, 0.4)',
              animation: 'qn-panel-slide-up 0.3s ease-out forwards',
            }}
          >
            {/* Scan-line decoration */}
            <div
              aria-hidden="true"
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                borderRadius: 'var(--radius-large, 12px)',
                pointerEvents: 'none',
                background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0, 240, 255, 0.015) 2px, rgba(0, 240, 255, 0.015) 4px)',
                opacity: 0.5,
              }}
            />

            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', position: 'relative' }}>
              <h2
                id="quick-note-overlay-title"
                style={{
                  margin: 0,
                  fontSize: '18px',
                  fontWeight: '600',
                  fontFamily: 'var(--font-heading, monospace)',
                  color: 'var(--color-primary, #00F0FF)',
                  letterSpacing: '-0.2px',
                }}
              >
                ⚡ Quick Capture
              </h2>
              <span
                style={{
                  fontSize: '11px',
                  fontFamily: 'var(--font-mono, monospace)',
                  color: 'var(--color-text-muted, #4A5568)',
                  padding: '2px 8px',
                  border: '1px solid var(--color-border, #2A3040)',
                  borderRadius: 'var(--radius-small, 4px)',
                }}
              >
                Ctrl+Shift+Q
              </span>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit}>
              <textarea
                ref={textareaRef}
                value={text}
                onChange={(e) => setText(e.target.value)}
                placeholder="What's on your mind? Capture it now..."
                data-testid="quick-note-overlay-input"
                aria-label="Quick note text"
                rows={4}
                disabled={submitting || success}
                onKeyDown={(e) => {
                  // Ctrl+Enter or Cmd+Enter to submit
                  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                    e.preventDefault();
                    handleSubmit(e);
                  }
                }}
                style={{
                  width: '100%',
                  padding: '12px 14px',
                  border: success
                    ? '1px solid var(--color-success, #39FF85)'
                    : '1px solid var(--color-border-glow, rgba(0, 240, 255, 0.2))',
                  borderRadius: 'var(--radius-medium, 8px)',
                  fontSize: '14px',
                  fontFamily: 'var(--font-ui, Inter, sans-serif)',
                  backgroundColor: 'var(--color-bg-elevated, #1E2330)',
                  color: 'var(--color-text-primary, #E8ECF0)',
                  resize: 'vertical',
                  minHeight: '100px',
                  outline: 'none',
                  transition: 'border-color 0.2s, box-shadow 0.2s',
                  boxShadow: success
                    ? '0 0 12px rgba(57, 255, 133, 0.2)'
                    : 'none',
                  boxSizing: 'border-box',
                }}
              />

              {/* Error message */}
              {error && (
                <p
                  data-testid="quick-note-overlay-error"
                  style={{
                    margin: '8px 0 0',
                    fontSize: '12px',
                    fontFamily: 'var(--font-mono, monospace)',
                    color: 'var(--color-alert, #FF2D7B)',
                  }}
                >
                  {error}
                </p>
              )}

              {/* Success message */}
              {success && (
                <p
                  data-testid="quick-note-overlay-success"
                  style={{
                    margin: '8px 0 0',
                    fontSize: '12px',
                    fontFamily: 'var(--font-mono, monospace)',
                    color: 'var(--color-success, #39FF85)',
                  }}
                >
                  ✓ Note captured
                </p>
              )}

              {/* Footer */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px', position: 'relative' }}>
                <span
                  style={{
                    fontSize: '11px',
                    fontFamily: 'var(--font-mono, monospace)',
                    color: 'var(--color-text-muted, #4A5568)',
                  }}
                >
                  Ctrl+Enter to save
                </span>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    onClick={close}
                    data-testid="quick-note-overlay-cancel"
                    disabled={submitting}
                    style={{
                      padding: '8px 14px',
                      border: '1px solid var(--color-border, #2A3040)',
                      borderRadius: 'var(--radius-medium, 8px)',
                      backgroundColor: 'transparent',
                      color: 'var(--color-text-secondary, #7A8599)',
                      fontSize: '13px',
                      fontFamily: 'var(--font-mono, monospace)',
                      cursor: submitting ? 'not-allowed' : 'pointer',
                      transition: 'border-color 0.2s',
                    }}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={!text.trim() || submitting || success}
                    data-testid="quick-note-overlay-submit"
                    style={{
                      padding: '8px 16px',
                      border: 'none',
                      borderRadius: 'var(--radius-medium, 8px)',
                      backgroundColor: text.trim() && !submitting && !success
                        ? 'var(--color-primary, #00F0FF)'
                        : 'var(--color-bg-elevated, #1E2330)',
                      color: text.trim() && !submitting && !success
                        ? 'var(--color-bg-base, #0D0F14)'
                        : 'var(--color-text-muted, #4A5568)',
                      fontSize: '13px',
                      fontWeight: '600',
                      fontFamily: 'var(--font-mono, monospace)',
                      cursor: text.trim() && !submitting && !success ? 'pointer' : 'not-allowed',
                      boxShadow: text.trim() && !submitting && !success
                        ? 'var(--glow-primary, 0 0 12px rgba(0, 240, 255, 0.2))'
                        : 'none',
                      transition: 'all 0.2s',
                    }}
                  >
                    {submitting ? 'Saving...' : success ? 'Saved ✓' : 'Capture'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Keyframe animations */}
      <style>{`
        @keyframes qn-overlay-fade-in {
          from {
            opacity: 0;
          }
          to {
            opacity: 1;
          }
        }

        @keyframes qn-panel-slide-up {
          from {
            opacity: 0;
            transform: translateY(20px) scale(0.97);
          }
          to {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
        }

        @media (prefers-reduced-motion: reduce) {
          @keyframes qn-overlay-fade-in {
            from { opacity: 1; }
            to { opacity: 1; }
          }
          @keyframes qn-panel-slide-up {
            from { opacity: 1; transform: none; }
            to { opacity: 1; transform: none; }
          }
        }
      `}</style>
    </>
  );
}
