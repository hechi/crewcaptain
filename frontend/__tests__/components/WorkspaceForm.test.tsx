import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkspaceForm from '@/components/workspace/WorkspaceForm';
import { Workspace } from '@/types/workspace';

describe('WorkspaceForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render empty form for create mode', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    expect(screen.getByLabelText(/Name/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description/)).toBeInTheDocument();
    expect(screen.getByText('Create')).toBeInTheDocument();
  });

  it('should render pre-filled form for edit mode', () => {
    const workspace: Workspace = {
      id: 'ws-1', name: 'My Team', description: 'Direct reports',
      displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    };
    render(<WorkspaceForm workspace={workspace} onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    expect((screen.getByLabelText(/Name/) as HTMLInputElement).value).toBe('My Team');
    expect((screen.getByLabelText(/Description/) as HTMLTextAreaElement).value).toBe('Direct reports');
    expect(screen.getByText('Update')).toBeInTheDocument();
  });

  it('should call onSubmit with form data', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'New Workspace' } });
    fireEvent.change(screen.getByLabelText(/Description/), { target: { value: 'A description' } });
    fireEvent.click(screen.getByText('Create'));
    expect(mockOnSubmit).toHaveBeenCalledWith({ name: 'New Workspace', description: 'A description' });
  });

  it('should show error when name is blank', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.click(screen.getByText('Create'));
    expect(screen.getByTestId('workspace-form-error')).toHaveTextContent('Name is required');
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('should show error when name exceeds 100 characters', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'a'.repeat(101) } });
    fireEvent.click(screen.getByText('Create'));
    expect(screen.getByTestId('workspace-form-error')).toHaveTextContent('Name must not exceed 100 characters');
  });

  it('should show error when description exceeds 500 characters', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'Valid Name' } });
    fireEvent.change(screen.getByLabelText(/Description/), { target: { value: 'a'.repeat(501) } });
    fireEvent.click(screen.getByText('Create'));
    expect(screen.getByTestId('workspace-form-error')).toHaveTextContent('Description must not exceed 500 characters');
  });

  it('should call onCancel when cancel button clicked', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.click(screen.getByText('Cancel'));
    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('should show Saving... when isSubmitting is true', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} isSubmitting={true} />);
    expect(screen.getByText('Saving...')).toBeInTheDocument();
  });

  it('should submit without description when empty', () => {
    render(<WorkspaceForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: 'No Desc' } });
    fireEvent.click(screen.getByText('Create'));
    expect(mockOnSubmit).toHaveBeenCalledWith({ name: 'No Desc', description: undefined });
  });
});
