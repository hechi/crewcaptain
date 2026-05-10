import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import QuickNoteCard from '@/components/quick-notes/QuickNoteCard';
import { QuickNote } from '@/types/quick-note';
import { Person } from '@/types/person';

describe('QuickNoteCard', () => {
  const mockNote: QuickNote = {
    id: 'note-1',
    personId: null,
    text: 'Remember to follow up on project timeline',
    sensitive: false,
    status: 'INBOX',
    attachedEntryId: null,
    createdAt: '2026-05-10T10:00:00Z',
    updatedAt: '2026-05-10T10:00:00Z',
  };

  const mockPersons: Person[] = [
    { id: 'person-1', name: 'Alice Smith', roleTitle: 'Engineer', moraleStatus: 'GREEN', moraleNote: null, tags: [], pinnedRememberItems: [], atAGlance: { last1on1Date: null, openActionItemsCount: 0, activePdpGoalsSummary: '' } } as Person,
    { id: 'person-2', name: 'Bob Jones', roleTitle: 'Designer', moraleStatus: 'GREEN', moraleNote: null, tags: [], pinnedRememberItems: [], atAGlance: { last1on1Date: null, openActionItemsCount: 0, activePdpGoalsSummary: '' } } as Person,
  ];

  const mockOnArchive = jest.fn();
  const mockOnConvert = jest.fn();
  const mockOnAttach = jest.fn();
  const mockOnAssignPerson = jest.fn();
  const mockOnDelete = jest.fn();
  const mockOnFetchEntries = jest.fn().mockResolvedValue([]);

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderCard = (note: QuickNote = mockNote) => {
    return render(
      <QuickNoteCard
        quickNote={note}
        persons={mockPersons}
        onArchive={mockOnArchive}
        onConvert={mockOnConvert}
        onAttach={mockOnAttach}
        onAssignPerson={mockOnAssignPerson}
        onDelete={mockOnDelete}
        onFetchEntries={mockOnFetchEntries}
      />
    );
  };

  it('should render quick note text', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-text')).toHaveTextContent('Remember to follow up on project timeline');
  });

  it('should render formatted date', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-date')).toBeInTheDocument();
  });

  it('should render status badge', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-status')).toHaveTextContent('INBOX');
  });

  it('should not render sensitive badge when not sensitive', () => {
    renderCard();
    expect(screen.queryByTestId('quick-note-sensitive-badge')).not.toBeInTheDocument();
  });

  it('should render sensitive badge when sensitive', () => {
    const sensitiveNote: QuickNote = { ...mockNote, sensitive: true };
    renderCard(sensitiveNote);
    expect(screen.getByTestId('quick-note-sensitive-badge')).toBeInTheDocument();
  });

  it('should show action buttons for INBOX notes', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-attach-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-convert-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-archive-btn')).toBeInTheDocument();
    expect(screen.getByTestId('quick-note-delete-btn')).toBeInTheDocument();
  });

  it('should show assign person button when no person assigned', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-assign-btn')).toBeInTheDocument();
  });

  it('should not show assign person button when person is assigned', () => {
    const assignedNote: QuickNote = { ...mockNote, personId: 'person-1' };
    renderCard(assignedNote);
    expect(screen.queryByTestId('quick-note-assign-btn')).not.toBeInTheDocument();
  });

  it('should show person badge when person is assigned', () => {
    const assignedNote: QuickNote = { ...mockNote, personId: 'person-1' };
    renderCard(assignedNote);
    expect(screen.getByTestId('quick-note-person-badge')).toHaveTextContent('Alice Smith');
  });

  it('should only show delete button for non-INBOX notes', () => {
    const archivedNote: QuickNote = { ...mockNote, status: 'ARCHIVED' };
    renderCard(archivedNote);
    expect(screen.queryByTestId('quick-note-attach-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('quick-note-convert-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('quick-note-archive-btn')).not.toBeInTheDocument();
    expect(screen.getByTestId('quick-note-delete-btn')).toBeInTheDocument();
  });

  it('should show person picker when assign button is clicked', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-assign-btn'));
    expect(screen.getByTestId('person-picker')).toBeInTheDocument();
    expect(screen.getByTestId('person-picker-select')).toBeInTheDocument();
  });

  it('should call onAssignPerson when a person is selected', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-assign-btn'));
    fireEvent.change(screen.getByTestId('person-picker-select'), { target: { value: 'person-1' } });
    expect(mockOnAssignPerson).toHaveBeenCalledWith('note-1', 'person-1');
  });

  it('should call onConvert when convert button is clicked and person is assigned', () => {
    const assignedNote: QuickNote = { ...mockNote, personId: 'person-1' };
    renderCard(assignedNote);
    fireEvent.click(screen.getByTestId('quick-note-convert-btn'));
    expect(mockOnConvert).toHaveBeenCalledWith('note-1', 'person-1');
  });

  it('should show convert person picker when no person assigned and convert clicked', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-convert-btn'));
    expect(screen.getByTestId('convert-person-picker')).toBeInTheDocument();
  });

  it('should call onArchive when archive button is clicked', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-archive-btn'));
    expect(mockOnArchive).toHaveBeenCalledWith('note-1');
  });

  it('should call onDelete when delete button is clicked', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-delete-btn'));
    expect(mockOnDelete).toHaveBeenCalledWith('note-1');
  });

  it('should show attach person picker when attach is clicked without person assigned', () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-attach-btn'));
    expect(screen.getByTestId('attach-person-picker')).toBeInTheDocument();
  });

  it('should show entry picker after selecting person in attach flow', async () => {
    renderCard();
    fireEvent.click(screen.getByTestId('quick-note-attach-btn'));
    fireEvent.change(screen.getByTestId('attach-person-picker-select'), { target: { value: 'person-1' } });
    // Entry picker should appear after person selection
    expect(screen.getByTestId('entry-picker')).toBeInTheDocument();
  });

  it('should render with correct test id', () => {
    renderCard();
    expect(screen.getByTestId('quick-note-card-note-1')).toBeInTheDocument();
  });

  it('should have accessible labels on action buttons', () => {
    renderCard();
    expect(screen.getByLabelText('Attach to 1:1')).toBeInTheDocument();
    expect(screen.getByLabelText('Convert to action item')).toBeInTheDocument();
    expect(screen.getByLabelText('Archive')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete quick note')).toBeInTheDocument();
  });
});
