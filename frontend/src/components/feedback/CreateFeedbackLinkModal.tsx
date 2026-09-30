'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  FeedbackTemplate,
  FeedbackTemplateDraft,
  CreateFeedbackLinkRequest,
  SaveFeedbackTemplateRequest,
} from '@/types/feedback';
import {
  listFeedbackTemplates,
  getFeedbackStarterTemplates,
  generateFeedbackTemplate,
  createFeedbackLink,
} from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import Modal from '@/components/Modal';
import FeedbackTemplateBuilder from '@/components/feedback/FeedbackTemplateBuilder';
import Spinner from '@/components/feedback/Spinner';

interface CreateFeedbackLinkModalProps {
  isOpen: boolean;
  personId: string;
  personName?: string;
  aiAvailable?: boolean;
  onClose: () => void;
  /** Called after a link is successfully created (e.g. to refresh the links list). */
  onCreated?: () => void;
}

const DEFAULT_DAYS = 14;
const MAX_DAYS = 90;

// Sentinel values for the template <select>.
const NEW_TEMPLATE = '__new__';

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

function publicUrl(token: string): string {
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  return `${origin}/f/${token}`;
}

/**
 * Modal for creating a feedback link for a person. The manager can choose a
 * saved template, build a new one inline (reusing FeedbackTemplateBuilder),
 * generate one with AI from a brief, or start from a provided starter.
 */
export default function CreateFeedbackLinkModal({
  isOpen,
  personId,
  personName,
  aiAvailable = false,
  onClose,
  onCreated,
}: CreateFeedbackLinkModalProps) {
  const { getToken } = useStableToken();

  const [templates, setTemplates] = useState<FeedbackTemplate[]>([]);
  const [starters, setStarters] = useState<FeedbackTemplateDraft[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('');
  const [inlineDraft, setInlineDraft] = useState<FeedbackTemplateDraft | null>(null);
  const [showBuilder, setShowBuilder] = useState(false);

  const [expiresInDays, setExpiresInDays] = useState(DEFAULT_DAYS);
  const [label, setLabel] = useState('');
  const [requestSubmitterInfo, setRequestSubmitterInfo] = useState(true);

  const [brief, setBrief] = useState('');
  const [generating, setGenerating] = useState(false);
  const [genError, setGenError] = useState<string | null>(null);

  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdToken, setCreatedToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const resetState = useCallback(() => {
    setSelectedTemplateId('');
    setInlineDraft(null);
    setShowBuilder(false);
    setExpiresInDays(DEFAULT_DAYS);
    setLabel('');
    setRequestSubmitterInfo(true);
    setBrief('');
    setGenError(null);
    setError(null);
    setCreatedToken(null);
    setCopied(false);
  }, []);

  const loadTemplates = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      const [tpls, strt] = await Promise.all([
        listFeedbackTemplates(token),
        getFeedbackStarterTemplates(token).catch(() => [] as FeedbackTemplateDraft[]),
      ]);
      setTemplates(tpls);
      setStarters(strt);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load templates');
    }
  }, [getToken]);

  useEffect(() => {
    if (isOpen) {
      resetState();
      loadTemplates();
    }
  }, [isOpen, resetState, loadTemplates]);

  const handleTemplateChange = (value: string) => {
    setSelectedTemplateId(value);
    setInlineDraft(null);
    if (value === NEW_TEMPLATE) {
      setShowBuilder(true);
    } else {
      setShowBuilder(false);
    }
  };

  const handleUseStarter = (index: number) => {
    const starter = starters[index];
    if (!starter) return;
    setSelectedTemplateId(NEW_TEMPLATE);
    setInlineDraft(starter);
    setShowBuilder(true);
  };

  const handleGenerate = async () => {
    const token = getToken();
    if (!token || !brief.trim()) return;
    setGenerating(true);
    setGenError(null);
    try {
      const result = await generateFeedbackTemplate(token, brief.trim());
      if (result.draft) {
        setSelectedTemplateId(NEW_TEMPLATE);
        setInlineDraft(result.draft);
        setShowBuilder(true);
      } else if (result.error) {
        setGenError(result.error);
      }
    } catch (err) {
      setGenError(err instanceof Error ? err.message : 'Failed to generate template');
    } finally {
      setGenerating(false);
    }
  };

  // When the builder saves, capture the assembled draft inline (we don't persist
  // the template — the link is created with inline questions).
  const handleBuilderSave = (payload: SaveFeedbackTemplateRequest) => {
    setInlineDraft({ title: payload.title, description: payload.description, questions: payload.questions });
    setShowBuilder(false);
  };

  const canCreate = (): boolean => {
    if (createdToken) return false;
    if (selectedTemplateId === NEW_TEMPLATE) {
      return !!inlineDraft && !!inlineDraft.title.trim() && inlineDraft.questions.length > 0;
    }
    return !!selectedTemplateId;
  };

  const handleCreate = async () => {
    const token = getToken();
    if (!token) return;
    setCreating(true);
    setError(null);
    try {
      const base = {
        expiresInDays,
        label: label.trim() || null,
        requestSubmitterInfo,
      };
      let payload: CreateFeedbackLinkRequest;
      if (selectedTemplateId === NEW_TEMPLATE && inlineDraft) {
        payload = {
          ...base,
          title: inlineDraft.title,
          description: inlineDraft.description ?? null,
          questions: inlineDraft.questions,
        };
      } else {
        payload = { ...base, templateId: selectedTemplateId };
      }
      const link = await createFeedbackLink(token, personId, payload);
      setCreatedToken(link.token);
      onCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create feedback link');
    } finally {
      setCreating(false);
    }
  };

  const handleCopy = async () => {
    if (!createdToken) return;
    try {
      await navigator.clipboard.writeText(publicUrl(createdToken));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // ignore
    }
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create feedback link" maxWidth="640px">
      <div data-testid="create-feedback-link-modal">
        {/* Warning banner */}
        <div
          data-testid="create-feedback-link-warning"
          style={{
            marginBottom: 'var(--space-4)',
            padding: '8px 12px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-alert-muted)',
            color: 'var(--color-alert)',
            fontSize: 'var(--text-small)',
          }}
        >
          Anyone with this link can submit feedback.
        </div>

        {createdToken ? (
          <div data-testid="create-feedback-link-success">
            <p style={{ margin: '0 0 8px', color: 'var(--color-text-primary)', fontSize: 'var(--text-body)', fontWeight: 'var(--weight-medium)' }}>
              ✓ Link created{personName ? ` for ${personName}` : ''}. Copy this URL and share it with the people you want feedback from:
            </p>
            <div
              data-testid="create-feedback-link-url"
              style={{
                padding: '8px 12px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-medium)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-primary)',
                fontFamily: 'var(--font-mono)',
                fontSize: 'var(--text-small)',
                wordBreak: 'break-all',
                marginBottom: '10px',
              }}
            >
              {publicUrl(createdToken)}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={handleCopy}
                data-testid="create-feedback-link-copy"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-primary)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  cursor: 'pointer',
                  fontWeight: 'var(--weight-medium)',
                }}
              >
                {copied ? 'Copied!' : 'Copy link'}
              </button>
              <button
                type="button"
                onClick={onClose}
                data-testid="create-feedback-link-done"
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
                Done
              </button>
            </div>
            <p style={{ margin: '10px 0 0', fontSize: 'var(--text-caption)', color: 'var(--color-text-muted)' }}>
              You can find this link again anytime under the Feedback tab → Links. Responses will appear under Feedback → Responses for this person.
            </p>
          </div>
        ) : (
          <>
            {/* Template selection */}
            <div style={{ marginBottom: '12px' }}>
              <label htmlFor="cfl-template" style={labelStyle}>Template</label>
              <select
                id="cfl-template"
                value={selectedTemplateId}
                onChange={(e) => handleTemplateChange(e.target.value)}
                data-testid="create-feedback-link-template-select"
                style={inputStyle}
              >
                <option value="">Select a template…</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>{t.title}</option>
                ))}
                <option value={NEW_TEMPLATE}>+ New template (build inline)</option>
              </select>
            </div>

            {/* Starters */}
            {starters.length > 0 && !showBuilder && (
              <div data-testid="create-feedback-link-starters" style={{ marginBottom: '12px' }}>
                <span style={labelStyle}>Or start from a starter</span>
                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  {starters.map((s, i) => (
                    <button
                      key={`${s.title}-${i}`}
                      type="button"
                      onClick={() => handleUseStarter(i)}
                      data-testid={`create-feedback-link-starter-${i}`}
                      style={{
                        padding: '4px 10px',
                        fontSize: 'var(--text-caption)',
                        fontFamily: 'var(--font-mono)',
                        border: '1px solid var(--color-border)',
                        borderRadius: 'var(--radius-full)',
                        backgroundColor: 'transparent',
                        color: 'var(--color-text-secondary)',
                        cursor: 'pointer',
                      }}
                    >
                      {s.title}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* AI generate */}
            {aiAvailable && !showBuilder && (
              <div style={{ marginBottom: '12px' }}>
                <label htmlFor="cfl-brief" style={labelStyle}>Generate with AI from a brief</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input
                    id="cfl-brief"
                    type="text"
                    value={brief}
                    onChange={(e) => setBrief(e.target.value)}
                    placeholder="e.g. peer feedback on collaboration and delivery"
                    data-testid="create-feedback-link-brief"
                    style={{ ...inputStyle, flex: 1 }}
                  />
                  <button
                    type="button"
                    onClick={handleGenerate}
                    disabled={generating || !brief.trim()}
                    data-testid="create-feedback-link-generate"
                    style={{
                      padding: '8px 14px',
                      fontSize: 'var(--text-caption)',
                      fontFamily: 'var(--font-mono)',
                      border: '1px solid var(--color-primary)',
                      borderRadius: 'var(--radius-medium)',
                      backgroundColor: 'transparent',
                      color: 'var(--color-primary)',
                      cursor: generating || !brief.trim() ? 'not-allowed' : 'pointer',
                      opacity: generating || !brief.trim() ? 0.6 : 1,
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {generating ? (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                        <Spinner size={13} label="Generating template" /> Generating…
                      </span>
                    ) : '✦ Generate'}
                  </button>
                </div>
                {genError && (
                  <div
                    data-testid="create-feedback-link-gen-error"
                    style={{ marginTop: '8px', color: 'var(--color-alert)', fontSize: 'var(--text-small)' }}
                  >
                    {genError}
                  </div>
                )}
              </div>
            )}

            {/* Inline builder */}
            {showBuilder && (
              <div style={{ marginBottom: '12px' }}>
                <FeedbackTemplateBuilder
                  initialDraft={inlineDraft}
                  onSave={handleBuilderSave}
                  onCancel={() => {
                    setShowBuilder(false);
                    setSelectedTemplateId('');
                    setInlineDraft(null);
                  }}
                />
              </div>
            )}

            {selectedTemplateId === NEW_TEMPLATE && inlineDraft && !showBuilder && (
              <div
                data-testid="create-feedback-link-inline-ready"
                style={{
                  marginBottom: '12px',
                  padding: '8px 12px',
                  border: '1px solid var(--color-primary)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-primary-muted)',
                  color: 'var(--color-text-primary)',
                  fontSize: 'var(--text-small)',
                }}
              >
                Using inline template “{inlineDraft.title}” ({inlineDraft.questions.length} questions).{' '}
                <button
                  type="button"
                  onClick={() => setShowBuilder(true)}
                  data-testid="create-feedback-link-edit-inline"
                  style={{ background: 'none', border: 'none', color: 'var(--color-primary)', cursor: 'pointer', textDecoration: 'underline', padding: 0 }}
                >
                  Edit
                </button>
              </div>
            )}

            {/* Expiry */}
            <div style={{ marginBottom: '12px' }}>
              <label htmlFor="cfl-expiry" style={labelStyle}>Expires in (days, max {MAX_DAYS})</label>
              <input
                id="cfl-expiry"
                type="number"
                min={1}
                max={MAX_DAYS}
                value={expiresInDays}
                onChange={(e) => {
                  const v = Number(e.target.value);
                  setExpiresInDays(Number.isNaN(v) ? DEFAULT_DAYS : Math.min(Math.max(v, 1), MAX_DAYS));
                }}
                data-testid="create-feedback-link-expiry"
                style={inputStyle}
              />
            </div>

            {/* Label */}
            <div style={{ marginBottom: '12px' }}>
              <label htmlFor="cfl-label" style={labelStyle}>Internal label (optional)</label>
              <input
                id="cfl-label"
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="e.g. Q1 360 review"
                data-testid="create-feedback-link-label"
                style={inputStyle}
              />
            </div>

            {/* Request submitter info */}
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', cursor: 'pointer', marginBottom: '16px' }}>
              <input
                type="checkbox"
                checked={requestSubmitterInfo}
                onChange={(e) => setRequestSubmitterInfo(e.target.checked)}
                data-testid="create-feedback-link-request-info"
                style={{ accentColor: 'var(--color-primary)', cursor: 'pointer' }}
              />
              Request name/email (optional for submitters)
            </label>

            {error && (
              <div
                data-testid="create-feedback-link-error"
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
                {error}
              </div>
            )}

            {/* Actions */}
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={handleCreate}
                disabled={creating || !canCreate()}
                data-testid="create-feedback-link-submit"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: 'none',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'var(--color-primary)',
                  color: 'var(--color-bg-base)',
                  cursor: creating || !canCreate() ? 'not-allowed' : 'pointer',
                  fontWeight: 'var(--weight-medium)',
                  opacity: creating || !canCreate() ? 0.6 : 1,
                }}
              >
                {creating ? 'Creating...' : 'Create link'}
              </button>
              <button
                type="button"
                onClick={onClose}
                data-testid="create-feedback-link-cancel"
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
          </>
        )}
      </div>
    </Modal>
  );
}
