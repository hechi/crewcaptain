import {
  listWorkspaces,
  getWorkspace,
  createWorkspace,
  updateWorkspace,
  deleteWorkspace,
  assignPersonToWorkspace,
} from '@/lib/api-client';
import { Workspace } from '@/types/workspace';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('Workspace API Client', () => {
  const token = 'test-token';

  beforeEach(() => {
    mockFetch.mockReset();
  });

  describe('listWorkspaces', () => {
    it('should fetch workspaces with auth header', async () => {
      const mockWorkspaces: Workspace[] = [
        { id: 'ws-1', name: 'My Team', description: 'Direct reports', displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockWorkspaces,
      });

      const result = await listWorkspaces(token);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces'),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${token}`,
          }),
        })
      );
      expect(result).toEqual(mockWorkspaces);
    });
  });

  describe('getWorkspace', () => {
    it('should fetch a single workspace by id', async () => {
      const mockWorkspace: Workspace = {
        id: 'ws-1', name: 'My Team', description: 'Direct reports',
        displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockWorkspace,
      });

      const result = await getWorkspace(token, 'ws-1');

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces/ws-1'),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${token}`,
          }),
        })
      );
      expect(result).toEqual(mockWorkspace);
    });
  });

  describe('createWorkspace', () => {
    it('should POST workspace with name and description', async () => {
      const mockWorkspace: Workspace = {
        id: 'ws-new', name: 'Mentees', description: 'People I mentor',
        displayOrder: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockWorkspace,
      });

      const result = await createWorkspace(token, { name: 'Mentees', description: 'People I mentor' });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ name: 'Mentees', description: 'People I mentor' }),
        })
      );
      expect(result).toEqual(mockWorkspace);
    });
  });

  describe('updateWorkspace', () => {
    it('should PUT workspace with updated fields', async () => {
      const mockWorkspace: Workspace = {
        id: 'ws-1', name: 'Updated Name', description: null,
        displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockWorkspace,
      });

      const result = await updateWorkspace(token, 'ws-1', { name: 'Updated Name' });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces/ws-1'),
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify({ name: 'Updated Name' }),
        })
      );
      expect(result).toEqual(mockWorkspace);
    });
  });

  describe('deleteWorkspace', () => {
    it('should DELETE workspace by id', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({}),
      });

      await deleteWorkspace(token, 'ws-1');

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces/ws-1'),
        expect.objectContaining({
          method: 'DELETE',
        })
      );
    });
  });

  describe('assignPersonToWorkspace', () => {
    it('should PUT person workspace assignment', async () => {
      const mockPerson = {
        id: 'person-1', name: 'Alice', workspaceId: 'ws-1',
        moraleStatus: 'UNKNOWN', tags: [], pinnedRememberItems: [],
        atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
        createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockPerson,
      });

      const result = await assignPersonToWorkspace(token, 'person-1', { workspaceId: 'ws-1' });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces/persons/person-1/workspace'),
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify({ workspaceId: 'ws-1' }),
        })
      );
      expect(result.workspaceId).toBe('ws-1');
    });

    it('should PUT null workspaceId to unassign', async () => {
      const mockPerson = {
        id: 'person-1', name: 'Alice', workspaceId: null,
        moraleStatus: 'UNKNOWN', tags: [], pinnedRememberItems: [],
        atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
        createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockPerson,
      });

      const result = await assignPersonToWorkspace(token, 'person-1', { workspaceId: null });

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/workspaces/persons/person-1/workspace'),
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify({ workspaceId: null }),
        })
      );
      expect(result.workspaceId).toBeNull();
    });

    it('should throw on error response', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: async () => ({ status: 404, error: 'Not Found', message: 'Person not found', timestamp: '2026-01-01T00:00:00Z' }),
      });

      await expect(assignPersonToWorkspace(token, 'bad-id', { workspaceId: 'ws-1' })).rejects.toThrow();
    });
  });
});
