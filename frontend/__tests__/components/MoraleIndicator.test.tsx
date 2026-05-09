import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MoraleIndicator from '@/components/MoraleIndicator';
import { MoraleStatus } from '@/types/person';

describe('MoraleIndicator', () => {
  it('should render green indicator for GREEN status', () => {
    render(<MoraleIndicator moraleStatus="GREEN" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ color: 'var(--color-morale-green)' });
    expect(indicator).toHaveStyle({ backgroundColor: 'rgba(57, 255, 133, 0.15)' });
    expect(indicator).toHaveTextContent('Green');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Green');
  });

  it('should render amber indicator for YELLOW status', () => {
    render(<MoraleIndicator moraleStatus="YELLOW" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ color: 'var(--color-morale-yellow)' });
    expect(indicator).toHaveStyle({ backgroundColor: 'rgba(255, 214, 0, 0.15)' });
    expect(indicator).toHaveTextContent('Yellow');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Yellow');
  });

  it('should render red indicator for RED status', () => {
    render(<MoraleIndicator moraleStatus="RED" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ color: 'var(--color-morale-red)' });
    expect(indicator).toHaveStyle({ backgroundColor: 'rgba(255, 45, 123, 0.15)' });
    expect(indicator).toHaveTextContent('Red');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Red');
  });

  it('should render gray indicator for UNKNOWN status', () => {
    render(<MoraleIndicator moraleStatus="UNKNOWN" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ color: 'var(--color-morale-unknown)' });
    expect(indicator).toHaveStyle({ backgroundColor: 'rgba(74, 85, 104, 0.2)' });
    expect(indicator).toHaveTextContent('Unknown');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Unknown');
  });

  it.each<[MoraleStatus, string]>([
    ['GREEN', 'var(--color-morale-green)'],
    ['YELLOW', 'var(--color-morale-yellow)'],
    ['RED', 'var(--color-morale-red)'],
    ['UNKNOWN', 'var(--color-morale-unknown)'],
  ])('should render correct text color for %s status', (status, expectedColor) => {
    render(<MoraleIndicator moraleStatus={status} />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ color: expectedColor });
  });
});
