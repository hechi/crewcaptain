import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonDetailPage from '@/app/people/[id]/page';
import { Person } from '@/types/person';

const mockPush = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useParams: () => ({ id: '123e4567-e89b-12d3-a456-426614174000' }),
  useSearchParams: () => ({
    get: () => null,
  }),
}));

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
}));

jest.mock('@/lib/api-client', () => ({
  getPerson: jest.fn(),
  updatePerson: jest.fn(),
  deletePerson: jest.fn(),
  setMorale: jest.fn(),
  addRememberItem: jest.fn(),
  updateRememberItem: jest.fn(),
  removeRememberItem: jest.fn(),
  reorderRememberItems: jest.fn(),
  exportPersonMarkdown: jest.fn(),
}));

import { useSession } from 'next-auth/react';
import { getPerson, exportPersonMarkdown } from '@/lib/api-client';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockGetPerson = getPerson as jest.MockedFunction<typeof getPerson>;
const mockExportPersonMarkdown = exportPersonMarkdown as jest.MockedFunction<typeof exportPersonMarkdown>;

const mockPerson: Person = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  name: 'Jane Smith',
  preferredName: 'Jane',
  roleTitle: 'Senior Engineer',
  timezone: 'Europe/Berlin',
  startDate: '2024-03-15',
  email: 'jane@example.com',
  tags: ['engineering', 'senior'],
  moraleStatus: 'GREEN',
  moraleNote: 'Doing great',
  pinnedRememberItems: [
    { id: 'item-1', text: 'Prefers async', color: 'cyan', tag: null, sensitive: false, displayOrder: 0, createdAt: '2025-05-08T12:00:00Z' },
  ],
  atAGlance: { last1on1Date: null, openActionItemsCount: null, activePdpGoalsSummary: null },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

describe('PersonDetailPage - Export Button', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSession.mockReturnValue({
      data: { accessToken: 'test-token', user: {}, expires: '' },
      status: 'authenticated',
      update: jest.fn(),
    } as any);
    mockGetPerson.mockResolvedValue(mockPerson);
  });

  it('should render export button', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument();
    });
  });

  it('should show export button with correct label', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toHaveAttribute(
        'aria-label',
        'Export person data as Markdown'
      );
    });
  });

  it('should have accessible label on export button', async () => {
    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toHaveAttribute(
        'aria-label',
        'Export person data as Markdown'
      );
    });
  });

  it('should call exportPersonMarkdown when export button is clicked', async () => {
    mockExportPersonMarkdown.mockResolvedValue('# Jane Smith\n\n## Profile\n');

    // Mock URL.createObjectURL and URL.revokeObjectURL
    const mockCreateObjectURL = jest.fn().mockReturnValue('blob:http://localhost/test');
    const mockRevokeObjectURL = jest.fn();
    global.URL.createObjectURL = mockCreateObjectURL;
    global.URL.revokeObjectURL = mockRevokeObjectURL;

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('export-button'));

    await waitFor(() => {
      expect(mockExportPersonMarkdown).toHaveBeenCalledWith(
        'test-token',
        '123e4567-e89b-12d3-a456-426614174000'
      );
    });
  });

  it('should show exporting state while export is in progress', async () => {
    let resolveExport: (value: string) => void;
    const exportPromise = new Promise<string>((resolve) => {
      resolveExport = resolve;
    });
    mockExportPersonMarkdown.mockReturnValue(exportPromise);

    global.URL.createObjectURL = jest.fn().mockReturnValue('blob:http://localhost/test');
    global.URL.revokeObjectURL = jest.fn();

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('export-button'));

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeDisabled();
    });

    // Resolve the export
    resolveExport!('# Export');

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).not.toBeDisabled();
    });
  });

  it('should show error message when export fails', async () => {
    mockExportPersonMarkdown.mockRejectedValue(new Error('Export failed'));

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('export-button'));

    await waitFor(() => {
      expect(screen.getByTestId('error-message')).toHaveTextContent('Export failed');
    });
  });

  it('should trigger file download with correct filename', async () => {
    mockExportPersonMarkdown.mockResolvedValue('# Jane Smith\n\n## Profile\n');

    const mockCreateObjectURL = jest.fn().mockReturnValue('blob:http://localhost/test');
    const mockRevokeObjectURL = jest.fn();
    global.URL.createObjectURL = mockCreateObjectURL;
    global.URL.revokeObjectURL = mockRevokeObjectURL;

    // Track anchor elements created for download
    const createdAnchors: HTMLAnchorElement[] = [];
    const originalCreateElement = document.createElement.bind(document);
    jest.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const el = originalCreateElement(tagName);
      if (tagName === 'a') {
        el.click = jest.fn();
        createdAnchors.push(el as HTMLAnchorElement);
      }
      return el;
    });

    render(<PersonDetailPage />);

    await waitFor(() => {
      expect(screen.getByTestId('export-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('export-button'));

    await waitFor(() => {
      expect(createdAnchors.length).toBeGreaterThan(0);
    });

    const anchor = createdAnchors[createdAnchors.length - 1];
    expect(anchor.download).toBe('Jane Smith.md');
    expect(anchor.href).toBe('blob:http://localhost/test');
    expect(anchor.click).toHaveBeenCalled();

    // Verify cleanup
    expect(mockRevokeObjectURL).toHaveBeenCalledWith('blob:http://localhost/test');
  });
});
