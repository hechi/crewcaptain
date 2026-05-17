'use client';

import { useState } from 'react';
import { CreatePdpGoalRequest, UpdatePdpGoalRequest, PdpGoal } from '@/types/pdp-goal';
import { optimizePdpGoal } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';

interface PdpGoalFormProps {
  /** If provided, the form is in edit mode */
  existingGoal?: PdpGoal;
  onSubmit: (data: CreatePdpGoalRequest | UpdatePdpGoalRequest) => void;
  onCancel: () => void;
  aiEnabled?: boolean;
}

/**
 * Form for creating or editing a PDP goal.
 * Supports title, description, target date fields, and an AI-powered SMART Check.
 */
export default function PdpGoalForm({ existingGoal, onSubmit, onCancel, aiEnabled = false }: PdpGoalFormProps) {
  const { getToken } = useStableToken();
  const [title, setTitle] = useState(existingGoal?.title || '');
  const [description, setDescription] = useState(existingGoal?.description || '');
  const [targetDate, setTargetDate] = useState(existingGoal?.targetDate || '');
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [aiSuggestion, setAiSuggestion] = useState<string | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);
  const [showComparison, setShowComparison] = useState(false);

  const isEditing = !!existingGoal;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    if (isEditing) {
      const data: UpdatePdpGoalRequest = {};
      if (title !== existingGoal.title) data.title = title;
      if (description !== (existingGoal.description || '')) data.description = description || null;
      if (targetDate !== (existingGoal.targetDate || '')) data.targetDate = targetDate || null;
      onSubmit(data);
    } else {
      const data: CreatePdpGoalRequest = {
        title: title.trim(),
        description: description.trim() || null,
        targetDate: targetDate || null,
      };
      onSubmit(data);
    }
  };

  const handleSmartCheck = async () => {
    const token = getToken();
    if (!token || !title.trim()) return;

    setIsOptimizing(true);
    setAiError(null);
    setAiSuggestion(null);
    setShowComparison(false);

    try {
      const response = await optimizePdpGoal(token, title.trim(), description.trim() || null);
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
      // and keep the existing title
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
    <form
      onSubmit={handleSubmit}
      data-testid="pdp-goal-form"
      style={{
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        backgroundColor: 'var(--color-bg-surface)',
      }}
    >
      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="pdp-goal-title"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Title *
        </label>
        <input
          id="pdp-goal-title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g., Improve public speaking"
          required
          data-testid="pdp-goal-title-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />
      </div>

      <div style={{ marginBottom: '12px' }}>
        <label
          htmlFor="pdp-goal-description"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Description
        </label>
        <textarea
          id="pdp-goal-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Describe the goal and success criteria..."
          rows={3}
          data-testid="pdp-goal-description-input"
          style={{
            width: '100%',
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
            resize: 'vertical',
            boxSizing: 'border-box',
            fontFamily: 'inherit',
          }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label
          htmlFor="pdp-goal-target-date"
          style={{
            display: 'block',
            fontSize: 'var(--text-caption)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-muted)',
            marginBottom: '4px',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          Target Date
        </label>
        <input
          id="pdp-goal-target-date"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          data-testid="pdp-goal-target-date-input"
          style={{
            padding: '8px 12px',
            fontSize: 'var(--text-body)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-primary)',
            outline: 'none',
          }}
        />
      </div>

      {/* AI SMART Check Button */}
      {aiEnabled && (
        <div style={{ marginBottom: '16px' }}>
          <button
            type="button"
            onClick={handleSmartCheck}
            disabled={isOptimizing || !title.trim()}
            data-testid="pdp-goal-smart-check-btn"
            style={{
              padding: '6px 14px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-primary)',
              borderRadius: 'var(--radius-full)',
              backgroundColor: isOptimizing ? 'var(--color-primary-muted)' : 'transparent',
              color: 'var(--color-primary)',
              cursor: isOptimizing || !title.trim() ? 'not-allowed' : 'pointer',
              opacity: isOptimizing || !title.trim() ? 0.6 : 1,
              transition: 'all 0.2s',
              animation: aiSuggestion ? 'glow-burst 0.6s ease-out' : undefined,
            }}
          >
            {isOptimizing ? '✦ Checking...' : '✦ SMART Check'}
          </button>
        </div>
      )}

      {/* AI Error */}
      {aiError && (
        <div
          data-testid="pdp-goal-ai-error"
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
          data-testid="pdp-goal-ai-comparison"
          style={{
            marginBottom: '16px',
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
              data-testid="pdp-goal-ai-apply-btn"
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
              data-testid="pdp-goal-ai-dismiss-btn"
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

      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          type="submit"
          data-testid="pdp-goal-submit-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-primary)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-primary-muted)',
            color: 'var(--color-primary)',
            cursor: 'pointer',
            fontWeight: 'var(--weight-medium)',
          }}
        >
          {isEditing ? 'Update Goal' : 'Create Goal'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          data-testid="pdp-goal-cancel-btn"
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-mono)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-medium)',
            backgroundColor: 'var(--color-bg-elevated)',
            color: 'var(--color-text-muted)',
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
