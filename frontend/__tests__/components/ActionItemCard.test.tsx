import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActionItemCard from '@/components/action-items/ActionItemCard';
import { ActionItem } from '@/types/action-item';

const mockItem: ActionItem = {
  id: 'item-1',
  personId: 'person-1',
  title: 'Follow up on project plan',
  description: 'Check progress with the team',
  ownerType: 'MANAGER',
  dueDate: '2026-05-20',
  status: 'OPEN',
  originatingEntryId: null,
  createdAt: '2026-05-10T10:00:00Z',
  updatedAt: '2026-05-10T10:00:00Z',
};

describe('ActionItemCard', () => {
  it('renders action item title', () => {
    render(<ActionItemCard item={mockItem} />);
    expect(screen.getByTestId('action-item-title')).toHaveTextContent('Follow up on project plan');
  });

  it('renders description when present', () => {
    render(<ActionItemCard item={mockItem} />);
    expect(screen.getByTestId('action-item-description')).toHaveTextContent('Check progress with the team');
  });

  it('does not render description when null', () => {
    const item = { ...mockItem, description: null };
    render(<ActionItemCard item={item} />);
    expect(screen.queryByTestId('action-item-description')).not.toBeInTheDocument();
  });

  it('renders due date', () => {
    render(<ActionItemCard item={mockItem} />);
    expect(screen.getByTestId('action-item-due-date')).toBeInTheDocument();
  });

  it('renders owner type badge for MANAGER', () => {
    render(<ActionItemCard item={mockItem} />);
    expect(screen.getByTestId('action-item-owner')).toHaveTextContent('Manager');
  });

  it('renders owner type badge for PERSON', () => {
    const item = { ...mockItem, ownerType: 'PERSON' as const };
    render(<ActionItemCard item={item} />);
    expect(screen.getByTestId('action-item-owner')).toHaveTextContent('Report');
  });

  it('renders status badge', () => {
    render(<ActionItemCard item={mockItem} />);
    expect(screen.getByTestId('action-item-status-badge')).toHaveTextContent('Open');
  });

  it('shows action buttons for OPEN items', () => {
    const onComplete = jest.fn();
    const onCancel = jest.fn();
    render(<ActionItemCard item={mockItem} onComplete={onComplete} onCancel={onCancel} />);
    expect(screen.getByTestId('action-item-complete-btn')).toBeInTheDocument();
    expect(screen.getByTestId('action-item-cancel-btn')).toBeInTheDocument();
  });

  it('does not show action buttons for DONE items', () => {
    const item = { ...mockItem, status: 'DONE' as const };
    render(<ActionItemCard item={item} onComplete={jest.fn()} onCancel={jest.fn()} />);
    expect(screen.queryByTestId('action-item-complete-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('action-item-cancel-btn')).not.toBeInTheDocument();
  });

  it('calls onComplete when complete button is clicked', () => {
    const onComplete = jest.fn();
    render(<ActionItemCard item={mockItem} onComplete={onComplete} />);
    fireEvent.click(screen.getByTestId('action-item-complete-btn'));
    expect(onComplete).toHaveBeenCalledWith('item-1');
  });

  it('calls onCancel when cancel button is clicked', () => {
    const onCancel = jest.fn();
    render(<ActionItemCard item={mockItem} onCancel={onCancel} />);
    fireEvent.click(screen.getByTestId('action-item-cancel-btn'));
    expect(onCancel).toHaveBeenCalledWith('item-1');
  });

  it('calls onDelete when delete button is clicked', () => {
    const onDelete = jest.fn();
    render(<ActionItemCard item={mockItem} onDelete={onDelete} />);
    fireEvent.click(screen.getByTestId('action-item-delete-btn'));
    expect(onDelete).toHaveBeenCalledWith('item-1');
  });

  it('calls onEdit when edit button is clicked', () => {
    const onEdit = jest.fn();
    render(<ActionItemCard item={mockItem} onEdit={onEdit} />);
    fireEvent.click(screen.getByTestId('action-item-edit-btn'));
    expect(onEdit).toHaveBeenCalledWith('item-1');
  });

  it('shows overdue indicator when item is past due', () => {
    const item = { ...mockItem, dueDate: '2020-01-01' };
    render(<ActionItemCard item={item} />);
    expect(screen.getByTestId('action-item-due-date')).toHaveTextContent('⚠');
  });

  it('applies strikethrough to DONE item title', () => {
    const item = { ...mockItem, status: 'DONE' as const };
    render(<ActionItemCard item={item} />);
    const title = screen.getByTestId('action-item-title');
    expect(title).toHaveStyle({ textDecoration: 'line-through' });
  });
});
