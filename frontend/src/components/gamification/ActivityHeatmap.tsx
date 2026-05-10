'use client';

import { ActivityDay } from '@/types/gamification';

interface ActivityHeatmapProps {
  /** Array of activity days with counts */
  days: ActivityDay[];
}

/**
 * Contribution-graph style activity heatmap.
 * Displays a grid of cells colored by activity intensity.
 * Uses accent colors from the design system.
 */
export default function ActivityHeatmap({ days }: ActivityHeatmapProps) {
  if (days.length === 0) {
    return (
      <div
        data-testid="activity-heatmap-empty"
        style={{
          padding: 'var(--space-4)',
          color: 'var(--color-text-muted)',
          fontSize: 'var(--text-caption)',
          fontFamily: 'var(--font-ui)',
          textAlign: 'center',
        }}
      >
        No activity data available
      </div>
    );
  }

  const maxCount = Math.max(...days.map((d) => d.count), 1);

  // Organize days into weeks (columns) with 7 rows each
  const weeks: ActivityDay[][] = [];
  let currentWeek: ActivityDay[] = [];

  // Pad the beginning to align with day of week
  const firstDate = new Date(days[0].date + 'T00:00:00');
  const startDayOfWeek = firstDate.getDay(); // 0=Sun, 6=Sat
  for (let i = 0; i < startDayOfWeek; i++) {
    currentWeek.push({ date: '', count: -1 }); // placeholder
  }

  for (const day of days) {
    currentWeek.push(day);
    if (currentWeek.length === 7) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
  }
  if (currentWeek.length > 0) {
    weeks.push(currentWeek);
  }

  return (
    <div
      data-testid="activity-heatmap"
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '4px',
      }}
    >
      {/* Heatmap grid */}
      <div
        style={{
          display: 'flex',
          gap: '2px',
          overflowX: 'auto',
          paddingBottom: '4px',
        }}
      >
        {weeks.map((week, weekIdx) => (
          <div
            key={weekIdx}
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '2px',
            }}
          >
            {week.map((day, dayIdx) => (
              <div
                key={`${weekIdx}-${dayIdx}`}
                data-testid={day.count >= 0 ? 'heatmap-cell' : undefined}
                title={day.count >= 0 ? `${day.date}: ${day.count} ${day.count === 1 ? 'activity' : 'activities'}` : undefined}
                aria-label={day.count >= 0 ? `${day.date}: ${day.count} activities` : undefined}
                style={{
                  width: '10px',
                  height: '10px',
                  borderRadius: '2px',
                  backgroundColor: day.count < 0
                    ? 'transparent'
                    : getHeatmapColor(day.count, maxCount),
                  transition: 'background-color 0.2s',
                }}
              />
            ))}
          </div>
        ))}
      </div>

      {/* Legend */}
      <div
        data-testid="heatmap-legend"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          marginTop: '4px',
        }}
      >
        <span
          style={{
            fontSize: '10px',
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-ui)',
          }}
        >
          Less
        </span>
        {[0, 0.25, 0.5, 0.75, 1].map((intensity, idx) => (
          <div
            key={idx}
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '2px',
              backgroundColor: getHeatmapColorByIntensity(intensity),
            }}
          />
        ))}
        <span
          style={{
            fontSize: '10px',
            color: 'var(--color-text-muted)',
            fontFamily: 'var(--font-ui)',
          }}
        >
          More
        </span>
      </div>
    </div>
  );
}

function getHeatmapColor(count: number, maxCount: number): string {
  if (count === 0) return 'var(--color-bg-elevated)';
  const intensity = count / maxCount;
  return getHeatmapColorByIntensity(intensity);
}

function getHeatmapColorByIntensity(intensity: number): string {
  if (intensity === 0) return 'var(--color-bg-elevated)';
  if (intensity <= 0.25) return 'rgba(0, 240, 255, 0.2)';
  if (intensity <= 0.5) return 'rgba(0, 240, 255, 0.4)';
  if (intensity <= 0.75) return 'rgba(0, 240, 255, 0.6)';
  return 'rgba(0, 240, 255, 0.85)';
}
