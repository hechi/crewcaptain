import { getGamificationStats } from '@/lib/api-client';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('Gamification API Client', () => {
  const token = 'test-token';

  beforeEach(() => {
    mockFetch.mockReset();
  });

  describe('getGamificationStats', () => {
    const mockStats = {
      streaks: { currentStreak: 5, longestStreak: 10, totalOneOnOnesHeld: 25 },
      achievements: [
        { type: 'FIRST_ONE_ON_ONE', unlocked: true, label: 'First 1:1', description: 'Hold your first 1:1 meeting', current: 25, target: 1 },
      ],
      activityHeatmap: [
        { date: '2026-05-01', count: 2 },
        { date: '2026-05-02', count: 0 },
      ],
      pdpProgress: { totalActive: 3, totalAchieved: 2, totalPaused: 1, totalDropped: 0, completionPercentage: 33 },
    };

    it('should fetch gamification stats with default parameters', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      const result = await getGamificationStats(token);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/gamification/stats'),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: 'Bearer test-token',
          }),
        })
      );
      expect(result).toEqual(mockStats);
    });

    it('should include heatmapDays parameter when provided', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      await getGamificationStats(token, { heatmapDays: 30 });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('heatmapDays=30'),
        expect.any(Object)
      );
    });

    it('should not include heatmapDays when not provided', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      await getGamificationStats(token);

      const calledUrl = mockFetch.mock.calls[0][0];
      expect(calledUrl).not.toContain('heatmapDays');
    });

    it('should throw on API error', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: () => Promise.resolve({
          status: 401,
          error: 'Unauthorized',
          message: 'Authentication failed',
          timestamp: '2026-05-10T00:00:00Z',
        }),
      });

      await expect(getGamificationStats(token)).rejects.toThrow();
    });

    it('should return streaks data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      const result = await getGamificationStats(token);

      expect(result.streaks.currentStreak).toBe(5);
      expect(result.streaks.longestStreak).toBe(10);
      expect(result.streaks.totalOneOnOnesHeld).toBe(25);
    });

    it('should return achievements data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      const result = await getGamificationStats(token);

      expect(result.achievements).toHaveLength(1);
      expect(result.achievements[0].type).toBe('FIRST_ONE_ON_ONE');
    });

    it('should return activity heatmap data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      const result = await getGamificationStats(token);

      expect(result.activityHeatmap).toHaveLength(2);
      expect(result.activityHeatmap[0].count).toBe(2);
    });

    it('should return PDP progress data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats),
      });

      const result = await getGamificationStats(token);

      expect(result.pdpProgress.totalActive).toBe(3);
      expect(result.pdpProgress.completionPercentage).toBe(33);
    });
  });
});
