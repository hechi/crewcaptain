import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActionItemStatusBadge from '@/components/action-items/ActionItemStatusBadge';

describe('ActionItemStatusBadge', () => {
  it('renders OPEN status with correct label', () => {
    render(<ActionItemStatusBadge status="OPEN" />);
    const badge = screen.getByTestId('action-item-status-badge');
    expect(badge).toHaveTextContent('Open');
    expect(badge).toHaveAttribute('aria-label', 'Status: Open');
  });

  it('renders DONE status with correct label', () => {
    render(<ActionItemStatusBadge status="DONE" />);
    const badge = screen.getByTestId('action-item-status-badge');
    expect(badge).toHaveTextContent('Done');
    expect(badge).toHaveAttribute('aria-label', 'Status: Done');
  });

  it('renders CANCELED status with correct label', () => {
    render(<ActionItemStatusBadge status="CANCELED" />);
    const badge = screen.getByTestId('action-item-status-badge');
    expect(badge).toHaveTextContent('Canceled');
    expect(badge).toHaveAttribute('aria-label', 'Status: Canceled');
  });
});
