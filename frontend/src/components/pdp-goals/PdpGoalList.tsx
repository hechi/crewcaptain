'use client';

import { useState } from 'react';
import { PdpGoal, PdpGoalStatus, CreatePdpGoalRequest, UpdatePdpGoalRequest } from '@/types/pdp-goal';
import PdpGoalCard from './PdpGoalCard';
import PdpGoalForm from './PdpGoalForm';
import EmptyState from '@/components/EmptyState';

interface PdpGoalListProps {
  goals: PdpGoal[];
  onCreateGoal: (data: CreatePdpGoalRequest) => void;
  onUpdateGoal: (goalId: string, data: UpdatePdpGoalRequest) => void;
  onAchieveGoal: (goalId: string) => void;
  onPauseGoal: (goalId: string) => void;
  onDropGoal: (goalId: string) => void;
  onResumeGoal: (goalId: string) => void;
  onDeleteGoal: (goalId: string) => void;
  onViewUpdates?: (goalId: string) => void;
  statusFilter: PdpGoalStatus | null;
  onStatusFilterChange: (status: PdpGoalStatus | null) => void;
}

/**
 * Displays a list of PDP goals with status filtering, inline create/edit forms,
 * and action buttons for status transitions.
 */
export default function PdpGoalList({
  goals,
  onCreateGoal,
  onUpdateGoal,
  onAchieveGoal,
  onPauseGoal,
  onDropGoal,
  onResumeGoal,
  onDeleteGoal,
  onViewUpdates,
  statusFilter,
  onStatusFilterChange,
}: PdpGoalListProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingGoalId, setEditingGoalId] = useState<string | null>(null);

  const statusOptions: { value: PdpGoalStatus | null; label: string }[] = [
    { value: null, label: 'All' },
    { value: 'ACTIVE', label: 'Active' },
    { value: 'ACHIEVED', label: 'Achieved' },
    { value: 'PAUSED', label: 'Paused' },
    { value: 'DROPPED', label: 'Dropped' },
  ];

  const handleCreate = (data: CreatePdpGoalRequest | UpdatePdpGoalRequest) => {
    onCreateGoal(data as CreatePdpGoalRequest);
    setShowCreateForm(false);
  };

  const handleUpdate = (goalId: string) => (data: CreatePdpGoalRequest | UpdatePdpGoalRequest) => {
    onUpdateGoal(goalId, data as UpdatePdpGoalRequest);
    setEditingGoalId(null);
  };

  return (
    <div data-testid="pdp-goal-list">
      {/* Header with filter and create button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {statusOptions.map((option) => (
            <button
              key={option.label}
              type="button"
              onClick={() => onStatusFilterChange(option.value)}
              data-testid={`pdp-goal-filter-${option.label.toLowerCase()}`}
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: `1px solid ${statusFilter === option.value ? 'var(--color-primary)' : 'var(--color-border)'}`,
                borderRadius: 'var(--radius-full)',
                backgroundColor: statusFilter === option.value ? 'var(--color-primary-muted)' : 'transparent',
                color: statusFilter === option.value ? 'var(--color-primary)' : 'var(--color-text-muted)',
                cursor: 'pointer',
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
        {!showCreateForm && (
          <button
            type="button"
            onClick={() => setShowCreateForm(true)}
            data-testid="pdp-goal-create-btn"
            style={{
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-primary)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-primary-muted)',
              color: 'var(--color-primary)',
              cursor: 'pointer',
              fontWeight: 'var(--weight-medium)',
            }}
          >
            + New Goal
          </button>
        )}
      </div>

      {/* Create form */}
      {showCreateForm && (
        <div style={{ marginBottom: '16px' }}>
          <PdpGoalForm
            onSubmit={handleCreate}
            onCancel={() => setShowCreateForm(false)}
          />
        </div>
      )}

      {/* Goals list */}
      {goals.length === 0 && !showCreateForm ? (
        <EmptyState
          message="No PDP goals yet — click '+ New Goal' to create a development goal for this person."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {goals.map((goal) => (
            editingGoalId === goal.id ? (
              <PdpGoalForm
                key={goal.id}
                existingGoal={goal}
                onSubmit={handleUpdate(goal.id)}
                onCancel={() => setEditingGoalId(null)}
              />
            ) : (
              <PdpGoalCard
                key={goal.id}
                goal={goal}
                onAchieve={onAchieveGoal}
                onPause={onPauseGoal}
                onDrop={onDropGoal}
                onResume={onResumeGoal}
                onDelete={onDeleteGoal}
                onEdit={(id) => setEditingGoalId(id)}
                onViewUpdates={onViewUpdates}
              />
            )
          ))}
        </div>
      )}
    </div>
  );
}
