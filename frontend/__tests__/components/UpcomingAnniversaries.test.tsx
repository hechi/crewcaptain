import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import UpcomingAnniversaries from '@/components/dashboard/UpcomingAnniversaries';
import { UpcomingAnniversary } from '@/types/dashboard';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  );
});

const mockAnniversaries: UpcomingAnniversary[] = [
  {
    personId: 'person-1',
    personName: 'Alice Smith',
    startDate: '2023-05-15',
    anniversaryDate: '2026-05-15',
    yearsCompleted: 3,
    daysUntil: 5,
  },
  {
    personId: 'person-2',
    personName: 'Bob Jones',
    startDate: '2025-05-20',
    anniversaryDate: '2026-05-20',
    yearsCompleted: 1,
    daysUntil: 10,
  },
];

describe('UpcomingAnniversaries', () => {
  it('should render empty state when no anniversaries', () => {
    render(<UpcomingAnniversaries anniversaries={[]} />);
    expect(screen.getByTestId('anniversaries-empty')).toBeInTheDocument();
    expect(screen.getByText(/no upcoming anniversaries/i)).toBeInTheDocument();
  });

  it('should render list of anniversaries', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    expect(screen.getByTestId('anniversaries-list')).toBeInTheDocument();
    expect(screen.getAllByTestId('anniversary-item')).toHaveLength(2);
  });

  it('should display person name as link', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    const personLinks = screen.getAllByTestId('anniversary-person');
    expect(personLinks[0]).toHaveTextContent('Alice Smith');
    expect(personLinks[0]).toHaveAttribute('href', '/people/person-1');
  });

  it('should display years completed with correct pluralization', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    expect(screen.getByText('3 years')).toBeInTheDocument();
    expect(screen.getByText('1 year')).toBeInTheDocument();
  });

  it('should display days until badge', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    const badges = screen.getAllByTestId('anniversary-days-until');
    expect(badges[0]).toHaveTextContent('in 5d');
    expect(badges[1]).toHaveTextContent('in 10d');
  });

  it('should display "Today!" when anniversary is today', () => {
    const todayAnniversary: UpcomingAnniversary[] = [{
      personId: 'person-3',
      personName: 'Charlie',
      startDate: '2024-05-10',
      anniversaryDate: '2026-05-10',
      yearsCompleted: 2,
      daysUntil: 0,
    }];
    render(<UpcomingAnniversaries anniversaries={todayAnniversary} />);
    expect(screen.getByText('Today!')).toBeInTheDocument();
  });

  it('should not render empty state when anniversaries exist', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    expect(screen.queryByTestId('anniversaries-empty')).not.toBeInTheDocument();
  });

  it('should display anniversary date', () => {
    render(<UpcomingAnniversaries anniversaries={mockAnniversaries} />);
    const dates = screen.getAllByTestId('anniversary-date');
    expect(dates).toHaveLength(2);
  });
});
