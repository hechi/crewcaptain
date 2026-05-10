import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import PdpGoalStatusBadge from '@/components/pdp-goals/PdpGoalStatusBadge';

describe('PdpGoalStatusBadge', () => {
  it('should render ACTIVE status', () => {
    render(<PdpGoalStatusBadge status="ACTIVE" />);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-status-badge')).toHaveAttribute('aria-label', 'Status: Active');
  });

  it('should render ACHIEVED status', () => {
    render(<PdpGoalStatusBadge status="ACHIEVED" />);
    expect(screen.getByText('Achieved')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-status-badge')).toHaveAttribute('aria-label', 'Status: Achieved');
  });

  it('should render PAUSED status', () => {
    render(<PdpGoalStatusBadge status="PAUSED" />);
    expect(screen.getByText('Paused')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-status-badge')).toHaveAttribute('aria-label', 'Status: Paused');
  });

  it('should render DROPPED status', () => {
    render(<PdpGoalStatusBadge status="DROPPED" />);
    expect(screen.getByText('Dropped')).toBeInTheDocument();
    expect(screen.getByTestId('pdp-goal-status-badge')).toHaveAttribute('aria-label', 'Status: Dropped');
  });
});
