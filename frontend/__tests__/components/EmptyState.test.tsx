import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import EmptyState from '@/components/EmptyState';

describe('EmptyState', () => {
  it('should render default message', () => {
    render(<EmptyState />);
    expect(screen.getByText('No people in your directory yet.')).toBeInTheDocument();
  });

  it('should render custom message', () => {
    render(<EmptyState message="Your team list is empty" />);
    expect(screen.getByText('Your team list is empty')).toBeInTheDocument();
  });

  it('should render CTA button when onAction is provided', () => {
    const onAction = jest.fn();
    render(<EmptyState onAction={onAction} />);
    expect(screen.getByTestId('empty-state-cta')).toBeInTheDocument();
    expect(screen.getByText('Add your first person')).toBeInTheDocument();
  });

  it('should not render CTA button when onAction is not provided', () => {
    render(<EmptyState />);
    expect(screen.queryByTestId('empty-state-cta')).not.toBeInTheDocument();
  });

  it('should call onAction when CTA button is clicked', () => {
    const onAction = jest.fn();
    render(<EmptyState onAction={onAction} />);

    fireEvent.click(screen.getByTestId('empty-state-cta'));
    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('should render custom action label', () => {
    const onAction = jest.fn();
    render(<EmptyState onAction={onAction} actionLabel="Get started" />);
    expect(screen.getByText('Get started')).toBeInTheDocument();
  });
});
