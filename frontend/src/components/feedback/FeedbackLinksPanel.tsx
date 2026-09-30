'use client';

import { useCallback, useEffect, useState } from 'react';
import { FeedbackLink, FeedbackLinkStatus } from '@/types/feedback';
import { listFeedbackLinks, revokeFeedbackLink, extendFeedbackLink } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface FeedbackLinksPanelProps {
  personId: string;
  /** Bumping this value triggers a refetch (e.g. after creating a link elsewhere). */
  refreshKey?: number;
}

const labelStyle: React.CSSProperties = {
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const statusColor: Record<FeedbackLinkStatus, string> = {
  ACTIVE: 'var(--color-primary)',
  EXPIRED: 'var(--color-text-muted)',
  REVOKED: 'var(--color-alert)',
};

function formatDate(iso?: string | null): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return iso;
  }
}

function publicUrl(token: string): string {
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  return `${origin}/f/${token}`;
}

function LinkRow({
  link,
  onRevoke,
  onExtend,
  busy,
}: {
  link: FeedbackLink;
  onRevoke: (id: string) => void;
  onExtend: (id: string, days: number) => void;
  busy: boolean;
}) {
  const [copied, setCopied] = useState(false);
  const [extendDays, setExtendDays] = useState(14);

  const handleCopy = async () => {
    const url = publicUrl(link.token);
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard may be unavailable; ignore silently.
    }
  };

  return (
    <div
      data-testid={`feedback-link-${link.id}`}
      style={{
        padding: 'var(--space-3)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-elevated)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', fontWeight: 'var(--weight-medium)' }}>
            {link.title}
            {link.label && (
              <span style={{ ...labelStyle, marginLeft: '8px' }}>({link.label})</span>
            )}
          </span>
          <span style={labelStyle}>
            {link.submissionCount} submission{link.submissionCount === 1 ? '' : 's'} · last {formatDate(link.lastSubmissionAt)}
          </span>
        </div>
        <span
          data-testid={`feedback-link-${link.id}-status`}
          style={{
            ...labelStyle,
            padding: '2px 8px',
            borderRadius: 'var(--radius-full)',
            border: `1px solid ${statusColor[link.status]}`,
            color: statusColor[link.status],
          }}
        >
          {link.status}
        </span>
      </div>

      <div style={{ ...labelStyle, marginTop: '6px' }}>
        Expires {formatDate(link.expiresAt)}
      </div>

      <div style={{ display: 'flex', gap: '6px', marginTop: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
        <button
          type="button"
          onClick={handleCopy}
          data-testid={`feedback-link-${link.id}-copy`}
          style={{
            padding: '4px 10px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'transparent',
            color: 'var(--color-primary)',
            cursor: 'pointer',
          }}
        >
          {copied ? 'Copied!' : 'Copy link'}
        </button>

        {link.status === 'ACTIVE' && (
          <>
            <div style={{ display: 'inline-flex', gap: '4px', alignItems: 'center' }}>
              <input
                type="number"
                min={1}
                max={90}
                value={extendDays}
                onChange={(e) => setExtendDays(Number(e.target.value))}
                data-testid={`feedback-link-${link.id}-extend-days`}
                aria-label="Days to extend"
                style={{
                  width: '64px',
                  padding: '4px 8px',
                  fontSize: 'var(--text-caption)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-bg-elevated)',
                  color: 'var(--color-text-primary)',
                }}
              />
              <button
                type="button"
                onClick={() => onExtend(link.id, extendDays)}
                disabled={busy}
                data-testid={`feedback-link-${link.id}-extend`}
                style={{
                  padding: '4px 10px',
                  fontSize: 'var(--text-caption)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'transparent',
                  color: 'var(--color-text-secondary)',
                  cursor: 'pointer',
                }}
              >
                Extend
              </button>
            </div>
            <button
              type="button"
              onClick={() => onRevoke(link.id)}
              disabled={busy}
              data-testid={`feedback-link-${link.id}-revoke`}
              style={{
                padding: '4px 10px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-alert)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'transparent',
                color: 'var(--color-alert)',
                cursor: 'pointer',
              }}
            >
              Revoke
            </button>
          </>
        )}
      </div>
    </div>
  );
}

/**
 * Lists the feedback links for a person. Managers can copy the public URL,
 * revoke active links, and extend their expiry.
 */
export default function FeedbackLinksPanel({ personId, refreshKey = 0 }: FeedbackLinksPanelProps) {
  const { getToken } = useStableToken();
  const [links, setLinks] = useState<FeedbackLink[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const fetchLinks = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await listFeedbackLinks(token, personId);
      setLinks(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load links');
    } finally {
      setLoading(false);
    }
  }, [getToken, personId]);

  useEffect(() => {
    fetchLinks();
  }, [fetchLinks, refreshKey]);

  const handleRevoke = async (id: string) => {
    const token = getToken();
    if (!token) return;
    if (!window.confirm('Revoke this feedback link? Anyone using it will no longer be able to submit.')) return;
    setBusy(true);
    try {
      await revokeFeedbackLink(token, id);
      await fetchLinks();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to revoke link');
    } finally {
      setBusy(false);
    }
  };

  const handleExtend = async (id: string, days: number) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    try {
      await extendFeedbackLink(token, id, days);
      await fetchLinks();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to extend link');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div data-testid="feedback-links-panel">
      <div
        data-testid="feedback-links-warning"
        style={{
          marginBottom: 'var(--space-4)',
          padding: '8px 12px',
          borderRadius: 'var(--radius-medium)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
          color: 'var(--color-text-secondary)',
          fontSize: 'var(--text-small)',
        }}
      >
        Anyone with this link can submit feedback.
      </div>

      {error && (
        <div
          data-testid="feedback-links-error"
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
        <div data-testid="feedback-links-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          Loading links...
        </div>
      ) : links.length === 0 ? (
        <div data-testid="feedback-links-empty" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
          No feedback links yet. Use “Create feedback link” above to generate a shareable link.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          {links.map((link) => (
            <LinkRow key={link.id} link={link} onRevoke={handleRevoke} onExtend={handleExtend} busy={busy} />
          ))}
        </div>
      )}
    </div>
  );
}
