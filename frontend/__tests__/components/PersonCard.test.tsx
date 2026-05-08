import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import PersonCard from '@/components/PersonCard';
import { Person } from '@/types/person';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

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
  moraleNote: null,
  pinnedRememberItems: [],
  atAGlance: {
    last1on1Date: null,
    openActionItemsCount: null,
    activePdpGoalsSummary: null,
  },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
};

describe('PersonCard', () => {
  it('should render person name', () => {
    render(<PersonCard person={mockPerson} />);
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
  });

  it('should render person role/title', () => {
    render(<PersonCard person={mockPerson} />);
    expect(screen.getByText('Senior Engineer')).toBeInTheDocument();
  });

  it('should render morale indicator', () => {
    render(<PersonCard person={mockPerson} />);
    expect(screen.getByTestId('morale-indicator')).toBeInTheDocument();
    expect(screen.getByText('Green')).toBeInTheDocument();
  });

  it('should link to person detail page', () => {
    render(<PersonCard person={mockPerson} />);
    const link = screen.getByTestId('person-card');
    expect(link).toHaveAttribute('href', '/people/123e4567-e89b-12d3-a456-426614174000');
  });

  it('should not render role when roleTitle is null', () => {
    const personWithoutRole = { ...mockPerson, roleTitle: null };
    render(<PersonCard person={personWithoutRole} />);
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.queryByText('Senior Engineer')).not.toBeInTheDocument();
  });
});
