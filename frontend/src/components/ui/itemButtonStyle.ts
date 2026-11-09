import type { CSSProperties } from 'react';

/**
 * Fixed box for every control in an item's action row (buttons and the due-date input).
 * An explicit height + line-height keeps rows uniform even when a label contains an emoji
 * (which otherwise falls back to another font and makes that button taller).
 */
export const ITEM_CONTROL_SIZE: CSSProperties = {
  boxSizing: 'border-box',
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  height: '28px',
  padding: '0 10px',
  lineHeight: '1',
  whiteSpace: 'nowrap',
  borderRadius: 'var(--radius-small)',
};

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
    ...ITEM_CONTROL_SIZE,
    fontSize: 'var(--text-caption)',
    fontFamily: 'var(--font-mono)',
    border: `1px solid ${c.border}`,
    backgroundColor: c.bg,
    color: disabled ? 'var(--color-text-muted)' : c.color,
    cursor: disabled ? 'not-allowed' : 'pointer',
  };
}
