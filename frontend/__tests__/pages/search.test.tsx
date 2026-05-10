import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import SearchPage from '@/app/search/page';
import * as apiClient from '@/lib/api-client';

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: jest.fn(() => ({
    data: { accessToken: 'test-token', user: { name: 'Test User' } },
    status: 'authenticated',
  })),
}));

// Mock next/navigation
const mockReplace = jest.fn();
jest.mock('next/navigation', () => ({
  useSearchParams: jest.fn(() => ({
    get: jest.fn((key: string) => {
      if (key === 'q') return '';
      if (key === 'page') return '0';
      return null;
    }),
    getAll: jest.fn(() => []),
  })),
  useRouter: jest.fn(() => ({
    replace: mockReplace,
  })),
}));

// Mock api-client
jest.mock('@/lib/api-client', () => ({
  search: jest.fn(),
}));

// Mock Pagination component
jest.mock('@/components/Pagination', () => {
  return function MockPagination({ currentPage, totalPages, onPageChange }: {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
  }) {
    return (
      <div data-testid="pagination">
        <span>Page {currentPage + 1} of {totalPages}</span>
        <button onClick={() => onPageChange(currentPage + 1)} data-testid="next-page">Next</button>
      </div>
    );
  };
});

describe('SearchPage', () => {
  const mockSearch = apiClient.search as jest.MockedFunction<typeof apiClient.search>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render search page with title', () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-title')).toHaveTextContent('Search');
  });

  it('should render search input', () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-input')).toBeInTheDocument();
  });

  it('should render search submit button', () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-submit')).toBeInTheDocument();
  });

  it('should render type filter buttons', () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-type-filters')).toBeInTheDocument();
    expect(screen.getByTestId('search-filter-PERSON')).toHaveTextContent('People');
    expect(screen.getByTestId('search-filter-ACTION_ITEM')).toHaveTextContent('Action Items');
    expect(screen.getByTestId('search-filter-KUDOS')).toHaveTextContent('Kudos');
  });

  it('should show initial state when no search performed', () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-initial-state')).toBeInTheDocument();
  });

  it('should perform search on form submit', async () => {
    mockSearch.mockResolvedValue({
      results: [
        {
          id: '123',
          type: 'PERSON',
          title: 'John Doe',
          snippet: 'Engineer',
          personId: '123',
          personName: 'John Doe',
          sensitive: false,
          createdAt: '2026-05-10T10:00:00Z',
          relevanceScore: 0.9,
        },
      ],
      query: 'john',
      totalCount: 1,
      page: 0,
      size: 20,
      totalPages: 1,
    });

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'john' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalledWith('test-token', {
        q: 'john',
        type: undefined,
        page: 0,
        size: 20,
      });
    });

    await waitFor(() => {
      expect(screen.getByTestId('search-results-summary')).toHaveTextContent('1 result for "john"');
    });
  });

  it('should display search results', async () => {
    mockSearch.mockResolvedValue({
      results: [
        {
          id: 'result-1',
          type: 'PERSON',
          title: 'Alice Smith',
          snippet: 'Product Manager',
          personId: 'result-1',
          personName: 'Alice Smith',
          sensitive: false,
          createdAt: '2026-05-10T10:00:00Z',
          relevanceScore: 0.9,
        },
        {
          id: 'result-2',
          type: 'ACTION_ITEM',
          title: 'Review Alice proposal',
          snippet: 'Need to review by Friday',
          personId: 'result-1',
          personName: 'Alice Smith',
          sensitive: false,
          createdAt: '2026-05-10T09:00:00Z',
          relevanceScore: 0.7,
        },
      ],
      query: 'alice',
      totalCount: 2,
      page: 0,
      size: 20,
      totalPages: 1,
    });

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'alice' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getByTestId('search-results-list')).toBeInTheDocument();
    });

    expect(screen.getByTestId('search-result-result-1')).toBeInTheDocument();
    expect(screen.getByTestId('search-result-result-2')).toBeInTheDocument();
  });

  it('should show empty state when no results', async () => {
    mockSearch.mockResolvedValue({
      results: [],
      query: 'nonexistent',
      totalCount: 0,
      page: 0,
      size: 20,
      totalPages: 0,
    });

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'nonexistent' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getByTestId('search-empty-state')).toBeInTheDocument();
    });

    expect(screen.getByTestId('search-results-summary')).toHaveTextContent('No results found for "nonexistent"');
  });

  it('should show error message on search failure', async () => {
    mockSearch.mockRejectedValue(new Error('Network error'));

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getByTestId('search-error')).toHaveTextContent('Network error');
    });
  });

  it('should not submit when query is empty', () => {
    render(<SearchPage />);

    const form = screen.getByTestId('search-input').closest('form')!;
    fireEvent.submit(form);

    expect(mockSearch).not.toHaveBeenCalled();
  });

  it('should show pagination when multiple pages', async () => {
    mockSearch.mockResolvedValue({
      results: Array.from({ length: 20 }, (_, i) => ({
        id: `result-${i}`,
        type: 'PERSON' as const,
        title: `Person ${i}`,
        snippet: null,
        personId: `result-${i}`,
        personName: `Person ${i}`,
        sensitive: false,
        createdAt: '2026-05-10T10:00:00Z',
        relevanceScore: 0.5,
      })),
      query: 'test',
      totalCount: 50,
      page: 0,
      size: 20,
      totalPages: 3,
    });

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getByTestId('pagination')).toBeInTheDocument();
    });
  });

  it('should toggle type filter on click', async () => {
    mockSearch.mockResolvedValue({
      results: [],
      query: 'test',
      totalCount: 0,
      page: 0,
      size: 20,
      totalPages: 0,
    });

    render(<SearchPage />);

    // Type a query first
    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalled();
    });

    // Click a type filter
    fireEvent.click(screen.getByTestId('search-filter-PERSON'));

    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalledWith('test-token', expect.objectContaining({
        type: ['PERSON'],
      }));
    });
  });

  it('should display plural results text', async () => {
    mockSearch.mockResolvedValue({
      results: [
        { id: '1', type: 'PERSON', title: 'A', snippet: null, personId: '1', personName: 'A', sensitive: false, createdAt: '2026-05-10T10:00:00Z', relevanceScore: 0.9 },
        { id: '2', type: 'PERSON', title: 'B', snippet: null, personId: '2', personName: 'B', sensitive: false, createdAt: '2026-05-10T10:00:00Z', relevanceScore: 0.8 },
      ],
      query: 'test',
      totalCount: 2,
      page: 0,
      size: 20,
      totalPages: 1,
    });

    render(<SearchPage />);

    const input = screen.getByTestId('search-input');
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.submit(input.closest('form')!);

    await waitFor(() => {
      expect(screen.getByTestId('search-results-summary')).toHaveTextContent('2 results for "test"');
    });
  });
});
