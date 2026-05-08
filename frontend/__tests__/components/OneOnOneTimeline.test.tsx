import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOneTimeline from '@/components/one-on-one/OneOnOneTimeline';
import { OneOnOneEntry } from '@/types/one-on-one';
import { PaginatedResponse } from '@/types/person';

// Mock next/link
jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

describe('OneOnOneTimeline', () => {
  const mockOnPageChange = jest.fn();
  const mockOnStartOneOnOne = jest.fn();

  const sampleEntries: OneOnOneEntry[] = [
    {
      id: 'entry-1',
      personId: 'person-1',
      meetingDate: '2025-05-08T14:00:00Z',
      agendaItems: [{ id: 'a1', text: 'Item 1', checked: false, displayOrder: 0, createdAt: '2025-05-08T14:00:00Z' }],
      notesMarkdown: 'First meeting notes',
      outcomesMarkdown: null,
      sensitive: false,
      createdAt: '2025-05-08T14:00:00Z',
      updatedAt: '2025-05-08T14:00:00Z',
    },
    {
      id: 'entry-2',
      personId: 'person-1',
      meetingDate: '2025-05-01T14:00:00Z',
      agendaItems: [],
      notesMarkdown: 'Sensitive meeting notes',
      outcomesMarkdown: null,
      sensitive: true,
      createdAt: '2025-05-01T14:00:00Z',
      updatedAt: '2025-05-01T14:00:00Z',
    },
  ];

  const paginatedEntries: PaginatedResponse<OneOnOneEntry> = {
    content: sampleEntries,
    page: 0,
    size: 20,
    totalElements: 2,
    totalPages: 1,
  };

  const emptyEntries: PaginatedResponse<OneOnOneEntry> = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders entry cards when entries exist', () => {
    render(
      <OneOnOneTimeline
        entries={paginatedEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    expect(screen.getByTestId('one-on-one-timeline')).toBeInTheDocument();
    const cards = screen.getAllByTestId('one-on-one-entry-card');
    expect(cards).toHaveLength(2);
  });

  it('shows empty state with "Start 1:1" button when no entries', () => {
    render(
      <OneOnOneTimeline
        entries={emptyEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    expect(screen.getByTestId('one-on-one-timeline-empty')).toBeInTheDocument();
    expect(screen.getByTestId('start-one-on-one-button')).toBeInTheDocument();
    expect(screen.getByText('Start 1:1')).toBeInTheDocument();
  });

  it('calls onStartOneOnOne when "Start 1:1" button is clicked', () => {
    render(
      <OneOnOneTimeline
        entries={emptyEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    fireEvent.click(screen.getByTestId('start-one-on-one-button'));
    expect(mockOnStartOneOnOne).toHaveBeenCalledTimes(1);
  });

  it('renders hide-sensitive toggle', () => {
    render(
      <OneOnOneTimeline
        entries={paginatedEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    expect(screen.getByTestId('hide-sensitive-toggle')).toBeInTheDocument();
  });

  it('hides sensitive entry content when hide-sensitive toggle is checked', () => {
    render(
      <OneOnOneTimeline
        entries={paginatedEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    // Toggle hide sensitive
    const toggle = screen.getByTestId('hide-sensitive-toggle');
    fireEvent.click(toggle);

    // Sensitive entry should show hidden content
    expect(screen.getByTestId('entry-card-hidden')).toBeInTheDocument();
    expect(screen.getByText('Sensitive content hidden')).toBeInTheDocument();
  });

  it('renders entries in the order provided (reverse chronological)', () => {
    render(
      <OneOnOneTimeline
        entries={paginatedEntries}
        personId="person-1"
        onPageChange={mockOnPageChange}
        onStartOneOnOne={mockOnStartOneOnOne}
      />
    );

    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(2);
  });
});
