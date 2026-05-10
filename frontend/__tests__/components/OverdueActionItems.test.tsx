import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import OverdueActionItems from '@/components/dashboard/OverdueActionItems';
import { DashboardActionItem } from '@/types/dashboard';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  );
});

const mockItems: DashboardActionItem[] = [
  {
    id: '1',
    personId: 'person-1',
    personName: 'Alice Smith',
    title: 'Review PR #42',
    dueDate: '2026-05-07',
    ownerType: 'PERSON',
  },
  {
    id: '2',
    personId: 'person-2',
    personName: 'Bob Jones',
    title: 'Submit quarterly report',
    dueDate: '2026-05-05',
    ownerType: 'MANAGER',
  },
];

describe('OverdueActionItems', () => {
  it('should render empty state when no items', () => {
    render(<OverdueActionItems items={[]} />);
    expect(screen.getByTestId('overdue-items-empty')).toBeInTheDocument();
    expect(screen.getByText(/all caught up/i)).toBeInTheDocument();
  });

  it('should render list of overdue items', () => {
    render(<OverdueActionItems items={mockItems} />);
    expect(screen.getByTestId('overdue-items-list')).toBeInTheDocument();
    expect(screen.getAllByTestId('overdue-item')).toHaveLength(2);
  });

  it('should display item title', () => {
    render(<OverdueActionItems items={mockItems} />);
    expect(screen.getByText('Review PR #42')).toBeInTheDocument();
    expect(screen.getByText('Submit quarterly report')).toBeInTheDocument();
  });

  it('should display person name as link', () => {
    render(<OverdueActionItems items={mockItems} />);
    const personLinks = screen.getAllByTestId('overdue-item-person');
    expect(personLinks[0]).toHaveTextContent('Alice Smith');
    expect(personLinks[0]).toHaveAttribute('href', '/people/person-1');
  });

  it('should display due date', () => {
    render(<OverdueActionItems items={mockItems} />);
    const dueDates = screen.getAllByTestId('overdue-item-due-date');
    expect(dueDates).toHaveLength(2);
  });

  it('should show "You" badge for MANAGER owner type', () => {
    render(<OverdueActionItems items={mockItems} />);
    expect(screen.getByText('You')).toBeInTheDocument();
  });

  it('should show "Them" badge for PERSON owner type', () => {
    render(<OverdueActionItems items={mockItems} />);
    expect(screen.getByText('Them')).toBeInTheDocument();
  });

  it('should not render empty state when items exist', () => {
    render(<OverdueActionItems items={mockItems} />);
    expect(screen.queryByTestId('overdue-items-empty')).not.toBeInTheDocument();
  });
});
