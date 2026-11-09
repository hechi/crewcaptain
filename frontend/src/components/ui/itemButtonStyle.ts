import type { CSSProperties } from 'react';

export type ItemButtonVariant = 'default' | 'primary' | 'success' | 'danger' | 'muted';

/**
 * The single button style shared by MyNoteCard and ActionItemCard (and therefore the
 * My Notes page, the notes side panel and the Action Items page), so action buttons have
 * the same size and shape everywhere. Only the colors differ per variant.
 */
export function itemButtonStyle(variant: ItemButtonVariant = 'default', disabled = false): CSSProperties {
  const colors: Record<ItemButtonVariant, { border: string; bg: string; color: string }> = {
    default: { border: 'var(--color-border)', bg: 'transparent', color: 'var(--color-text-secondary)' },
    muted: { border: 'var(--color-border)', bg: 'transparent', color: 'var(--color-text-muted)' },
    primary: { border: 'var(--color-primary)', bg: 'var(--color-primary-muted)', color: 'var(--color-primary)' },
    success: { border: 'var(--color-morale-green)', bg: 'rgba(16, 185, 129, 0.1)', color: 'var(--color-morale-green)' },
    danger: { border: 'var(--color-alert-muted)', bg: 'var(--color-alert-muted)', color: 'var(--color-alert)' },
  };
  const c = colors[variant];
  return {
    padding: '4px 10px',
    fontSize: 'var(--text-caption)',
    fontFamily: 'var(--font-mono)',
    border: `1px solid ${c.border}`,
    borderRadius: 'var(--radius-small)',
    backgroundColor: c.bg,
    color: disabled ? 'var(--color-text-muted)' : c.color,
    cursor: disabled ? 'not-allowed' : 'pointer',
  };
}
