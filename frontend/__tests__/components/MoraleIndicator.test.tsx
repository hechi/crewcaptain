import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MoraleIndicator from '@/components/MoraleIndicator';
import { MoraleStatus } from '@/types/person';

describe('MoraleIndicator', () => {
  it('should render green color for GREEN status', () => {
    render(<MoraleIndicator moraleStatus="GREEN" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ backgroundColor: 'var(--color-morale-green)' });
    expect(indicator).toHaveTextContent('Green');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Green');
  });

  it('should render amber color for YELLOW status', () => {
    render(<MoraleIndicator moraleStatus="YELLOW" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ backgroundColor: 'var(--color-morale-yellow)' });
    expect(indicator).toHaveTextContent('Yellow');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Yellow');
  });

  it('should render red color for RED status', () => {
    render(<MoraleIndicator moraleStatus="RED" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ backgroundColor: 'var(--color-morale-red)' });
    expect(indicator).toHaveTextContent('Red');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Red');
  });

  it('should render gray color for UNKNOWN status', () => {
    render(<MoraleIndicator moraleStatus="UNKNOWN" />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ backgroundColor: 'var(--color-morale-unknown)' });
    expect(indicator).toHaveTextContent('Unknown');
    expect(indicator).toHaveAttribute('aria-label', 'Morale: Unknown');
  });

  it.each<[MoraleStatus, string]>([
    ['GREEN', 'var(--color-morale-green)'],
    ['YELLOW', 'var(--color-morale-yellow)'],
    ['RED', 'var(--color-morale-red)'],
    ['UNKNOWN', 'var(--color-morale-unknown)'],
  ])('should render correct color for %s status', (status, expectedColor) => {
    render(<MoraleIndicator moraleStatus={status} />);
    const indicator = screen.getByTestId('morale-indicator');
    expect(indicator).toHaveStyle({ backgroundColor: expectedColor });
  });
});
