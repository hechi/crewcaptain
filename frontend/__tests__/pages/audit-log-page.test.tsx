import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import AuditLogPage from '@/app/audit-log/page';

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: () => ({
    data: { accessToken: 'test-token', user: { name: 'Test User' } },
    status: 'authenticated',
  }),
  signOut: jest.fn(),
}));

// Mock the API client
jest.mock('@/lib/api-client', () => ({
  getAuditLog: jest.fn(),
  getUnreadNotificationCount: jest.fn().mockResolvedValue({ count: 0 }),
}));

import { getAuditLog } from '@/lib/api-client';
const mockGetAuditLog = getAuditLog as jest.MockedFunction<typeof getAuditLog>;

const mockEntries = [
  {
    id: '1',
    action: 'CREATE' as const,
    entityType: 'PERSON' as const,
    entityId: 'person-1',
    personId: 'person-1',
    summary: 'Created person "Alice Smith"',
    createdAt: new Date().toISOString(),
  },
  {
    id: '2',
    action: 'DELETE' as const,
    entityType: 'ACTION_ITEM' as const,
    entityId: 'ai-1',
    personId: 'person-1',
    summary: 'Deleted action item "Fix bug"',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
  },
];

describe('AuditLogPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the page title', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(<AuditLogPage />);

    expect(screen.getByText('Audit Log')).toBeInTheDocument();
  });

  it('should show loading state initially', () => {
    mockGetAuditLog.mockReturnValue(new Promise(() => {})); // never resolves

    render(<AuditLogPage />);

    expect(screen.getByTestId('audit-log-loading')).toBeInTheDocument();
  });

  it('should show empty state when no entries', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByTestId('audit-log-empty')).toBeInTheDocument();
    });
    expect(screen.getByText('No audit log entries')).toBeInTheDocument();
  });

  it('should display audit log entries', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByText('Created person "Alice Smith"')).toBeInTheDocument();
    });
    expect(screen.getByText('Deleted action item "Fix bug"')).toBeInTheDocument();
    expect(screen.getAllByTestId('audit-log-entry')).toHaveLength(2);
  });

  it('should show error state on API failure', async () => {
    mockGetAuditLog.mockRejectedValue(new Error('Network error'));

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByTestId('audit-log-error')).toBeInTheDocument();
    });
    expect(screen.getByText('Failed to load audit log')).toBeInTheDocument();
  });

  it('should filter by entity type', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByTestId('entity-type-filter')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('entity-type-filter'), {
      target: { value: 'PERSON' },
    });

    await waitFor(() => {
      expect(mockGetAuditLog).toHaveBeenCalledWith(
        'test-token',
        expect.objectContaining({ entityType: 'PERSON' })
      );
    });
  });

  it('should filter by action', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByTestId('action-filter')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('action-filter'), {
      target: { value: 'DELETE' },
    });

    await waitFor(() => {
      expect(mockGetAuditLog).toHaveBeenCalledWith(
        'test-token',
        expect.objectContaining({ action: 'DELETE' })
      );
    });
  });

  it('should display total count', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByText('2 entries total')).toBeInTheDocument();
    });
  });

  it('should show action badges with correct labels', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByText('Created')).toBeInTheDocument();
      expect(screen.getByText('Deleted')).toBeInTheDocument();
    });
  });

  it('should show entity type badges', async () => {
    mockGetAuditLog.mockResolvedValue({
      content: mockEntries,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });

    render(<AuditLogPage />);

    await waitFor(() => {
      expect(screen.getByText('Person')).toBeInTheDocument();
      expect(screen.getByText('Action Item')).toBeInTheDocument();
    });
  });
});
