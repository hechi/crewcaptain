'use client';

import { useState, useEffect, useCallback } from 'react';
import { useSession } from 'next-auth/react';
import { Link2, Loader2, Search, Users, Target } from 'lucide-react';
import { useStableToken } from '@/lib/useStableToken';
import { listPersons } from '@/lib/api-client';
import { listPdpGoalsByPerson } from '@/lib/api-client';
import { getLinkedPdpGoals, linkPdpGoalToStrategyGoal, unlinkPdpGoalFromStrategyGoal } from '@/lib/api-client';
import { Person } from '@/types/person';
import { PdpGoal } from '@/types/pdp-goal';
import { LinkedPdpGoalInfo } from '@/types/strategy-goal';
import Modal from '@/components/Modal';

interface LinkManagementModalProps {
  strategyGoalId: string;
  strategyGoalTitle: string;
  isOpen: boolean;
  onClose: () => void;
  onLinksChanged: () => void;
}

interface PdpGoalWithPerson extends PdpGoal {
  personId: string;
  personName: string;
}

export default function LinkManagementModal({
  strategyGoalId,
  strategyGoalTitle,
  isOpen,
  onClose,
  onLinksChanged,
}: LinkManagementModalProps) {
  const { status } = useSession();
  const getToken = useStableToken();
  const [people, setPeople] = useState<Person[]>([]);
  const [allPdpGoals, setAllPdpGoals] = useState<PdpGoalWithPerson[]>([]);
  const [linkedGoalIds, setLinkedGoalIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [linkingInProgress, setLinkingInProgress] = useState<Set<string>>(new Set());

  const fetchData = useCallback(async () => {
    const token = getToken();
    if (!token || !isOpen) return;

    try {
      setLoading(true);
      setError(null);

      // Fetch linked goals and people in parallel
      const [linkedGoalsData, peopleData] = await Promise.all([
        getLinkedPdpGoals(token, strategyGoalId),
        listPersons(token, { page: 0, size: 100 }),
      ]);

      setLinkedGoalIds(new Set(linkedGoalsData.map((g) => g.pdpGoalId)));
      setPeople(peopleData.content);

      // Fetch all ACTIVE PDP goals for all people
      const pdpGoalsPromises = peopleData.content.map(async (person) => {
        try {
          const goals = await listPdpGoalsByPerson(token, person.id, { status: 'ACTIVE', page: 0, size: 100 });
          return goals.content.map((goal) => ({
            ...goal,
            personId: person.id,
            personName: person.preferredName || person.name,
          }));
        } catch {
          return [];
        }
      });

      const pdpGoalsArrays = await Promise.all(pdpGoalsPromises);
      setAllPdpGoals(pdpGoalsArrays.flat());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [getToken, isOpen, strategyGoalId]);

  useEffect(() => {
    if (isOpen && status === 'authenticated') {
      fetchData();
    }
  }, [isOpen, status, fetchData]);

  const handleToggleLink = async (pdpGoalId: string, personId: string) => {
    const token = getToken();
    if (!token) return;

    setLinkingInProgress((prev) => new Set(prev).add(pdpGoalId));

    try {
      if (linkedGoalIds.has(pdpGoalId)) {
        await unlinkPdpGoalFromStrategyGoal(token, strategyGoalId, pdpGoalId);
        setLinkedGoalIds((prev) => {
          const newSet = new Set(prev);
          newSet.delete(pdpGoalId);
          return newSet;
        });
      } else {
        await linkPdpGoalToStrategyGoal(token, strategyGoalId, { pdpGoalId, personId });
        setLinkedGoalIds((prev) => new Set(prev).add(pdpGoalId));
      }
      onLinksChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update link');
    } finally {
      setLinkingInProgress((prev) => {
        const newSet = new Set(prev);
        newSet.delete(pdpGoalId);
        return newSet;
      });
    }
  };

  const filteredGoals = allPdpGoals.filter(
    (goal) =>
      goal.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      goal.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      goal.personName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Group by person for display
  const groupedByPerson = filteredGoals.reduce((acc, goal) => {
    if (!acc[goal.personName]) {
      acc[goal.personName] = [];
    }
    acc[goal.personName].push(goal);
    return acc;
  }, {} as Record<string, PdpGoalWithPerson[]>);

  const sortedPersonNames = Object.keys(groupedByPerson).sort();

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Manage Links: ${strategyGoalTitle}`}>
      <div style={{ minWidth: '500px', maxWidth: '700px' }}>
        <p style={{ margin: '0 0 16px', fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)' }}>
          Link team members&apos; PDP goals to this strategy objective. Only ACTIVE goals are shown.
        </p>

        {error && (
          <div
            style={{
              padding: '12px 16px',
              backgroundColor: 'var(--color-alert-muted)',
              border: '1px solid var(--color-alert)',
              borderRadius: 'var(--radius-medium)',
              color: 'var(--color-alert)',
              marginBottom: '16px',
            }}
          >
            {error}
          </div>
        )}

        {/* Search */}
        <div style={{ marginBottom: '16px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 12px',
              backgroundColor: 'var(--color-bg-elevated)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
            }}
          >
            <Search size={16} color="var(--color-text-muted)" />
            <input
              type="text"
              placeholder="Search goals or people..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                flex: 1,
                border: 'none',
                background: 'transparent',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                fontFamily: 'var(--font-mono)',
                outline: 'none',
              }}
            />
          </div>
        </div>

        {/* Stats */}
        <div
          style={{
            display: 'flex',
            gap: '16px',
            marginBottom: '16px',
            padding: '8px 12px',
            backgroundColor: 'var(--color-primary-muted)',
            borderRadius: 'var(--radius-medium)',
          }}
        >
          <span style={{ fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-primary)' }}>
            <Target size={12} style={{ display: 'inline', marginRight: '4px' }} />
            {linkedGoalIds.size} linked
          </span>
          <span style={{ fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)' }}>
            <Users size={12} style={{ display: 'inline', marginRight: '4px' }} />
            {allPdpGoals.length} total active
          </span>
        </div>

        {/* Goals List */}
        <div
          style={{
            maxHeight: '400px',
            overflow: 'auto',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
          }}
        >
          {loading ? (
            <div style={{ padding: '32px', textAlign: 'center' }}>
              <Loader2 size={24} style={{ animation: 'spin 1s linear infinite' }} />
              <p style={{ marginTop: '8px', color: 'var(--color-text-muted)' }}>Loading PDP goals...</p>
            </div>
          ) : filteredGoals.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              {searchQuery ? 'No goals match your search' : 'No active PDP goals found'}
            </div>
          ) : (
            sortedPersonNames.map((personName) => (
              <div key={personName} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <div
                  style={{
                    padding: '8px 12px',
                    backgroundColor: 'var(--color-bg-elevated)',
                    fontSize: 'var(--text-caption)',
                    fontFamily: 'var(--font-mono)',
                    color: 'var(--color-primary)',
                    fontWeight: 'var(--weight-semibold)',
                  }}
                >
                  <Users size={12} style={{ display: 'inline', marginRight: '6px' }} />
                  {personName}
                </div>
                {groupedByPerson[personName].map((goal) => {
                  const isLinked = linkedGoalIds.has(goal.id);
                  const isProcessing = linkingInProgress.has(goal.id);

                  return (
                    <div
                      key={goal.id}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '12px',
                        padding: '12px',
                        backgroundColor: isLinked ? 'var(--color-primary-muted)' : 'var(--color-bg-surface)',
                        borderBottom: '1px solid var(--color-border-subtle)',
                      }}
                    >
                      <button
                        onClick={() => !isProcessing && handleToggleLink(goal.id, goal.personId)}
                        disabled={isProcessing}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: '24px',
                          height: '24px',
                          border: `2px solid ${isLinked ? 'var(--color-primary)' : 'var(--color-border)'}`,
                          borderRadius: 'var(--radius-small)',
                          backgroundColor: isLinked ? 'var(--color-primary)' : 'transparent',
                          cursor: isProcessing ? 'not-allowed' : 'pointer',
                          flexShrink: 0,
                          marginTop: '2px',
                        }}
                      >
                        {isProcessing ? (
                          <Loader2 size={14} color="var(--color-text-muted)" style={{ animation: 'spin 1s linear infinite' }} />
                        ) : isLinked ? (
                          <Link2 size={14} color="var(--color-bg-base)" />
                        ) : null}
                      </button>

                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div
                          style={{
                            fontSize: 'var(--text-body)',
                            fontWeight: 'var(--weight-medium)',
                            color: 'var(--color-text-primary)',
                            marginBottom: '4px',
                          }}
                        >
                          {goal.title}
                        </div>
                        {goal.description && (
                          <div
                            style={{
                              fontSize: 'var(--text-small)',
                              color: 'var(--color-text-secondary)',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                          >
                            {goal.description}
                          </div>
                        )}
                        {goal.targetDate && (
                          <div
                            style={{
                              fontSize: 'var(--text-caption)',
                              fontFamily: 'var(--font-mono)',
                              color: 'var(--color-text-muted)',
                              marginTop: '4px',
                            }}
                          >
                            Target: {new Date(goal.targetDate).toLocaleDateString()}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
          <button
            onClick={onClose}
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
            Done
          </button>
        </div>
      </div>
    </Modal>
  );
}
