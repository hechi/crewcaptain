'use client';

import { useState } from 'react';
import { CreateKudosRequest } from '@/types/kudos';
import { refineKudos } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface KudosFormProps {
  onSubmit: (data: CreateKudosRequest) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
  aiEnabled?: boolean;
}

/**
 * Form for creating a new kudos entry with date, text, optional tags,
 * and an AI-powered "Refine" button that uses the SBI framework.
 */
export default function KudosForm({ onSubmit, onCancel, isSubmitting = false, aiEnabled = false }: KudosFormProps) {
  const { getToken } = useStableToken();
  const [text, setText] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [tagsInput, setTagsInput] = useState('');
  const [isRefining, setIsRefining] = useState(false);
  const [aiSuggestion, setAiSuggestion] = useState<string | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);
  const [showComparison, setShowComparison] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;

    const tags = tagsInput
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0);

    onSubmit({
      text: text.trim(),
      date,
      tags: tags.length > 0 ? tags : undefined,
    });
  };

  const handleRefine = async () => {
    const token = getToken();
    if (!token || !text.trim()) return;

    setIsRefining(true);
    setAiError(null);
    setAiSuggestion(null);
    setShowComparison(false);

    try {
      const response = await refineKudos(token, text.trim());
      if (response.result) {
        setAiSuggestion(response.result);
        setShowComparison(true);
      } else if (response.error) {
        setAiError(response.error);
      }
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'Failed to refine kudos');
    } finally {
      setIsRefining(false);
    }
  };

  const handleApplySuggestion = () => {
    if (aiSuggestion) {
      setText(aiSuggestion);
      setAiSuggestion(null);
      setShowComparison(false);
    }
  };

  const handleDismissSuggestion = () => {
    setAiSuggestion(null);
    setShowComparison(false);
  };

  return (
    <form
      onSubmit={handleSubmit}
      data-testid="kudos-form"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      {/* Date */}
      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="kudos-date"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
          }}
        >
          Date
        </label>
        <input
          id="kudos-date"
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          data-testid="kudos-date-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      {/* Text */}
      <div style={{ marginBottom: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
          <label
            htmlFor="kudos-text"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-muted)',
            }}
          >
            Recognition *
          </label>
          {aiEnabled && (
            <button
              type="button"
              onClick={handleRefine}
              disabled={isRefining || !text.trim()}
              data-testid="kudos-refine-btn"
              style={{
                padding: '2px 10px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-primary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: isRefining ? 'var(--color-primary-muted)' : 'transparent',
                color: 'var(--color-primary)',
                cursor: isRefining || !text.trim() ? 'not-allowed' : 'pointer',
                opacity: isRefining || !text.trim() ? 0.6 : 1,
                transition: 'all 0.2s',
                animation: aiSuggestion ? 'glow-burst 0.6s ease-out' : undefined,
              }}
            >
              {isRefining ? '✦ Refining...' : '✦ Refine'}
            </button>
          )}
        </div>
        <textarea
          id="kudos-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="What did they do well? Be specific..."
          required
          rows={3}
          data-testid="kudos-text-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            resize: 'vertical',
            fontFamily: 'inherit',
          }}
        />
      </div>

      {/* AI Error */}
      {aiError && (
        <div
          data-testid="kudos-ai-error"
          style={{
            marginBottom: '12px',
            padding: '8px 12px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {aiError}
        </div>
      )}

      {/* AI Comparison View */}
      {showComparison && aiSuggestion && (
        <div
          data-testid="kudos-ai-comparison"
          style={{
            marginBottom: '12px',
            padding: 'var(--space-3)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-primary)',
            backgroundColor: 'var(--color-primary-muted)',
          }}
        >
          <div style={{
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-primary)',
            marginBottom: '8px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}>
            ✦ AI Suggestion (SBI Framework)
          </div>
          <p style={{
            margin: '0 0 12px',
            fontSize: 'var(--text-body)',
            color: 'var(--color-text-primary)',
            lineHeight: '1.5',
            whiteSpace: 'pre-wrap',
          }}>
            {aiSuggestion}
          </p>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              onClick={handleApplySuggestion}
              data-testid="kudos-ai-apply-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-primary)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-primary)',
                color: 'var(--color-bg-base)',
                cursor: 'pointer',
                fontWeight: 'var(--weight-medium)',
              }}
            >
              Apply
            </button>
            <button
              type="button"
              onClick={handleDismissSuggestion}
              data-testid="kudos-ai-dismiss-btn"
              style={{
                padding: '4px 12px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'transparent',
                color: 'var(--color-text-muted)',
                cursor: 'pointer',
              }}
            >
              Keep Original
            </button>
          </div>
        </div>
      )}

      {/* Tags */}
      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="kudos-tags"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
          }}
        >
          Tags (comma-separated, optional)
        </label>
        <input
          id="kudos-tags"
          type="text"
          value={tagsInput}
          onChange={(e) => setTagsInput(e.target.value)}
          placeholder="e.g. impact, collaboration, leadership"
          data-testid="kudos-tags-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            fontSize: 'var(--text-body)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          type="submit"
          disabled={isSubmitting || !text.trim()}
          data-testid="kudos-submit-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: isSubmitting || !text.trim() ? 'not-allowed' : 'pointer',
            fontWeight: 'var(--weight-medium)',
            opacity: isSubmitting || !text.trim() ? 0.6 : 1,
          }}
        >
          {isSubmitting ? 'Saving...' : 'Give Kudos'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          data-testid="kudos-cancel-btn"
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
    </form>
  );
}
