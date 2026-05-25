'use client';

import { useState, useEffect, useCallback } from 'react';
import { getAuditLog } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { AuditLogEntry, AuditAction, AuditEntityType } from '@/types/audit-log';
import Pagination from '@/components/Pagination';

const ENTITY_TYPE_LABELS: Record<AuditEntityType, string> = {
  PERSON: 'Person',
  ONE_ON_ONE_ENTRY: '1:1 Entry',
  ONE_ON_ONE_SERIES: '1:1 Series',
  ACTION_ITEM: 'Action Item',
  PDP_GOAL: 'PDP Goal',
  PDP_UPDATE: 'PDP Update',
  KUDOS: 'Kudos',
  QUICK_NOTE: 'Quick Note',
  USER_SETTINGS: 'Settings',
  WORKSPACE: 'Workspace',
  STRATEGY_GOAL: 'Strategy Goal',
};

const ACTION_LABELS: Record<AuditAction, string> = {
  CREATE: 'Created',
  UPDATE: 'Updated',
  DELETE: 'Deleted',
  RESTORE: 'Restored',
  LINK: 'Linked',
  UNLINK: 'Unlinked',
};

const ACTION_COLORS: Record<AuditAction, string> = {
  CREATE: 'var(--color-success, #22c55e)',
  UPDATE: 'var(--color-primary)',
  DELETE: 'var(--color-danger, #ef4444)',
  RESTORE: 'var(--color-warning, #f59e0b)',
  LINK: 'var(--color-primary)',
  UNLINK: 'var(--color-danger, #ef4444)',
};

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}

export default function AuditLogPage() {
  const { getToken, isAuthenticated } = useStableToken();
  const [entries, setEntries] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [entityTypeFilter, setEntityTypeFilter] = useState<AuditEntityType | ''>('');
  const [actionFilter, setActionFilter] = useState<AuditAction | ''>('');

  const fetchAuditLog = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      setLoading(true);
      const data = await getAuditLog(token, {
        entityType: entityTypeFilter || undefined,
        action: actionFilter || undefined,
        page,
        size: 20,
      });
      setEntries(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch {
      setError('Failed to load audit log');
    } finally {
      setLoading(false);
    }
  }, [getToken, page, entityTypeFilter, actionFilter]);

  useEffect(() => {
    fetchAuditLog();
  }, [fetchAuditLog]);

  return (
    <main
      data-testid="audit-log-page"
      style={{
        maxWidth: '800px',
        margin: '0 auto',
        padding: '32px 24px',
      }}
    >
        <h1
          style={{
            margin: '0 0 24px 0',
            fontSize: '24px',
            fontFamily: 'var(--font-heading)',
            fontWeight: 700,
            color: 'var(--color-text-primary)',
          }}
        >
          Audit Log
        </h1>

        <div
          style={{
            display: 'flex',
            gap: '12px',
            marginBottom: '20px',
            flexWrap: 'wrap',
          }}
        >
          <select
            data-testid="entity-type-filter"
            value={entityTypeFilter}
            onChange={(e) => {
              setEntityTypeFilter(e.target.value as AuditEntityType | '');
              setPage(0);
            }}
            aria-label="Filter by entity type"
          >
            <option value="">All types</option>
            {Object.entries(ENTITY_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>

          <select
            data-testid="action-filter"
            value={actionFilter}
            onChange={(e) => {
              setActionFilter(e.target.value as AuditAction | '');
              setPage(0);
            }}
            aria-label="Filter by action"
          >
            <option value="">All actions</option>
            {Object.entries(ACTION_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        {loading && (
          <p
            data-testid="audit-log-loading"
            style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '48px 0' }}
          >
            Loading audit log...
          </p>
        )}

        {error && (
          <p
            data-testid="audit-log-error"
            style={{ textAlign: 'center', color: 'var(--color-danger, #ef4444)', padding: '48px 0' }}
          >
            {error}
          </p>
        )}

        {!loading && !error && entries.length === 0 && (
          <div
            data-testid="audit-log-empty"
            style={{
              textAlign: 'center',
              padding: '64px 24px',
              color: 'var(--color-text-secondary)',
            }}
          >
            <p style={{ fontSize: '32px', marginBottom: '12px' }}>📋</p>
            <p style={{ fontSize: '15px', fontWeight: 500 }}>No audit log entries</p>
            <p style={{ fontSize: '13px', marginTop: '8px' }}>
              Actions you perform (create, update, delete) will be recorded here for your traceability.
            </p>
          </div>
        )}

        {!loading && !error && entries.length > 0 && (
          <>
            <div
              style={{
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium, 8px)',
                overflow: 'hidden',
              }}
            >
              {entries.map((entry) => (
                <div
                  key={entry.id}
                  data-testid="audit-log-entry"
                  style={{
                    padding: '14px 16px',
                    borderBottom: '1px solid var(--color-border)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                  }}
                >
                  <span
                    style={{
                      display: 'inline-block',
                      padding: '2px 8px',
                      fontSize: '11px',
                      fontWeight: 600,
                      borderRadius: '4px',
                      color: ACTION_COLORS[entry.action],
                      backgroundColor: `${ACTION_COLORS[entry.action]}18`,
                      border: `1px solid ${ACTION_COLORS[entry.action]}40`,
                      textTransform: 'uppercase',
                      letterSpacing: '0.5px',
                      flexShrink: 0,
                    }}
                  >
                    {ACTION_LABELS[entry.action]}
                  </span>

                  <span
                    style={{
                      display: 'inline-block',
                      padding: '2px 6px',
                      fontSize: '11px',
                      fontWeight: 500,
                      borderRadius: '4px',
                      color: 'var(--color-text-secondary)',
                      backgroundColor: 'var(--color-surface-elevated, var(--color-surface))',
                      border: '1px solid var(--color-border)',
                      flexShrink: 0,
                    }}
                  >
                    {ENTITY_TYPE_LABELS[entry.entityType]}
                  </span>

                  <span
                    style={{
                      flex: 1,
                      fontSize: '13px',
                      color: 'var(--color-text-primary)',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {entry.summary}
                  </span>

                  <span
                    style={{
                      fontSize: '12px',
                      color: 'var(--color-text-secondary)',
                      flexShrink: 0,
                    }}
                    title={new Date(entry.createdAt).toLocaleString()}
                  >
                    {formatRelativeTime(entry.createdAt)}
                  </span>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div style={{ marginTop: '24px' }}>
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setPage}
                />
              </div>
            )}

            <p
              style={{
                marginTop: '12px',
                fontSize: '12px',
                color: 'var(--color-text-secondary)',
                textAlign: 'center',
              }}
            >
              {totalElements} entr{totalElements !== 1 ? 'ies' : 'y'} total
            </p>
          </>
        )}
      </main>
  );
}
