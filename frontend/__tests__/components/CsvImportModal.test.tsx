import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import CsvImportModal from '@/components/CsvImportModal';

// Mock the api-client
jest.mock('@/lib/api-client', () => ({
  importPersonsCsv: jest.fn(),
}));

import { importPersonsCsv } from '@/lib/api-client';

const mockImportPersonsCsv = importPersonsCsv as jest.MockedFunction<typeof importPersonsCsv>;

describe('CsvImportModal', () => {
  const mockOnClose = jest.fn();
  const mockOnImportComplete = jest.fn();

  const defaultProps = {
    isOpen: true,
    onClose: mockOnClose,
    onImportComplete: mockOnImportComplete,
    token: 'test-token',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not render when isOpen is false', () => {
    render(<CsvImportModal {...defaultProps} isOpen={false} />);
    expect(screen.queryByTestId('csv-import-modal')).not.toBeInTheDocument();
  });

  it('should render modal when isOpen is true', () => {
    render(<CsvImportModal {...defaultProps} />);
    expect(screen.getByTestId('csv-import-modal')).toBeInTheDocument();
    expect(screen.getByText('Import People from CSV')).toBeInTheDocument();
  });

  it('should render file input', () => {
    render(<CsvImportModal {...defaultProps} />);
    expect(screen.getByTestId('csv-file-input')).toBeInTheDocument();
  });

  it('should render import button disabled when no file selected', () => {
    render(<CsvImportModal {...defaultProps} />);
    const importButton = screen.getByTestId('csv-import-submit');
    expect(importButton).toBeDisabled();
  });

  it('should show error for non-CSV file', () => {
    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['content'], 'test.json', { type: 'application/json' });
    fireEvent.change(input, { target: { files: [file] } });

    expect(screen.getByTestId('import-error')).toHaveTextContent('Please select a CSV file');
  });

  it('should accept CSV file and enable import button', async () => {
    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const csvContent = 'name,email\nAlice,alice@example.com';
    const file = new File([csvContent], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });
  });

  it('should show preview table after file selection', async () => {
    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const csvContent = 'name,email\nAlice,alice@example.com\nBob,bob@example.com';
    const file = new File([csvContent], 'people.csv', { type: 'text/csv' });

    // Mock FileReader
    const mockFileReader = {
      readAsText: jest.fn(),
      onload: null as any,
      result: csvContent,
    };
    jest.spyOn(window, 'FileReader').mockImplementation(() => mockFileReader as any);

    fireEvent.change(input, { target: { files: [file] } });

    // Trigger the onload callback
    if (mockFileReader.onload) {
      mockFileReader.onload({ target: { result: csvContent } });
    }

    await waitFor(() => {
      expect(screen.getByTestId('csv-preview-table')).toBeInTheDocument();
    });
  });

  it('should call importPersonsCsv when import button is clicked', async () => {
    mockImportPersonsCsv.mockResolvedValue({
      successCount: 2,
      errorCount: 0,
      errors: [],
    });

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const csvContent = 'name,email\nAlice,alice@example.com';
    const file = new File([csvContent], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(mockImportPersonsCsv).toHaveBeenCalledWith('test-token', file);
    });
  });

  it('should show success result after import', async () => {
    mockImportPersonsCsv.mockResolvedValue({
      successCount: 3,
      errorCount: 0,
      errors: [],
    });

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice\nBob\nCharlie'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('import-result')).toBeInTheDocument();
      expect(screen.getByText('3 people imported successfully')).toBeInTheDocument();
    });
  });

  it('should show errors in result when import has failures', async () => {
    mockImportPersonsCsv.mockResolvedValue({
      successCount: 1,
      errorCount: 2,
      errors: ['Row 3: Name must not be blank', 'Row 4: Invalid start_date'],
    });

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('import-result')).toBeInTheDocument();
      expect(screen.getByText(/1 person imported successfully/)).toBeInTheDocument();
      expect(screen.getByText('Row 3: Name must not be blank')).toBeInTheDocument();
      expect(screen.getByText('Row 4: Invalid start_date')).toBeInTheDocument();
    });
  });

  it('should show error message when import API call fails', async () => {
    mockImportPersonsCsv.mockRejectedValue(new Error('Network error'));

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('import-error')).toHaveTextContent('Network error');
    });
  });

  it('should call onImportComplete after successful import', async () => {
    const result = { successCount: 2, errorCount: 0, errors: [] };
    mockImportPersonsCsv.mockResolvedValue(result);

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice\nBob'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(mockOnImportComplete).toHaveBeenCalledWith(result);
    });
  });

  it('should call onClose when cancel button is clicked', () => {
    render(<CsvImportModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('csv-import-close'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should call onClose when backdrop is clicked', () => {
    render(<CsvImportModal {...defaultProps} />);
    fireEvent.click(screen.getByTestId('csv-import-modal'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should show Done button after import completes', async () => {
    mockImportPersonsCsv.mockResolvedValue({
      successCount: 1,
      errorCount: 0,
      errors: [],
    });

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-close')).toHaveTextContent('Done');
      expect(screen.queryByTestId('csv-import-submit')).not.toBeInTheDocument();
    });
  });

  it('should display format instructions', () => {
    render(<CsvImportModal {...defaultProps} />);
    expect(screen.getByText(/Upload a CSV file/)).toBeInTheDocument();
    expect(screen.getByText(/required/)).toBeInTheDocument();
  });

  it('should show singular person text for single import', async () => {
    mockImportPersonsCsv.mockResolvedValue({
      successCount: 1,
      errorCount: 0,
      errors: [],
    });

    render(<CsvImportModal {...defaultProps} />);
    const input = screen.getByTestId('csv-file-input');

    const file = new File(['name\nAlice'], 'people.csv', { type: 'text/csv' });
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(screen.getByTestId('csv-import-submit')).not.toBeDisabled();
    });

    fireEvent.click(screen.getByTestId('csv-import-submit'));

    await waitFor(() => {
      expect(screen.getByText('1 person imported successfully')).toBeInTheDocument();
    });
  });
});
