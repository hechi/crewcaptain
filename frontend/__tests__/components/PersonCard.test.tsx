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
  workspaceId: null,
  atAGlance: {
    last1on1Date: null,
    openActionItemsCount: null,
    activePdpGoalsSummary: null,
  },
  createdAt: '2025-05-08T12:00:00Z',
  updatedAt: '2025-05-08T12:00:00Z',
  deletedAt: null,
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

  it('should show "Add sticky note" CTA when no remember items', () => {
    render(<PersonCard person={mockPerson} />);
    expect(screen.getByTestId('add-sticky-cta')).toBeInTheDocument();
    expect(screen.getByText('Add sticky note')).toBeInTheDocument();
  });

  it('should show sticky note previews for non-sensitive items', () => {
    const personWithNotes = {
      ...mockPerson,
      pinnedRememberItems: [
        { id: '1', text: 'Has 2 kids — picks up early Fridays', color: 'cyan' as const, tag: 'Family', sensitive: false, displayOrder: 0, createdAt: '2024-01-01T00:00:00Z' },
        { id: '2', text: 'Building house', color: 'amber' as const, tag: null, sensitive: false, displayOrder: 1, createdAt: '2024-01-02T00:00:00Z' },
      ],
    };
    render(<PersonCard person={personWithNotes} />);
    expect(screen.getByTestId('sticky-note-previews')).toBeInTheDocument();
  });

  it('should hide sensitive sticky notes from previews', () => {
    const personWithSensitive = {
      ...mockPerson,
      pinnedRememberItems: [
        { id: '1', text: 'Secret info', color: 'pink' as const, tag: null, sensitive: true, displayOrder: 0, createdAt: '2024-01-01T00:00:00Z' },
      ],
    };
    render(<PersonCard person={personWithSensitive} />);
    expect(screen.queryByText('Secret info')).not.toBeInTheDocument();
    expect(screen.getByText('+sensitive')).toBeInTheDocument();
  });
});
