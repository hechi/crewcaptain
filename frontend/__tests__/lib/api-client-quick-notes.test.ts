import {
  createQuickNote,
  getQuickNote,
  updateQuickNote,
  deleteQuickNote,
  listQuickNotes,
  assignQuickNoteToPerson,
  attachQuickNote,
  convertQuickNote,
  archiveQuickNote,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';

const mockToken = 'test-jwt-token';
const quickNoteId = '770e8400-e29b-41d4-a716-446655440001';
const personId = '550e8400-e29b-41d4-a716-446655440000';

const mockQuickNote = {
  id: quickNoteId,
  personId: null,
  text: 'Remember to follow up',
  sensitive: false,
  status: 'INBOX',
  attachedEntryId: null,
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

const mockPaginatedNotes = {
  content: [mockQuickNote],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

let mockFetch: jest.Mock;

beforeEach(() => {
  mockFetch = jest.fn();
  global.fetch = mockFetch;
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('createQuickNote', () => {
  it('should send POST request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockQuickNote),
    });

    const data = { text: 'Remember to follow up' };
    await createQuickNote(mockToken, data);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return created quick note', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockQuickNote),
    });

    const result = await createQuickNote(mockToken, { text: 'Test' });
    expect(result).toEqual(mockQuickNote);
  });

  it('should send with personId and sensitive flag', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, personId, sensitive: true }),
    });

    const data = { text: 'Sensitive note', personId, sensitive: true };
    await createQuickNote(mockToken, data);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes', {
      method: 'POST',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should throw ApiException on error', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
      json: () => Promise.resolve({
        status: 400,
        error: 'Bad Request',
        message: 'Text must not be blank',
        timestamp: '2026-05-10T10:00:00Z',
      }),
    });

    await expect(createQuickNote(mockToken, { text: '' })).rejects.toThrow(ApiException);
  });
});

describe('getQuickNote', () => {
  it('should send GET request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockQuickNote),
    });

    await getQuickNote(mockToken, quickNoteId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return quick note', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockQuickNote),
    });

    const result = await getQuickNote(mockToken, quickNoteId);
    expect(result).toEqual(mockQuickNote);
  });
});

describe('updateQuickNote', () => {
  it('should send PUT request with correct URL and body', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, text: 'Updated' }),
    });

    const data = { text: 'Updated' };
    await updateQuickNote(mockToken, quickNoteId, data);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('deleteQuickNote', () => {
  it('should send DELETE request with correct URL', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(undefined),
    });

    await deleteQuickNote(mockToken, quickNoteId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('listQuickNotes', () => {
  it('should send GET request without params', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken);

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should send GET request with status filter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken, { status: 'INBOX' });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes?status=INBOX', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should send GET request with personId filter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken, { personId });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes?personId=${personId}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should send GET request with pagination', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken, { page: 1, size: 10 });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes?page=1&size=10', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should send GET request with selfAssigned filter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken, { selfAssigned: true });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes?selfAssigned=true', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should send GET request with selfAssigned and status filter', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    await listQuickNotes(mockToken, { selfAssigned: true, status: 'INBOX' });

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/quick-notes?status=INBOX&selfAssigned=true', {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });

  it('should return paginated response', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockPaginatedNotes),
    });

    const result = await listQuickNotes(mockToken);
    expect(result).toEqual(mockPaginatedNotes);
  });
});

describe('assignQuickNoteToPerson', () => {
  it('should send POST request with personId', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, personId }),
    });

    await assignQuickNoteToPerson(mockToken, quickNoteId, { personId });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}/assign`, {
      method: 'POST',
      body: JSON.stringify({ personId }),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('attachQuickNote', () => {
  it('should send POST request to attach endpoint with entryId', async () => {
    const entryId = '880e8400-e29b-41d4-a716-446655440002';
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, status: 'ATTACHED', attachedEntryId: entryId }),
    });

    await attachQuickNote(mockToken, quickNoteId, { entryId });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}/attach`, {
      method: 'POST',
      body: JSON.stringify({ entryId }),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('convertQuickNote', () => {
  it('should send POST request to convert endpoint with personId', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, status: 'CONVERTED', personId }),
    });

    await convertQuickNote(mockToken, quickNoteId, { personId });

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}/convert`, {
      method: 'POST',
      body: JSON.stringify({ personId }),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});

describe('archiveQuickNote', () => {
  it('should send POST request to archive endpoint', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ...mockQuickNote, status: 'ARCHIVED' }),
    });

    await archiveQuickNote(mockToken, quickNoteId);

    expect(mockFetch).toHaveBeenCalledWith(`/api/v1/quick-notes/${quickNoteId}/archive`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${mockToken}`,
      },
    });
  });
});
