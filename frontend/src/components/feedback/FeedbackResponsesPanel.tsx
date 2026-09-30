'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  FeedbackResponseItem,
  FeedbackResponseStatus,
  FeedbackConversionType,
} from '@/types/feedback';
import {
  listFeedbackResponses,
  updateFeedbackResponse,
  deleteFeedbackResponse,
  bulkUpdateFeedbackResponses,
  convertFeedbackResponse,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface FeedbackResponsesPanelProps {
  personId: string;
}

type StatusFilter = 'ALL' | FeedbackResponseStatus;

const CONVERT_OPTIONS: { value: FeedbackConversionType; label: string }[] = [
  { value: 'KUDO', label: 'Kudo' },
  { value: 'QUICK_NOTE', label: 'Quick Note' },
  { value: 'ACTION_ITEM', label: 'Action Item' },
];

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const smallBtn = (variant: 'default' | 'primary' | 'alert' = 'default'): React.CSSProperties => ({
  padding: '4px 10px',
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  borderRadius: 'var(--radius-medium)',
  cursor: 'pointer',
  border:
    variant === 'alert'
      ? '1px solid var(--color-alert)'
      : variant === 'primary'
      ? '1px solid var(--color-primary)'
      : '1px solid var(--color-border)',
  backgroundColor: variant === 'primary' ? 'var(--color-primary)' : 'transparent',
  color:
    variant === 'primary'
      ? 'var(--color-bg-base)'
      : variant === 'alert'
      ? 'var(--color-alert)'
      : 'var(--color-text-secondary)',
});

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return iso;
  }
}

/** Renders a single answer value: rating as N/5, likert label, or text. */
function AnswerValue({ ratingValue, textValue }: { ratingValue?: number | null; textValue?: string | null }) {
  if (ratingValue != null) {
    return <span>{ratingValue}/5</span>;
  }
  if (textValue) {
    return <span>{textValue}</span>;
  }
  return <span style={{ color: 'var(--color-text-muted)' }}>—</span>;
}

const LONG_TEXT_THRESHOLD = 280;

function ResponseCard({
  response,
  selected,
  onToggleSelect,
  onApprove,
  onFlag,
  onPin,
  onDelete,
  onConvert,
  busy,
}: {
  response: FeedbackResponseItem;
  selected: boolean;
  onToggleSelect: (id: string) => void;
  onApprove: (id: string) => void;
  onFlag: (r: FeedbackResponseItem) => void;
  onPin: (r: FeedbackResponseItem) => void;
  onDelete: (id: string) => void;
  onConvert: (id: string, type: FeedbackConversionType) => void;
  busy: boolean;
}) {
  const combinedLength = useMemo(
    () =>
      (response.additionalComments?.length ?? 0) +
      response.answers.reduce((n, a) => n + (a.textValue?.length ?? 0), 0),
    [response]
  );
  const [expanded, setExpanded] = useState(false);
  const collapsible = combinedLength > LONG_TEXT_THRESHOLD;
  const [convertType, setConvertType] = useState<FeedbackConversionType>('KUDO');

  const submitter = response.anonymous || !response.submitterName ? 'Anonymous' : response.submitterName;

  return (
    <div
      data-testid={`feedback-response-${response.id}`}
      style={{
        padding: 'var(--space-3)',
        border: `1px solid ${response.flagged ? 'var(--color-alert)' : 'var(--color-border)'}`,
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-elevated)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <input
            type="checkbox"
            checked={selected}
            onChange={() => onToggleSelect(response.id)}
            data-testid={`feedback-response-${response.id}-select`}
            aria-label={`Select response from ${submitter}`}
            style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
          />
          <div>
            <div style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', fontWeight: 'var(--weight-medium)' }}>
              {submitter}
              {response.pinned && (
                <span data-testid={`feedback-response-${response.id}-pinned-badge`} style={{ marginLeft: '6px' }}>📌</span>
              )}
            </div>
            <div style={labelStyle}>{formatDate(response.createdAt)}</div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
          <span
            data-testid={`feedback-response-${response.id}-status`}
            style={{
              ...labelStyle,
              padding: '2px 8px',
              borderRadius: 'var(--radius-full)',
              border: '1px solid var(--color-border)',
              color: response.status === 'APPROVED' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            }}
          >
            {response.status}
          </span>
        </div>
      </div>

      {/* Answers */}
      <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
        {response.answers.map((a, i) => {
          const isLongText = (a.textValue?.length ?? 0) > LONG_TEXT_THRESHOLD;
          const show = !collapsible || expanded || !isLongText;
          return (
            <div key={`${a.questionId}-${i}`} data-testid={`feedback-answer-${response.id}-${a.questionId}`}>
              <span style={labelStyle}>{a.questionId}</span>{' '}
              <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>
                {show ? <AnswerValue ratingValue={a.ratingValue} textValue={a.textValue} /> : <em style={{ color: 'var(--color-text-muted)' }}>…</em>}
              </span>
            </div>
          );
        })}
      </div>

      {response.additionalComments && (
        <div style={{ marginTop: '8px' }}>
          <div style={labelStyle}>Additional comments</div>
          <p
            data-testid={`feedback-response-${response.id}-comments`}
            style={{
              margin: '2px 0 0',
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-primary)',
              whiteSpace: 'pre-wrap',
              lineHeight: 1.5,
            }}
          >
            {collapsible && !expanded
              ? `${response.additionalComments.slice(0, LONG_TEXT_THRESHOLD)}…`
              : response.additionalComments}
          </p>
        </div>
      )}

      {collapsible && (
        <button
          type="button"
          onClick={() => setExpanded((e) => !e)}
          data-testid={`feedback-response-${response.id}-toggle`}
          style={{ ...smallBtn(), marginTop: '8px' }}
        >
          {expanded ? 'Show less' : 'Show more'}
        </button>
      )}

      {response.convertedToType && (
        <div data-testid={`feedback-response-${response.id}-converted`} style={{ ...labelStyle, marginTop: '8px' }}>
          Converted to {response.convertedToType}
        </div>
      )}

      {/* Actions */}
      <div style={{ display: 'flex', gap: '6px', marginTop: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
        {response.status !== 'APPROVED' && (
          <button
            type="button"
            onClick={() => onApprove(response.id)}
            disabled={busy}
            data-testid={`feedback-response-${response.id}-approve`}
            style={smallBtn('primary')}
          >
            Approve
          </button>
        )}
        <button
          type="button"
          onClick={() => onFlag(response)}
          disabled={busy}
          data-testid={`feedback-response-${response.id}-flag`}
          style={smallBtn(response.flagged ? 'alert' : 'default')}
        >
          {response.flagged ? 'Unflag' : 'Flag'}
        </button>
        <button
          type="button"
          onClick={() => onPin(response)}
          disabled={busy}
          data-testid={`feedback-response-${response.id}-pin`}
          style={smallBtn()}
        >
          {response.pinned ? 'Unpin' : 'Pin'}
        </button>
        <button
          type="button"
          onClick={() => onDelete(response.id)}
          disabled={busy}
          data-testid={`feedback-response-${response.id}-delete`}
          style={smallBtn('alert')}
        >
          Delete
        </button>

        <div style={{ display: 'inline-flex', gap: '4px', alignItems: 'center', marginLeft: 'auto' }}>
          <select
            value={convertType}
            onChange={(e) => setConvertType(e.target.value as FeedbackConversionType)}
            data-testid={`feedback-response-${response.id}-convert-select`}
            aria-label="Convert response to"
            style={{
              padding: '4px 8px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
            }}
          >
            {CONVERT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => onConvert(response.id, convertType)}
            disabled={busy}
            data-testid={`feedback-response-${response.id}-convert`}
            style={smallBtn()}
          >
            Convert
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * Manager view of feedback responses for a person: filter by status, toggle
 * flagged-only, per-response moderation (approve/flag/pin/delete/convert), and
 * bulk approve/delete. Refetches after each mutation.
 */
export default function FeedbackResponsesPanel({ personId }: FeedbackResponsesPanelProps) {
  const { getToken } = useStableToken();
  const [responses, setResponses] = useState<FeedbackResponseItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [flaggedOnly, setFlaggedOnly] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [busy, setBusy] = useState(false);

  const fetchResponses = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await listFeedbackResponses(token, personId, {
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        flagged: flaggedOnly ? true : undefined,
      });
      setResponses(result);
      setSelectedIds(new Set());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load responses');
    } finally {
      setLoading(false);
    }
  }, [getToken, personId, statusFilter, flaggedOnly]);

  useEffect(() => {
    fetchResponses();
  }, [fetchResponses]);

  const pendingCount = useMemo(() => responses.filter((r) => r.status === 'PENDING').length, [responses]);

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleApprove = async (id: string) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    try {
      await updateFeedbackResponse(token, id, { approve: true });
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to approve');
    } finally {
      setBusy(false);
    }
  };

  const handleFlag = async (r: FeedbackResponseItem) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    try {
      await updateFeedbackResponse(token, r.id, { flagged: !r.flagged });
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update flag');
    } finally {
      setBusy(false);
    }
  };

  const handlePin = async (r: FeedbackResponseItem) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    try {
      await updateFeedbackResponse(token, r.id, { pinned: !r.pinned });
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update pin');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    if (!window.confirm('Delete this feedback response? This cannot be undone.')) return;
    setBusy(true);
    try {
      await deleteFeedbackResponse(token, id);
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    } finally {
      setBusy(false);
    }
  };

  const handleConvert = async (id: string, type: FeedbackConversionType) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    try {
      await convertFeedbackResponse(token, id, { type });
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to convert');
    } finally {
      setBusy(false);
    }
  };

  const handleBulk = async (action: 'APPROVE' | 'DELETE') => {
    const token = getToken();
    if (!token || selectedIds.size === 0) return;
    if (action === 'DELETE' && !window.confirm(`Delete ${selectedIds.size} selected response(s)? This cannot be undone.`)) return;
    setBusy(true);
    try {
      await bulkUpdateFeedbackResponses(token, { responseIds: Array.from(selectedIds), action });
      await fetchResponses();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update selected responses');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div data-testid="feedback-responses-panel">
      {/* Toolbar */}
      <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap', marginBottom: 'var(--space-4)' }}>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
          data-testid="feedback-responses-status-filter"
          aria-label="Filter responses by status"
          style={{
            padding: '6px 10px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        >
          <option value="ALL">All</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
        </select>

        <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={flaggedOnly}
            onChange={(e) => setFlaggedOnly(e.target.checked)}
            data-testid="feedback-responses-flagged-toggle"
            style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
          />
          Flagged only
        </label>

        <span data-testid="feedback-responses-pending-count" style={labelStyle}>
          {pendingCount} new
        </span>

        <div style={{ display: 'inline-flex', gap: '6px', marginLeft: 'auto' }}>
          <button
            type="button"
            onClick={() => handleBulk('APPROVE')}
            disabled={busy || selectedIds.size === 0}
            data-testid="feedback-responses-bulk-approve"
            style={{ ...smallBtn('primary'), opacity: selectedIds.size === 0 ? 0.5 : 1 }}
          >
            Approve selected
          </button>
          <button
            type="button"
            onClick={() => handleBulk('DELETE')}
            disabled={busy || selectedIds.size === 0}
            data-testid="feedback-responses-bulk-delete"
            style={{ ...smallBtn('alert'), opacity: selectedIds.size === 0 ? 0.5 : 1 }}
          >
            Delete selected
          </button>
        </div>
      </div>

      {error && (
        <div
          data-testid="feedback-responses-error"
          style={{
            marginBottom: 'var(--space-3)',
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

      {loading ? (
        <div data-testid="feedback-responses-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading responses...
        </div>
      ) : responses.length === 0 ? (
        <div data-testid="feedback-responses-empty" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          No feedback responses yet.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          {responses.map((r) => (
            <ResponseCard
              key={r.id}
              response={r}
              selected={selectedIds.has(r.id)}
              onToggleSelect={toggleSelect}
              onApprove={handleApprove}
              onFlag={handleFlag}
              onPin={handlePin}
              onDelete={handleDelete}
              onConvert={handleConvert}
              busy={busy}
            />
          ))}
        </div>
      )}
    </div>
  );
}
