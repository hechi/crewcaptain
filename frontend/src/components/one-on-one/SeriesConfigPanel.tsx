'use client';

import { useState } from 'react';
import { CadenceType, OneOnOneSeries, UpsertSeriesRequest } from '@/types/one-on-one';
import MarkdownEditor from './MarkdownEditor';

interface SeriesConfigPanelProps {
  /** Existing series config, if any */
  series?: OneOnOneSeries | null;
  /** Called when the user saves the configuration */
  onSave: (data: UpsertSeriesRequest) => void;
  /** Whether the form is currently saving */
  isSaving?: boolean;
}

const CADENCE_OPTIONS: { value: CadenceType; label: string }[] = [
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'BIWEEKLY', label: 'Biweekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'CUSTOM', label: 'Custom' },
];

/**
 * Configuration panel for 1:1 series settings.
 * Includes cadence type selector, custom interval input (shown only for Custom),
 * Markdown editor for template, and save button.
 */
export default function SeriesConfigPanel({ series, onSave, isSaving = false }: SeriesConfigPanelProps) {
  const [cadenceType, setCadenceType] = useState<CadenceType>(series?.cadenceType || 'WEEKLY');
  const [customIntervalDays, setCustomIntervalDays] = useState<string>(
    series?.customIntervalDays?.toString() || ''
  );
  const [templateMarkdown, setTemplateMarkdown] = useState<string>(
    series?.templateMarkdown || ''
  );
  const [error, setError] = useState<string | null>(null);

  function handleSave(e: React.FormEvent) {
    e.preventDefault();

    // Validate custom interval
    if (cadenceType === 'CUSTOM') {
      const interval = parseInt(customIntervalDays, 10);
      if (!customIntervalDays || isNaN(interval) || interval <= 0) {
        setError('Custom interval must be a positive number of days');
        return;
      }
    }

    setError(null);

    const data: UpsertSeriesRequest = {
      cadenceType,
      customIntervalDays: cadenceType === 'CUSTOM' ? parseInt(customIntervalDays, 10) : null,
      templateMarkdown: templateMarkdown || null,
    };

    onSave(data);
  }

  return (
    <form
      data-testid="series-config-panel"
      onSubmit={handleSave}
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        padding: '20px',
        border: '1px solid var(--color-neutral-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-neutral-surface)',
      }}
    >
      <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>
        1:1 Series Configuration
      </h3>

      {/* Cadence Type Selector */}
      <fieldset style={{ border: 'none', padding: 0, margin: 0 }}>
        <legend
          style={{ fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '8px', color: 'var(--color-neutral-text)' }}
        >
          Meeting Cadence
        </legend>
        <div
          data-testid="cadence-selector"
          style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}
          role="radiogroup"
          aria-label="Meeting cadence"
        >
          {CADENCE_OPTIONS.map((option) => (
            <label
              key={option.value}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 14px',
                border: `1px solid ${cadenceType === option.value ? 'var(--color-secondary)' : 'var(--color-neutral-border)'}`,
                borderRadius: 'var(--radius-medium)',
                fontSize: 'var(--text-body)',
                cursor: 'pointer',
                backgroundColor: cadenceType === option.value ? 'rgba(47, 180, 163, 0.08)' : 'var(--color-neutral-surface)',
                color: cadenceType === option.value ? 'var(--color-secondary-dark)' : 'var(--color-neutral-text)',
                fontWeight: cadenceType === option.value ? 'var(--weight-medium)' : 'var(--weight-regular)',
              }}
            >
              <input
                type="radio"
                name="cadenceType"
                value={option.value}
                checked={cadenceType === option.value}
                onChange={() => {
                  setCadenceType(option.value);
                  if (error) setError(null);
                }}
                data-testid={`cadence-option-${option.value.toLowerCase()}`}
                style={{ width: '14px', height: '14px' }}
              />
              {option.label}
            </label>
          ))}
        </div>
      </fieldset>

      {/* Custom Interval (shown only when Custom selected) */}
      {cadenceType === 'CUSTOM' && (
        <div data-testid="custom-interval-section">
          <label
            htmlFor="custom-interval"
            style={{ display: 'block', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '6px', color: 'var(--color-neutral-text)' }}
          >
            Custom Interval (days)
          </label>
          <input
            id="custom-interval"
            type="number"
            min="1"
            value={customIntervalDays}
            onChange={(e) => {
              setCustomIntervalDays(e.target.value);
              if (error) setError(null);
            }}
            placeholder="e.g. 10"
            data-testid="custom-interval-input"
            aria-invalid={!!error}
            aria-describedby={error ? 'custom-interval-error' : undefined}
            style={{
              width: '120px',
              padding: '8px 12px',
              border: `1px solid ${error ? 'var(--color-error)' : 'var(--color-neutral-border)'}`,
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
            }}
          />
          {error && (
            <p
              id="custom-interval-error"
              data-testid="custom-interval-error"
              role="alert"
              style={{ margin: '4px 0 0', fontSize: 'var(--text-caption)', color: 'var(--color-error)' }}
            >
              {error}
            </p>
          )}
        </div>
      )}

      {/* Template Markdown Editor */}
      <div>
        <label
          style={{ display: 'block', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)', marginBottom: '6px', color: 'var(--color-neutral-text)' }}
        >
          Meeting Template
        </label>
        <p style={{ margin: '0 0 8px', fontSize: 'var(--text-small)', color: 'var(--color-neutral-text-muted)' }}>
          This template will prefill the notes field when creating new 1:1 entries.
        </p>
        <MarkdownEditor
          value={templateMarkdown}
          onChange={setTemplateMarkdown}
          placeholder="## Agenda\n- [ ] Review action items\n- [ ] Check-in\n\n## Notes\n\n## Outcomes"
          label="Meeting template"
        />
      </div>

      {/* Save Button */}
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <button
          type="submit"
          disabled={isSaving}
          data-testid="series-config-save"
          style={{
            padding: '10px 20px',
            backgroundColor: isSaving ? 'var(--color-secondary-light)' : 'var(--color-secondary)',
            color: '#fff',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            fontWeight: 'var(--weight-medium)',
            cursor: isSaving ? 'not-allowed' : 'pointer',
          }}
        >
          {isSaving ? 'Saving...' : 'Save Configuration'}
        </button>
      </div>
    </form>
  );
}
