import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import RememberItemsList from '@/components/RememberItemsList';
import { PinnedRememberItem } from '@/types/person';

const mockItems: PinnedRememberItem[] = [
  { id: 'item-1', text: 'Prefers async communication', color: 'cyan', tag: null, sensitive: false, displayOrder: 0, createdAt: '2025-01-01T00:00:00Z' },
  { id: 'item-2', text: 'Working on promotion case', color: 'cyan', tag: null, sensitive: false, displayOrder: 1, createdAt: '2025-01-02T00:00:00Z' },
  { id: 'item-3', text: 'Has a dog named Max', color: 'cyan', tag: null, sensitive: false, displayOrder: 2, createdAt: '2025-01-03T00:00:00Z' },
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
    fireEvent.click(screen.getByRole('button', { name: /add/i }));

    expect(onAdd).toHaveBeenCalledWith('New item text');
  });

  it('should clear input after adding an item', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const input = screen.getByLabelText('New remember item') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'New item text' } });
    fireEvent.click(screen.getByRole('button', { name: /add/i }));

    expect(input.value).toBe('');
  });

  it('should not call onAdd when input is empty', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    fireEvent.click(screen.getByRole('button', { name: /add/i }));
    expect(onAdd).not.toHaveBeenCalled();
  });

  it('should call onRemove when delete button is clicked', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const deleteButtons = screen.getAllByLabelText(/remove/i);
    fireEvent.click(deleteButtons[1]);

    expect(onRemove).toHaveBeenCalledWith('item-2');
  });

  it('should call onReorder when moving item down via keyboard', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const dragHandles = screen.getAllByLabelText(/reorder/i);
    fireEvent.keyDown(dragHandles[0], { key: 'ArrowDown' });

    expect(onReorder).toHaveBeenCalledWith(['item-2', 'item-1', 'item-3']);
  });

  it('should call onReorder when moving item up via keyboard', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const dragHandles = screen.getAllByLabelText(/reorder/i);
    fireEvent.keyDown(dragHandles[1], { key: 'ArrowUp' });

    expect(onReorder).toHaveBeenCalledWith(['item-2', 'item-1', 'item-3']);
  });

  it('should not move item up when already at the top', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const dragHandles = screen.getAllByLabelText(/reorder/i);
    fireEvent.keyDown(dragHandles[0], { key: 'ArrowUp' });

    expect(onReorder).not.toHaveBeenCalled();
  });

  it('should not move item down when already at the bottom', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const dragHandles = screen.getAllByLabelText(/reorder/i);
    fireEvent.keyDown(dragHandles[2], { key: 'ArrowDown' });

    expect(onReorder).not.toHaveBeenCalled();
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

  it('should render drag handles for each item', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const dragHandles = screen.getAllByLabelText(/reorder/i);
    expect(dragHandles).toHaveLength(3);
  });

  it('should have draggable attribute on items', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    const items = screen.getAllByTestId('remember-item');
    items.forEach(item => {
      expect(item).toHaveAttribute('draggable', 'true');
    });
  });

  it('should show helper text about drag and keyboard reordering', () => {
    const onAdd = jest.fn();
    const onRemove = jest.fn();
    const onReorder = jest.fn();
    render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

    expect(screen.getByText(/drag to reorder or use tab \+ arrow keys/i)).toBeInTheDocument();
  });

  describe('Drag and Drop', () => {
    it('should handle drag start and end', () => {
      const onAdd = jest.fn();
      const onRemove = jest.fn();
      const onReorder = jest.fn();
      render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

      const items = screen.getAllByTestId('remember-item');
      
      // Drag start
      fireEvent.dragStart(items[0]);
      
      // Drag end
      fireEvent.dragEnd(items[0]);
    });

    it('should handle drag over and drop to reorder', () => {
      const onAdd = jest.fn();
      const onRemove = jest.fn();
      const onReorder = jest.fn();
      render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

      const items = screen.getAllByTestId('remember-item');
      
      // Drag item 0 to position 1
      fireEvent.dragStart(items[0]);
      fireEvent.dragOver(items[1]);
      fireEvent.drop(items[1]);

      expect(onReorder).toHaveBeenCalledWith(['item-2', 'item-1', 'item-3']);
    });

    it('should not call onReorder when dropping on same position', () => {
      const onAdd = jest.fn();
      const onRemove = jest.fn();
      const onReorder = jest.fn();
      render(<RememberItemsList items={mockItems} onAdd={onAdd} onRemove={onRemove} onReorder={onReorder} />);

      const items = screen.getAllByTestId('remember-item');
      
      // Drag and drop on same item
      fireEvent.dragStart(items[0]);
      fireEvent.dragOver(items[0]);
      fireEvent.drop(items[0]);

      expect(onReorder).not.toHaveBeenCalled();
    });
  });
});
