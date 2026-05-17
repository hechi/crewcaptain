import { getUserSettings, updateUserSettings } from '@/lib/api-client';
import { UserSettings, UpdateUserSettingsRequest } from '@/types/settings';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('Settings API Client', () => {
  const token = 'test-token';

  beforeEach(() => {
    mockFetch.mockReset();
  });

  describe('getUserSettings', () => {
    it('should fetch settings with auth header', async () => {
      const mockSettings: UserSettings = {
        dueSoonDays: 3,
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: true,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiApiBaseUrl: null,
        aiModelName: null,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockSettings,
      });

      const result = await getUserSettings(token);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/settings'),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${token}`,
          }),
        })
      );
      expect(result).toEqual(mockSettings);
    });

    it('should throw on 401 response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: async () => ({
          status: 401,
          error: 'Unauthorized',
          message: 'Authentication failed',
          timestamp: '2026-01-01T00:00:00Z',
        }),
      });

      await expect(getUserSettings(token)).rejects.toThrow();
    });

    it('should return settings with light theme', async () => {
      const mockSettings: UserSettings = {
        dueSoonDays: 7,
        staleOneOnOneDays: 21,
        anniversaryLookaheadDays: 60,
        theme: 'LIGHT',
        showAchievements: false,
        notifyActionItemOverdue: false,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: false,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiApiBaseUrl: null,
        aiModelName: null,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockSettings,
      });

      const result = await getUserSettings(token);
      expect(result.theme).toBe('LIGHT');
      expect(result.showAchievements).toBe(false);
    });
  });

  describe('updateUserSettings', () => {
    it('should send PUT request with settings data', async () => {
      const request: UpdateUserSettingsRequest = {
        dueSoonDays: 5,
        staleOneOnOneDays: 10,
        anniversaryLookaheadDays: 45,
        theme: 'LIGHT',
        showAchievements: false,
        notifyActionItemOverdue: false,
        notifyActionItemDueSoon: false,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      const mockResponse: UserSettings = {
        ...request,
        aiApiBaseUrl: null,
        aiModelName: null,
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await updateUserSettings(token, request);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/settings'),
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(request),
          headers: expect.objectContaining({
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
          }),
        })
      );
      expect(result).toEqual(mockResponse);
    });

    it('should throw on 400 response for invalid data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        json: async () => ({
          status: 400,
          error: 'Bad Request',
          message: 'Validation failed',
          timestamp: '2026-01-01T00:00:00Z',
        }),
      });

      const request: UpdateUserSettingsRequest = {
        dueSoonDays: 0, // invalid
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: true,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      await expect(updateUserSettings(token, request)).rejects.toThrow();
    });

    it('should throw on 401 response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: async () => ({
          status: 401,
          error: 'Unauthorized',
          message: 'Authentication failed',
          timestamp: '2026-01-01T00:00:00Z',
        }),
      });

      const request: UpdateUserSettingsRequest = {
        dueSoonDays: 3,
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: true,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      await expect(updateUserSettings(token, request)).rejects.toThrow();
    });

    it('should handle network errors', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      const request: UpdateUserSettingsRequest = {
        dueSoonDays: 3,
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: true,
        notifyActionItemDueSoon: true,
        notifyStaleOneOnOne: true,
        notifyUpcomingAnniversary: true,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      await expect(updateUserSettings(token, request)).rejects.toThrow('Network error');
    });

    it('should update all notification toggles', async () => {
      const request: UpdateUserSettingsRequest = {
        dueSoonDays: 3,
        staleOneOnOneDays: 14,
        anniversaryLookaheadDays: 30,
        theme: 'DARK',
        showAchievements: true,
        notifyActionItemOverdue: false,
        notifyActionItemDueSoon: false,
        notifyStaleOneOnOne: false,
        notifyUpcomingAnniversary: false,
        aiEnabled: false,
        aiPrivacyMode: true,
        aiWritingStyle: 'NARRATIVE',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => request,
      });

      const result = await updateUserSettings(token, request);
      expect(result.notifyActionItemOverdue).toBe(false);
      expect(result.notifyActionItemDueSoon).toBe(false);
      expect(result.notifyStaleOneOnOne).toBe(false);
      expect(result.notifyUpcomingAnniversary).toBe(false);
    });
  });
});
