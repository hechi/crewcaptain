'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { Person, MoraleStatus, PaginatedResponse } from '@/types/person';
import { OneOnOneEntry, OneOnOneSeries } from '@/types/one-on-one';
import {
  getPerson,
  updatePerson,
  deletePerson,
  setMorale,
  addRememberItem,
  removeRememberItem,
  reorderRememberItems,
  listOneOnOneEntries,
  getOneOnOneSeries,
  upsertOneOnOneSeries,
} from '@/lib/api-client';
import { UpsertSeriesRequest } from '@/types/one-on-one';
import PersonForm from '@/components/PersonForm';
import MoraleIndicator from '@/components/MoraleIndicator';
import RememberItemsList from '@/components/RememberItemsList';
import OneOnOneTimeline from '@/components/one-on-one/OneOnOneTimeline';
import SeriesConfigPanel from '@/components/one-on-one/SeriesConfigPanel';

type Tab = 'details' | 'one-on-ones';

export default function PersonDetailPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const params = useParams();
  const personId = params.id as string;

  const [person, setPerson] = useState<Person | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [moraleFormStatus, setMoraleFormStatus] = useState<MoraleStatus>('UNKNOWN');
  const [moraleNote, setMoraleNote] = useState('');

  // Tab state
  const [activeTab, setActiveTab] = useState<Tab>('details');

  // 1:1 state
  const [entries, setEntries] = useState<PaginatedResponse<OneOnOneEntry> | null>(null);
  const [entriesLoading, setEntriesLoading] = useState(false);
  const [entriesPage, setEntriesPage] = useState(0);
  const [series, setSeries] = useState<OneOnOneSeries | null>(null);
  const [showSeriesConfig, setShowSeriesConfig] = useState(false);
  const [seriesSaving, setSeriesSaving] = useState(false);

  const token = session?.accessToken as string;

  const fetchPerson = useCallback(async () => {
    if (status !== 'authenticated' || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await getPerson(token, personId);
      setPerson(result);
      setMoraleFormStatus(result.moraleStatus);
      setMoraleNote(result.moraleNote || '');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load person');
    } finally {
      setLoading(false);
    }
  }, [token, status, personId]);

  const fetchEntries = useCallback(async (page: number = 0) => {
    if (status !== 'authenticated' || !token) return;

    setEntriesLoading(true);
    try {
      const result = await listOneOnOneEntries(token, personId, page);
      setEntries(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load 1:1 entries');
    } finally {
      setEntriesLoading(false);
    }
  }, [token, status, personId]);

  const fetchSeries = useCallback(async () => {
    if (status !== 'authenticated' || !token) return;

    try {
      const result = await getOneOnOneSeries(token, personId);
      setSeries(result);
    } catch {
      // 404 means no series configured — that's fine
      setSeries(null);
    }
  }, [token, status, personId]);

  useEffect(() => {
    fetchPerson();
  }, [fetchPerson]);

  useEffect(() => {
    if (activeTab === 'one-on-ones') {
      fetchEntries(entriesPage);
      fetchSeries();
    }
  }, [activeTab, entriesPage, fetchEntries, fetchSeries]);

  const handleUpdate = async (data: {
    name: string;
    preferredName?: string;
    roleTitle?: string;
    timezone?: string;
    startDate?: string;
    email?: string;
    tags?: string[];
  }) => {
    try {
      const updated = await updatePerson(token, personId, data);
      setPerson(updated);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update person');
    }
  };

  const handleDelete = async () => {
    try {
      await deletePerson(token, personId);
      router.push('/people');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete person');
    }
  };

  const handleMoraleUpdate = async () => {
    try {
      const updated = await setMorale(token, personId, {
        status: moraleFormStatus,
        note: moraleNote || undefined,
      });
      setPerson(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update morale');
    }
  };

  const handleAddRememberItem = async (text: string) => {
    try {
      const items = await addRememberItem(token, personId, text);
      setPerson((prev) => prev ? { ...prev, pinnedRememberItems: items } : prev);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add remember item');
    }
  };

  const handleRemoveRememberItem = async (itemId: string) => {
    try {
      const items = await removeRememberItem(token, personId, itemId);
      setPerson((prev) => prev ? { ...prev, pinnedRememberItems: items } : prev);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove remember item');
    }
  };

  const handleReorderRememberItems = async (orderedIds: string[]) => {
    try {
      const items = await reorderRememberItems(token, personId, orderedIds);
      setPerson((prev) => prev ? { ...prev, pinnedRememberItems: items } : prev);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reorder remember items');
    }
  };

  const handlePageChange = (page: number) => {
    setEntriesPage(page);
  };

  const handleStartOneOnOne = () => {
    router.push(`/people/${personId}/one-on-ones/new`);
  };

  const handleSaveSeriesConfig = async (data: UpsertSeriesRequest) => {
    setSeriesSaving(true);
    try {
      const updated = await upsertOneOnOneSeries(token, personId, data);
      setSeries(updated);
      setShowSeriesConfig(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save series configuration');
    } finally {
      setSeriesSaving(false);
    }
  };

  if (status === 'loading') {
    return <div data-testid="loading">Loading...</div>;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  if (loading) {
    return <div data-testid="loading">Loading...</div>;
  }

  if (error && !person) {
    return (
      <div data-testid="error-message" style={{ color: 'var(--color-error)', padding: 'var(--space-6)' }}>
        {error}
      </div>
    );
  }

  if (!person) {
    return <div data-testid="not-found">Person not found.</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: 'var(--space-6)' }}>
      <button
        type="button"
        onClick={() => router.push('/people')}
        style={{
          marginBottom: 'var(--space-4)',
          padding: '6px 12px',
          background: 'var(--color-neutral-surface)',
          border: '1px solid var(--color-neutral-border)',
          borderRadius: 'var(--radius-medium)',
          cursor: 'pointer',
          fontSize: 'var(--text-body)',
          color: 'var(--color-neutral-text-secondary)',
        }}
      >
        ← Back to People
      </button>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-error)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 'var(--text-h2)', fontFamily: 'var(--font-heading)', fontWeight: 'var(--weight-bold)', color: 'var(--color-primary)' }}>{person.name}</h1>
          {person.roleTitle && (
            <p style={{ margin: '4px 0 0', fontSize: '16px', color: 'var(--color-neutral-text-muted)' }}>{person.roleTitle}</p>
          )}
        </div>
        <MoraleIndicator moraleStatus={person.moraleStatus} />
      </div>

      {/* Tabs */}
      <div
        data-testid="person-tabs"
        style={{
          display: 'flex',
          gap: '0',
          borderBottom: '2px solid var(--color-neutral-border)',
          marginBottom: 'var(--space-6)',
        }}
        role="tablist"
        aria-label="Person sections"
      >
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'details'}
          aria-controls="tab-panel-details"
          onClick={() => setActiveTab('details')}
          data-testid="tab-details"
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: activeTab === 'details' ? '2px solid var(--color-secondary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'details' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'details' ? 'var(--color-secondary-dark)' : 'var(--color-neutral-text-muted)',
            cursor: 'pointer',
            marginBottom: '-2px',
          }}
        >
          Details
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'one-on-ones'}
          aria-controls="tab-panel-one-on-ones"
          onClick={() => setActiveTab('one-on-ones')}
          data-testid="tab-one-on-ones"
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: activeTab === 'one-on-ones' ? '2px solid var(--color-secondary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'one-on-ones' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'one-on-ones' ? 'var(--color-secondary-dark)' : 'var(--color-neutral-text-muted)',
            cursor: 'pointer',
            marginBottom: '-2px',
          }}
        >
          1:1s
        </button>
      </div>

      {/* Details Tab Panel */}
      {activeTab === 'details' && (
        <div id="tab-panel-details" role="tabpanel" aria-labelledby="tab-details">
          {/* Edit / Delete controls */}
          <div style={{ display: 'flex', gap: 'var(--space-3)', marginBottom: 'var(--space-6)' }}>
            <button
              type="button"
              onClick={() => setIsEditing(!isEditing)}
              data-testid="edit-button"
              style={{
                padding: '8px 16px',
                border: '1px solid var(--color-neutral-border)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: 'var(--color-neutral-surface)',
                fontSize: 'var(--text-body)',
                color: 'var(--color-primary)',
                fontWeight: 'var(--weight-medium)',
              }}
            >
              {isEditing ? 'Cancel Edit' : 'Edit'}
            </button>
            <button
              type="button"
              onClick={() => setShowDeleteConfirm(true)}
              data-testid="delete-button"
              style={{
                padding: '8px 16px',
                border: '1px solid var(--color-error-border)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: 'var(--color-neutral-surface)',
                color: 'var(--color-error)',
                fontSize: 'var(--text-body)',
              }}
            >
              Delete
            </button>
          </div>

          {/* Delete confirmation */}
          {showDeleteConfirm && (
            <div data-testid="delete-confirmation" style={{ padding: 'var(--space-4)', border: '1px solid var(--color-error-border)', borderRadius: 'var(--radius-medium)', marginBottom: 'var(--space-6)', background: 'var(--color-error-bg)' }}>
              <p style={{ margin: '0 0 12px', fontWeight: 'var(--weight-medium)' }}>Are you sure you want to delete this person?</p>
              <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                <button
                  type="button"
                  onClick={handleDelete}
                  data-testid="confirm-delete-button"
                  style={{
                    padding: '8px 16px',
                    backgroundColor: 'var(--color-error)',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 'var(--radius-medium)',
                    cursor: 'pointer',
                    fontSize: 'var(--text-body)',
                  }}
                >
                  Yes, Delete
                </button>
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  data-testid="cancel-delete-button"
                  style={{
                    padding: '8px 16px',
                    border: '1px solid var(--color-neutral-border)',
                    borderRadius: 'var(--radius-medium)',
                    cursor: 'pointer',
                    background: 'var(--color-neutral-surface)',
                    fontSize: 'var(--text-body)',
                  }}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Edit form */}
          {isEditing && (
            <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-neutral-surface)' }}>
              <h2 style={{ margin: '0 0 16px', fontSize: '18px', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>Edit Person</h2>
              <PersonForm
                mode="edit"
                initialData={person}
                onSubmit={handleUpdate}
                onCancel={() => setIsEditing(false)}
              />
            </div>
          )}

          {/* Person details */}
          {!isEditing && (
            <div data-testid="person-details" style={{ marginBottom: 'var(--space-6)' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-3)', marginBottom: 'var(--space-6)' }}>
                {person.preferredName && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Preferred Name</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)' }}>{person.preferredName}</p>
                  </div>
                )}
                {person.email && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Email</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)' }}>{person.email}</p>
                  </div>
                )}
                {person.timezone && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Timezone</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)' }}>{person.timezone}</p>
                  </div>
                )}
                {person.startDate && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Start Date</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)' }}>{person.startDate}</p>
                  </div>
                )}
                {person.tags.length > 0 && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Tags</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)' }}>{person.tags.join(', ')}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Morale update section */}
          <div data-testid="morale-section" style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-neutral-surface)' }}>
            <h3 style={{ margin: '0 0 12px', fontSize: '16px', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>Morale</h3>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div>
                <label htmlFor="morale-status" style={{ display: 'block', fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)', marginBottom: '4px' }}>Status</label>
                <select
                  id="morale-status"
                  value={moraleFormStatus}
                  onChange={(e) => setMoraleFormStatus(e.target.value as MoraleStatus)}
                  data-testid="morale-status-select"
                  style={{ padding: '8px 12px', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', fontSize: 'var(--text-body)' }}
                >
                  <option value="GREEN">Green</option>
                  <option value="YELLOW">Yellow</option>
                  <option value="RED">Red</option>
                  <option value="UNKNOWN">Unknown</option>
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label htmlFor="morale-note" style={{ display: 'block', fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)', marginBottom: '4px' }}>Note</label>
                <input
                  id="morale-note"
                  type="text"
                  value={moraleNote}
                  onChange={(e) => setMoraleNote(e.target.value)}
                  placeholder="Optional note..."
                  data-testid="morale-note-input"
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', fontSize: 'var(--text-body)' }}
                />
              </div>
              <button
                type="button"
                onClick={handleMoraleUpdate}
                data-testid="update-morale-button"
                style={{
                  padding: '8px 16px',
                  backgroundColor: 'var(--color-secondary)',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 'var(--radius-medium)',
                  fontSize: 'var(--text-body)',
                  cursor: 'pointer',
                  fontWeight: 'var(--weight-medium)',
                }}
              >
                Update Morale
              </button>
            </div>
          </div>

          {/* Remember Items */}
          <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-neutral-surface)' }}>
            <RememberItemsList
              items={person.pinnedRememberItems}
              onAdd={handleAddRememberItem}
              onRemove={handleRemoveRememberItem}
              onReorder={handleReorderRememberItems}
            />
          </div>

          {/* At-a-Glance */}
          <div data-testid="at-a-glance-section" style={{ padding: 'var(--space-4)', border: '1px solid var(--color-neutral-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-neutral-surface)' }}>
            <h3 style={{ margin: '0 0 12px', fontSize: '16px', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>At a Glance</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 'var(--space-4)' }}>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Last 1:1</span>
                <p data-testid="last-1on1-date" style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: person.atAGlance.last1on1Date ? 'var(--color-neutral-text)' : 'var(--color-neutral-text-muted)' }}>
                  {person.atAGlance.last1on1Date
                    ? new Date(person.atAGlance.last1on1Date).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
                    : 'No 1:1s yet'}
                </p>
              </div>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Open Action Items</span>
                <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-neutral-text-muted)' }}>
                  {person.atAGlance.openActionItemsCount ?? 'N/A'}
                </p>
              </div>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-neutral-text-muted)' }}>Active PDP Goals</span>
                <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-neutral-text-muted)' }}>
                  {person.atAGlance.activePdpGoalsSummary || 'No goals set'}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 1:1s Tab Panel */}
      {activeTab === 'one-on-ones' && (
        <div id="tab-panel-one-on-ones" role="tabpanel" aria-labelledby="tab-one-on-ones">
          {/* 1:1s toolbar: Start 1:1 button + settings gear */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <button
              type="button"
              onClick={handleStartOneOnOne}
              data-testid="start-one-on-one-nav-button"
              style={{
                padding: '10px 20px',
                backgroundColor: 'var(--color-accent)',
                color: '#fff',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-medium)',
                cursor: 'pointer',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              Start 1:1
            </button>
            <button
              type="button"
              onClick={() => setShowSeriesConfig(!showSeriesConfig)}
              data-testid="series-config-toggle"
              aria-label="1:1 series settings"
              title="1:1 series settings"
              style={{
                padding: '8px',
                border: '1px solid var(--color-neutral-border)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: showSeriesConfig ? 'var(--color-secondary)' : 'var(--color-neutral-surface)',
                color: showSeriesConfig ? '#fff' : 'inherit',
                fontSize: '18px',
                lineHeight: 1,
              }}
            >
              ⚙️
            </button>
          </div>

          {/* Series Configuration Panel */}
          {showSeriesConfig && (
            <div style={{ marginBottom: 'var(--space-6)' }}>
              <SeriesConfigPanel
                series={series}
                onSave={handleSaveSeriesConfig}
                isSaving={seriesSaving}
              />
            </div>
          )}

          {/* Timeline */}
          {entriesLoading && !entries ? (
            <div data-testid="entries-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-neutral-text-muted)' }}>
              Loading 1:1 entries...
            </div>
          ) : entries ? (
            <OneOnOneTimeline
              entries={entries}
              personId={personId}
              onPageChange={handlePageChange}
              onStartOneOnOne={handleStartOneOnOne}
            />
          ) : null}
        </div>
      )}
    </div>
  );
}
