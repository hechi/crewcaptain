import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import DueSoonActionItems from '@/components/dashboard/DueSoonActionItems';
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
    title: 'Prepare presentation',
    dueDate: '2026-05-12',
    ownerType: 'MANAGER',
  },
  {
    id: '2',
    personId: 'person-2',
    personName: 'Bob Jones',
    title: 'Code review',
    dueDate: '2026-05-13',
    ownerType: 'PERSON',
  },
];

describe('DueSoonActionItems', () => {
  it('should render empty state when no items', () => {
    render(<DueSoonActionItems items={[]} />);
    expect(screen.getByTestId('due-soon-items-empty')).toBeInTheDocument();
    expect(screen.getByText(/no items due soon/i)).toBeInTheDocument();
  });

  it('should render list of due soon items', () => {
    render(<DueSoonActionItems items={mockItems} />);
    expect(screen.getByTestId('due-soon-items-list')).toBeInTheDocument();
    expect(screen.getAllByTestId('due-soon-item')).toHaveLength(2);
  });

  it('should display item title', () => {
    render(<DueSoonActionItems items={mockItems} />);
    expect(screen.getByText('Prepare presentation')).toBeInTheDocument();
    expect(screen.getByText('Code review')).toBeInTheDocument();
  });

  it('should display person name as link', () => {
    render(<DueSoonActionItems items={mockItems} />);
    const personLinks = screen.getAllByTestId('due-soon-item-person');
    expect(personLinks[0]).toHaveTextContent('Alice Smith');
    expect(personLinks[0]).toHaveAttribute('href', '/people/person-1');
  });

  it('should display due date', () => {
    render(<DueSoonActionItems items={mockItems} />);
    const dueDates = screen.getAllByTestId('due-soon-item-due-date');
    expect(dueDates).toHaveLength(2);
  });

  it('should show "You" badge for MANAGER owner type', () => {
    render(<DueSoonActionItems items={mockItems} />);
    expect(screen.getByText('You')).toBeInTheDocument();
  });

  it('should show "Them" badge for PERSON owner type', () => {
    render(<DueSoonActionItems items={mockItems} />);
    expect(screen.getByText('Them')).toBeInTheDocument();
  });

  it('should not render empty state when items exist', () => {
    render(<DueSoonActionItems items={mockItems} />);
    expect(screen.queryByTestId('due-soon-items-empty')).not.toBeInTheDocument();
  });
});
