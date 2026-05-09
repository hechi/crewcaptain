'use client';

interface SensitiveToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
}

/**
 * Checkbox/switch for marking content as sensitive.
 * Provides a clear visual indicator of the sensitive state.
 */
export default function SensitiveToggle({ checked, onChange, label = 'Mark as sensitive' }: SensitiveToggleProps) {
  const toggleId = 'sensitive-toggle';

  return (
    <div data-testid="sensitive-toggle" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
      <button
        id={toggleId}
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        onClick={() => onChange(!checked)}
        data-testid="sensitive-toggle-switch"
        style={{
          position: 'relative',
          width: '44px',
          height: '24px',
          borderRadius: '12px',
          border: 'none',
          backgroundColor: checked ? 'var(--color-warning)' : 'var(--color-border)',
          cursor: 'pointer',
          transition: 'background-color 0.2s',
          padding: 0,
        }}
      >
        <span
          style={{
            position: 'absolute',
            top: '2px',
            left: checked ? '22px' : '2px',
            width: '20px',
            height: '20px',
            borderRadius: '50%',
            backgroundColor: checked ? 'var(--color-bg-base)' : 'var(--color-text-muted)',
            transition: 'left 0.2s',
            boxShadow: checked ? '0 0 6px rgba(255, 214, 0, 0.4)' : '0 1px 3px rgba(0,0,0,0.3)',
          }}
        />
      </button>
      <label
        htmlFor={toggleId}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          fontSize: 'var(--text-body)',
          color: checked ? 'var(--color-warning)' : 'var(--color-text-secondary)',
          cursor: 'pointer',
          fontWeight: checked ? 'var(--weight-medium)' : 'var(--weight-regular)',
        }}
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        {label}
      </label>
    </div>
  );
}
