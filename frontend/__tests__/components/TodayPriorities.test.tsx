import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import TodayPriorities from '@/components/dashboard/TodayPriorities';
import { TodayPriorityItem } from '@/types/dashboard';

describe('TodayPriorities', () => {
  const overdue: TodayPriorityItem = {
    kind: 'OVERDUE',
    id: 'ai-1',
    title: 'Review PR',
    personId: 'person-1',
    personName: 'Alice Smith',
    dueDate: '2026-05-07',
  };

  const dueSoon: TodayPriorityItem = {
    kind: 'DUE_SOON',
    id: 'ai-2',
    title: 'Submit report',
    personId: null,
    personName: null,
    dueDate: '2026-05-12',
  };

  const inboxNote: TodayPriorityItem = {
    kind: 'INBOX_NOTE',
    id: 'note-1',
    title: 'Prep board deck',
    personId: null,
    personName: null,
    dueDate: null,
  };

  it('should show an empty state when there are no priorities', () => {
    render(<TodayPriorities items={[]} />);
    expect(screen.getByTestId('today-priorities-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('today-priorities-list')).not.toBeInTheDocument();
  });

  it('should render a list of priority rows', () => {
    render(<TodayPriorities items={[overdue, dueSoon, inboxNote]} />);
    expect(screen.getByTestId('today-priorities-list')).toBeInTheDocument();
    expect(screen.getAllByTestId('today-priority-item')).toHaveLength(3);
  });

  it('should render each priority title', () => {
    render(<TodayPriorities items={[overdue, dueSoon, inboxNote]} />);
    expect(screen.getByText('Review PR')).toBeInTheDocument();
    expect(screen.getByText('Submit report')).toBeInTheDocument();
    expect(screen.getByText('Prep board deck')).toBeInTheDocument();
  });

  it('should link a person-scoped action item to the person page', () => {
    render(<TodayPriorities items={[overdue]} />);
    const row = screen.getByTestId('today-priority-item');
    const link = row.querySelector('a');
    expect(link).toHaveAttribute('href', '/people/person-1');
  });

  it('should link an inbox note to the My Notes page', () => {
    render(<TodayPriorities items={[inboxNote]} />);
    const row = screen.getByTestId('today-priority-item');
    const link = row.querySelector('a');
    expect(link).toHaveAttribute('href', '/my-notes');
  });

  it('should label each row with its kind', () => {
    render(<TodayPriorities items={[overdue, dueSoon, inboxNote]} />);
    expect(screen.getByText('Overdue')).toBeInTheDocument();
    expect(screen.getByText('Due soon')).toBeInTheDocument();
    expect(screen.getByText('Note')).toBeInTheDocument();
  });

  it('should show a due date for action items and none for notes', () => {
    render(<TodayPriorities items={[overdue, inboxNote]} />);
    const dueDates = screen.getAllByTestId('today-priority-due-date');
    // Only the action item has a due date
    expect(dueDates).toHaveLength(1);
  });
});
