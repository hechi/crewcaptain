'use client';

import { useCallback, useEffect, useState } from 'react';
import { Person } from '@/types/person';
import { FeedbackTemplate, BulkFeedbackLinkResult } from '@/types/feedback';
import { listFeedbackTemplates, bulkCreateFeedbackLinks } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import Modal from '@/components/Modal';

interface BulkFeedbackLinksModalProps {
  isOpen: boolean;
  /** People the manager can select (e.g. the currently loaded page). */
  people: Person[];
  onClose: () => void;
}

const DEFAULT_DAYS = 14;
const MAX_DAYS = 90;

const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  marginBottom: '4px',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 12px',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-medium)',
  fontSize: 'var(--text-body)',
  backgroundColor: 'var(--color-bg-elevated)',
  color: 'var(--color-text-primary)',
  boxSizing: 'border-box',
};

function publicUrl(token: string): string {
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  return `${origin}/f/${token}`;
}

/** Escapes a value for a CSV cell (quotes if needed, doubles inner quotes). */
function csvCell(value: string): string {
  if (/[",\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

/**
 * Bulk-creates feedback links for multiple selected people using a shared
 * template, then presents a results table and a client-side CSV download.
 */
export default function BulkFeedbackLinksModal({ isOpen, people, onClose }: BulkFeedbackLinksModalProps) {
  const { getToken } = useStableToken();

  const [templates, setTemplates] = useState<FeedbackTemplate[]>([]);
  const [templateId, setTemplateId] = useState('');
  const [expiresInDays, setExpiresInDays] = useState(DEFAULT_DAYS);
  const [label, setLabel] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<BulkFeedbackLinkResult | null>(null);

  const resetState = useCallback(() => {
    setTemplateId('');
    setExpiresInDays(DEFAULT_DAYS);
    setLabel('');
    setSelectedIds(new Set());
    setSubmitting(false);
    setError(null);
    setResult(null);
  }, []);

  const loadTemplates = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      const tpls = await listFeedbackTemplates(token);
      setTemplates(tpls);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load templates');
    }
  }, [getToken]);

  useEffect(() => {
    if (isOpen) {
      resetState();
      loadTemplates();
    }
  }, [isOpen, resetState, loadTemplates]);

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    setSelectedIds((prev) => (prev.size === people.length ? new Set() : new Set(people.map((p) => p.id))));
  };

  const canSubmit = !!templateId && selectedIds.size > 0 && !submitting;

  const handleSubmit = async () => {
    const token = getToken();
    if (!token || !canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await bulkCreateFeedbackLinks(token, {
        personIds: Array.from(selectedIds),
        templateId,
        expiresInDays,
        label: label.trim() || null,
      });
      setResult(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create feedback links');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDownloadCsv = () => {
    if (!result) return;
    const header = 'person,link';
    const rows = result.links.map((l) => `${csvCell(l.personName)},${csvCell(publicUrl(l.token))}`);
    const csv = [header, ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'feedback-links.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Bulk feedback links" maxWidth="680px">
      <div data-testid="bulk-feedback-links-modal">
        {result ? (
          <div data-testid="bulk-feedback-links-results">
            <p style={{ margin: '0 0 12px', color: 'var(--color-text-primary)', fontSize: 'var(--text-body)' }}>
              Created {result.links.length} feedback link{result.links.length === 1 ? '' : 's'}.
            </p>
            <table
              data-testid="bulk-feedback-links-table"
              style={{ width: '100%', borderCollapse: 'collapse', fontSize: 'var(--text-small)', marginBottom: '12px' }}
            >
              <thead>
                <tr>
                  <th style={{ ...labelStyle, textAlign: 'left', padding: '6px 8px' }}>Person</th>
                  <th style={{ ...labelStyle, textAlign: 'left', padding: '6px 8px' }}>Link</th>
                  <th style={{ ...labelStyle, textAlign: 'left', padding: '6px 8px' }}></th>
                </tr>
              </thead>
              <tbody>
                {result.links.map((l) => (
                  <tr key={l.linkId} data-testid={`bulk-feedback-link-row-${l.personId}`} style={{ borderTop: '1px solid var(--color-border)' }}>
                    <td style={{ padding: '6px 8px', color: 'var(--color-text-primary)' }}>{l.personName}</td>
                    <td style={{ padding: '6px 8px', color: 'var(--color-primary)', fontFamily: 'var(--font-mono)', wordBreak: 'break-all' }}>
                      {publicUrl(l.token)}
                    </td>
                    <td style={{ padding: '6px 8px' }}>
                      <button
                        type="button"
                        onClick={() => navigator.clipboard?.writeText(publicUrl(l.token))}
                        data-testid={`bulk-feedback-link-copy-${l.personId}`}
                        style={{
                          padding: '2px 10px',
                          fontSize: 'var(--text-caption)',
                          fontFamily: 'var(--font-mono)',
                          border: '1px solid var(--color-primary)',
                          borderRadius: 'var(--radius-medium)',
                          backgroundColor: 'transparent',
                          color: 'var(--color-primary)',
                          cursor: 'pointer',
                        }}
                      >
                        Copy
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={handleDownloadCsv}
                data-testid="bulk-feedback-links-download-csv"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-primary)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  cursor: 'pointer',
                  fontWeight: 'var(--weight-medium)',
                }}
              >
                Download CSV
              </button>
              <button
                type="button"
                onClick={onClose}
                data-testid="bulk-feedback-links-done"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'transparent',
                  color: 'var(--color-text-secondary)',
                  cursor: 'pointer',
                }}
              >
                Done
              </button>
            </div>
          </div>
        ) : (
          <>
            <div
              data-testid="bulk-feedback-links-warning"
              style={{
                marginBottom: 'var(--space-4)',
                padding: '8px 12px',
                borderRadius: 'var(--radius-medium)',
                border: '1px solid var(--color-alert)',
                backgroundColor: 'var(--color-alert-muted)',
                color: 'var(--color-alert)',
                fontSize: 'var(--text-small)',
              }}
            >
              Anyone with these links can submit feedback.
            </div>

            {/* Template */}
            <div style={{ marginBottom: '12px' }}>
              <label htmlFor="bfl-template" style={labelStyle}>Template</label>
              <select
                id="bfl-template"
                value={templateId}
                onChange={(e) => setTemplateId(e.target.value)}
                data-testid="bulk-feedback-links-template-select"
                style={inputStyle}
              >
                <option value="">Select a template…</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>{t.title}</option>
                ))}
              </select>
            </div>

            {/* Expiry + label */}
            <div style={{ display: 'flex', gap: '12px', marginBottom: '12px', flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 140px' }}>
                <label htmlFor="bfl-expiry" style={labelStyle}>Expires in (days, max {MAX_DAYS})</label>
                <input
                  id="bfl-expiry"
                  type="number"
                  min={1}
                  max={MAX_DAYS}
                  value={expiresInDays}
                  onChange={(e) => {
                    const v = Number(e.target.value);
                    setExpiresInDays(Number.isNaN(v) ? DEFAULT_DAYS : Math.min(Math.max(v, 1), MAX_DAYS));
                  }}
                  data-testid="bulk-feedback-links-expiry"
                  style={inputStyle}
                />
              </div>
              <div style={{ flex: '1 1 140px' }}>
                <label htmlFor="bfl-label" style={labelStyle}>Internal label (optional)</label>
                <input
                  id="bfl-label"
                  type="text"
                  value={label}
                  onChange={(e) => setLabel(e.target.value)}
                  placeholder="e.g. Q1 360 review"
                  data-testid="bulk-feedback-links-label"
                  style={inputStyle}
                />
              </div>
            </div>

            {/* People selection */}
            <div style={{ marginBottom: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span style={{ ...labelStyle, marginBottom: 0 }}>Select people ({selectedIds.size} selected)</span>
                <button
                  type="button"
                  onClick={toggleAll}
                  data-testid="bulk-feedback-links-select-all"
                  style={{
                    padding: '2px 10px',
                    fontSize: 'var(--text-caption)',
                    fontFamily: 'var(--font-mono)',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-medium)',
                    backgroundColor: 'transparent',
                    color: 'var(--color-text-secondary)',
                    cursor: 'pointer',
                  }}
                >
                  {selectedIds.size === people.length && people.length > 0 ? 'Clear all' : 'Select all'}
                </button>
              </div>
              <div
                style={{
                  maxHeight: '200px',
                  overflowY: 'auto',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  padding: 'var(--space-2)',
                }}
              >
                {people.length === 0 ? (
                  <div style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-small)', padding: '4px' }}>
                    No people to select.
                  </div>
                ) : (
                  people.map((p) => (
                    <label
                      key={p.id}
                      style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px', cursor: 'pointer', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}
                    >
                      <input
                        type="checkbox"
                        checked={selectedIds.has(p.id)}
                        onChange={() => toggleSelect(p.id)}
                        data-testid={`bulk-feedback-links-person-${p.id}`}
                        style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
                      />
                      {p.name}
                    </label>
                  ))
                )}
              </div>
            </div>

            {error && (
              <div
                data-testid="bulk-feedback-links-error"
                style={{
                  marginBottom: '12px',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-medium)',
                  border: '1px solid var(--color-alert)',
                  backgroundColor: 'var(--color-alert-muted)',
                  color: 'var(--color-alert)',
                  fontSize: 'var(--text-small)',
                }}
              >
                {error}
              </div>
            )}

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={!canSubmit}
                data-testid="bulk-feedback-links-submit"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: 'none',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  cursor: canSubmit ? 'pointer' : 'not-allowed',
                  fontWeight: 'var(--weight-medium)',
                  opacity: canSubmit ? 1 : 0.6,
                }}
              >
                {submitting ? 'Creating...' : 'Create links'}
              </button>
              <button
                type="button"
                onClick={onClose}
                data-testid="bulk-feedback-links-cancel"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'transparent',
                  color: 'var(--color-text-secondary)',
                  cursor: 'pointer',
                }}
              >
                Cancel
              </button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
