import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import KudosCard from '@/components/kudos/KudosCard';
import { Kudos } from '@/types/kudos';

describe('KudosCard', () => {
  const mockKudos: Kudos = {
    id: 'kudos-1',
    personId: 'person-1',
    date: '2026-05-10',
    text: 'Great job on the presentation!',
    tags: ['impact', 'collaboration'],
    createdAt: '2026-05-10T10:00:00Z',
    updatedAt: '2026-05-10T10:00:00Z',
  };

  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render kudos text', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    expect(screen.getByTestId('kudos-text')).toHaveTextContent('Great job on the presentation!');
  });

  it('should render formatted date', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    expect(screen.getByTestId('kudos-date')).toBeInTheDocument();
  });

  it('should render tags', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    const tagsContainer = screen.getByTestId('kudos-tags');
    expect(tagsContainer).toHaveTextContent('impact');
    expect(tagsContainer).toHaveTextContent('collaboration');
  });

  it('should not render tags section when tags are empty', () => {
    const kudosNoTags: Kudos = { ...mockKudos, tags: [] };
    render(<KudosCard kudos={kudosNoTags} onDelete={mockOnDelete} />);
    expect(screen.queryByTestId('kudos-tags')).not.toBeInTheDocument();
  });

  it('should call onDelete with kudos id when delete button is clicked', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    fireEvent.click(screen.getByTestId('kudos-delete-btn'));
    expect(mockOnDelete).toHaveBeenCalledWith('kudos-1');
  });

  it('should render with correct test id', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    expect(screen.getByTestId('kudos-card-kudos-1')).toBeInTheDocument();
  });

  it('should render delete button with accessible label', () => {
    render(<KudosCard kudos={mockKudos} onDelete={mockOnDelete} />);
    expect(screen.getByLabelText('Delete kudos')).toBeInTheDocument();
  });

  it('should render multiline text with whitespace preserved', () => {
    const multilineKudos: Kudos = { ...mockKudos, text: 'Line 1\nLine 2\nLine 3' };
    render(<KudosCard kudos={multilineKudos} onDelete={mockOnDelete} />);
    expect(screen.getByTestId('kudos-text')).toHaveTextContent('Line 1');
    expect(screen.getByTestId('kudos-text')).toHaveTextContent('Line 2');
  });
});
