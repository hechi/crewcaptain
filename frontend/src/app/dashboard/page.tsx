'use client';

import { useEffect, useState, useCallback } from 'react';
import { useSession } from 'next-auth/react';
import { DashboardResponse } from '@/types/dashboard';
import { GamificationStats } from '@/types/gamification';
import { getDashboard, getGamificationStats, getUserSettings } from '@/lib/api-client';
import OverdueActionItems from '@/components/dashboard/OverdueActionItems';
import DueSoonActionItems from '@/components/dashboard/DueSoonActionItems';
import StaleOneOnOnes from '@/components/dashboard/StaleOneOnOnes';
import UpcomingAnniversaries from '@/components/dashboard/UpcomingAnniversaries';
import { ProgressRing, StreakCounter, AchievementBadge, ActivityHeatmap } from '@/components/gamification';
import { UserSettings } from '@/types/settings';

export default function DashboardPage() {
  const { data: session, status } = useSession();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [gamification, setGamification] = useState<GamificationStats | null>(null);
  const [userSettings, setUserSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async () => {
    if (status !== 'authenticated' || !session?.accessToken) return;

    setLoading(true);
    setError(null);
    try {
      const token = session.accessToken as string;
      // Fetch settings first so we can use thresholds for the dashboard query
      const settingsResult = await getUserSettings(token);
      setUserSettings(settingsResult);

      const [dashResult, gamResult] = await Promise.all([
        getDashboard(token, {
          dueSoonDays: settingsResult.dueSoonDays,
          anniversaryLookaheadDays: settingsResult.anniversaryLookaheadDays,
        }),
        getGamificationStats(token),
      ]);
      setDashboard(dashResult);
      setGamification(gamResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }, [session, status]);

  useEffect(() => {
    fetchDashboard();
  }, [fetchDashboard]);

  if (status === 'loading' || loading) {
    return (
      <div
        data-testid="dashboard-loading"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          color: 'var(--color-text-muted)',
        }}
      >
        Loading dashboard...
      </div>
    );
  }

  if (error) {
    return (
      <div
        data-testid="dashboard-error"
        style={{
          padding: 'var(--space-6)',
          maxWidth: '1200px',
          margin: '0 auto',
        }}
      >
        <div
          style={{
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-bg-surface)',
            color: 'var(--color-alert)',
          }}
        >
          {error}
        </div>
      </div>
    );
  }

  if (!dashboard) return null;

  const totalAlerts =
    dashboard.overdueActionItems.length +
    dashboard.staleOneOnOnes.length;

  return (
    <div
      data-testid="dashboard-page"
      style={{
        padding: 'var(--space-6)',
        maxWidth: '1200px',
        margin: '0 auto',
        fontFamily: 'var(--font-ui)',
      }}
    >
      {/* Header */}
      <div style={{ marginBottom: 'var(--space-6)' }}>
        <h1
          data-testid="dashboard-title"
          style={{
            fontSize: 'var(--text-heading)',
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-text-primary)',
            margin: '0 0 8px 0',
          }}
        >
          Dashboard
        </h1>
        {totalAlerts > 0 && (
          <p
            data-testid="dashboard-alert-summary"
            style={{
              fontSize: '14px',
              color: 'var(--color-text-secondary)',
              margin: 0,
            }}
          >
            {totalAlerts} {totalAlerts === 1 ? 'item needs' : 'items need'} your attention
          </p>
        )}
      </div>

      {/* Gamification Stats */}
      {gamification && (
        <div
          data-testid="dashboard-gamification"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: 'var(--space-4)',
            marginBottom: 'var(--space-6)',
          }}
        >
          {/* Streak Counter */}
          <section data-testid="dashboard-section-streak">
            <h3
              style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                fontWeight: 'var(--weight-medium)',
                color: 'var(--color-text-secondary)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                margin: '0 0 8px 0',
              }}
            >
              1:1 Streak
            </h3>
            <StreakCounter
              currentStreak={gamification.streaks.currentStreak}
              longestStreak={gamification.streaks.longestStreak}
              totalOneOnOnesHeld={gamification.streaks.totalOneOnOnesHeld}
            />
          </section>

          {/* PDP Progress Ring */}
          <section
            data-testid="dashboard-section-pdp-progress"
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              padding: 'var(--space-4)',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-surface)',
            }}
          >
            <h3
              style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                fontWeight: 'var(--weight-medium)',
                color: 'var(--color-text-secondary)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                margin: '0 0 12px 0',
              }}
            >
              PDP Goals
            </h3>
            <ProgressRing
              percentage={gamification.pdpProgress.completionPercentage}
              size={100}
              label="Achieved"
              color="var(--color-success)"
            />
            <div
              style={{
                display: 'flex',
                gap: '12px',
                marginTop: '12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                color: 'var(--color-text-secondary)',
              }}
            >
              <span data-testid="pdp-active-count">{gamification.pdpProgress.totalActive} active</span>
              <span data-testid="pdp-achieved-count">{gamification.pdpProgress.totalAchieved} achieved</span>
            </div>
          </section>

          {/* Activity Heatmap */}
          <section
            data-testid="dashboard-section-activity"
            style={{
              padding: 'var(--space-4)',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-surface)',
            }}
          >
            <h3
              style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                fontWeight: 'var(--weight-medium)',
                color: 'var(--color-text-secondary)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                margin: '0 0 8px 0',
              }}
            >
              Activity
            </h3>
            <ActivityHeatmap days={gamification.activityHeatmap} />
          </section>
        </div>
      )}

      {/* Achievements */}
      {gamification && gamification.achievements.length > 0 && (userSettings?.showAchievements !== false) && (
        <section
          data-testid="dashboard-section-achievements"
          style={{ marginBottom: 'var(--space-6)' }}
        >
          <h3
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              fontWeight: 'var(--weight-medium)',
              color: 'var(--color-text-secondary)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
              margin: '0 0 12px 0',
            }}
          >
            Achievements ({gamification.achievements.filter(a => a.unlocked).length}/{gamification.achievements.length})
          </h3>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
              gap: '8px',
            }}
          >
            {gamification.achievements.map((achievement) => (
              <AchievementBadge key={achievement.type} achievement={achievement} />
            ))}
          </div>
        </section>
      )}

      {/* Grid layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
          gap: 'var(--space-5)',
        }}
      >
        {/* Overdue Action Items */}
        <section
          data-testid="dashboard-section-overdue"
          style={{
            padding: 'var(--space-5)',
            borderRadius: 'var(--radius-large)',
            border: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-elevated)',
          }}
        >
          <h2
            style={{
              fontSize: 'var(--text-subheading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-alert)',
              margin: '0 0 16px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            Overdue
            {dashboard.overdueActionItems.length > 0 && (
              <span
                style={{
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: 'rgba(255, 107, 107, 0.15)',
                  color: 'var(--color-alert)',
                }}
              >
                {dashboard.overdueActionItems.length}
              </span>
            )}
          </h2>
          <OverdueActionItems items={dashboard.overdueActionItems} />
        </section>

        {/* Due Soon Action Items */}
        <section
          data-testid="dashboard-section-due-soon"
          style={{
            padding: 'var(--space-5)',
            borderRadius: 'var(--radius-large)',
            border: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-elevated)',
          }}
        >
          <h2
            style={{
              fontSize: 'var(--text-subheading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-warning)',
              margin: '0 0 16px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            Due Soon
            {dashboard.dueSoonActionItems.length > 0 && (
              <span
                style={{
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: 'rgba(255, 193, 7, 0.15)',
                  color: 'var(--color-warning)',
                }}
              >
                {dashboard.dueSoonActionItems.length}
              </span>
            )}
          </h2>
          <DueSoonActionItems items={dashboard.dueSoonActionItems} />
        </section>

        {/* Stale 1:1 Reminders */}
        <section
          data-testid="dashboard-section-stale"
          style={{
            padding: 'var(--space-5)',
            borderRadius: 'var(--radius-large)',
            border: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-elevated)',
          }}
        >
          <h2
            style={{
              fontSize: 'var(--text-subheading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-warning)',
              margin: '0 0 16px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            Stale 1:1s
            {dashboard.staleOneOnOnes.length > 0 && (
              <span
                style={{
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: 'rgba(255, 193, 7, 0.15)',
                  color: 'var(--color-warning)',
                }}
              >
                {dashboard.staleOneOnOnes.length}
              </span>
            )}
          </h2>
          <StaleOneOnOnes reminders={dashboard.staleOneOnOnes} />
        </section>

        {/* Upcoming Anniversaries */}
        <section
          data-testid="dashboard-section-anniversaries"
          style={{
            padding: 'var(--space-5)',
            borderRadius: 'var(--radius-large)',
            border: '1px solid var(--color-border)',
            backgroundColor: 'var(--color-bg-elevated)',
          }}
        >
          <h2
            style={{
              fontSize: 'var(--text-subheading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-primary)',
              margin: '0 0 16px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            Anniversaries
            {dashboard.upcomingAnniversaries.length > 0 && (
              <span
                style={{
                  fontSize: '12px',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: 'var(--color-primary-muted)',
                  color: 'var(--color-primary)',
                }}
              >
                {dashboard.upcomingAnniversaries.length}
              </span>
            )}
          </h2>
          <UpcomingAnniversaries anniversaries={dashboard.upcomingAnniversaries} />
        </section>
      </div>
    </div>
  );
}
