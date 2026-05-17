import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import KudosList from '@/components/kudos/KudosList';
import { Kudos } from '@/types/kudos';

jest.mock('@/lib/useStableToken', () => ({
  useStableToken: () => ({
    getToken: () => 'test-token',
    isAuthenticated: true,
    status: 'authenticated',
  }),
}));

jest.mock('@/lib/api-client', () => ({
  refineKudos: jest.fn(),
}));

describe('KudosList', () => {
  const mockKudos: Kudos[] = [
    {
      id: 'kudos-1',
      personId: 'person-1',
      date: '2026-05-10',
      text: 'Great presentation!',
      tags: ['impact'],
      createdAt: '2026-05-10T10:00:00Z',
      updatedAt: '2026-05-10T10:00:00Z',
    },
    {
      id: 'kudos-2',
      personId: 'person-1',
      date: '2026-05-08',
      text: 'Excellent teamwork!',
      tags: ['collaboration'],
      createdAt: '2026-05-08T10:00:00Z',
      updatedAt: '2026-05-08T10:00:00Z',
    },
  ];

  const mockOnCreateKudos = jest.fn();
  const mockOnDeleteKudos = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render kudos list with count', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    expect(screen.getByText('Kudos (2)')).toBeInTheDocument();
  });

  it('should render all kudos cards', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    expect(screen.getByTestId('kudos-card-kudos-1')).toBeInTheDocument();
    expect(screen.getByTestId('kudos-card-kudos-2')).toBeInTheDocument();
  });

  it('should show empty state when no kudos', () => {
    render(
      <KudosList kudos={[]} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    expect(screen.getByText(/No kudos yet/)).toBeInTheDocument();
  });

  it('should show create form when create button is clicked', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    fireEvent.click(screen.getByTestId('kudos-create-btn'));
    expect(screen.getByTestId('kudos-form')).toBeInTheDocument();
  });

  it('should hide create button when form is shown', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    fireEvent.click(screen.getByTestId('kudos-create-btn'));
    expect(screen.queryByTestId('kudos-create-btn')).not.toBeInTheDocument();
  });

  it('should call onCreateKudos when form is submitted', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    fireEvent.click(screen.getByTestId('kudos-create-btn'));

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: 'New kudos!' },
    });
    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnCreateKudos).toHaveBeenCalled();
  });

  it('should hide form when cancel is clicked', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    fireEvent.click(screen.getByTestId('kudos-create-btn'));
    fireEvent.click(screen.getByTestId('kudos-cancel-btn'));
    expect(screen.queryByTestId('kudos-form')).not.toBeInTheDocument();
  });

  it('should call onDeleteKudos when delete is clicked on a card', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    const deleteButtons = screen.getAllByTestId('kudos-delete-btn');
    fireEvent.click(deleteButtons[0]);
    expect(mockOnDeleteKudos).toHaveBeenCalledWith('kudos-1');
  });

  it('should render with correct test id', () => {
    render(
      <KudosList kudos={mockKudos} onCreateKudos={mockOnCreateKudos} onDeleteKudos={mockOnDeleteKudos} />
    );
    expect(screen.getByTestId('kudos-list')).toBeInTheDocument();
  });
});
