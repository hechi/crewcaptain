import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import AgendaItemList, { AgendaItemInput } from '@/components/one-on-one/AgendaItemList';

// Mock crypto.randomUUID
Object.defineProperty(globalThis, 'crypto', {
  value: { randomUUID: () => 'test-uuid-' + Math.random().toString(36).substr(2, 9) },
});

describe('AgendaItemList', () => {
  const mockOnChange = jest.fn();

  const sampleItems: AgendaItemInput[] = [
    { id: 'item-1', text: 'Review Q2 goals', checked: false },
    { id: 'item-2', text: 'Discuss project timeline', checked: true },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders existing agenda items', () => {
    render(<AgendaItemList items={sampleItems} onChange={mockOnChange} />);

    expect(screen.getByText('Review Q2 goals')).toBeInTheDocument();
    expect(screen.getByText('Discuss project timeline')).toBeInTheDocument();
  });

  it('renders add button and input', () => {
    render(<AgendaItemList items={[]} onChange={mockOnChange} />);

    expect(screen.getByTestId('agenda-item-input')).toBeInTheDocument();
    expect(screen.getByTestId('agenda-item-add-button')).toBeInTheDocument();
  });

  it('adds a new item when add button is clicked with valid text', () => {
    render(<AgendaItemList items={sampleItems} onChange={mockOnChange} />);

    const input = screen.getByTestId('agenda-item-input');
    fireEvent.change(input, { target: { value: 'New agenda item' } });
    fireEvent.click(screen.getByTestId('agenda-item-add-button'));

    expect(mockOnChange).toHaveBeenCalledTimes(1);
    const newItems = mockOnChange.mock.calls[0][0];
    expect(newItems).toHaveLength(3);
    expect(newItems[2].text).toBe('New agenda item');
    expect(newItems[2].checked).toBe(false);
  });

  it('validates blank text and shows error when add is clicked with empty input', () => {
    render(<AgendaItemList items={[]} onChange={mockOnChange} />);

    fireEvent.click(screen.getByTestId('agenda-item-add-button'));

    expect(screen.getByTestId('agenda-item-error')).toBeInTheDocument();
    expect(screen.getByText('Agenda item text cannot be blank')).toBeInTheDocument();
    expect(mockOnChange).not.toHaveBeenCalled();
  });

  it('validates blank text when input is only whitespace', () => {
    render(<AgendaItemList items={[]} onChange={mockOnChange} />);

    const input = screen.getByTestId('agenda-item-input');
    fireEvent.change(input, { target: { value: '   ' } });
    fireEvent.click(screen.getByTestId('agenda-item-add-button'));

    expect(screen.getByTestId('agenda-item-error')).toBeInTheDocument();
    expect(mockOnChange).not.toHaveBeenCalled();
  });

  it('removes an item when remove button is clicked', () => {
    render(<AgendaItemList items={sampleItems} onChange={mockOnChange} />);

    fireEvent.click(screen.getByTestId('agenda-item-remove-0'));

    expect(mockOnChange).toHaveBeenCalledTimes(1);
    const newItems = mockOnChange.mock.calls[0][0];
    expect(newItems).toHaveLength(1);
    expect(newItems[0].id).toBe('item-2');
  });

  it('toggles checked state when checkbox is clicked', () => {
    render(<AgendaItemList items={sampleItems} onChange={mockOnChange} />);

    fireEvent.click(screen.getByTestId('agenda-item-checkbox-0'));

    expect(mockOnChange).toHaveBeenCalledTimes(1);
    const newItems = mockOnChange.mock.calls[0][0];
    expect(newItems[0].checked).toBe(true);
    expect(newItems[1].checked).toBe(true); // unchanged
  });

  it('clears error when user types in the input', () => {
    render(<AgendaItemList items={[]} onChange={mockOnChange} />);

    // Trigger error
    fireEvent.click(screen.getByTestId('agenda-item-add-button'));
    expect(screen.getByTestId('agenda-item-error')).toBeInTheDocument();

    // Type to clear error
    const input = screen.getByTestId('agenda-item-input');
    fireEvent.change(input, { target: { value: 'a' } });
    expect(screen.queryByTestId('agenda-item-error')).not.toBeInTheDocument();
  });
});
