'use client';

import { useState, useEffect } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { Target, TrendingUp, Users, ArrowRight, Loader2 } from 'lucide-react';
import { useStableToken } from '@/lib/useStableToken';
import { getAllAlignmentScores, listStrategyGoals } from '@/lib/api-client';
import { AlignmentScore } from '@/types/strategy-goal';

export default function StrategyProgressCard() {
  const { status } = useSession();
  const router = useRouter();
  const getToken = useStableToken();
  const [alignmentScores, setAlignmentScores] = useState<AlignmentScore[]>([]);
  const [activeGoalsCount, setActiveGoalsCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (status !== 'authenticated') return;

      const token = getToken();
      if (!token) return;

      try {
        setLoading(true);
        const [scoresData, goalsData] = await Promise.all([
          getAllAlignmentScores(token),
          listStrategyGoals(token, { status: 'ACTIVE', page: 0, size: 1 }),
        ]);

        setAlignmentScores(scoresData.scores);
        setActiveGoalsCount(goalsData.totalElements);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load strategy data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [status, getToken]);

  if (loading) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          minHeight: '150px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Loader2 size={24} style={{ animation: 'spin 1s linear infinite' }} />
      </div>
    );
  }

  if (error || activeGoalsCount === 0) {
    return (
      <div
        style={{
          padding: 'var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
          <Target size={24} color="var(--color-primary)" />
          <h3 style={{ margin: 0, fontSize: 'var(--text-h3)', fontWeight: 'var(--weight-semibold)' }}>
            Strategy Progress
          </h3>
        </div>
        <p style={{ margin: 0, color: 'var(--color-text-secondary)', fontSize: 'var(--text-body)' }}>
          No active strategy goals yet.
        </p>
        <button
          onClick={() => router.push('/strategy')}
          style={{
            marginTop: '12px',
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-medium)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary-muted)',
            color: 'var(--color-primary)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          Create Strategy Goal
          <ArrowRight size={16} />
        </button>
      </div>
    );
  }

  // Note: This counts total link instances, not unique PDP goals
  // A PDP goal linked to multiple strategy goals is counted multiple times
  // This can exceed 100% and is intentional - it shows total alignment coverage
  const totalLinkedGoals = alignmentScores.reduce((sum, s) => sum + s.linkedPdpGoals, 0);
  const totalActivePdpGoals = alignmentScores[0]?.totalActivePdpGoals || 0;
  const rawPercentage = totalActivePdpGoals > 0
    ? Math.round((totalLinkedGoals / totalActivePdpGoals) * 100)
    : 0;
  // Cap at 100% for display purposes, but show actual count
  const overallAlignmentPercentage = Math.min(rawPercentage, 100);

  return (
    <div
      style={{
        padding: 'var(--space-4)',
        backgroundColor: 'var(--color-bg-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Target size={24} color="var(--color-primary)" />
          <h3 style={{ margin: 0, fontSize: 'var(--text-h3)', fontWeight: 'var(--weight-semibold)' }}>
            Strategy Progress
          </h3>
        </div>
        <button
          onClick={() => router.push('/strategy')}
          style={{
            padding: '6px 12px',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'transparent',
            color: 'var(--color-primary)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
          }}
        >
          View All
          <ArrowRight size={14} />
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', marginBottom: '16px' }}>
        <div
          style={{
            padding: '12px',
            backgroundColor: 'var(--color-bg-elevated)',
            borderRadius: 'var(--radius-medium)',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              fontSize: 'var(--text-h2)',
              fontWeight: 'var(--weight-bold)',
              color: 'var(--color-primary)',
            }}
          >
            {activeGoalsCount}
          </div>
          <div
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            Active Goals
          </div>
        </div>

        <div
          style={{
            padding: '12px',
            backgroundColor: 'var(--color-bg-elevated)',
            borderRadius: 'var(--radius-medium)',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              fontSize: 'var(--text-h2)',
              fontWeight: 'var(--weight-bold)',
              color: 'var(--color-morale-green)',
            }}
          >
            {totalLinkedGoals}
          </div>
          <div
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            Linked PDP Goals
          </div>
        </div>

        <div
          style={{
            padding: '12px',
            backgroundColor: 'var(--color-bg-elevated)',
            borderRadius: 'var(--radius-medium)',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              fontSize: 'var(--text-h2)',
              fontWeight: 'var(--weight-bold)',
              color:
                overallAlignmentPercentage >= 70
                  ? 'var(--color-morale-green)'
                  : overallAlignmentPercentage >= 40
                    ? 'var(--color-morale-yellow)'
                    : 'var(--color-morale-red)',
            }}
          >
            {overallAlignmentPercentage}%
          </div>
          <div
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            Aligned
          </div>
        </div>
      </div>

      {alignmentScores.length > 0 && (
        <div>
          <h4
            style={{
              margin: '0 0 8px',
              fontSize: 'var(--text-small)',
              fontWeight: 'var(--weight-medium)',
              color: 'var(--color-text-secondary)',
            }}
          >
            Top Goals by Alignment
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {alignmentScores
              .filter((s) => s.linkedPdpGoals > 0)
              .sort((a, b) => b.alignmentPercentage - a.alignmentPercentage)
              .slice(0, 3)
              .map((score) => (
                <div
                  key={score.strategyGoalId}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '8px 12px',
                    backgroundColor: 'var(--color-bg-elevated)',
                    borderRadius: 'var(--radius-small)',
                  }}
                >
                  <span
                    style={{
                      fontSize: 'var(--text-small)',
                      color: 'var(--color-text-primary)',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      maxWidth: '200px',
                    }}
                  >
                    {score.strategyGoalTitle}
                  </span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div
                      style={{
                        width: '60px',
                        height: '4px',
                        backgroundColor: 'var(--color-border)',
                        borderRadius: 'var(--radius-full)',
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${score.alignmentPercentage}%`,
                          height: '100%',
                          backgroundColor:
                            score.alignmentPercentage >= 70
                              ? 'var(--color-morale-green)'
                              : score.alignmentPercentage >= 40
                                ? 'var(--color-morale-yellow)'
                                : 'var(--color-morale-red)',
                          transition: 'width 0.3s ease',
                        }}
                      />
                    </div>
                    <span
                      style={{
                        fontSize: 'var(--text-caption)',
                        fontFamily: 'var(--font-mono)',
                        color: 'var(--color-text-muted)',
                        minWidth: '35px',
                        textAlign: 'right',
                      }}
                    >
                      {score.alignmentPercentage}%
                    </span>
                  </div>
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
