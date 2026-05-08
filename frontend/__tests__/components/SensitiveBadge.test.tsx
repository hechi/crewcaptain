import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import SensitiveBadge from '@/components/one-on-one/SensitiveBadge';

describe('SensitiveBadge', () => {
  it('renders the badge element', () => {
    render(<SensitiveBadge />);

    expect(screen.getByTestId('sensitive-badge')).toBeInTheDocument();
  });

  it('renders "Sensitive" text', () => {
    render(<SensitiveBadge />);

    expect(screen.getByText('Sensitive')).toBeInTheDocument();
  });

  it('renders lock icon (svg element)', () => {
    render(<SensitiveBadge />);

    const badge = screen.getByTestId('sensitive-badge');
    const svg = badge.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('has role="img" with accessible label', () => {
    render(<SensitiveBadge />);

    const badge = screen.getByRole('img', { name: 'Sensitive content' });
    expect(badge).toBeInTheDocument();
  });

  it('has title attribute for tooltip', () => {
    render(<SensitiveBadge />);

    const badge = screen.getByTestId('sensitive-badge');
    expect(badge).toHaveAttribute('title', 'Sensitive');
  });
});
