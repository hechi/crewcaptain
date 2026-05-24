'use client';

import { useState, useEffect } from 'react';
import { useSession } from 'next-auth/react';
import { Target, Loader2 } from 'lucide-react';
import { useStableToken } from '@/lib/useStableToken';
import { getStrategyGoalsByPdpGoal } from '@/lib/api-client';
import { StrategyGoalBasicInfo } from '@/types/strategy-goal';

interface PdpGoalAlignmentBadgesProps {
  personId: string;
  pdpGoalId: string;
}

export default function PdpGoalAlignmentBadges({ personId, pdpGoalId }: PdpGoalAlignmentBadgesProps) {
  const { status } = useSession();
  const getToken = useStableToken();
  const [linkedGoals, setLinkedGoals] = useState<StrategyGoalBasicInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchLinkedGoals = async () => {
      if (status !== 'authenticated') return;

      const token = getToken();
      if (!token) return;

      try {
        setLoading(true);
        const goals = await getStrategyGoalsByPdpGoal(token, personId, pdpGoalId);
        setLinkedGoals(goals);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load linked goals');
      } finally {
        setLoading(false);
      }
    };

    fetchLinkedGoals();
  }, [status, getToken, personId, pdpGoalId]);

  if (loading) {
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          fontSize: 'var(--text-caption)',
          fontFamily: 'var(--font-mono)',
          color: 'var(--color-text-muted)',
        }}
      >
        <Loader2 size={12} style={{ animation: 'spin 1s linear infinite' }} />
        Loading...
      </span>
    );
  }

  if (error || linkedGoals.length === 0) {
    return null;
  }

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '8px' }}>
      {linkedGoals.map((goal) => (
        <span
          key={goal.strategyGoalId}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
            padding: '2px 8px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            backgroundColor:
              goal.status === 'ACTIVE'
                ? 'var(--color-primary-muted)'
                : goal.status === 'ACHIEVED'
                  ? 'rgba(16, 185, 129, 0.1)'
                  : 'var(--color-bg-elevated)',
            color:
              goal.status === 'ACTIVE'
                ? 'var(--color-primary)'
                : goal.status === 'ACHIEVED'
                  ? 'var(--color-morale-green)'
                  : 'var(--color-text-muted)',
            borderRadius: 'var(--radius-full)',
            border: `1px solid ${
              goal.status === 'ACTIVE'
                ? 'var(--color-primary)'
                : goal.status === 'ACHIEVED'
                  ? 'var(--color-morale-green)'
                  : 'var(--color-border)'
            }`,
          }}
          title={`Aligned with strategy goal: ${goal.title}`}
        >
          <Target size={10} />
          {goal.title.length > 25 ? `${goal.title.substring(0, 25)}...` : goal.title}
        </span>
      ))}
    </div>
  );
}
