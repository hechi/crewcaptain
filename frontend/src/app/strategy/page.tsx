'use client';

import { useState, useEffect, useCallback } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import LoadingScreen from '@/components/LoadingScreen';
import StrategyGoalCard from '@/components/strategy/StrategyGoalCard';
import StrategyGoalForm from '@/components/strategy/StrategyGoalForm';
import Modal from '@/components/Modal';
import { Target, Plus, AlertCircle, Link2, Users } from 'lucide-react';
import {
  listStrategyGoals,
  createStrategyGoal,
  updateStrategyGoal,
  deleteStrategyGoal,
  achieveStrategyGoal,
  dropStrategyGoal,
  getGapAnalysis,
  getAllAlignmentScores,
} from '@/lib/api-client';
import {
  StrategyGoal,
  StrategyGoalStatus,
  CreateStrategyGoalRequest,
  UpdateStrategyGoalRequest,
  GapAnalysis,
  AlignmentScore,
} from '@/types/strategy-goal';
import { useStableToken } from '@/lib/useStableToken';

export default function StrategyPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const getToken = useStableToken();
  
  const [goals, setGoals] = useState<StrategyGoal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<StrategyGoalStatus | ''>('');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingGoal, setEditingGoal] = useState<StrategyGoal | null>(null);
  const [deletingGoalId, setDeletingGoalId] = useState<string | null>(null);
  const [gapAnalysis, setGapAnalysis] = useState<GapAnalysis | null>(null);
  const [alignmentScores, setAlignmentScores] = useState<AlignmentScore[]>([]);
  const [showGapAnalysis, setShowGapAnalysis] = useState(false);

  const fetchData = useCallback(async () => {
    const token = getToken();
    if (!token) return;

    try {
      setLoading(true);
      const [goalsResponse, gapData, scoresData] = await Promise.all([
        listStrategyGoals(token, {
          status: statusFilter || undefined,
          page: 0,
          size: 100,
        }),
        getGapAnalysis(token),
        getAllAlignmentScores(token),
      ]);
      
      setGoals(goalsResponse.content);
      setGapAnalysis(gapData);
      setAlignmentScores(scoresData.scores);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load strategy goals');
    } finally {
      setLoading(false);
    }
  }, [getToken, statusFilter]);

  useEffect(() => {
    if (status === 'authenticated') {
      fetchData();
    }
  }, [status, fetchData]);

  if (status === 'loading' || loading) {
    return <LoadingScreen message="Loading strategy hub..." />;
  }

  if (status === 'unauthenticated') {
    router.push('/auth/signin');
    return null;
  }

  const handleCreateGoal = async (data: CreateStrategyGoalRequest) => {
    const token = getToken();
    if (!token) return;

    try {
      await createStrategyGoal(token, data);
      setIsCreateModalOpen(false);
      fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create strategy goal');
    }
  };

  const handleUpdateGoal = async (data: UpdateStrategyGoalRequest) => {
    const token = getToken();
    if (!token || !editingGoal) return;

    try {
      await updateStrategyGoal(token, editingGoal.id, data);
      setEditingGoal(null);
      fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update strategy goal');
    }
  };

  const handleDeleteGoal = async () => {
    const token = getToken();
    if (!token || !deletingGoalId) return;

    try {
      await deleteStrategyGoal(token, deletingGoalId);
      setDeletingGoalId(null);
      fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete strategy goal');
    }
  };

  const handleAchieveGoal = async (id: string) => {
    const token = getToken();
    if (!token) return;

    try {
      await achieveStrategyGoal(token, id);
      fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to achieve strategy goal');
    }
  };

  const handleDropGoal = async (id: string) => {
    const token = getToken();
    if (!token) return;

    try {
      await dropStrategyGoal(token, id);
      fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to drop strategy goal');
    }
  };

  const activeGoalsCount = goals.filter(g => g.status === 'ACTIVE').length;
  const achievedGoalsCount = goals.filter(g => g.status === 'ACHIEVED').length;

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: 'var(--space-6)' }}>
      {/* Header */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Target size={32} color="var(--color-primary)" />
            <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700 }}>My Strategy</h1>
          </div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 20px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 'var(--weight-semibold)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              cursor: 'pointer',
            }}
          >
            <Plus size={18} />
            New Strategy Goal
          </button>
        </div>

        {/* Stats Bar */}
        <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
          <div style={{
            padding: '8px 16px',
            backgroundColor: 'var(--color-bg-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
          }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>
              Active: {activeGoalsCount}
            </span>
          </div>
          <div style={{
            padding: '8px 16px',
            backgroundColor: 'var(--color-bg-surface)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
          }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>
              Achieved: {achievedGoalsCount}
            </span>
          </div>
        </div>

        {error && (
          <div style={{
            padding: '12px 16px',
            backgroundColor: 'var(--color-alert-muted)',
            border: '1px solid var(--color-alert)',
            borderRadius: 'var(--radius-medium)',
            color: 'var(--color-alert)',
            marginBottom: '16px',
          }}>
            {error}
          </div>
        )}

        {/* Filter Bar */}
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StrategyGoalStatus | '')}
            style={{
              padding: '8px 12px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-primary)',
              cursor: 'pointer',
            }}
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="ACHIEVED">Achieved</option>
            <option value="DROPPED">Dropped</option>
          </select>

          <button
            onClick={() => setShowGapAnalysis(!showGapAnalysis)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: showGapAnalysis ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
              color: showGapAnalysis ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            <AlertCircle size={16} />
            Gap Analysis
          </button>
        </div>
      </div>

      {/* Gap Analysis Panel */}
      {showGapAnalysis && gapAnalysis && (
        <div style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}>
          <h3 style={{ margin: '0 0 16px', fontSize: 'var(--text-h3)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={20} color="var(--color-warning)" />
            Gap Analysis
          </h3>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            {/* Unlinked PDP Goals */}
            <div>
              <h4 style={{ margin: '0 0 8px', fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
                <Link2 size={14} style={{ display: 'inline', marginRight: '4px' }} />
                Unlinked PDP Goals
              </h4>
              {gapAnalysis.unlinkedPdpGoals.length === 0 ? (
                <p style={{ margin: 0, fontSize: 'var(--text-small)', color: 'var(--color-text-muted)' }}>
                  All PDP goals are linked to strategy goals
                </p>
              ) : (
                <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                  {gapAnalysis.unlinkedPdpGoals.slice(0, 5).map((goal) => (
                    <li
                      key={goal.pdpGoalId}
                      style={{
                        padding: '4px 8px',
                        marginBottom: '4px',
                        backgroundColor: 'var(--color-bg-elevated)',
                        borderRadius: 'var(--radius-small)',
                        fontSize: 'var(--text-small)',
                      }}
                    >
                      {goal.title}
                    </li>
                  ))}
                  {gapAnalysis.unlinkedPdpGoals.length > 5 && (
                    <li style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', padding: '4px 8px' }}>
                      +{gapAnalysis.unlinkedPdpGoals.length - 5} more
                    </li>
                  )}
                </ul>
              )}
            </div>

            {/* Empty Strategy Goals */}
            <div>
              <h4 style={{ margin: '0 0 8px', fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
                <Users size={14} style={{ display: 'inline', marginRight: '4px' }} />
                Strategy Goals Without Contributors
              </h4>
              {gapAnalysis.emptyStrategyGoals.length === 0 ? (
                <p style={{ margin: 0, fontSize: 'var(--text-small)', color: 'var(--color-text-muted)' }}>
                  All strategy goals have linked PDP goals
                </p>
              ) : (
                <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                  {gapAnalysis.emptyStrategyGoals.slice(0, 5).map((goal) => (
                    <li
                      key={goal.strategyGoalId}
                      style={{
                        padding: '4px 8px',
                        marginBottom: '4px',
                        backgroundColor: 'var(--color-bg-elevated)',
                        borderRadius: 'var(--radius-small)',
                        fontSize: 'var(--text-small)',
                      }}
                    >
                      {goal.title}
                    </li>
                  ))}
                  {gapAnalysis.emptyStrategyGoals.length > 5 && (
                    <li style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-muted)', padding: '4px 8px' }}>
                      +{gapAnalysis.emptyStrategyGoals.length - 5} more
                    </li>
                  )}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Strategy Goals Grid */}
      {goals.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: 'var(--space-12)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}>
          <Target size={48} color="var(--color-text-muted)" style={{ marginBottom: '16px' }} />
          <h3 style={{ margin: '0 0 8px', color: 'var(--color-text-secondary)' }}>No Strategy Goals Yet</h3>
          <p style={{ margin: 0, color: 'var(--color-text-muted)', marginBottom: '16px' }}>
            Define high-level objectives and link them to your team&apos;s PDP goals
          </p>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            style={{
              padding: '10px 20px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 'var(--weight-semibold)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              cursor: 'pointer',
            }}
          >
            Create Your First Strategy Goal
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(400px, 1fr))', gap: '16px' }}>
          {goals.map((goal) => (
            <StrategyGoalCard
              key={goal.id}
              goal={{
                ...goal,
                linkedPdpGoalCount: alignmentScores.find(s => s.strategyGoalId === goal.id)?.linkedPdpGoals ?? 0,
              }}
              onAchieve={handleAchieveGoal}
              onDrop={handleDropGoal}
              onEdit={() => setEditingGoal(goal)}
              onDelete={() => setDeletingGoalId(goal.id)}
              onManageLinks={() => {}}
            />
          ))}
        </div>
      )}

      {/* Create Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Strategy Goal"
      >
        <StrategyGoalForm
          onSubmit={handleCreateGoal}
          onCancel={() => setIsCreateModalOpen(false)}
          submitLabel="Create Strategy Goal"
        />
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={!!editingGoal}
        onClose={() => setEditingGoal(null)}
        title="Edit Strategy Goal"
      >
        {editingGoal && (
          <StrategyGoalForm
            initialData={{
              title: editingGoal.title,
              description: editingGoal.description,
              targetDate: editingGoal.targetDate,
              sensitive: editingGoal.sensitive,
            }}
            onSubmit={handleUpdateGoal}
            onCancel={() => setEditingGoal(null)}
            submitLabel="Save Changes"
          />
        )}
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!deletingGoalId}
        onClose={() => setDeletingGoalId(null)}
        title="Delete Strategy Goal"
      >
        <p style={{ marginBottom: '16px' }}>
          Are you sure you want to delete this strategy goal? This action cannot be undone.
        </p>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={handleDeleteGoal}
            style={{
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 'var(--weight-semibold)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-alert)',
              color: 'var(--color-bg-base)',
              cursor: 'pointer',
            }}
          >
            Delete
          </button>
          <button
            onClick={() => setDeletingGoalId(null)}
            style={{
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-bg-elevated)',
              color: 'var(--color-text-secondary)',
              cursor: 'pointer',
            }}
          >
            Cancel
          </button>
        </div>
      </Modal>
    </div>
  );
}
