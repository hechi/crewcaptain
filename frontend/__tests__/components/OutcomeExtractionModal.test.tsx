import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import OutcomeExtractionModal from '@/components/one-on-one/OutcomeExtractionModal';
import * as apiClient from '@/lib/api-client';

jest.mock('@/lib/api-client');

const mockExtractOutcomes = apiClient.extractOutcomes as jest.MockedFunction<typeof apiClient.extractOutcomes>;
const mockApplyOutcomes = apiClient.applyOutcomes as jest.MockedFunction<typeof apiClient.applyOutcomes>;

describe('OutcomeExtractionModal', () => {
  const defaultProps = {
    token: 'test-token',
    personId: 'person-123',
    entryId: 'entry-456',
    onClose: jest.fn(),
    onApplied: jest.fn(),
    existingActionItemTitles: [],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render the modal with loading state initially', () => {
    mockExtractOutcomes.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ actionItems: [], decisions: [], error: null }), 100))
    );

    render(<OutcomeExtractionModal {...defaultProps} />);
    expect(screen.getByTestId('outcome-extraction-modal')).toBeInTheDocument();
    expect(screen.getByTestId('extraction-loading')).toBeInTheDocument();
    expect(screen.getByText('Analyzing meeting notes...')).toBeInTheDocument();
  });

  it('should display extracted action items grouped by owner', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Review PR', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
        { title: 'Write docs', ownerType: 'PERSON', suggestedDaysToDue: 7 },
      ],
      decisions: ['Agreed on new timeline'],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Your Action Items')).toBeInTheDocument();
    });

    expect(screen.getByText('Their Action Items')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Review PR')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Write docs')).toBeInTheDocument();
    expect(screen.getByText('Due in 3d')).toBeInTheDocument();
    expect(screen.getByText('Due in 7d')).toBeInTheDocument();
  });

  it('should display decisions section', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: ['Move to biweekly', 'Approved new architecture'],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Key Decisions')).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue('Move to biweekly')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Approved new architecture')).toBeInTheDocument();
  });

  it('should display error when extraction fails', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: [],
      error: 'AI not configured',
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('extraction-error')).toBeInTheDocument();
    });

    expect(screen.getByText('AI not configured')).toBeInTheDocument();
  });

  it('should display error when API call throws', async () => {
    mockExtractOutcomes.mockRejectedValue(new Error('Network error'));

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('extraction-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should show empty state when no items found', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('extraction-empty')).toBeInTheDocument();
    });

    expect(screen.getByText('No action items or decisions were found in the notes.')).toBeInTheDocument();
  });

  it('should allow toggling action items', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 5 },
      ],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Task 1')).toBeInTheDocument();
    });

    const checkbox = screen.getByRole('checkbox', { name: /Select action item: Task 1/i });
    expect(checkbox).toBeChecked();

    fireEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  it('should allow editing action item titles', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Original title', ownerType: 'MANAGER', suggestedDaysToDue: 5 },
      ],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Original title')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('Original title');
    fireEvent.change(input, { target: { value: 'Edited title' } });
    expect(screen.getByDisplayValue('Edited title')).toBeInTheDocument();
  });

  it('should allow editing decision text', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: ['Original decision'],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Original decision')).toBeInTheDocument();
    });

    const input = screen.getByDisplayValue('Original decision');
    fireEvent.change(input, { target: { value: 'Edited decision' } });
    expect(screen.getByDisplayValue('Edited decision')).toBeInTheDocument();
  });

  it('should mark duplicate action items and pre-uncheck them', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Existing task', ownerType: 'MANAGER', suggestedDaysToDue: 5 },
        { title: 'New task', ownerType: 'PERSON', suggestedDaysToDue: 7 },
      ],
      decisions: [],
      error: null,
    });

    render(
      <OutcomeExtractionModal
        {...defaultProps}
        existingActionItemTitles={['Existing task']}
      />
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue('Existing task')).toBeInTheDocument();
    });

    expect(screen.getByTestId('duplicate-badge-0')).toBeInTheDocument();
    expect(screen.getByText('⚠ Possible duplicate')).toBeInTheDocument();

    // Duplicate should be unchecked
    const duplicateCheckbox = screen.getByRole('checkbox', { name: /Select action item: Existing task/i });
    expect(duplicateCheckbox).not.toBeChecked();

    // Non-duplicate should be checked
    const newCheckbox = screen.getByRole('checkbox', { name: /Select action item: New task/i });
    expect(newCheckbox).toBeChecked();
  });

  it('should call applyOutcomes with selected items on Sync All', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
        { title: 'Task 2', ownerType: 'PERSON', suggestedDaysToDue: 7 },
      ],
      decisions: ['Decision 1'],
      error: null,
    });

    mockApplyOutcomes.mockResolvedValue({
      actionItemsCreated: 2,
      decisionsAppended: 1,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('apply-outcomes-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('apply-outcomes-btn'));

    await waitFor(() => {
      expect(mockApplyOutcomes).toHaveBeenCalledWith(
        'test-token',
        'person-123',
        'entry-456',
        {
          actionItems: [
            { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
            { title: 'Task 2', ownerType: 'PERSON', suggestedDaysToDue: 7 },
          ],
          decisions: ['Decision 1'],
        }
      );
    });
  });

  it('should show success message after applying', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
      ],
      decisions: ['Decision 1'],
      error: null,
    });

    mockApplyOutcomes.mockResolvedValue({
      actionItemsCreated: 1,
      decisionsAppended: 1,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('apply-outcomes-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('apply-outcomes-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('extraction-success')).toBeInTheDocument();
    });

    expect(screen.getByText(/Created 1 action item and appended 1 decision/)).toBeInTheDocument();
  });

  it('should show error when no items are selected', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
      ],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('apply-outcomes-btn')).toBeInTheDocument();
    });

    // Uncheck the only item
    const checkbox = screen.getByRole('checkbox', { name: /Select action item: Task 1/i });
    fireEvent.click(checkbox);

    fireEvent.click(screen.getByTestId('apply-outcomes-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('extraction-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Please select at least one item to apply.')).toBeInTheDocument();
  });

  it('should close modal when backdrop is clicked', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('outcome-extraction-modal-backdrop')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('outcome-extraction-modal-backdrop'));
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it('should close modal when close button is clicked', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('close-modal-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('close-modal-btn'));
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it('should not close modal when modal content is clicked', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [],
      decisions: [],
      error: null,
    });

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('outcome-extraction-modal')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('outcome-extraction-modal'));
    expect(defaultProps.onClose).not.toHaveBeenCalled();
  });

  it('should show error when apply fails', async () => {
    mockExtractOutcomes.mockResolvedValue({
      actionItems: [
        { title: 'Task 1', ownerType: 'MANAGER', suggestedDaysToDue: 3 },
      ],
      decisions: [],
      error: null,
    });

    mockApplyOutcomes.mockRejectedValue(new Error('Server error'));

    render(<OutcomeExtractionModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('apply-outcomes-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('apply-outcomes-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('extraction-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Server error')).toBeInTheDocument();
  });
});
