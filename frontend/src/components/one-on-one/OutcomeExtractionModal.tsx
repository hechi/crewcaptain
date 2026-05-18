'use client';

import { useState, useEffect } from 'react';
import { extractOutcomes, applyOutcomes } from '@/lib/api-client';
import { ExtractedActionItem } from '@/types/settings';
import { Sparkles } from 'lucide-react';

interface OutcomeExtractionModalProps {
  token: string;
  personId: string;
  entryId: string;
  onClose: () => void;
  onApplied: () => void;
  existingActionItemTitles?: string[];
}

interface EditableActionItem extends ExtractedActionItem {
  selected: boolean;
  editedTitle: string;
  isDuplicate: boolean;
}

interface EditableDecision {
  text: string;
  selected: boolean;
  editedText: string;
}

export default function OutcomeExtractionModal({
  token,
  personId,
  entryId,
  onClose,
  onApplied,
  existingActionItemTitles = [],
}: OutcomeExtractionModalProps) {
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionItems, setActionItems] = useState<EditableActionItem[]>([]);
  const [decisions, setDecisions] = useState<EditableDecision[]>([]);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const doExtract = async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await extractOutcomes(token, personId, entryId);
        if (result.error) {
          setError(result.error);
        } else {
          const normalizedExisting = existingActionItemTitles.map(t => t.toLowerCase().trim());
          setActionItems(
            result.actionItems.map((item) => {
              const isDuplicate = normalizedExisting.includes(item.title.toLowerCase().trim());
              return {
                ...item,
                selected: !isDuplicate,
                editedTitle: item.title,
                isDuplicate,
              };
            })
          );
          setDecisions(
            result.decisions.map((d) => ({
              text: d,
              selected: true,
              editedText: d,
            }))
          );
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to extract outcomes');
      } finally {
        setLoading(false);
      }
    };
    doExtract();
  }, [token, personId, entryId, existingActionItemTitles]);

  const handleApply = async () => {
    setApplying(true);
    setError(null);
    try {
      const selectedItems = actionItems
        .filter((item) => item.selected && item.editedTitle.trim())
        .map((item) => ({
          title: item.editedTitle.trim(),
          ownerType: item.ownerType,
          suggestedDaysToDue: item.suggestedDaysToDue,
        }));

      const selectedDecisions = decisions
        .filter((d) => d.selected && d.editedText.trim())
        .map((d) => d.editedText.trim());

      if (selectedItems.length === 0 && selectedDecisions.length === 0) {
        setError('Please select at least one item to apply.');
        setApplying(false);
        return;
      }

      const result = await applyOutcomes(token, personId, entryId, {
        actionItems: selectedItems,
        decisions: selectedDecisions,
      });

      setSuccess(
        `Created ${result.actionItemsCreated} action item${result.actionItemsCreated !== 1 ? 's' : ''}` +
        (result.decisionsAppended > 0
          ? ` and appended ${result.decisionsAppended} decision${result.decisionsAppended !== 1 ? 's' : ''}`
          : '')
      );

      setTimeout(() => {
        onApplied();
      }, 1500);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to apply outcomes');
    } finally {
      setApplying(false);
    }
  };

  const toggleActionItem = (index: number) => {
    setActionItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, selected: !item.selected } : item))
    );
  };

  const updateActionItemTitle = (index: number, title: string) => {
    setActionItems((prev) =>
      prev.map((item, i) => (i === index ? { ...item, editedTitle: title } : item))
    );
  };

  const toggleDecision = (index: number) => {
    setDecisions((prev) =>
      prev.map((d, i) => (i === index ? { ...d, selected: !d.selected } : d))
    );
  };

  const updateDecisionText = (index: number, text: string) => {
    setDecisions((prev) =>
      prev.map((d, i) => (i === index ? { ...d, editedText: text } : d))
    );
  };

  const managerItems = actionItems.filter((item) => item.ownerType === 'MANAGER');
  const personItems = actionItems.filter((item) => item.ownerType === 'PERSON');

  return (
    <div
      data-testid="outcome-extraction-modal-backdrop"
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: 'var(--space-4)',
      }}
    >
      <div
        data-testid="outcome-extraction-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Extract Outcomes"
        style={{
          width: '100%',
          maxWidth: '640px',
          maxHeight: '80vh',
          overflow: 'auto',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border-glow)',
          backgroundColor: 'var(--color-bg-surface)',
          backdropFilter: 'blur(12px)',
          boxShadow: '0 0 30px var(--color-primary-muted), 0 20px 60px rgba(0,0,0,0.5)',
          padding: 'var(--space-6)',
          position: 'relative',
        }}
      >
        {/* Scan-line texture */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.01) 2px, rgba(0,255,255,0.01) 4px)',
            pointerEvents: 'none',
            borderRadius: 'var(--radius-large)',
          }}
        />

        <div style={{ position: 'relative', zIndex: 1 }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
            <h2
              style={{
                margin: 0,
                fontSize: 'var(--text-h3)',
                fontFamily: 'var(--font-heading)',
                fontWeight: 'var(--weight-bold)',
                color: 'var(--color-primary)',
                letterSpacing: '0.5px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <Sparkles size={18} /> Extract Outcomes
            </h2>
            <button
              type="button"
              onClick={onClose}
              data-testid="close-modal-btn"
              aria-label="Close modal"
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--color-text-secondary)',
                fontSize: '20px',
                cursor: 'pointer',
                padding: '4px 8px',
              }}
            >
              ✕
            </button>
          </div>

          {/* Loading state */}
          {loading && (
            <div
              data-testid="extraction-loading"
              style={{
                padding: 'var(--space-8)',
                textAlign: 'center',
                color: 'var(--color-text-secondary)',
                animation: 'pulse 1.5s ease-in-out infinite',
              }}
            >
              <div style={{ fontSize: 'var(--text-h3)', marginBottom: 'var(--space-2)' }}>✦</div>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 'var(--text-small)' }}>
                Analyzing meeting notes...
              </span>
            </div>
          )}

          {/* Error state */}
          {error && !loading && (
            <div
              data-testid="extraction-error"
              style={{
                padding: 'var(--space-4)',
                borderRadius: 'var(--radius-medium)',
                border: '1px solid var(--color-alert-muted)',
                backgroundColor: 'rgba(255, 50, 50, 0.05)',
                color: 'var(--color-alert)',
                fontSize: 'var(--text-body)',
                marginBottom: 'var(--space-4)',
              }}
            >
              {error}
            </div>
          )}

          {/* Success state */}
          {success && (
            <div
              data-testid="extraction-success"
              style={{
                padding: 'var(--space-4)',
                borderRadius: 'var(--radius-medium)',
                border: '1px solid var(--color-success)',
                backgroundColor: 'rgba(0, 255, 100, 0.05)',
                color: 'var(--color-success)',
                fontSize: 'var(--text-body)',
                textAlign: 'center',
                fontFamily: 'var(--font-mono)',
              }}
            >
              ✓ {success}
            </div>
          )}

          {/* Results */}
          {!loading && !error && !success && (
            <>
              {/* Manager Action Items */}
              {managerItems.length > 0 && (
                <div style={{ marginBottom: 'var(--space-5)' }}>
                  <h3
                    style={{
                      margin: '0 0 var(--space-3) 0',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-heading)',
                      fontWeight: 'var(--weight-semibold)',
                      color: 'var(--color-primary)',
                    }}
                  >
                    Your Action Items
                  </h3>
                  {managerItems.map((item) => {
                    const globalIndex = actionItems.indexOf(item);
                    return (
                      <ActionItemRow
                        key={globalIndex}
                        item={item}
                        index={globalIndex}
                        accentColor="var(--color-primary)"
                        onToggle={toggleActionItem}
                        onUpdateTitle={updateActionItemTitle}
                      />
                    );
                  })}
                </div>
              )}

              {/* Person Action Items */}
              {personItems.length > 0 && (
                <div style={{ marginBottom: 'var(--space-5)' }}>
                  <h3
                    style={{
                      margin: '0 0 var(--space-3) 0',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-heading)',
                      fontWeight: 'var(--weight-semibold)',
                      color: 'var(--color-accent)',
                    }}
                  >
                    Their Action Items
                  </h3>
                  {personItems.map((item) => {
                    const globalIndex = actionItems.indexOf(item);
                    return (
                      <ActionItemRow
                        key={globalIndex}
                        item={item}
                        index={globalIndex}
                        accentColor="var(--color-accent)"
                        onToggle={toggleActionItem}
                        onUpdateTitle={updateActionItemTitle}
                      />
                    );
                  })}
                </div>
              )}

              {/* Decisions */}
              {decisions.length > 0 && (
                <div style={{ marginBottom: 'var(--space-5)' }}>
                  <h3
                    style={{
                      margin: '0 0 var(--space-3) 0',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-heading)',
                      fontWeight: 'var(--weight-semibold)',
                      color: 'var(--color-text-primary)',
                    }}
                  >
                    Key Decisions
                  </h3>
                  {decisions.map((decision, index) => (
                    <div
                      key={index}
                      data-testid={`decision-row-${index}`}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 'var(--space-3)',
                        padding: '10px 12px',
                        borderRadius: 'var(--radius-small)',
                        border: '1px solid var(--color-border)',
                        backgroundColor: 'var(--color-bg-elevated)',
                        marginBottom: 'var(--space-2)',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={decision.selected}
                        onChange={() => toggleDecision(index)}
                        aria-label={`Select decision: ${decision.text}`}
                        style={{ marginTop: '4px', accentColor: 'var(--color-primary)' }}
                      />
                      <input
                        type="text"
                        value={decision.editedText}
                        onChange={(e) => updateDecisionText(index, e.target.value)}
                        aria-label={`Edit decision: ${decision.text}`}
                        style={{
                          flex: 1,
                          background: 'transparent',
                          border: 'none',
                          color: 'var(--color-text-primary)',
                          fontSize: 'var(--text-body)',
                          fontFamily: 'var(--font-body)',
                          outline: 'none',
                          opacity: decision.selected ? 1 : 0.5,
                        }}
                      />
                    </div>
                  ))}
                </div>
              )}

              {/* Empty state */}
              {actionItems.length === 0 && decisions.length === 0 && (
                <div
                  data-testid="extraction-empty"
                  style={{
                    padding: 'var(--space-6)',
                    textAlign: 'center',
                    color: 'var(--color-text-secondary)',
                    fontSize: 'var(--text-body)',
                  }}
                >
                  No action items or decisions were found in the notes.
                </div>
              )}

              {/* Actions */}
              {(actionItems.length > 0 || decisions.length > 0) && (
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)', marginTop: 'var(--space-4)' }}>
                  <button
                    type="button"
                    onClick={onClose}
                    style={{
                      padding: '8px 16px',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-medium)',
                      background: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-secondary)',
                      cursor: 'pointer',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    data-testid="apply-outcomes-btn"
                    onClick={handleApply}
                    disabled={applying}
                    style={{
                      padding: '8px 20px',
                      border: '1px solid var(--color-border-glow)',
                      borderRadius: 'var(--radius-medium)',
                      background: applying ? 'var(--color-bg-elevated)' : 'var(--color-primary-muted)',
                      color: 'var(--color-primary)',
                      cursor: applying ? 'not-allowed' : 'pointer',
                      fontSize: 'var(--text-body)',
                      fontWeight: 'var(--weight-medium)',
                      fontFamily: 'var(--font-mono)',
                      opacity: applying ? 0.6 : 1,
                      boxShadow: applying ? 'none' : '0 0 8px var(--color-primary-muted)',
                      transition: 'all 0.2s',
                    }}
                  >
                    {applying ? 'Applying...' : 'Sync All'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>

        {/* CSS animation */}
        <style>{`
          @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
          }
          @media (prefers-reduced-motion: reduce) {
            [data-testid="extraction-loading"] {
              animation: none !important;
            }
          }
        `}</style>
      </div>
    </div>
  );
}

// --- Sub-component for action item rows ---

function ActionItemRow({
  item,
  index,
  accentColor,
  onToggle,
  onUpdateTitle,
}: {
  item: EditableActionItem;
  index: number;
  accentColor: string;
  onToggle: (index: number) => void;
  onUpdateTitle: (index: number, title: string) => void;
}) {
  return (
    <div
      data-testid={`action-item-row-${index}`}
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 'var(--space-3)',
        padding: '10px 12px',
        borderRadius: 'var(--radius-small)',
        border: `1px solid ${item.isDuplicate ? 'var(--color-warning, #f59e0b)' : 'var(--color-border)'}`,
        backgroundColor: 'var(--color-bg-elevated)',
        marginBottom: 'var(--space-2)',
        borderLeft: `3px solid ${accentColor}`,
        opacity: item.selected ? 1 : 0.5,
        transition: 'opacity 0.2s',
      }}
    >
      <input
        type="checkbox"
        checked={item.selected}
        onChange={() => onToggle(index)}
        aria-label={`Select action item: ${item.editedTitle}`}
        style={{ marginTop: '4px', accentColor }}
      />
      <div style={{ flex: 1 }}>
        <input
          type="text"
          value={item.editedTitle}
          onChange={(e) => onUpdateTitle(index, e.target.value)}
          aria-label={`Edit action item: ${item.title}`}
          style={{
            width: '100%',
            background: 'transparent',
            border: 'none',
            color: 'var(--color-text-primary)',
            fontSize: 'var(--text-body)',
            fontFamily: 'var(--font-body)',
            outline: 'none',
          }}
        />
        <div style={{ display: 'flex', gap: 'var(--space-3)', marginTop: '4px' }}>
          {item.suggestedDaysToDue && (
            <span style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', fontFamily: 'var(--font-mono)' }}>
              Due in {item.suggestedDaysToDue}d
            </span>
          )}
          {item.isDuplicate && (
            <span
              data-testid={`duplicate-badge-${index}`}
              style={{
                fontSize: 'var(--text-small)',
                color: 'var(--color-warning, #f59e0b)',
                fontFamily: 'var(--font-mono)',
              }}
            >
              ⚠ Possible duplicate
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
