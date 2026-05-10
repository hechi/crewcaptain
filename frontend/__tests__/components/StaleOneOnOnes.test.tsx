import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StaleOneOnOnes from '@/components/dashboard/StaleOneOnOnes';
import { StaleOneOnOneReminder } from '@/types/dashboard';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  );
});

const mockReminders: StaleOneOnOneReminder[] = [
  {
    personId: 'person-1',
    personName: 'Alice Smith',
    cadenceType: 'WEEKLY',
    customIntervalDays: null,
    lastMeetingDate: '2026-04-25T10:00:00Z',
    daysSinceLastMeeting: 15,
    expectedIntervalDays: 7,
  },
  {
    personId: 'person-2',
    personName: 'Bob Jones',
    cadenceType: 'CUSTOM',
    customIntervalDays: 10,
    lastMeetingDate: '2026-04-20T10:00:00Z',
    daysSinceLastMeeting: 20,
    expectedIntervalDays: 10,
  },
];

describe('StaleOneOnOnes', () => {
  it('should render empty state when no reminders', () => {
    render(<StaleOneOnOnes reminders={[]} />);
    expect(screen.getByTestId('stale-1on1s-empty')).toBeInTheDocument();
    expect(screen.getByText(/all 1:1s are on track/i)).toBeInTheDocument();
  });

  it('should render list of stale reminders', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    expect(screen.getByTestId('stale-1on1s-list')).toBeInTheDocument();
    expect(screen.getAllByTestId('stale-1on1-item')).toHaveLength(2);
  });

  it('should display person name as link', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    const personLinks = screen.getAllByTestId('stale-1on1-person');
    expect(personLinks[0]).toHaveTextContent('Alice Smith');
    expect(personLinks[0]).toHaveAttribute('href', '/people/person-1');
  });

  it('should display days since last meeting', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    expect(screen.getByText('15 days since last 1:1')).toBeInTheDocument();
    expect(screen.getByText('20 days since last 1:1')).toBeInTheDocument();
  });

  it('should display cadence label for weekly', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    expect(screen.getByText('Cadence: weekly')).toBeInTheDocument();
  });

  it('should display cadence label for custom interval', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    expect(screen.getByText('Cadence: every 10 days')).toBeInTheDocument();
  });

  it('should display overdue badge with correct days', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    const badges = screen.getAllByTestId('stale-1on1-overdue-badge');
    expect(badges[0]).toHaveTextContent('8d overdue'); // 15 - 7
    expect(badges[1]).toHaveTextContent('10d overdue'); // 20 - 10
  });

  it('should not render empty state when reminders exist', () => {
    render(<StaleOneOnOnes reminders={mockReminders} />);
    expect(screen.queryByTestId('stale-1on1s-empty')).not.toBeInTheDocument();
  });

  it('should display biweekly cadence label', () => {
    const biweeklyReminder: StaleOneOnOneReminder[] = [{
      personId: 'person-3',
      personName: 'Charlie',
      cadenceType: 'BIWEEKLY',
      customIntervalDays: null,
      lastMeetingDate: '2026-04-20T10:00:00Z',
      daysSinceLastMeeting: 20,
      expectedIntervalDays: 14,
    }];
    render(<StaleOneOnOnes reminders={biweeklyReminder} />);
    expect(screen.getByText('Cadence: biweekly')).toBeInTheDocument();
  });

  it('should display monthly cadence label', () => {
    const monthlyReminder: StaleOneOnOneReminder[] = [{
      personId: 'person-4',
      personName: 'Diana',
      cadenceType: 'MONTHLY',
      customIntervalDays: null,
      lastMeetingDate: '2026-04-01T10:00:00Z',
      daysSinceLastMeeting: 39,
      expectedIntervalDays: 30,
    }];
    render(<StaleOneOnOnes reminders={monthlyReminder} />);
    expect(screen.getByText('Cadence: monthly')).toBeInTheDocument();
  });
});
