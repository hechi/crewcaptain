'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'next/navigation';
import {
  FeedbackAnswer,
  FeedbackQuestion,
  PublicFeedbackForm,
  PublicFeedbackSubmission,
} from '@/types/feedback';
import { getPublicFeedbackForm, submitPublicFeedback } from '@/lib/api-client';
import { ApiException } from '@/types/api';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'loaded'; form: PublicFeedbackForm }
  | { kind: 'error'; message: string };

const LIKERT_OPTIONS = [
  { value: 1, label: 'Strongly disagree' },
  { value: 2, label: 'Somewhat disagree' },
  { value: 3, label: 'Neutral' },
  { value: 4, label: 'Somewhat agree' },
  { value: 5, label: 'Strongly agree' },
];

/** Visually-hidden style for the honeypot field. */
const honeypotStyle: React.CSSProperties = {
  position: 'absolute',
  width: '1px',
  height: '1px',
  padding: 0,
  margin: '-1px',
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: 0,
};

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  padding: 'var(--space-6)',
  backgroundColor: 'var(--color-bg-base)',
  fontFamily: 'var(--font-ui)',
};

const panelStyle: React.CSSProperties = {
  maxWidth: '640px',
  margin: '0 auto',
  padding: 'var(--space-5)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-large)',
  backgroundColor: 'var(--color-bg-surface)',
};

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

function messageForError(err: unknown): string {
  if (err instanceof ApiException) {
    if (err.status === 404) return 'This feedback link was not found';
    if (err.status === 410) {
      // Distinguish revoked vs expired by message text; default to expired.
      if (/revok/i.test(err.message)) return 'This link has been revoked';
      return 'This link has expired';
    }
    if (err.status === 429) return 'Too many submissions from this device. Please try again later.';
    return err.message || 'Something went wrong';
  }
  return err instanceof Error ? err.message : 'Something went wrong';
}

/** Evaluates a single-level showIf rule against the current answers. */
function isVisible(question: FeedbackQuestion, answers: Record<string, number | string | undefined>): boolean {
  const rule = question.showIf;
  if (!rule || !rule.questionId) return true;
  const raw = answers[rule.questionId];
  if (typeof raw !== 'number') return false;
  switch (rule.operator) {
    case 'LTE':
      return raw <= rule.value;
    case 'GTE':
      return raw >= rule.value;
    case 'EQ':
      return raw === rule.value;
    default:
      return false;
  }
}

export default function PublicFeedbackPage() {
  const params = useParams();
  const linkToken = Array.isArray(params?.token) ? params.token[0] : (params?.token as string | undefined) ?? '';

  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [answers, setAnswers] = useState<Record<string, number | string | undefined>>({});
  const [anonymous, setAnonymous] = useState(true);
  const [submitterName, setSubmitterName] = useState('');
  const [submitterEmail, setSubmitterEmail] = useState('');
  const [comments, setComments] = useState('');
  const [website, setWebsite] = useState(''); // honeypot
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const load = useCallback(async () => {
    setState({ kind: 'loading' });
    try {
      const form = await getPublicFeedbackForm(linkToken);
      setState({ kind: 'loaded', form });
    } catch (err) {
      setState({ kind: 'error', message: messageForError(err) });
    }
  }, [linkToken]);

  useEffect(() => {
    load();
  }, [load]);

  const form = state.kind === 'loaded' ? state.form : null;

  const visibleQuestions = useMemo(
    () => (form ? form.questions.filter((q) => isVisible(q, answers)) : []),
    [form, answers]
  );

  const setRating = (id: string, value: number) => setAnswers((prev) => ({ ...prev, [id]: value }));
  const setText = (id: string, value: string) => setAnswers((prev) => ({ ...prev, [id]: value }));

  const canSubmit = useMemo(() => {
    if (!form) return false;
    for (const q of visibleQuestions) {
      if (!q.required) continue;
      const val = answers[q.id];
      if (q.type === 'TEXT') {
        if (typeof val !== 'string' || !val.trim()) return false;
      } else if (typeof val !== 'number') {
        return false;
      }
    }
    return true;
  }, [form, visibleQuestions, answers]);

  const handleSubmit = async () => {
    if (!form || !canSubmit) return;
    setSubmitting(true);
    setSubmitError(null);

    const submittedAnswers: FeedbackAnswer[] = visibleQuestions.map((q) => {
      const val = answers[q.id];
      if (q.type === 'TEXT') {
        return { questionId: q.id, textValue: typeof val === 'string' ? val : null, ratingValue: null };
      }
      return { questionId: q.id, ratingValue: typeof val === 'number' ? val : null, textValue: null };
    });

    const submission: PublicFeedbackSubmission = {
      anonymous,
      submitterName: anonymous ? null : (submitterName.trim() || null),
      submitterEmail: anonymous ? null : (submitterEmail.trim() || null),
      answers: submittedAnswers,
      additionalComments: comments.trim() || null,
      website,
    };

    try {
      await submitPublicFeedback(linkToken, submission);
      setSubmitted(true);
    } catch (err) {
      if (err instanceof ApiException && (err.status === 404 || err.status === 410)) {
        // Link became unavailable — surface a terminal message.
        setState({ kind: 'error', message: messageForError(err) });
        return;
      }
      setSubmitError(messageForError(err));
    } finally {
      setSubmitting(false);
    }
  };

  // --- Render states ---

  if (state.kind === 'loading') {
    return (
      <div style={pageStyle}>
        <meta name="robots" content="noindex" />
        <div data-testid="public-feedback-loading" style={{ ...panelStyle, color: 'var(--color-text-muted)', textAlign: 'center' }}>
          Loading...
        </div>
      </div>
    );
  }

  if (state.kind === 'error') {
    return (
      <div style={pageStyle}>
        <meta name="robots" content="noindex" />
        <div data-testid="public-feedback-unavailable" style={{ ...panelStyle, textAlign: 'center' }}>
          <p style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', margin: 0 }}>
            {state.message}
          </p>
        </div>
      </div>
    );
  }

  if (submitted) {
    return (
      <div style={pageStyle}>
        <meta name="robots" content="noindex" />
        <div data-testid="public-feedback-confirmation" style={{ ...panelStyle, textAlign: 'center' }}>
          <p style={{ fontSize: 'var(--text-h3)', color: 'var(--color-text-primary)', margin: 0 }}>
            Thanks — your feedback was recorded.
          </p>
        </div>
      </div>
    );
  }

  const personName = form!.personName;

  return (
    <div style={pageStyle}>
      <meta name="robots" content="noindex" />
      <form
        data-testid="public-feedback-form"
        onSubmit={(e) => {
          e.preventDefault();
          handleSubmit();
        }}
        style={panelStyle}
      >
        <h1
          data-testid="public-feedback-title"
          style={{
            fontSize: 'var(--text-h2)',
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-text-primary)',
            margin: '0 0 var(--space-2) 0',
          }}
        >
          Provide feedback for {personName}
        </h1>
        <p
          data-testid="public-feedback-subtitle"
          style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', margin: '0 0 var(--space-5) 0' }}
        >
          Your input helps {personName} reflect and improve. Responses are reviewed by their manager.
        </p>

        {/* Honeypot — real users leave this empty */}
        <div style={honeypotStyle} aria-hidden="true">
          <label htmlFor="pf-website">Website</label>
          <input
            id="pf-website"
            type="text"
            name="website"
            data-testid="public-feedback-honeypot"
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
            tabIndex={-1}
            autoComplete="off"
          />
        </div>

        {/* Questions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          {visibleQuestions.map((q) => (
            <div key={q.id} data-testid={`public-feedback-question-${q.id}`}>
              <span style={labelStyle}>
                {q.text}{q.required ? ' *' : ''}
              </span>

              {q.type === 'RATING' && (
                <div>
                  <div role="radiogroup" aria-label={q.text} style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    {[1, 2, 3, 4, 5].map((v) => {
                      const selected = answers[q.id] === v;
                      return (
                        <button
                          key={v}
                          type="button"
                          role="radio"
                          aria-checked={selected}
                          onClick={() => setRating(q.id, v)}
                          data-testid={`public-feedback-rating-${q.id}-${v}`}
                          style={{
                            width: '44px',
                            height: '44px',
                            border: selected ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
                            borderRadius: 'var(--radius-medium)',
                            backgroundColor: selected ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
                            color: selected ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                            fontFamily: 'var(--font-mono)',
                            fontSize: 'var(--text-body)',
                            cursor: 'pointer',
                          }}
                        >
                          {v}
                        </button>
                      );
                    })}
                  </div>
                  {(q.lowLabel || q.highLabel) && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>
                      <span data-testid={`public-feedback-low-label-${q.id}`}>{q.lowLabel}</span>
                      <span data-testid={`public-feedback-high-label-${q.id}`}>{q.highLabel}</span>
                    </div>
                  )}
                </div>
              )}

              {q.type === 'LIKERT' && (
                <div role="radiogroup" aria-label={q.text} style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {LIKERT_OPTIONS.map((opt) => {
                    const selected = answers[q.id] === opt.value;
                    return (
                      <button
                        key={opt.value}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        onClick={() => setRating(q.id, opt.value)}
                        data-testid={`public-feedback-likert-${q.id}-${opt.value}`}
                        style={{
                          textAlign: 'left',
                          padding: '8px 12px',
                          border: selected ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
                          borderRadius: 'var(--radius-medium)',
                          backgroundColor: selected ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
                          color: selected ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                          fontSize: 'var(--text-body)',
                          cursor: 'pointer',
                        }}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>
              )}

              {q.type === 'TEXT' && (
                <textarea
                  value={typeof answers[q.id] === 'string' ? (answers[q.id] as string) : ''}
                  onChange={(e) => setText(q.id, e.target.value)}
                  rows={3}
                  data-testid={`public-feedback-text-${q.id}`}
                  aria-label={q.text}
                  style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }}
                />
              )}
            </div>
          ))}
        </div>

        {/* Additional comments */}
        <div style={{ marginTop: 'var(--space-4)' }}>
          <label htmlFor="pf-comments" style={labelStyle}>Anything else? (optional)</label>
          <textarea
            id="pf-comments"
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            rows={3}
            data-testid="public-feedback-comments"
            style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit' }}
          />
        </div>

        {/* Submitter info */}
        {form!.requestSubmitterInfo && (
          <div data-testid="public-feedback-submitter" style={{ marginTop: 'var(--space-4)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            <label
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: 'var(--text-body)', color: 'var(--color-text-primary)', cursor: 'pointer' }}
            >
              <input
                type="checkbox"
                checked={anonymous}
                onChange={(e) => setAnonymous(e.target.checked)}
                data-testid="public-feedback-anonymous-toggle"
                style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
              />
              Submit anonymously
            </label>
            {!anonymous && (
              <>
                <div>
                  <label htmlFor="pf-name" style={labelStyle}>Your name (optional)</label>
                  <input
                    id="pf-name"
                    type="text"
                    value={submitterName}
                    onChange={(e) => setSubmitterName(e.target.value)}
                    data-testid="public-feedback-name"
                    style={inputStyle}
                  />
                </div>
                <div>
                  <label htmlFor="pf-email" style={labelStyle}>Your email (optional)</label>
                  <input
                    id="pf-email"
                    type="email"
                    value={submitterEmail}
                    onChange={(e) => setSubmitterEmail(e.target.value)}
                    data-testid="public-feedback-email"
                    style={inputStyle}
                  />
                </div>
              </>
            )}
          </div>
        )}

        {submitError && (
          <div
            data-testid="public-feedback-error"
            style={{
              marginTop: 'var(--space-4)',
              padding: '8px 12px',
              borderRadius: 'var(--radius-medium)',
              border: '1px solid var(--color-alert)',
              backgroundColor: 'var(--color-alert-muted)',
              color: 'var(--color-alert)',
              fontSize: 'var(--text-small)',
            }}
          >
            {submitError}
          </div>
        )}

        <button
          type="submit"
          disabled={!canSubmit || submitting}
          data-testid="public-feedback-submit"
          style={{
            marginTop: 'var(--space-5)',
            padding: '10px 20px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: !canSubmit || submitting ? 'not-allowed' : 'pointer',
            fontWeight: 'var(--weight-medium)',
            opacity: !canSubmit || submitting ? 0.6 : 1,
          }}
        >
          {submitting ? 'Sending...' : 'Send feedback'}
        </button>
      </form>
    </div>
  );
}
