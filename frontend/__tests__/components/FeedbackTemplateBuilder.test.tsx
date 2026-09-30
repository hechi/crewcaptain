import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import FeedbackTemplateBuilder from '@/components/feedback/FeedbackTemplateBuilder';
import { FeedbackTemplateDraft } from '@/types/feedback';

describe('FeedbackTemplateBuilder', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with one empty question by default', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);
    expect(screen.getByTestId('feedback-template-builder')).toBeInTheDocument();
    expect(screen.getByTestId('ftb-question-0')).toBeInTheDocument();
    expect(screen.queryByTestId('ftb-question-1')).not.toBeInTheDocument();
  });

  it('adds and removes questions', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);

    fireEvent.click(screen.getByTestId('ftb-add-question'));
    expect(screen.getByTestId('ftb-question-1')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('ftb-question-1-remove'));
    expect(screen.queryByTestId('ftb-question-1')).not.toBeInTheDocument();
  });

  it('shows rating labels only for RATING type and hides them for TEXT', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);

    // Default type RATING -> labels visible
    expect(screen.getByTestId('ftb-question-0-rating-labels')).toBeInTheDocument();

    fireEvent.change(screen.getByTestId('ftb-question-0-type'), { target: { value: 'TEXT' } });
    expect(screen.queryByTestId('ftb-question-0-rating-labels')).not.toBeInTheDocument();

    fireEvent.change(screen.getByTestId('ftb-question-0-type'), { target: { value: 'RATING' } });
    expect(screen.getByTestId('ftb-question-0-rating-labels')).toBeInTheDocument();
  });

  it('only offers earlier RATING/LIKERT questions as showIf targets', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);

    // Q1 is RATING by default; the first question has no earlier question -> showIf unavailable
    expect(screen.getByTestId('ftb-question-0-showif-unavailable')).toBeInTheDocument();

    // Add Q2 (TEXT) then Q3
    fireEvent.click(screen.getByTestId('ftb-add-question'));
    fireEvent.change(screen.getByTestId('ftb-question-1-type'), { target: { value: 'TEXT' } });
    fireEvent.click(screen.getByTestId('ftb-add-question'));

    // Q3 showIf target dropdown should include q1 (RATING) but NOT q2 (TEXT)
    const select = screen.getByTestId('ftb-question-2-showif-question') as HTMLSelectElement;
    const optionValues = Array.from(select.querySelectorAll('option')).map((o) => (o as HTMLOptionElement).value);
    expect(optionValues).toContain('q1');
    expect(optionValues).not.toContain('q2');
  });

  it('saves with the expected payload for a new template', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('ftb-title-input'), { target: { value: 'Peer feedback' } });
    fireEvent.change(screen.getByTestId('ftb-description-input'), { target: { value: 'Quarterly' } });
    fireEvent.change(screen.getByTestId('ftb-question-0-text'), { target: { value: 'How was collaboration?' } });
    fireEvent.change(screen.getByTestId('ftb-question-0-low-label'), { target: { value: 'Poor' } });
    fireEvent.change(screen.getByTestId('ftb-question-0-high-label'), { target: { value: 'Great' } });
    fireEvent.click(screen.getByTestId('ftb-question-0-required'));

    fireEvent.click(screen.getByTestId('ftb-save'));

    expect(mockOnSave).toHaveBeenCalledWith({
      title: 'Peer feedback',
      description: 'Quarterly',
      questions: [
        {
          id: 'q1',
          type: 'RATING',
          text: 'How was collaboration?',
          required: true,
          lowLabel: 'Poor',
          highLabel: 'Great',
          showIf: null,
        },
      ],
    });
  });

  it('saves a showIf rule targeting an earlier rating question', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);

    fireEvent.change(screen.getByTestId('ftb-title-input'), { target: { value: 'T' } });
    fireEvent.change(screen.getByTestId('ftb-question-0-text'), { target: { value: 'Rate it' } });

    fireEvent.click(screen.getByTestId('ftb-add-question'));
    fireEvent.change(screen.getByTestId('ftb-question-1-type'), { target: { value: 'TEXT' } });
    fireEvent.change(screen.getByTestId('ftb-question-1-text'), { target: { value: 'Why low?' } });
    fireEvent.change(screen.getByTestId('ftb-question-1-showif-question'), { target: { value: 'q1' } });
    fireEvent.change(screen.getByTestId('ftb-question-1-showif-operator'), { target: { value: 'LTE' } });
    fireEvent.change(screen.getByTestId('ftb-question-1-showif-value'), { target: { value: '2' } });

    fireEvent.click(screen.getByTestId('ftb-save'));

    expect(mockOnSave).toHaveBeenCalledTimes(1);
    const payload = mockOnSave.mock.calls[0][0];
    expect(payload.questions[1].showIf).toEqual({ questionId: 'q1', operator: 'LTE', value: 2 });
  });

  it('blocks save with a validation error when title is missing', () => {
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} />);
    fireEvent.change(screen.getByTestId('ftb-question-0-text'), { target: { value: 'Q' } });
    fireEvent.click(screen.getByTestId('ftb-save'));

    expect(mockOnSave).not.toHaveBeenCalled();
    expect(screen.getByTestId('ftb-error')).toHaveTextContent('Title is required');
  });

  it('applies overflow-safe styles so selects fit inside a narrow container (e.g. the modal)', () => {
    // A RATING q1 followed by a q2 whose "show if" targets q1 renders all the selects
    // that previously overflowed the modal. They must be allowed to shrink (minWidth 0)
    // and never exceed the container (maxWidth 100%).
    const draft: FeedbackTemplateDraft = {
      title: 'T',
      description: null,
      questions: [
        { id: 'q1', type: 'RATING', text: 'How would you rate the overall collaboration on this project?', required: true, lowLabel: null, highLabel: null, showIf: null },
        { id: 'q2', type: 'TEXT', text: 'Explain', required: false, showIf: { questionId: 'q1', operator: 'LTE', value: 3 } },
      ],
    };
    render(<FeedbackTemplateBuilder onSave={mockOnSave} onCancel={mockOnCancel} initialDraft={draft} />);

    // Type select is fixed-width but capped at the container width.
    const typeSelect = screen.getByTestId('ftb-question-0-type');
    expect(typeSelect).toHaveStyle({ maxWidth: '100%' });

    // The show-if target select (long option labels) must shrink and cap. minWidth:0 is
    // what lets a flex child shrink below its content width; maxWidth:100% caps overflow.
    const showIfQuestion = screen.getByTestId('ftb-question-1-showif-question');
    expect(showIfQuestion).toHaveStyle({ maxWidth: '100%' });
    expect(showIfQuestion.style.minWidth).toBe('0');

    const showIfOperator = screen.getByTestId('ftb-question-1-showif-operator');
    expect(showIfOperator).toHaveStyle({ maxWidth: '100%' });

    const showIfValue = screen.getByTestId('ftb-question-1-showif-value');
    expect(showIfValue).toHaveStyle({ maxWidth: '100%' });

    // The builder root clips horizontal overflow.
    expect(screen.getByTestId('feedback-template-builder')).toHaveStyle({ overflowX: 'hidden', maxWidth: '100%' });
  });

  it('shows the server error message inline', () => {
    render(
      <FeedbackTemplateBuilder
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        serverError="Template title already exists"
      />
    );
    expect(screen.getByTestId('ftb-error')).toHaveTextContent('Template title already exists');
  });

  it('pre-fills from an initial draft when editing', () => {
    const draft: FeedbackTemplateDraft = {
      title: 'Existing',
      description: 'Desc',
      questions: [{ id: 'q1', type: 'LIKERT', text: 'Agree?', required: true, showIf: null }],
    };
    render(
      <FeedbackTemplateBuilder templateId="tpl-1" initialDraft={draft} onSave={mockOnSave} onCancel={mockOnCancel} />
    );
    expect(screen.getByTestId('ftb-title-input')).toHaveValue('Existing');
    expect(screen.getByTestId('ftb-question-0-text')).toHaveValue('Agree?');
    expect(screen.getByTestId('ftb-save')).toHaveTextContent('Save changes');
  });
});
