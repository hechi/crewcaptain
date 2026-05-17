import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import KudosForm from '@/components/kudos/KudosForm';

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

describe('KudosForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the form with all fields', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);
    expect(screen.getByTestId('kudos-form')).toBeInTheDocument();
    expect(screen.getByTestId('kudos-date-input')).toBeInTheDocument();
    expect(screen.getByTestId('kudos-text-input')).toBeInTheDocument();
    expect(screen.getByTestId('kudos-tags-input')).toBeInTheDocument();
  });

  it('should submit form with text and date', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: 'Great work on the project!' },
    });
    fireEvent.change(screen.getByTestId('kudos-date-input'), {
      target: { value: '2026-05-10' },
    });

    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnSubmit).toHaveBeenCalledWith({
      text: 'Great work on the project!',
      date: '2026-05-10',
      tags: undefined,
    });
  });

  it('should submit form with tags', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: 'Excellent collaboration!' },
    });
    fireEvent.change(screen.getByTestId('kudos-tags-input'), {
      target: { value: 'impact, collaboration, leadership' },
    });

    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        tags: ['impact', 'collaboration', 'leadership'],
      })
    );
  });

  it('should not submit when text is empty', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('should not submit when text is whitespace only', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: '   ' },
    });
    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('should call onCancel when cancel button is clicked', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.click(screen.getByTestId('kudos-cancel-btn'));

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('should disable submit button when isSubmitting is true', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} isSubmitting={true} />);

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: 'Some text' },
    });

    expect(screen.getByTestId('kudos-submit-btn')).toBeDisabled();
  });

  it('should show Saving text when isSubmitting is true', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} isSubmitting={true} />);
    expect(screen.getByTestId('kudos-submit-btn')).toHaveTextContent('Saving...');
  });

  it('should trim whitespace from tags', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('kudos-text-input'), {
      target: { value: 'Good work' },
    });
    fireEvent.change(screen.getByTestId('kudos-tags-input'), {
      target: { value: '  impact  ,  collaboration  ' },
    });

    fireEvent.click(screen.getByTestId('kudos-submit-btn'));

    expect(mockOnSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        tags: ['impact', 'collaboration'],
      })
    );
  });

  it('should show Refine button when aiEnabled is true', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} aiEnabled={true} />);
    expect(screen.getByTestId('kudos-refine-btn')).toBeInTheDocument();
  });

  it('should not show Refine button when aiEnabled is false', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} aiEnabled={false} />);
    expect(screen.queryByTestId('kudos-refine-btn')).not.toBeInTheDocument();
  });

  it('should disable Refine button when text is empty', () => {
    render(<KudosForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} aiEnabled={true} />);
    expect(screen.getByTestId('kudos-refine-btn')).toBeDisabled();
  });
});
