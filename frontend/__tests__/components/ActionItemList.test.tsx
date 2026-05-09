import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ActionItemList from '@/components/action-items/ActionItemList';
import { PaginatedActionItemResponse } from '@/types/action-item';

const mockData: PaginatedActionItemResponse = {
  content: [
    {
      id: 'item-1',
      personId: 'person-1',
      title: 'First task',
      description: null,
      ownerType: 'MANAGER',
      dueDate: '2026-05-20',
      status: 'OPEN',
      originatingEntryId: null,
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
    },
    {
      id: 'item-2',
      personId: 'person-1',
      title: 'Second task',
      description: 'With description',
      ownerType: 'PERSON',
      dueDate: null,
      status: 'DONE',
      originatingEntryId: null,
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

const emptyData: PaginatedActionItemResponse = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

describe('ActionItemList', () => {
  it('renders action item cards', () => {
    render(<ActionItemList data={mockData} />);
    const cards = screen.getAllByTestId('action-item-card');
    expect(cards).toHaveLength(2);
  });

  it('shows empty state when no items', () => {
    render(<ActionItemList data={emptyData} emptyMessage="No tasks found" />);
    expect(screen.getByText('No tasks found')).toBeInTheDocument();
  });

  it('renders status filter buttons when onStatusFilterChange provided', () => {
    render(<ActionItemList data={mockData} onStatusFilterChange={jest.fn()} statusFilter={null} />);
    expect(screen.getByTestId('filter-all')).toBeInTheDocument();
    expect(screen.getByTestId('filter-open')).toBeInTheDocument();
    expect(screen.getByTestId('filter-done')).toBeInTheDocument();
    expect(screen.getByTestId('filter-canceled')).toBeInTheDocument();
  });

  it('calls onStatusFilterChange with correct value', () => {
    const onFilterChange = jest.fn();
    render(<ActionItemList data={mockData} onStatusFilterChange={onFilterChange} statusFilter={null} />);
    fireEvent.click(screen.getByTestId('filter-open'));
    expect(onFilterChange).toHaveBeenCalledWith('OPEN');
  });

  it('calls onStatusFilterChange with null for ALL filter', () => {
    const onFilterChange = jest.fn();
    render(<ActionItemList data={mockData} onStatusFilterChange={onFilterChange} statusFilter="OPEN" />);
    fireEvent.click(screen.getByTestId('filter-all'));
    expect(onFilterChange).toHaveBeenCalledWith(null);
  });

  it('does not render filter bar when onStatusFilterChange not provided', () => {
    render(<ActionItemList data={mockData} />);
    expect(screen.queryByTestId('action-item-filter-bar')).not.toBeInTheDocument();
  });

  it('passes onComplete to cards', () => {
    const onComplete = jest.fn();
    render(<ActionItemList data={mockData} onComplete={onComplete} />);
    // First item is OPEN, should have complete button
    fireEvent.click(screen.getByTestId('action-item-complete-btn'));
    expect(onComplete).toHaveBeenCalledWith('item-1');
  });

  it('renders pagination when multiple pages', () => {
    const multiPageData = { ...mockData, totalPages: 3 };
    render(<ActionItemList data={multiPageData} onPageChange={jest.fn()} />);
    // Pagination component should be rendered
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('does not render pagination for single page', () => {
    render(<ActionItemList data={mockData} onPageChange={jest.fn()} />);
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
  });
});
