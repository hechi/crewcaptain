'use client';

import { useState } from 'react';
import { CreateStrategyGoalRequest, UpdateStrategyGoalRequest } from '@/types/strategy-goal';
import { optimizeStrategyGoal } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface StrategyGoalFormProps {
  initialData?: {
    title: string;
    description: string | null;
    targetDate: string | null;
    sensitive: boolean;
  };
  onSubmit: (data: CreateStrategyGoalRequest | UpdateStrategyGoalRequest) => void;
  onCancel: () => void;
  submitLabel: string;
  aiEnabled?: boolean;
}

export default function StrategyGoalForm({
  initialData,
  onSubmit,
  onCancel,
  submitLabel,
  aiEnabled = false,
}: StrategyGoalFormProps) {
  const { getToken } = useStableToken();
  const [title, setTitle] = useState(initialData?.title ?? '');
  const [description, setDescription] = useState(initialData?.description ?? '');
  const [targetDate, setTargetDate] = useState(initialData?.targetDate ?? '');
  const [sensitive, setSensitive] = useState(initialData?.sensitive ?? false);
  const [error, setError] = useState<string | null>(null);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [aiSuggestion, setAiSuggestion] = useState<string | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);
  const [showComparison, setShowComparison] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('Title is required');
      return;
    }

    if (title.length > 500) {
      setError('Title must not exceed 500 characters');
      return;
    }

    if (description && description.length > 5000) {
      setError('Description must not exceed 5000 characters');
      return;
    }

    onSubmit({
      title: title.trim(),
      description: description || null,
      targetDate: targetDate || null,
      sensitive,
    });
  };

  const handleSmartCheck = async () => {
    const token = getToken();
    if (!token || !title.trim()) return;

    setIsOptimizing(true);
    setAiError(null);
    setAiSuggestion(null);
    setShowComparison(false);

    try {
      const response = await optimizeStrategyGoal(token, title.trim(), description.trim() || null);
      if (response.result) {
        setAiSuggestion(response.result);
        setShowComparison(true);
      } else if (response.error) {
        setAiError(response.error);
      }
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'Failed to optimize goal');
    } finally {
      setIsOptimizing(false);
    }
  };

  const handleApplySuggestion = () => {
    if (!aiSuggestion) return;

    // Parse the AI response to extract title and description
    const lines = aiSuggestion.split('\n');
    let newTitle = '';
    let newDescription = '';
    let explanationStarted = false;

    for (const line of lines) {
      // Match various formats: "Title: ...", "**Title:** ...", "Title - ..."
      const titleMatch = line.match(/^\*{0,2}Title\*{0,2}[:\-]\s*(.+)/i);
      const descMatch = line.match(/^\*{0,2}Description\*{0,2}[:\-]\s*(.+)/i);
      const explMatch = line.match(/^\*{0,2}Explanation\*{0,2}[:\-]/i);

      if (explMatch) {
        explanationStarted = true;
        continue;
      }
      if (explanationStarted) continue;

      if (titleMatch) {
        newTitle = titleMatch[1].trim();
      } else if (descMatch) {
        newDescription = descMatch[1].trim();
      }
    }

    // If parsing found at least a title, apply it; otherwise apply the whole response as description
    if (newTitle) {
      setTitle(newTitle);
      if (newDescription) {
        setDescription(newDescription);
      }
    } else {
      // Fallback: if the AI didn't use the structured format, apply the whole thing as description
      setDescription(aiSuggestion.trim());
    }

    setAiSuggestion(null);
    setShowComparison(false);
  };

  const handleDismissSuggestion = () => {
    setAiSuggestion(null);
    setShowComparison(false);
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {error && (
        <div
          style={{
            padding: '8px 12px',
            backgroundColor: 'var(--color-alert-muted)',
            border: '1px solid var(--color-alert)',
            borderRadius: 'var(--radius-medium)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="title"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Title *
        </label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g., Modernize Tech Stack"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
          <label
            htmlFor="description"
            style={{
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-text-secondary)',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            Description
          </label>
          {aiEnabled && (
            <button
              type="button"
              onClick={handleSmartCheck}
              disabled={isOptimizing || !title.trim()}
              data-testid="strategy-goal-smart-check-btn"
              style={{
                padding: '2px 10px',
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                border: '1px solid var(--color-primary)',
                borderRadius: 'var(--radius-full)',
                backgroundColor: isOptimizing ? 'var(--color-primary-muted)' : 'transparent',
                color: 'var(--color-primary)',
                cursor: isOptimizing || !title.trim() ? 'not-allowed' : 'pointer',
                opacity: isOptimizing || !title.trim() ? 0.6 : 1,
                transition: 'all 0.2s',
              }}
            >
              {isOptimizing ? '✦ Checking...' : '✦ SMART Check'}
            </button>
          )}
        </div>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe the strategy goal..."
          rows={4}
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            resize: 'vertical',
          }}
        />
      </div>

      <div>
        <label
          htmlFor="targetDate"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Target Date
        </label>
        <input
          id="targetDate"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-ui)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
          }}
        />
      </div>

      <div>
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            cursor: 'pointer',
          }}
        >
          <input
            type="checkbox"
            checked={sensitive}
            onChange={(e) => setSensitive(e.target.checked)}
            style={{ cursor: 'pointer' }}
          />
          <span>Mark as sensitive (content will be encrypted)</span>
        </label>
      </div>

      {/* AI Error */}
      {aiError && (
        <div
          data-testid="strategy-goal-ai-error"
          style={{
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
          data-testid="strategy-goal-ai-comparison"
          style={{
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
            ✦ SMART Optimization
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
              data-testid="strategy-goal-ai-apply-btn"
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
              data-testid="strategy-goal-ai-dismiss-btn"
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

      <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
        <button
          type="submit"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            fontWeight: 'var(--weight-semibold)',
            border: 'none',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary)',
            color: 'var(--color-bg-base)',
            cursor: 'pointer',
          }}
        >
          {submitLabel}
        </button>
        <button
          type="button"
          onClick={onCancel}
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
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
