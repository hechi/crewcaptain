import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import RememberItemsList from '@/components/RememberItemsList';
import { PinnedRememberItem } from '@/types/person';

const mockItems: PinnedRememberItem[] = [
  { id: 'item-1', text: 'Prefers async communication', displayOrder: 0, createdAt: '2025-01-01T00:00:00Z' },
  { id: 'item-2', text: 'Working on promotion case', displayOrder: 1, createdAt: '2025-01-02T00:00:00Z' },
  { id: 'item-3', text: 'Has a dog named Max', displayOrder: 2, createdAt: '2025-01-03T00:00:00Z' },
];

describe('RememberItemsList', () => {
  it('should render items in order', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const items = screen.getAllByTestId('remember-item');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Prefers async communication');
    expect(items[1]).toHaveTextContent('Working on promotion case');
    expect(items[2]).toHaveTextContent('Has a dog named Max');
  });

  it('should show empty message when no items', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={[]} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    expect(screen.getByText('No pinned items yet.')).toBeInTheDocument();
  });

  it('should call onAdd when adding a new item', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const input = screen.getByLabelText('New remember item');
    fireEvent.change(input, { target: { value: 'New item text' } });
    fireEvent.click(screen.getByText('Add'));

    expect(onAdd).toHaveBeenCalledWith('New item text');
  });

  it('should clear input after adding an item', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const input = screen.getByLabelText('New remember item') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'New item text' } });
    fireEvent.click(screen.getByText('Add'));

    expect(input.value).toBe('');
  });

  it('should not call onAdd when input is empty', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    fireEvent.click(screen.getByText('Add'));
    expect(onAdd).not.toHaveBeenCalled();
  });

  it('should call onRemove when remove button is clicked', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const removeButtons = screen.getAllByText('Remove');
    fireEvent.click(removeButtons[1]);

    expect(onRemove).toHaveBeenCalledWith('item-2');
  });

  it('should call onReorder when moving item down', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const downButton = screen.getByLabelText('Move "Prefers async communication" down');
    fireEvent.click(downButton);

    expect(onReorder).toHaveBeenCalledWith(['item-2', 'item-1', 'item-3']);
  });

  it('should call onReorder when moving item up', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const upButton = screen.getByLabelText('Move "Working on promotion case" up');
    fireEvent.click(upButton);

    expect(onReorder).toHaveBeenCalledWith(['item-2', 'item-1', 'item-3']);
  });

  it('should add item on Enter key press', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const input = screen.getByLabelText('New remember item');
    fireEvent.change(input, { target: { value: 'Enter item' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onAdd).toHaveBeenCalledWith('Enter item');
  });
});
