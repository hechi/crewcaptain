'use client';

import { useMemo, useState } from 'react';
import {
  FeedbackQuestion,
  FeedbackQuestionType,
  FeedbackTemplateDraft,
  SaveFeedbackTemplateRequest,
  ShowIfOperator,
} from '@/types/feedback';

export const MAX_QUESTIONS = 20;

const OPERATOR_LABELS: Record<ShowIfOperator, string> = {
  LTE: '≤ (at most)',
  GTE: '≥ (at least)',
  EQ: '= (exactly)',
};

interface FeedbackTemplateBuilderProps {
  /** Existing template id when editing; undefined when creating. */
  templateId?: string | null;
  /** Initial values (from a saved template, starter, or AI draft). */
  initialDraft?: FeedbackTemplateDraft | null;
  /** Called with the assembled payload when the user saves. */
  onSave: (payload: SaveFeedbackTemplateRequest) => void | Promise<void>;
  onCancel: () => void;
  isSaving?: boolean;
  /** Server-side validation error message (e.g. from a 400 response). */
  serverError?: string | null;
}

/** Generates the next unused question id in the qN sequence. */
function nextQuestionId(questions: FeedbackQuestion[]): string {
  let n = questions.length + 1;
  const existing = new Set(questions.map((q) => q.id));
  while (existing.has(`q${n}`)) n += 1;
  return `q${n}`;
}

function makeQuestion(id: string): FeedbackQuestion {
  return { id, type: 'RATING', text: '', required: false, lowLabel: null, highLabel: null, showIf: null };
}

const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: 'var(--text-caption)',
  fontFamily: 'var(--font-mono)',
  color: 'var(--color-text-muted)',
  marginBottom: '4px',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
};

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 12px',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-medium)',
  fontSize: 'var(--text-body)',
  backgroundColor: 'var(--color-bg-elevated)',
  color: 'var(--color-text-primary)',
  boxSizing: 'border-box',
};

/**
 * Reusable editor for a feedback template: title, description, and an ordered
 * list of questions. Enforces client-side rules (max 20 questions, unique ids,
 * showIf may only target an earlier RATING/LIKERT question).
 */
export default function FeedbackTemplateBuilder({
  templateId,
  initialDraft,
  onSave,
  onCancel,
  isSaving = false,
  serverError = null,
}: FeedbackTemplateBuilderProps) {
  const [title, setTitle] = useState(initialDraft?.title ?? '');
  const [description, setDescription] = useState(initialDraft?.description ?? '');
  const [questions, setQuestions] = useState<FeedbackQuestion[]>(
    initialDraft?.questions?.length ? initialDraft.questions.map((q) => ({ ...q })) : [makeQuestion('q1')]
  );
  const [validationError, setValidationError] = useState<string | null>(null);

  const atMax = questions.length >= MAX_QUESTIONS;

  const addQuestion = () => {
    if (atMax) return;
    setQuestions((prev) => [...prev, makeQuestion(nextQuestionId(prev))]);
  };

  const removeQuestion = (index: number) => {
    setQuestions((prev) => {
      const removed = prev[index];
      const next = prev.filter((_, i) => i !== index);
      // Drop showIf rules that referenced the removed question.
      return next.map((q) => (q.showIf?.questionId === removed.id ? { ...q, showIf: null } : q));
    });
  };

  const moveQuestion = (index: number, dir: -1 | 1) => {
    setQuestions((prev) => {
      const target = index + dir;
      if (target < 0 || target >= prev.length) return prev;
      const next = [...prev];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  };

  const updateQuestion = (index: number, patch: Partial<FeedbackQuestion>) => {
    setQuestions((prev) => prev.map((q, i) => (i === index ? { ...q, ...patch } : q)));
  };

  const changeType = (index: number, type: FeedbackQuestionType) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== index) return q;
        // Clear rating labels when leaving RATING.
        if (type === 'RATING') return { ...q, type };
        return { ...q, type, lowLabel: null, highLabel: null };
      })
    );
  };

  const changeShowIf = (index: number, patch: Partial<NonNullable<FeedbackQuestion['showIf']>> | null) => {
    setQuestions((prev) =>
      prev.map((q, i) => {
        if (i !== index) return q;
        if (patch === null) return { ...q, showIf: null };
        const current = q.showIf ?? { questionId: '', operator: 'GTE' as ShowIfOperator, value: 3 };
        return { ...q, showIf: { ...current, ...patch } };
      })
    );
  };

  /** Questions before `index` that are RATING/LIKERT — the only valid showIf targets. */
  const eligibleTargets = useMemo(
    () =>
      questions.map((_, index) =>
        questions.slice(0, index).filter((q) => q.type === 'RATING' || q.type === 'LIKERT')
      ),
    [questions]
  );

  const handleSave = () => {
    setValidationError(null);

    if (!title.trim()) {
      setValidationError('Title is required.');
      return;
    }
    if (questions.length === 0) {
      setValidationError('Add at least one question.');
      return;
    }
    if (questions.length > MAX_QUESTIONS) {
      setValidationError(`A template may have at most ${MAX_QUESTIONS} questions.`);
      return;
    }
    for (let i = 0; i < questions.length; i += 1) {
      const q = questions[i];
      if (!q.text.trim()) {
        setValidationError(`Question ${i + 1} needs text.`);
        return;
      }
      if (q.showIf) {
        const target = questions.slice(0, i).find((t) => t.id === q.showIf!.questionId);
        if (!target || (target.type !== 'RATING' && target.type !== 'LIKERT')) {
          setValidationError(`Question ${i + 1}'s "show if" must reference an earlier rating or Likert question.`);
          return;
        }
      }
    }

    const payload: SaveFeedbackTemplateRequest = {
      title: title.trim(),
      description: description.trim() ? description.trim() : null,
      questions: questions.map((q) => ({
        id: q.id,
        type: q.type,
        text: q.text.trim(),
        required: q.required,
        lowLabel: q.type === 'RATING' ? (q.lowLabel?.trim() || null) : null,
        highLabel: q.type === 'RATING' ? (q.highLabel?.trim() || null) : null,
        showIf: q.showIf && q.showIf.questionId ? { ...q.showIf } : null,
      })),
    };
    onSave(payload);
  };

  return (
    <div
      data-testid="feedback-template-builder"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
        maxWidth: '100%',
        boxSizing: 'border-box',
        overflowX: 'hidden',
      }}
    >
      <h2
        style={{
          fontSize: 'var(--text-h3)',
          fontFamily: 'var(--font-heading)',
          fontWeight: 'var(--weight-semibold)',
          color: 'var(--color-text-primary)',
          margin: '0 0 var(--space-4) 0',
        }}
      >
        {templateId ? 'Edit template' : 'New template'}
      </h2>

      {/* Title */}
      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="ftb-title" style={labelStyle}>Title *</label>
        <input
          id="ftb-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g. Quarterly peer feedback"
          data-testid="ftb-title-input"
          style={inputStyle}
        />
      </div>

      {/* Description */}
      <div style={{ marginBottom: '16px' }}>
        <label htmlFor="ftb-description" style={labelStyle}>Description (optional)</label>
        <textarea
          id="ftb-description"
          value={description ?? ''}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="What this feedback is for..."
          rows={2}
          data-testid="ftb-description-input"
          style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }}
        />
      </div>

      {/* Questions */}
      <div data-testid="ftb-questions" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {questions.map((q, index) => {
          const targets = eligibleTargets[index];
          return (
            <div
              key={q.id}
              data-testid={`ftb-question-${index}`}
              style={{
                padding: 'var(--space-3)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                maxWidth: '100%',
                boxSizing: 'border-box',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span style={{ ...labelStyle, marginBottom: 0 }}>Question {index + 1} · {q.id}</span>
                <div style={{ display: 'flex', gap: '4px' }}>
                  <button
                    type="button"
                    onClick={() => moveQuestion(index, -1)}
                    disabled={index === 0}
                    data-testid={`ftb-question-${index}-up`}
                    aria-label={`Move question ${index + 1} up`}
                    style={iconBtnStyle(index === 0)}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    onClick={() => moveQuestion(index, 1)}
                    disabled={index === questions.length - 1}
                    data-testid={`ftb-question-${index}-down`}
                    aria-label={`Move question ${index + 1} down`}
                    style={iconBtnStyle(index === questions.length - 1)}
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    onClick={() => removeQuestion(index)}
                    data-testid={`ftb-question-${index}-remove`}
                    aria-label={`Remove question ${index + 1}`}
                    style={{
                      ...iconBtnStyle(false),
                      color: 'var(--color-alert)',
                      borderColor: 'var(--color-alert)',
                    }}
                  >
                    ✕
                  </button>
                </div>
              </div>

              {/* Type + text */}
              <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                <select
                  value={q.type}
                  onChange={(e) => changeType(index, e.target.value as FeedbackQuestionType)}
                  data-testid={`ftb-question-${index}-type`}
                  aria-label={`Question ${index + 1} type`}
                  style={{ ...inputStyle, width: '140px', flex: '0 0 auto', maxWidth: '100%' }}
                >
                  <option value="RATING">Rating (1–5)</option>
                  <option value="LIKERT">Likert</option>
                  <option value="TEXT">Text</option>
                </select>
                <input
                  type="text"
                  value={q.text}
                  onChange={(e) => updateQuestion(index, { text: e.target.value })}
                  placeholder="Question text"
                  data-testid={`ftb-question-${index}-text`}
                  style={{ ...inputStyle, flex: '1 1 0', minWidth: 0 }}
                />
              </div>

              {/* Rating labels */}
              {q.type === 'RATING' && (
                <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }} data-testid={`ftb-question-${index}-rating-labels`}>
                  <input
                    type="text"
                    value={q.lowLabel ?? ''}
                    onChange={(e) => updateQuestion(index, { lowLabel: e.target.value })}
                    placeholder="Low label (optional)"
                    data-testid={`ftb-question-${index}-low-label`}
                    style={{ ...inputStyle, flex: 1 }}
                  />
                  <input
                    type="text"
                    value={q.highLabel ?? ''}
                    onChange={(e) => updateQuestion(index, { highLabel: e.target.value })}
                    placeholder="High label (optional)"
                    data-testid={`ftb-question-${index}-high-label`}
                    style={{ ...inputStyle, flex: 1 }}
                  />
                </div>
              )}

              {/* Required */}
              <label
                style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', cursor: 'pointer' }}
              >
                <input
                  type="checkbox"
                  checked={q.required}
                  onChange={(e) => updateQuestion(index, { required: e.target.checked })}
                  data-testid={`ftb-question-${index}-required`}
                  style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
                />
                Required
              </label>

              {/* Show if */}
              <div style={{ marginTop: '8px' }}>
                {targets.length === 0 ? (
                  <span
                    data-testid={`ftb-question-${index}-showif-unavailable`}
                    style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}
                  >
                    Add an earlier rating/Likert question to enable conditional display.
                  </span>
                ) : (
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                    <span style={{ ...labelStyle, marginBottom: 0 }}>Show if</span>
                    <select
                      value={q.showIf?.questionId ?? ''}
                      onChange={(e) =>
                        e.target.value ? changeShowIf(index, { questionId: e.target.value }) : changeShowIf(index, null)
                      }
                      data-testid={`ftb-question-${index}-showif-question`}
                      aria-label={`Question ${index + 1} show-if target`}
                      style={{ ...inputStyle, flex: '1 1 140px', minWidth: 0, maxWidth: '100%', textOverflow: 'ellipsis' }}
                    >
                      <option value="">Always show</option>
                      {targets.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.id}: {t.text || '(untitled)'}
                        </option>
                      ))}
                    </select>
                    {q.showIf?.questionId && (
                      <>
                        <select
                          value={q.showIf.operator}
                          onChange={(e) => changeShowIf(index, { operator: e.target.value as ShowIfOperator })}
                          data-testid={`ftb-question-${index}-showif-operator`}
                          aria-label={`Question ${index + 1} show-if operator`}
                          style={{ ...inputStyle, width: 'auto', flex: '0 1 auto', minWidth: 0, maxWidth: '100%' }}
                        >
                          {(Object.keys(OPERATOR_LABELS) as ShowIfOperator[]).map((op) => (
                            <option key={op} value={op}>{OPERATOR_LABELS[op]}</option>
                          ))}
                        </select>
                        <select
                          value={q.showIf.value}
                          onChange={(e) => changeShowIf(index, { value: Number(e.target.value) })}
                          data-testid={`ftb-question-${index}-showif-value`}
                          aria-label={`Question ${index + 1} show-if value`}
                          style={{ ...inputStyle, width: 'auto', flex: '0 0 auto', minWidth: 0, maxWidth: '100%' }}
                        >
                          {[1, 2, 3, 4, 5].map((v) => (
                            <option key={v} value={v}>{v}</option>
                          ))}
                        </select>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Add question */}
      <button
        type="button"
        onClick={addQuestion}
        disabled={atMax}
        data-testid="ftb-add-question"
        style={{
          marginTop: '12px',
          padding: '8px 16px',
          fontSize: 'var(--text-body)',
          fontFamily: 'var(--font-mono)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          backgroundColor: 'transparent',
          color: atMax ? 'var(--color-text-muted)' : 'var(--color-primary)',
          cursor: atMax ? 'not-allowed' : 'pointer',
          opacity: atMax ? 0.6 : 1,
        }}
      >
        + Add question
      </button>
      {atMax && (
        <span data-testid="ftb-max-questions" style={{ marginLeft: '8px', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>
          Maximum of {MAX_QUESTIONS} questions reached.
        </span>
      )}

      {/* Errors */}
      {(validationError || serverError) && (
        <div
          data-testid="ftb-error"
          style={{
            marginTop: '12px',
            padding: '8px 12px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {validationError || serverError}
        </div>
      )}

      {/* Actions */}
      <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
        <button
          type="button"
          onClick={handleSave}
          disabled={isSaving}
          data-testid="ftb-save"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: isSaving ? 'not-allowed' : 'pointer',
            fontWeight: 'var(--weight-medium)',
            opacity: isSaving ? 0.6 : 1,
          }}
        >
          {isSaving ? 'Saving...' : templateId ? 'Save changes' : 'Create template'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          data-testid="ftb-cancel"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'transparent',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}

function iconBtnStyle(disabled: boolean): React.CSSProperties {
  return {
    padding: '2px 8px',
    fontSize: 'var(--text-caption)',
    fontFamily: 'var(--font-mono)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-small)',
    backgroundColor: 'transparent',
    color: disabled ? 'var(--color-text-muted)' : 'var(--color-text-secondary)',
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.5 : 1,
  };
}
