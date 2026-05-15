'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams, useSearchParams } from 'next/navigation';
import { Person, MoraleStatus, PaginatedResponse } from '@/types/person';
import { OneOnOneEntry, OneOnOneSeries } from '@/types/one-on-one';
import { ActionItem, ActionItemStatus, PaginatedActionItemResponse, CreateActionItemRequest, UpdateActionItemRequest } from '@/types/action-item';
import { PdpGoal, PdpGoalStatus, PaginatedPdpGoalResponse, CreatePdpGoalRequest, UpdatePdpGoalRequest } from '@/types/pdp-goal';
import { Kudos, PaginatedKudosResponse, CreateKudosRequest } from '@/types/kudos';
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
  listActionItemsByPerson,
  createActionItem,
  completeActionItem,
  cancelActionItem,
  deleteActionItem,
  updateActionItem,
  listPdpGoalsByPerson,
  createPdpGoal,
  updatePdpGoal,
  achievePdpGoal,
  pausePdpGoal,
  dropPdpGoal,
  resumePdpGoal,
  deletePdpGoal,
  listKudosByPerson,
  createKudos,
  deleteKudos,
  exportPersonMarkdown,
  generateReviewPacket,
} from '@/lib/api-client';
import { UpsertSeriesRequest } from '@/types/one-on-one';
import { useStableToken } from '@/lib/useStableToken';
import PersonForm from '@/components/PersonForm';
import MoraleIndicator from '@/components/MoraleIndicator';
import RememberItemsList from '@/components/RememberItemsList';
import OneOnOneTimeline from '@/components/one-on-one/OneOnOneTimeline';
import SeriesConfigPanel from '@/components/one-on-one/SeriesConfigPanel';
import ActionItemList from '@/components/action-items/ActionItemList';
import ActionItemForm from '@/components/action-items/ActionItemForm';
import PdpGoalList from '@/components/pdp-goals/PdpGoalList';
import KudosList from '@/components/kudos/KudosList';
import ReviewPacketModal from '@/components/ReviewPacketModal';
import WorkspaceAssignment from '@/components/workspace/WorkspaceAssignment';
import LoadingScreen from '@/components/LoadingScreen';

type Tab = 'details' | 'one-on-ones' | 'action-items' | 'pdp-goals' | 'kudos';

export default function PersonDetailPage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const personId = params.id as string;

  const validTabs: Tab[] = ['details', 'one-on-ones', 'action-items', 'pdp-goals', 'kudos'];
  const tabParam = searchParams.get('tab') as Tab | null;
  const initialTab: Tab = tabParam && validTabs.includes(tabParam) ? tabParam : 'details';

  const [person, setPerson] = useState<Person | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [moraleFormStatus, setMoraleFormStatus] = useState<MoraleStatus>('UNKNOWN');
  const [moraleNote, setMoraleNote] = useState('');

  // Tab state
  const [activeTab, setActiveTab] = useState<Tab>(initialTab);

  // 1:1 state
  const [entries, setEntries] = useState<PaginatedResponse<OneOnOneEntry> | null>(null);
  const [entriesLoading, setEntriesLoading] = useState(false);
  const [entriesPage, setEntriesPage] = useState(0);
  const [series, setSeries] = useState<OneOnOneSeries | null>(null);
  const [showSeriesConfig, setShowSeriesConfig] = useState(false);
  const [seriesSaving, setSeriesSaving] = useState(false);

  // Action items state
  const [actionItems, setActionItems] = useState<PaginatedActionItemResponse | null>(null);
  const [actionItemsLoading, setActionItemsLoading] = useState(false);
  const [actionItemsPage, setActionItemsPage] = useState(0);
  const [actionItemsStatusFilter, setActionItemsStatusFilter] = useState<ActionItemStatus | null>(null);
  const [showActionItemForm, setShowActionItemForm] = useState(false);
  const [editingActionItem, setEditingActionItem] = useState<ActionItem | null>(null);
  const [actionItemSubmitting, setActionItemSubmitting] = useState(false);

  // PDP Goals state
  const [pdpGoals, setPdpGoals] = useState<PaginatedPdpGoalResponse | null>(null);
  const [pdpGoalsLoading, setPdpGoalsLoading] = useState(false);
  const [pdpGoalsStatusFilter, setPdpGoalsStatusFilter] = useState<PdpGoalStatus | null>(null);

  // Kudos state
  const [kudos, setKudos] = useState<PaginatedKudosResponse | null>(null);
  const [kudosLoading, setKudosLoading] = useState(false);
  const [kudosSubmitting, setKudosSubmitting] = useState(false);

  // Export state
  const [exporting, setExporting] = useState(false);

  // Review packet state
  const [showReviewPacketModal, setShowReviewPacketModal] = useState(false);
  const [generatingReviewPacket, setGeneratingReviewPacket] = useState(false);

  const fetchPerson = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

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
  }, [getToken, isAuthenticated, personId]);

  const fetchEntries = useCallback(async (page: number = 0) => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setEntriesLoading(true);
    try {
      const result = await listOneOnOneEntries(token, personId, page);
      setEntries(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load 1:1 entries');
    } finally {
      setEntriesLoading(false);
    }
  }, [getToken, isAuthenticated, personId]);

  const fetchSeries = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    try {
      const result = await getOneOnOneSeries(token, personId);
      setSeries(result);
    } catch {
      // 404 means no series configured — that's fine
      setSeries(null);
    }
  }, [getToken, isAuthenticated, personId]);

  const fetchActionItems = useCallback(async (page: number = 0, statusFilter: ActionItemStatus | null = null) => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setActionItemsLoading(true);
    try {
      const result = await listActionItemsByPerson(token, personId, {
        page,
        status: statusFilter || undefined,
      });
      setActionItems(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load action items');
    } finally {
      setActionItemsLoading(false);
    }
  }, [getToken, isAuthenticated, personId]);

  const fetchPdpGoals = useCallback(async (statusFilter: PdpGoalStatus | null = null) => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setPdpGoalsLoading(true);
    try {
      const result = await listPdpGoalsByPerson(token, personId, {
        status: statusFilter || undefined,
      });
      setPdpGoals(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load PDP goals');
    } finally {
      setPdpGoalsLoading(false);
    }
  }, [getToken, isAuthenticated, personId]);

  const fetchKudos = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setKudosLoading(true);
    try {
      const result = await listKudosByPerson(token, personId);
      setKudos(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load kudos');
    } finally {
      setKudosLoading(false);
    }
  }, [getToken, isAuthenticated, personId]);

  useEffect(() => {
    fetchPerson();
  }, [fetchPerson]);

  useEffect(() => {
    if (activeTab === 'one-on-ones') {
      fetchEntries(entriesPage);
      fetchSeries();
    }
  }, [activeTab, entriesPage, fetchEntries, fetchSeries]);

  useEffect(() => {
    if (activeTab === 'action-items') {
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
    }
  }, [activeTab, actionItemsPage, actionItemsStatusFilter, fetchActionItems]);

  useEffect(() => {
    if (activeTab === 'pdp-goals') {
      fetchPdpGoals(pdpGoalsStatusFilter);
    }
  }, [activeTab, pdpGoalsStatusFilter, fetchPdpGoals]);

  useEffect(() => {
    if (activeTab === 'kudos') {
      fetchKudos();
    }
  }, [activeTab, fetchKudos]);

  const handleUpdate = async (data: {
    name: string;
    preferredName?: string;
    roleTitle?: string;
    timezone?: string;
    startDate?: string;
    email?: string;
    tags?: string[];
  }) => {
    const token = getToken();
    if (!token) return;
    try {
      const updated = await updatePerson(token, personId, data);
      setPerson(updated);
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update person');
    }
  };

  const handleDelete = async () => {
    const token = getToken();
    if (!token) return;
    try {
      await deletePerson(token, personId);
      router.push('/people');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete person');
    }
  };

  const handleMoraleUpdate = async () => {
    const token = getToken();
    if (!token) return;
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
    const token = getToken();
    if (!token) return;
    try {
      const items = await addRememberItem(token, personId, text);
      setPerson((prev) => prev ? { ...prev, pinnedRememberItems: items } : prev);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add remember item');
    }
  };

  const handleRemoveRememberItem = async (itemId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      const items = await removeRememberItem(token, personId, itemId);
      setPerson((prev) => prev ? { ...prev, pinnedRememberItems: items } : prev);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove remember item');
    }
  };

  const handleReorderRememberItems = async (orderedIds: string[]) => {
    const token = getToken();
    if (!token) return;
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
    const token = getToken();
    if (!token) return;
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

  const handleCreateActionItem = async (data: CreateActionItemRequest | UpdateActionItemRequest) => {
    const token = getToken();
    if (!token) return;
    setActionItemSubmitting(true);
    try {
      await createActionItem(token, personId, data as CreateActionItemRequest);
      setShowActionItemForm(false);
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create action item');
    } finally {
      setActionItemSubmitting(false);
    }
  };

  const handleUpdateActionItem = async (data: CreateActionItemRequest | UpdateActionItemRequest) => {
    if (!editingActionItem) return;
    const token = getToken();
    if (!token) return;
    setActionItemSubmitting(true);
    try {
      await updateActionItem(token, personId, editingActionItem.id, data as UpdateActionItemRequest);
      setEditingActionItem(null);
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update action item');
    } finally {
      setActionItemSubmitting(false);
    }
  };

  const handleCompleteActionItem = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await completeActionItem(token, personId, id);
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to complete action item');
    }
  };

  const handleCancelActionItem = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await cancelActionItem(token, personId, id);
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel action item');
    }
  };

  const handleDeleteActionItem = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteActionItem(token, personId, id);
      fetchActionItems(actionItemsPage, actionItemsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete action item');
    }
  };

  const handleEditActionItem = (id: string) => {
    const item = actionItems?.content.find((i) => i.id === id);
    if (item) {
      setEditingActionItem(item);
      setShowActionItemForm(false);
    }
  };

  // PDP Goal handlers
  const handleCreatePdpGoal = async (data: CreatePdpGoalRequest) => {
    const token = getToken();
    if (!token) return;
    try {
      await createPdpGoal(token, personId, data);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create PDP goal');
    }
  };

  const handleUpdatePdpGoal = async (goalId: string, data: UpdatePdpGoalRequest) => {
    const token = getToken();
    if (!token) return;
    try {
      await updatePdpGoal(token, personId, goalId, data);
      fetchPdpGoals(pdpGoalsStatusFilter);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update PDP goal');
    }
  };

  const handleAchievePdpGoal = async (goalId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await achievePdpGoal(token, personId, goalId);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to achieve PDP goal');
    }
  };

  const handlePausePdpGoal = async (goalId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await pausePdpGoal(token, personId, goalId);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to pause PDP goal');
    }
  };

  const handleDropPdpGoal = async (goalId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await dropPdpGoal(token, personId, goalId);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to drop PDP goal');
    }
  };

  const handleResumePdpGoal = async (goalId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await resumePdpGoal(token, personId, goalId);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to resume PDP goal');
    }
  };

  const handleDeletePdpGoal = async (goalId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deletePdpGoal(token, personId, goalId);
      fetchPdpGoals(pdpGoalsStatusFilter);
      fetchPerson();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete PDP goal');
    }
  };

  // Kudos handlers
  const handleCreateKudos = async (data: CreateKudosRequest) => {
    const token = getToken();
    if (!token) return;
    setKudosSubmitting(true);
    try {
      await createKudos(token, personId, data);
      fetchKudos();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create kudos');
    } finally {
      setKudosSubmitting(false);
    }
  };

  const handleDeleteKudos = async (kudosId: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteKudos(token, personId, kudosId);
      fetchKudos();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete kudos');
    }
  };

  const handleExport = async () => {
    const token = getToken();
    if (!token) return;
    setExporting(true);
    try {
      const markdown = await exportPersonMarkdown(token, personId);
      const blob = new Blob([markdown], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${person?.name || 'export'}.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export person data');
    } finally {
      setExporting(false);
    }
  };

  const handleGenerateReviewPacket = async (dateFrom: string, dateTo: string) => {
    const token = getToken();
    if (!token) return;
    setGeneratingReviewPacket(true);
    try {
      const markdown = await generateReviewPacket(token, personId, { dateFrom, dateTo });
      const blob = new Blob([markdown], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${person?.name || 'review'}-review-packet.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setShowReviewPacketModal(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate review packet');
    } finally {
      setGeneratingReviewPacket(false);
    }
  };

  if (status === 'loading') {
    return <LoadingScreen message="Loading person" />;
  }

  if (status === 'unauthenticated') {
    return <div data-testid="unauthenticated">Please sign in to access this page.</div>;
  }

  if (loading) {
    return <LoadingScreen message="Loading person" />;
  }

  if (error && !person) {
    return (
      <div data-testid="error-message" style={{ color: 'var(--color-alert)', padding: 'var(--space-6)' }}>
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
          background: 'var(--color-bg-elevated)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          cursor: 'pointer',
          fontSize: 'var(--text-body)',
          color: 'var(--color-text-secondary)',
          transition: 'border-color 0.2s',
        }}
      >
        ← Back to People
      </button>

      {error && (
        <div data-testid="error-message" style={{ color: 'var(--color-alert)', marginBottom: 'var(--space-4)' }}>
          {error}
        </div>
      )}

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
        <div>
          <h1 style={{
            margin: 0,
            fontSize: 'var(--text-h2)',
            fontWeight: 'var(--weight-bold)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
            letterSpacing: '-0.3px',
          }}>
            {person.name}
          </h1>
          {person.roleTitle && (
            <p style={{ margin: '4px 0 0', fontSize: '16px', color: 'var(--color-text-secondary)' }}>{person.roleTitle}</p>
          )}
          <div style={{ marginTop: '8px' }}>
            <WorkspaceAssignment
              token={getToken() || ''}
              personId={personId}
              currentWorkspaceId={person.workspaceId}
              onAssigned={(wsId) => setPerson(prev => prev ? { ...prev, workspaceId: wsId } : prev)}
            />
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
          <button
            type="button"
            onClick={handleExport}
            disabled={exporting}
            data-testid="export-button"
            aria-label="Export person data as Markdown"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '8px 16px',
              height: '36px',
              width: '160px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              cursor: exporting ? 'not-allowed' : 'pointer',
              background: 'var(--color-bg-elevated)',
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-secondary)',
              fontFamily: 'var(--font-mono)',
              opacity: exporting ? 0.6 : 1,
              transition: 'border-color 0.2s',
              boxSizing: 'border-box',
            }}
          >
            {exporting ? 'Exporting...' : '↓ Export'}
          </button>
          <button
            type="button"
            onClick={() => setShowReviewPacketModal(true)}
            data-testid="review-packet-button"
            aria-label="Generate review packet"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '8px 16px',
              height: '36px',
              width: '160px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              cursor: 'pointer',
              background: 'var(--color-bg-elevated)',
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-secondary)',
              fontFamily: 'var(--font-mono)',
              transition: 'border-color 0.2s',
              boxSizing: 'border-box',
            }}
          >
            Review Packet
          </button>
          <MoraleIndicator moraleStatus={person.moraleStatus} />
        </div>
      </div>

      {/* Tabs */}
      <div
        data-testid="person-tabs"
        style={{
          display: 'flex',
          gap: '0',
          borderBottom: '1px solid var(--color-border)',
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
            borderBottom: activeTab === 'details' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'details' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'details' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            marginBottom: '-1px',
            fontFamily: 'var(--font-mono)',
            transition: 'color 0.2s',
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
            borderBottom: activeTab === 'one-on-ones' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'one-on-ones' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'one-on-ones' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            marginBottom: '-1px',
            fontFamily: 'var(--font-mono)',
            transition: 'color 0.2s',
          }}
        >
          1:1s
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'action-items'}
          aria-controls="tab-panel-action-items"
          onClick={() => setActiveTab('action-items')}
          data-testid="tab-action-items"
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: activeTab === 'action-items' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'action-items' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'action-items' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            marginBottom: '-1px',
            fontFamily: 'var(--font-mono)',
            transition: 'color 0.2s',
          }}
        >
          Action Items
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'pdp-goals'}
          aria-controls="tab-panel-pdp-goals"
          onClick={() => setActiveTab('pdp-goals')}
          data-testid="tab-pdp-goals"
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: activeTab === 'pdp-goals' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'pdp-goals' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'pdp-goals' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            marginBottom: '-1px',
            fontFamily: 'var(--font-mono)',
            transition: 'color 0.2s',
          }}
        >
          PDP Goals
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'kudos'}
          aria-controls="tab-panel-kudos"
          onClick={() => setActiveTab('kudos')}
          data-testid="tab-kudos"
          style={{
            padding: '10px 20px',
            border: 'none',
            borderBottom: activeTab === 'kudos' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontSize: 'var(--text-body)',
            fontWeight: activeTab === 'kudos' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            color: activeTab === 'kudos' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            marginBottom: '-1px',
            fontFamily: 'var(--font-mono)',
            transition: 'color 0.2s',
          }}
        >
          Kudos
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
                border: '1px solid var(--color-border-glow)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: 'var(--color-primary-muted)',
                fontSize: 'var(--text-body)',
                color: 'var(--color-primary)',
                fontWeight: 'var(--weight-medium)',
                fontFamily: 'var(--font-mono)',
                transition: 'box-shadow 0.2s',
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
                border: '1px solid var(--color-alert-muted)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: 'var(--color-alert-muted)',
                color: 'var(--color-alert)',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              Delete
            </button>
          </div>

          {/* Delete confirmation */}
          {showDeleteConfirm && (
            <div data-testid="delete-confirmation" style={{ padding: 'var(--space-4)', border: '1px solid var(--color-alert)', borderRadius: 'var(--radius-medium)', marginBottom: 'var(--space-6)', background: 'var(--color-alert-muted)' }}>
              <p style={{ margin: '0 0 12px', fontWeight: 'var(--weight-medium)', color: 'var(--color-text-primary)' }}>Are you sure you want to delete this person?</p>
              <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
                <button
                  type="button"
                  onClick={handleDelete}
                  data-testid="confirm-delete-button"
                  style={{
                    padding: '8px 16px',
                    backgroundColor: 'var(--color-alert)',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 'var(--radius-medium)',
                    cursor: 'pointer',
                    fontSize: 'var(--text-body)',
                    fontFamily: 'var(--font-mono)',
                    boxShadow: 'var(--glow-alert)',
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
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-medium)',
                    cursor: 'pointer',
                    background: 'var(--color-bg-elevated)',
                    fontSize: 'var(--text-body)',
                    color: 'var(--color-text-secondary)',
                  }}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Edit form */}
          {isEditing && (
            <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
              <h2 style={{ margin: '0 0 16px', fontSize: '18px', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>Edit Person</h2>
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
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Preferred Name</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>{person.preferredName}</p>
                  </div>
                )}
                {person.email && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Email</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>{person.email}</p>
                  </div>
                )}
                {person.timezone && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Timezone</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>{person.timezone}</p>
                  </div>
                )}
                {person.startDate && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Start Date</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>{person.startDate}</p>
                  </div>
                )}
                {person.tags.length > 0 && (
                  <div>
                    <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Tags</span>
                    <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>{person.tags.join(', ')}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Morale update section */}
          <div data-testid="morale-section" style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 'var(--text-caption)', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-mono)', color: 'var(--color-primary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Morale</h3>
            <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div>
                <label htmlFor="morale-status" style={{ display: 'block', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', marginBottom: '4px', fontFamily: 'var(--font-mono)' }}>Status</label>
                <select
                  id="morale-status"
                  value={moraleFormStatus}
                  onChange={(e) => setMoraleFormStatus(e.target.value as MoraleStatus)}
                  data-testid="morale-status-select"
                >
                  <option value="GREEN">Green</option>
                  <option value="YELLOW">Yellow</option>
                  <option value="RED">Red</option>
                  <option value="UNKNOWN">Unknown</option>
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label htmlFor="morale-note" style={{ display: 'block', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', marginBottom: '4px', fontFamily: 'var(--font-mono)' }}>Note</label>
                <input
                  id="morale-note"
                  type="text"
                  value={moraleNote}
                  onChange={(e) => setMoraleNote(e.target.value)}
                  placeholder="Optional note..."
                  data-testid="morale-note-input"
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', fontSize: 'var(--text-body)', backgroundColor: 'var(--color-bg-elevated)', color: 'var(--color-text-primary)' }}
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
                  fontFamily: 'var(--font-mono)',
                  boxShadow: '0 0 8px rgba(168, 85, 247, 0.2)',
                }}
              >
                Update Morale
              </button>
            </div>
          </div>

          {/* Remember Items */}
          <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
            <RememberItemsList
              items={person.pinnedRememberItems}
              onAdd={handleAddRememberItem}
              onRemove={handleRemoveRememberItem}
              onReorder={handleReorderRememberItems}
            />
          </div>

          {/* At-a-Glance */}
          <div data-testid="at-a-glance-section" style={{ padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 'var(--text-caption)', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-heading)', color: 'var(--color-primary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>At a Glance</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 'var(--space-4)' }}>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>Last 1:1</span>
                <p data-testid="last-1on1-date" style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', fontFamily: 'var(--font-mono)', color: person.atAGlance.last1on1Date ? 'var(--color-text-primary)' : 'var(--color-text-muted)' }}>
                  {person.atAGlance.last1on1Date
                    ? new Date(person.atAGlance.last1on1Date).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
                    : 'No 1:1s yet'}
                </p>
              </div>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>Open Actions</span>
                <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-secondary)' }}>
                  {person.atAGlance.openActionItemsCount ?? 'N/A'}
                </p>
              </div>
              <div>
                <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>PDP Goals</span>
                <p style={{ margin: '2px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
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
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-bg-base)',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-semibold)',
                fontFamily: 'var(--font-mono)',
                cursor: 'pointer',
                boxShadow: 'var(--glow-primary)',
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
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                cursor: 'pointer',
                background: showSeriesConfig ? 'var(--color-secondary)' : 'var(--color-bg-elevated)',
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
            <div data-testid="entries-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
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

      {/* Action Items Tab Panel */}
      {activeTab === 'action-items' && (
        <div id="tab-panel-action-items" role="tabpanel" aria-labelledby="tab-action-items">
          {/* Toolbar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <button
              type="button"
              onClick={() => { setShowActionItemForm(true); setEditingActionItem(null); }}
              data-testid="create-action-item-btn"
              style={{
                padding: '10px 20px',
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-bg-base)',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-semibold)',
                fontFamily: 'var(--font-mono)',
                cursor: 'pointer',
                boxShadow: 'var(--glow-primary)',
              }}
            >
              + New Action Item
            </button>
          </div>

          {/* Create Form */}
          {showActionItemForm && (
            <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
              <h3 style={{ margin: '0 0 16px', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>
                New Action Item
              </h3>
              <ActionItemForm
                mode="create"
                onSubmit={handleCreateActionItem}
                onCancel={() => setShowActionItemForm(false)}
                isSubmitting={actionItemSubmitting}
              />
            </div>
          )}

          {/* Edit Form */}
          {editingActionItem && (
            <div style={{ marginBottom: 'var(--space-6)', padding: 'var(--space-4)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', backgroundColor: 'var(--color-bg-surface)' }}>
              <h3 style={{ margin: '0 0 16px', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)', fontFamily: 'var(--font-heading)', color: 'var(--color-text-primary)' }}>
                Edit Action Item
              </h3>
              <ActionItemForm
                mode="edit"
                initialData={{
                  title: editingActionItem.title,
                  description: editingActionItem.description,
                  ownerType: editingActionItem.ownerType,
                  dueDate: editingActionItem.dueDate,
                }}
                onSubmit={handleUpdateActionItem}
                onCancel={() => setEditingActionItem(null)}
                isSubmitting={actionItemSubmitting}
              />
            </div>
          )}

          {/* List */}
          {actionItemsLoading && !actionItems ? (
            <div data-testid="action-items-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
              Loading action items...
            </div>
          ) : actionItems ? (
            <ActionItemList
              data={actionItems}
              onComplete={handleCompleteActionItem}
              onCancel={handleCancelActionItem}
              onDelete={handleDeleteActionItem}
              onEdit={handleEditActionItem}
              onPageChange={(page) => setActionItemsPage(page)}
              statusFilter={actionItemsStatusFilter}
              onStatusFilterChange={setActionItemsStatusFilter}
              showCreateButton={!showActionItemForm}
              onCreateClick={() => setShowActionItemForm(true)}
              emptyMessage="No action items yet — click '+ New Action Item' to create one"
            />
          ) : null}
        </div>
      )}

      {/* PDP Goals Tab Panel */}
      {activeTab === 'pdp-goals' && (
        <div id="tab-panel-pdp-goals" role="tabpanel" aria-labelledby="tab-pdp-goals">
          {pdpGoalsLoading && !pdpGoals ? (
            <div data-testid="pdp-goals-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
              Loading PDP goals...
            </div>
          ) : (
            <PdpGoalList
              goals={pdpGoals?.content || []}
              onCreateGoal={handleCreatePdpGoal}
              onUpdateGoal={handleUpdatePdpGoal}
              onAchieveGoal={handleAchievePdpGoal}
              onPauseGoal={handlePausePdpGoal}
              onDropGoal={handleDropPdpGoal}
              onResumeGoal={handleResumePdpGoal}
              onDeleteGoal={handleDeletePdpGoal}
              statusFilter={pdpGoalsStatusFilter}
              onStatusFilterChange={setPdpGoalsStatusFilter}
            />
          )}
        </div>
      )}

      {/* Kudos Tab Panel */}
      {activeTab === 'kudos' && (
        <div id="tab-panel-kudos" role="tabpanel" aria-labelledby="tab-kudos">
          {kudosLoading && !kudos ? (
            <div data-testid="kudos-loading" style={{ textAlign: 'center', padding: 'var(--space-6)', color: 'var(--color-text-muted)' }}>
              Loading kudos...
            </div>
          ) : (
            <KudosList
              kudos={kudos?.content || []}
              onCreateKudos={handleCreateKudos}
              onDeleteKudos={handleDeleteKudos}
              isSubmitting={kudosSubmitting}
            />
          )}
        </div>
      )}

      {/* Review Packet Modal */}
      {showReviewPacketModal && (
        <ReviewPacketModal
          personName={person.name}
          onGenerate={handleGenerateReviewPacket}
          onClose={() => setShowReviewPacketModal(false)}
          generating={generatingReviewPacket}
        />
      )}
    </div>
  );
}
