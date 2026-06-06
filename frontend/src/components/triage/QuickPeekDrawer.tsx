'use client';

import { useEffect, useState, useCallback } from 'react';
import { X } from 'lucide-react';
import { TriageItem } from '@/types/triage';
import { getPerson, listOneOnOneEntries, listActionItemsByPerson, listKudosByPerson } from '@/lib/api-client';
import { Person } from '@/types/person';
import { OneOnOneEntry } from '@/types/one-on-one';
import { ActionItem } from '@/types/action-item';
import { Kudos } from '@/types/kudos';

interface QuickPeekDrawerProps {
  item: TriageItem;
  token: string;
  onClose: () => void;
}

export default function QuickPeekDrawer({ item, token, onClose }: QuickPeekDrawerProps) {
  const [person, setPerson] = useState<Person | null>(null);
  const [lastEntry, setLastEntry] = useState<OneOnOneEntry | null>(null);
  const [openActions, setOpenActions] = useState<ActionItem[]>([]);
  const [recentKudos, setRecentKudos] = useState<Kudos[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchContext = useCallback(async () => {
    setLoading(true);
    try {
      const [personData, entriesData, actionsData, kudosData] = await Promise.all([
        getPerson(token, item.personId),
        listOneOnOneEntries(token, item.personId, 0, 1),
        listActionItemsByPerson(token, item.personId, { status: 'OPEN', size: 5 }),
        listKudosByPerson(token, item.personId, { size: 3 }),
      ]);
      setPerson(personData);
      setLastEntry(entriesData.content?.[0] || null);
      setOpenActions(actionsData.content || []);
      setRecentKudos(kudosData.content || []);
    } catch {
      // Silently handle — drawer shows what it can
    } finally {
      setLoading(false);
    }
  }, [token, item.personId]);

  useEffect(() => {
    fetchContext();
  }, [fetchContext]);

  // Close on Escape
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  const moraleColor = person?.moraleStatus
    ? { GREEN: 'var(--color-morale-green)', YELLOW: 'var(--color-morale-yellow)', RED: 'var(--color-morale-red)', UNKNOWN: 'var(--color-morale-unknown)' }[person.moraleStatus]
    : 'var(--color-morale-unknown)';

  return (
    <div
      data-testid="quick-peek-drawer"
      style={{
        position: 'fixed',
        top: 0,
        right: 0,
        bottom: 0,
        width: '380px',
        maxWidth: '90vw',
        background: 'var(--glass-elevated-bg)',
        backdropFilter: 'var(--glass-elevated-blur)',
        borderLeft: 'var(--glass-elevated-border)',
        boxShadow: '-4px 0 24px rgba(0,0,0,0.3), var(--glow-primary)',
        zIndex: 100,
        display: 'flex',
        flexDirection: 'column',
        animation: 'slideInRight 0.2s ease-out',
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: 'var(--space-4)',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <span
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              backgroundColor: moraleColor,
              flexShrink: 0,
            }}
            aria-label={`Morale: ${person?.moraleStatus || 'unknown'}`}
          />
          <h3
            style={{
              margin: 0,
              fontSize: 'var(--text-body-large)',
              fontFamily: 'var(--font-heading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-text-primary)',
            }}
          >
            {person?.preferredName || person?.name || item.personName}
          </h3>
        </div>
        <button
          onClick={onClose}
          aria-label="Close drawer"
          data-testid="drawer-close-btn"
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--color-text-muted)',
            cursor: 'pointer',
            padding: '4px',
            borderRadius: 'var(--radius-small)',
          }}
        >
          <X size={18} />
        </button>
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto', padding: 'var(--space-4)' }}>
        {loading ? (
          <div style={{ color: 'var(--color-text-muted)', fontSize: 'var(--text-small)', fontFamily: 'var(--font-mono)' }}>
            Loading context...
          </div>
        ) : (
          <>
            {/* Person info */}
            {person?.roleTitle && (
              <p style={{ margin: '0 0 var(--space-4) 0', fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)' }}>
                {person.roleTitle}
              </p>
            )}

            {/* Last 1:1 */}
            <section style={{ marginBottom: 'var(--space-5)' }}>
              <h4 style={sectionHeadingStyle}>Last 1:1</h4>
              {lastEntry ? (
                <div style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-primary)' }}>
                  <span style={{ color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
                    {new Date(lastEntry.meetingDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                  </span>
                  {lastEntry.sensitive ? (
                    <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
                      Sensitive content hidden
                    </p>
                  ) : lastEntry.outcomesMarkdown ? (
                    <p style={{ margin: '4px 0 0', whiteSpace: 'pre-wrap', lineHeight: 1.4 }}>
                      {lastEntry.outcomesMarkdown.slice(0, 200)}
                      {lastEntry.outcomesMarkdown.length > 200 && '...'}
                    </p>
                  ) : (
                    <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)' }}>No outcomes recorded</p>
                  )}
                </div>
              ) : (
                <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', margin: 0 }}>
                  No 1:1 entries yet
                </p>
              )}
            </section>

            {/* Open Action Items */}
            <section style={{ marginBottom: 'var(--space-5)' }}>
              <h4 style={sectionHeadingStyle}>Open Action Items ({openActions.length})</h4>
              {openActions.length === 0 ? (
                <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', margin: 0 }}>None</p>
              ) : (
                <ul style={{ margin: 0, padding: '0 0 0 16px', listStyle: 'disc' }}>
                  {openActions.map((a) => (
                    <li
                      key={a.id}
                      style={{
                        fontSize: 'var(--text-small)',
                        color: 'var(--color-text-primary)',
                        marginBottom: '4px',
                      }}
                    >
                      {a.title}
                      {a.dueDate && (
                        <span style={{ color: 'var(--color-text-muted)', marginLeft: '6px', fontFamily: 'var(--font-mono)', fontSize: 'var(--text-caption)' }}>
                          due {a.dueDate}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </section>

            {/* Recent Kudos */}
            <section>
              <h4 style={sectionHeadingStyle}>Recent Kudos</h4>
              {recentKudos.length === 0 ? (
                <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', margin: 0 }}>None yet</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {recentKudos.map((k) => (
                    <div
                      key={k.id}
                      style={{
                        fontSize: 'var(--text-small)',
                        padding: 'var(--space-2) var(--space-3)',
                        borderRadius: 'var(--radius-small)',
                        border: '1px solid var(--color-border)',
                        backgroundColor: 'var(--color-bg-surface)',
                      }}
                    >
                      <p style={{ margin: 0, color: 'var(--color-text-primary)', lineHeight: 1.4 }}>
                        {k.text.slice(0, 120)}{k.text.length > 120 && '...'}
                      </p>
                      <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
                        {k.date}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  );
}

const sectionHeadingStyle: React.CSSProperties = {
  margin: '0 0 var(--space-2) 0',
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  fontWeight: 500,
  color: 'var(--color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};
