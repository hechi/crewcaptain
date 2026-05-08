import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import OneOnOneEntryCard from '@/components/one-on-one/OneOnOneEntryCard';
import { OneOnOneEntry } from '@/types/one-on-one';

// Mock next/link
jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

describe('OneOnOneEntryCard', () => {
  const baseEntry: OneOnOneEntry = {
    id: 'entry-1',
    personId: 'person-1',
    meetingDate: '2025-05-08T14:00:00Z',
    agendaItems: [
      { id: 'a1', text: 'Review goals', checked: false, displayOrder: 0, createdAt: '2025-05-08T14:00:00Z' },
      { id: 'a2', text: 'Discuss timeline', checked: true, displayOrder: 1, createdAt: '2025-05-08T14:00:00Z' },
    ],
    notesMarkdown: 'These are some meeting notes about the project status and next steps.',
    outcomesMarkdown: 'Agreed on timeline.',
    sensitive: false,
    createdAt: '2025-05-08T14:00:00Z',
    updatedAt: '2025-05-08T14:00:00Z',
  };

  it('renders the meeting date', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" />);

    expect(screen.getByTestId('entry-card-date')).toBeInTheDocument();
  });

  it('renders notes preview', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" />);

    expect(screen.getByTestId('entry-card-notes-preview')).toBeInTheDocument();
    expect(screen.getByTestId('entry-card-notes-preview').textContent).toBe(
      'These are some meeting notes about the project status and next steps.'
    );
  });

  it('truncates notes preview to ~100 characters', () => {
    const longNotes = 'A'.repeat(150);
    const entry: OneOnOneEntry = { ...baseEntry, notesMarkdown: longNotes };

    render(<OneOnOneEntryCard entry={entry} personId="person-1" />);

    const preview = screen.getByTestId('entry-card-notes-preview');
    expect(preview.textContent).toHaveLength(101); // 100 chars + ellipsis character
    expect(preview.textContent).toContain('…');
  });

  it('shows agenda count', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" />);

    expect(screen.getByTestId('entry-card-agenda-count')).toBeInTheDocument();
    expect(screen.getByTestId('entry-card-agenda-count').textContent).toBe('2 items');
  });

  it('shows sensitive badge when sensitive is true', () => {
    const sensitiveEntry: OneOnOneEntry = { ...baseEntry, sensitive: true };

    render(<OneOnOneEntryCard entry={sensitiveEntry} personId="person-1" />);

    expect(screen.getByTestId('sensitive-badge')).toBeInTheDocument();
  });

  it('does not show sensitive badge when sensitive is false', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" />);

    expect(screen.queryByTestId('sensitive-badge')).not.toBeInTheDocument();
  });

  it('hides content when hideSensitiveContent is true and entry is sensitive', () => {
    const sensitiveEntry: OneOnOneEntry = { ...baseEntry, sensitive: true };

    render(<OneOnOneEntryCard entry={sensitiveEntry} personId="person-1" hideSensitiveContent={true} />);

    expect(screen.getByTestId('entry-card-hidden')).toBeInTheDocument();
    expect(screen.getByText('Sensitive content hidden')).toBeInTheDocument();
    expect(screen.queryByTestId('entry-card-notes-preview')).not.toBeInTheDocument();
  });

  it('shows content when hideSensitiveContent is true but entry is not sensitive', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" hideSensitiveContent={true} />);

    expect(screen.queryByTestId('entry-card-hidden')).not.toBeInTheDocument();
    expect(screen.getByTestId('entry-card-notes-preview')).toBeInTheDocument();
  });

  it('shows "No notes" when notesMarkdown is null', () => {
    const noNotesEntry: OneOnOneEntry = { ...baseEntry, notesMarkdown: null };

    render(<OneOnOneEntryCard entry={noNotesEntry} personId="person-1" />);

    expect(screen.getByTestId('entry-card-no-notes')).toBeInTheDocument();
    expect(screen.getByText('No notes')).toBeInTheDocument();
  });

  it('links to the correct entry detail page', () => {
    render(<OneOnOneEntryCard entry={baseEntry} personId="person-1" />);

    const link = screen.getByTestId('one-on-one-entry-card');
    expect(link).toHaveAttribute('href', '/people/person-1/one-on-ones/entry-1');
  });
});
