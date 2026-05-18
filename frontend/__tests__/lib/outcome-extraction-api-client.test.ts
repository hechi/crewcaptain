import { extractOutcomes, applyOutcomes } from '@/lib/api-client';

// Mock fetch globally
const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('AI Outcome Extraction API Client', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('extractOutcomes', () => {
    it('should call the extract-outcomes endpoint with POST', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          actionItems: [
            { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 7 },
          ],
          decisions: ['Decision 1'],
          error: null,
        }),
      });

      const result = await extractOutcomes('test-token', 'person-123', 'entry-456');

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/persons/person-123/one-on-one-entries/entry-456/extract-outcomes'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token',
            'Content-Type': 'application/json',
          }),
        })
      );

      expect(result.actionItems).toHaveLength(1);
      expect(result.actionItems[0].title).toBe('Task 1');
      expect(result.decisions).toEqual(['Decision 1']);
      expect(result.error).toBeNull();
    });

    it('should return error response from API', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          actionItems: [],
          decisions: [],
          error: 'AI not configured',
        }),
      });

      const result = await extractOutcomes('test-token', 'person-123', 'entry-456');

      expect(result.error).toBe('AI not configured');
      expect(result.actionItems).toHaveLength(0);
    });

    it('should throw on HTTP error', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: () => Promise.resolve({
          status: 404,
          error: 'Not Found',
          message: 'Entry not found',
          timestamp: '2026-05-18T00:00:00Z',
        }),
      });

      await expect(extractOutcomes('test-token', 'person-123', 'entry-456'))
        .rejects.toThrow();
    });
  });

  describe('applyOutcomes', () => {
    it('should call the apply-outcomes endpoint with POST and body', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({
          actionItemsCreated: 2,
          decisionsAppended: 1,
        }),
      });

      const request = {
        actionItems: [
          { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 7 },
          { title: 'Task 2', ownerType: 'PERSON', suggestedDaysToDue: null },
        ],
        decisions: ['Decision 1'],
      };

      const result = await applyOutcomes('test-token', 'person-123', 'entry-456', request);

      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining('/persons/person-123/one-on-one-entries/entry-456/apply-outcomes'),
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer test-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(request),
        })
      );

      expect(result.actionItemsCreated).toBe(2);
      expect(result.decisionsAppended).toBe(1);
    });

    it('should throw on HTTP error', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: () => Promise.resolve({
          status: 404,
          error: 'Not Found',
          message: 'Person not found',
          timestamp: '2026-05-18T00:00:00Z',
        }),
      });

      await expect(
        applyOutcomes('test-token', 'person-123', 'entry-456', {
          actionItems: [],
          decisions: [],
        })
      ).rejects.toThrow();
    });
  });
});
