'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  FeedbackTemplate,
  FeedbackTemplateDraft,
  SaveFeedbackTemplateRequest,
} from '@/types/feedback';
import {
  listFeedbackTemplates,
  getFeedbackTemplate,
  getFeedbackStarterTemplates,
  createFeedbackTemplate,
  updateFeedbackTemplate,
  deleteFeedbackTemplate,
  generateFeedbackTemplate,
  getUserSettings,
} from '@/lib/api-client';
import { ApiException } from '@/types/api';
import { useStableToken } from '@/lib/useStableToken';
import LoadingScreen from '@/components/LoadingScreen';
import FeedbackTemplateBuilder from '@/components/feedback/FeedbackTemplateBuilder';
import Spinner from '@/components/feedback/Spinner';

type BuilderState =
  | { open: false }
  | { open: true; templateId: string | null; draft: FeedbackTemplateDraft | null };

const cardStyle: React.CSSProperties = {
  padding: 'var(--space-4)',
  border: '1px solid var(--color-border)',
  borderRadius: 'var(--radius-medium)',
  backgroundColor: 'var(--color-bg-surface)',
};

export default function FeedbackTemplatesPage() {
  const { getToken, isAuthenticated, status } = useStableToken();

  const [templates, setTemplates] = useState<FeedbackTemplate[]>([]);
  const [starters, setStarters] = useState<FeedbackTemplateDraft[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [builder, setBuilder] = useState<BuilderState>({ open: false });
  const [saving, setSaving] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const [brief, setBrief] = useState('');
  const [generating, setGenerating] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  // Whether AI is effectively available (personal config or admin team defaults).
  // Mirrors how other AI features gate visibility.
  const [aiAvailable, setAiAvailable] = useState(false);

  const fetchTemplates = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;
    setLoading(true);
    setError(null);
    try {
      const [list, starterList, settings] = await Promise.all([
        listFeedbackTemplates(token),
        getFeedbackStarterTemplates(token).catch(() => [] as FeedbackTemplateDraft[]),
        getUserSettings(token).catch(() => null),
      ]);
      setTemplates(list);
      setStarters(starterList);
      setAiAvailable(settings?.aiAvailable ?? false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load templates');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated]);

  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  const openNew = () => {
    setServerError(null);
    setBuilder({ open: true, templateId: null, draft: null });
  };

  const openStarter = (starter: FeedbackTemplateDraft) => {
    setServerError(null);
    setBuilder({ open: true, templateId: null, draft: starter });
  };

  const openEdit = async (id: string) => {
    const token = getToken();
    if (!token) return;
    setServerError(null);
    try {
      const full = await getFeedbackTemplate(token, id);
      setBuilder({
        open: true,
        templateId: full.id,
        draft: { title: full.title, description: full.description, questions: full.questions },
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to open template');
    }
  };

  const handleGenerate = async () => {
    const token = getToken();
    if (!token || !brief.trim()) return;
    setGenerating(true);
    setAiError(null);
    try {
      const result = await generateFeedbackTemplate(token, brief.trim());
      if (result.draft) {
        setServerError(null);
        setBuilder({ open: true, templateId: null, draft: result.draft });
        setBrief('');
      } else {
        setAiError(result.error || 'Failed to generate template');
      }
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'Failed to generate template');
    } finally {
      setGenerating(false);
    }
  };

  const handleSave = async (payload: SaveFeedbackTemplateRequest) => {
    const token = getToken();
    if (!token || !builder.open) return;
    setSaving(true);
    setServerError(null);
    try {
      if (builder.templateId) {
        await updateFeedbackTemplate(token, builder.templateId, payload);
      } else {
        await createFeedbackTemplate(token, payload);
      }
      setBuilder({ open: false });
      await fetchTemplates();
    } catch (err) {
      if (err instanceof ApiException && err.status === 400) {
        setServerError(err.message);
      } else {
        setServerError(err instanceof Error ? err.message : 'Failed to save template');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    const token = getToken();
    if (!token) return;
    try {
      await deleteFeedbackTemplate(token, id);
      setTemplates((prev) => prev.filter((t) => t.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete template');
    }
  };

  if (status === 'loading' || loading) {
    return <LoadingScreen message="Loading feedback templates" />;
  }

  return (
    <div
      data-testid="feedback-templates-page"
      style={{
        padding: 'var(--space-6)',
        maxWidth: '900px',
        margin: '0 auto',
        fontFamily: 'var(--font-ui)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-5)' }}>
        <h1
          data-testid="feedback-templates-title"
          style={{
            fontSize: 'var(--text-h1)',
            fontFamily: 'var(--font-heading)',
            fontWeight: 'var(--weight-bold)',
            color: 'var(--color-text-primary)',
            margin: 0,
          }}
        >
          Feedback Templates
        </h1>
        {!builder.open && (
          <button
            type="button"
            onClick={openNew}
            data-testid="feedback-templates-new-btn"
            style={{
              padding: '8px 16px',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              backgroundColor: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              cursor: 'pointer',
              fontWeight: 'var(--weight-medium)',
            }}
          >
            New template
          </button>
        )}
      </div>

      {error && (
        <div
          data-testid="feedback-templates-error"
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
          {error}
        </div>
      )}

      {builder.open ? (
        <FeedbackTemplateBuilder
          templateId={builder.templateId}
          initialDraft={builder.draft}
          onSave={handleSave}
          onCancel={() => setBuilder({ open: false })}
          isSaving={saving}
          serverError={serverError}
        />
      ) : (
        <>
          {/* AI generate — only shown when AI is effectively available
              (personal config or admin team defaults), matching other AI features. */}
          {aiAvailable && (
          <section
            data-testid="feedback-templates-ai"
            style={{ ...cardStyle, marginBottom: 'var(--space-5)' }}
          >
            <label htmlFor="ft-ai-brief" style={{
              display: 'block',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              color: 'var(--color-primary)',
              marginBottom: '4px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}>
              ✦ Generate with AI
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                id="ft-ai-brief"
                type="text"
                value={brief}
                onChange={(e) => setBrief(e.target.value)}
                placeholder="Describe the feedback you want to collect..."
                data-testid="feedback-templates-ai-brief"
                style={{
                  flex: 1,
                  padding: '8px 12px',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-medium)',
                  fontSize: 'var(--text-body)',
                  backgroundColor: 'var(--color-bg-elevated)',
                  color: 'var(--color-text-primary)',
                }}
              />
              <button
                type="button"
                onClick={handleGenerate}
                disabled={generating || !brief.trim()}
                data-testid="feedback-templates-ai-generate-btn"
                style={{
                  padding: '8px 16px',
                  fontSize: 'var(--text-body)',
                  fontFamily: 'var(--font-mono)',
                  border: '1px solid var(--color-primary)',
                  borderRadius: 'var(--radius-medium)',
                  backgroundColor: 'transparent',
                  color: 'var(--color-primary)',
                  cursor: generating || !brief.trim() ? 'not-allowed' : 'pointer',
                  opacity: generating || !brief.trim() ? 0.6 : 1,
                }}
              >
                {generating ? (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    <Spinner size={14} label="Generating template" /> Generating...
                  </span>
                ) : '✦ Generate with AI'}
              </button>
            </div>
            {aiError && (
              <div
                data-testid="feedback-templates-ai-error"
                style={{
                  marginTop: '8px',
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
          </section>
          )}

          {/* Starters */}
          {starters.length > 0 && (
            <section data-testid="feedback-templates-starters" style={{ marginBottom: 'var(--space-5)' }}>
              <h2 style={{
                fontSize: 'var(--text-caption)',
                fontFamily: 'var(--font-mono)',
                color: 'var(--color-text-muted)',
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                margin: '0 0 var(--space-3) 0',
              }}>
                Start from a starter
              </h2>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {starters.map((starter, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => openStarter(starter)}
                    data-testid={`feedback-templates-starter-${i}`}
                    style={{
                      padding: '8px 14px',
                      fontSize: 'var(--text-small)',
                      fontFamily: 'var(--font-mono)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-full)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-secondary)',
                      cursor: 'pointer',
                    }}
                  >
                    {starter.title}
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* Templates list */}
          {templates.length === 0 ? (
            <div
              data-testid="feedback-templates-empty"
              style={{ ...cardStyle, color: 'var(--color-text-muted)', textAlign: 'center' }}
            >
              No templates yet. Create one{starters.length > 0 ? ', start from a starter' : ''}{aiAvailable ? ', or generate with AI' : ''}.
            </div>
          ) : (
            <div data-testid="feedback-templates-list" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {templates.map((t) => (
                <div
                  key={t.id}
                  data-testid={`feedback-template-row-${t.id}`}
                  style={{ ...cardStyle, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px' }}
                >
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: 'var(--text-body)', fontWeight: 'var(--weight-semibold)', color: 'var(--color-text-primary)' }}>
                      {t.title}
                    </div>
                    {t.description && (
                      <div style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                        {t.description}
                      </div>
                    )}
                    <div style={{ fontSize: 'var(--text-caption)', fontFamily: 'var(--font-mono)', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                      {t.questions.length} {t.questions.length === 1 ? 'question' : 'questions'}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
                    <button
                      type="button"
                      onClick={() => openEdit(t.id)}
                      data-testid={`feedback-template-edit-${t.id}`}
                      style={{
                        padding: '6px 12px',
                        fontSize: 'var(--text-small)',
                        fontFamily: 'var(--font-mono)',
                        border: '1px solid var(--color-border)',
                        borderRadius: 'var(--radius-medium)',
                        backgroundColor: 'transparent',
                        color: 'var(--color-primary)',
                        cursor: 'pointer',
                      }}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(t.id)}
                      data-testid={`feedback-template-delete-${t.id}`}
                      style={{
                        padding: '6px 12px',
                        fontSize: 'var(--text-small)',
                        fontFamily: 'var(--font-mono)',
                        border: '1px solid var(--color-alert)',
                        borderRadius: 'var(--radius-medium)',
                        backgroundColor: 'transparent',
                        color: 'var(--color-alert)',
                        cursor: 'pointer',
                      }}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
