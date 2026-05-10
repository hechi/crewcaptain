import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import QuickNoteList from '@/components/quick-notes/QuickNoteList';
import { QuickNote } from '@/types/quick-note';
import { Person } from '@/types/person';

describe('QuickNoteList', () => {
  const mockNotes: QuickNote[] = [
    {
      id: 'note-1',
      personId: null,
      text: 'First note',
      sensitive: false,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
    },
    {
      id: 'note-2',
      personId: 'person-1',
      text: 'Second note',
      sensitive: true,
      status: 'INBOX',
      attachedEntryId: null,
      createdAt: '2026-05-10T11:00:00Z',
      updatedAt: '2026-05-10T11:00:00Z',
    },
  ];

  const mockPersons: Person[] = [
    { id: 'person-1', name: 'Alice Smith', roleTitle: 'Engineer', moraleStatus: 'GREEN', moraleNote: null, tags: [], pinnedRememberItems: [], atAGlance: { last1on1Date: null, openActionItemsCount: 0, activePdpGoalsSummary: '' } } as Person,
  ];

  const mockOnCreateNote = jest.fn();
  const mockOnArchive = jest.fn();
  const mockOnConvert = jest.fn();
  const mockOnAttach = jest.fn();
  const mockOnAssignPerson = jest.fn();
  const mockOnDelete = jest.fn();
  const mockOnStatusFilterChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the list container', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByTestId('quick-note-list')).toBeInTheDocument();
  });

  it('should render the create form', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByTestId('quick-note-form')).toBeInTheDocument();
  });

  it('should render status filter buttons', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByTestId('quick-note-status-filter')).toBeInTheDocument();
    expect(screen.getByTestId('filter-all')).toBeInTheDocument();
    expect(screen.getByTestId('filter-INBOX')).toBeInTheDocument();
    expect(screen.getByTestId('filter-ATTACHED')).toBeInTheDocument();
    expect(screen.getByTestId('filter-CONVERTED')).toBeInTheDocument();
    expect(screen.getByTestId('filter-ARCHIVED')).toBeInTheDocument();
  });

  it('should render all quick note cards', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByTestId('quick-note-card-note-1')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-card-note-2')).toBeInTheDocument();
  });

  it('should render empty state when no notes', () => {
    render(
      <QuickNoteList
        quickNotes={[]}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByText('No quick notes yet — capture something above!')).toBeInTheDocument();
  });

  it('should call onStatusFilterChange when filter button is clicked', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    screen.getByTestId('filter-INBOX').click();
    expect(mockOnStatusFilterChange).toHaveBeenCalledWith('INBOX');
  });

  it('should call onStatusFilterChange with null when All is clicked', () => {
    render(
      <QuickNoteList
        quickNotes={mockNotes}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        statusFilter="INBOX"
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    screen.getByTestId('filter-all').click();
    expect(mockOnStatusFilterChange).toHaveBeenCalledWith(null);
  });

  it('should pass isSubmitting to form', () => {
    render(
      <QuickNoteList
        quickNotes={[]}
        persons={mockPersons}
        onCreateNote={mockOnCreateNote}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        isSubmitting={true}
        statusFilter={null}
        onStatusFilterChange={mockOnStatusFilterChange}
      />
    );
    expect(screen.getByTestId('quick-note-submit-btn')).toHaveTextContent('Saving...');
  });
});
