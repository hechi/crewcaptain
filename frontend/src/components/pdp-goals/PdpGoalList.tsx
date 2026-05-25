'use client';

import { useState } from 'react';
import { PdpGoal, PdpGoalStatus, CreatePdpGoalRequest, UpdatePdpGoalRequest } from '@/types/pdp-goal';
import PdpGoalCard from './PdpGoalCard';
import PdpGoalForm from './PdpGoalForm';
import EmptyState from '@/components/EmptyState';

interface PdpGoalListProps {
  goals: PdpGoal[];
  personId: string;
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
  aiEnabled?: boolean;
}

/**
 * Displays a list of PDP goals with status filtering, inline create/edit forms,
 * and action buttons for status transitions.
 */
export default function PdpGoalList({
  goals,
  personId,
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
  aiEnabled = false,
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
      {/* Header with create button and filter */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        {!showCreateForm ? (
          <button
            type="button"
            onClick={() => setShowCreateForm(true)}
            data-testid="pdp-goal-create-btn"
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
            + New Goal
          </button>
        ) : (
          <div />
        )}
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
      </div>

      {/* Create form */}
      {showCreateForm && (
        <div style={{ marginBottom: '16px' }}>
          <PdpGoalForm
            onSubmit={handleCreate}
            onCancel={() => setShowCreateForm(false)}
            aiEnabled={aiEnabled}
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
                aiEnabled={aiEnabled}
              />
            ) : (
              <PdpGoalCard
                key={goal.id}
                goal={goal}
                personId={personId}
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
