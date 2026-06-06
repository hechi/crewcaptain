'use client';

import { useEffect, useState, useCallback } from 'react';
import { UserSettings, UpdateUserSettingsRequest, Theme, AiWritingStyle } from '@/types/settings';
import { getUserSettings, updateUserSettings } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { useTheme } from '@/components/ThemeProvider';
import LoadingScreen from '@/components/LoadingScreen';

export default function SettingsPage() {
  const { getToken, isAuthenticated, status } = useStableToken();
  const { setTheme } = useTheme();
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // Form state
  const [dueSoonDays, setDueSoonDays] = useState(3);
  const [staleOneOnOneDays, setStaleOneOnOneDays] = useState(14);
  const [anniversaryLookaheadDays, setAnniversaryLookaheadDays] = useState(30);
  const [theme, setThemeState] = useState<Theme>('DARK');
  const [showAchievements, setShowAchievements] = useState(true);
  const [notifyActionItemOverdue, setNotifyActionItemOverdue] = useState(true);
  const [notifyActionItemDueSoon, setNotifyActionItemDueSoon] = useState(true);
  const [notifyStaleOneOnOne, setNotifyStaleOneOnOne] = useState(true);
  const [notifyUpcomingAnniversary, setNotifyUpcomingAnniversary] = useState(true);
  const [aiEnabled, setAiEnabled] = useState(false);
  const [aiApiBaseUrl, setAiApiBaseUrl] = useState('');
  const [aiApiKey, setAiApiKey] = useState('');
  const [aiModelName, setAiModelName] = useState('');
  const [aiPrivacyMode, setAiPrivacyMode] = useState(true);
  const [aiWritingStyle, setAiWritingStyle] = useState<AiWritingStyle>('NARRATIVE');
  const [aiAutoExecuteCommands, setAiAutoExecuteCommands] = useState(false);
  const [kudosRefinementPrompt, setKudosRefinementPrompt] = useState('');
  const [pdpOptimizationPrompt, setPdpOptimizationPrompt] = useState('');
  const [agendaPrepPrompt, setAgendaPrepPrompt] = useState('');
  const [narrativePrompt, setNarrativePrompt] = useState('');
  const [outcomeExtractorPrompt, setOutcomeExtractorPrompt] = useState('');
  const [trendRadarPrompt, setTrendRadarPrompt] = useState('');
  const [linkSuggestionsPrompt, setLinkSuggestionsPrompt] = useState('');
  const [triageHintPrompt, setTriageHintPrompt] = useState('');
  const [commandTerminalPrompt, setCommandTerminalPrompt] = useState('');

  const fetchSettings = useCallback(async () => {
    const token = getToken();
    if (!isAuthenticated || !token) return;

    setLoading(true);
    setError(null);
    try {
      const result = await getUserSettings(token);
      setSettings(result);
      setDueSoonDays(result.dueSoonDays);
      setStaleOneOnOneDays(result.staleOneOnOneDays);
      setAnniversaryLookaheadDays(result.anniversaryLookaheadDays);
      setThemeState(result.theme);
      setShowAchievements(result.showAchievements);
      setNotifyActionItemOverdue(result.notifyActionItemOverdue);
      setNotifyActionItemDueSoon(result.notifyActionItemDueSoon);
      setNotifyStaleOneOnOne(result.notifyStaleOneOnOne);
      setNotifyUpcomingAnniversary(result.notifyUpcomingAnniversary);
      setAiEnabled(result.aiEnabled);
      setAiApiBaseUrl(result.aiApiBaseUrl || '');
      setAiApiKey('');
      setAiModelName(result.aiModelName || '');
      setAiPrivacyMode(result.aiPrivacyMode);
      setAiWritingStyle(result.aiWritingStyle || 'NARRATIVE');
      setAiAutoExecuteCommands(result.aiAutoExecuteCommands);
      setKudosRefinementPrompt(result.kudosRefinementPrompt || '');
      setPdpOptimizationPrompt(result.pdpOptimizationPrompt || '');
      setAgendaPrepPrompt(result.agendaPrepPrompt || '');
      setNarrativePrompt(result.narrativePrompt || '');
      setOutcomeExtractorPrompt(result.outcomeExtractorPrompt || '');
      setTrendRadarPrompt(result.trendRadarPrompt || '');
      setLinkSuggestionsPrompt(result.linkSuggestionsPrompt || '');
      setTriageHintPrompt(result.triageHintPrompt || '');
      setCommandTerminalPrompt(result.commandTerminalPrompt || '');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  }, [getToken, isAuthenticated]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleSave = async () => {
    const token = getToken();
    if (!token) return;

    setSaving(true);
    setError(null);
    setSuccess(false);
    try {
      const request: UpdateUserSettingsRequest = {
        dueSoonDays,
        staleOneOnOneDays,
        anniversaryLookaheadDays,
        theme,
        showAchievements,
        notifyActionItemOverdue,
        notifyActionItemDueSoon,
        notifyStaleOneOnOne,
        notifyUpcomingAnniversary,
        aiEnabled,
        aiApiBaseUrl: aiApiBaseUrl || null,
        aiApiKey: aiApiKey || null,
        aiModelName: aiModelName || null,
        aiPrivacyMode,
        aiWritingStyle,
        aiAutoExecuteCommands,
        kudosRefinementPrompt: kudosRefinementPrompt || null,
        pdpOptimizationPrompt: pdpOptimizationPrompt || null,
        agendaPrepPrompt: agendaPrepPrompt || null,
        narrativePrompt: narrativePrompt || null,
        outcomeExtractorPrompt: outcomeExtractorPrompt || null,
        trendRadarPrompt: trendRadarPrompt || null,
        linkSuggestionsPrompt: linkSuggestionsPrompt || null,
        triageHintPrompt: triageHintPrompt || null,
        commandTerminalPrompt: commandTerminalPrompt || null,
      };
      const result = await updateUserSettings(token, request);
      setSettings(result);
      setTheme(result.theme);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  if (status === 'loading' || loading) {
    return <LoadingScreen message="Loading settings" />;
  }

  if (error && !settings) {
    return (
      <div
        data-testid="settings-error"
        style={{
          padding: 'var(--space-6)',
          maxWidth: '800px',
          margin: '0 auto',
        }}
      >
        <div
          style={{
            padding: 'var(--space-4)',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-alert)',
            backgroundColor: 'var(--color-bg-surface)',
            color: 'var(--color-alert)',
          }}
        >
          {error}
        </div>
      </div>
    );
  }

  return (
    <div
      data-testid="settings-page"
      style={{
        padding: 'var(--space-6)',
        maxWidth: '800px',
        margin: '0 auto',
        fontFamily: 'var(--font-ui)',
      }}
    >
      <h1
        data-testid="settings-title"
        style={{
          fontSize: 'var(--text-h1)',
          fontFamily: 'var(--font-heading)',
          fontWeight: 'var(--weight-bold)',
          color: 'var(--color-text-primary)',
          margin: '0 0 var(--space-6) 0',
        }}
      >
        Settings
      </h1>

      {/* Theme Section */}
      <section
        data-testid="settings-section-theme"
        style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-5)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
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
          Appearance
        </h2>
        <div style={{ display: 'flex', gap: 'var(--space-3)' }}>
          <button
            type="button"
            data-testid="theme-dark-btn"
            onClick={() => setThemeState('DARK')}
            style={{
              padding: '10px 20px',
              borderRadius: 'var(--radius-medium)',
              border: theme === 'DARK' ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
              backgroundColor: theme === 'DARK' ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
              color: theme === 'DARK' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
              fontWeight: 'var(--weight-medium)',
              fontSize: 'var(--text-body)',
              transition: 'all 0.2s',
            }}
          >
            Dark
          </button>
          <button
            type="button"
            data-testid="theme-light-btn"
            onClick={() => setThemeState('LIGHT')}
            style={{
              padding: '10px 20px',
              borderRadius: 'var(--radius-medium)',
              border: theme === 'LIGHT' ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
              backgroundColor: theme === 'LIGHT' ? 'var(--color-primary-muted)' : 'var(--color-bg-elevated)',
              color: theme === 'LIGHT' ? 'var(--color-primary)' : 'var(--color-text-secondary)',
              cursor: 'pointer',
              fontWeight: 'var(--weight-medium)',
              fontSize: 'var(--text-body)',
              transition: 'all 0.2s',
            }}
          >
            Light
          </button>
        </div>
      </section>

      {/* Dashboard Thresholds Section */}
      <section
        data-testid="settings-section-thresholds"
        style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-5)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
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
          Dashboard Thresholds
        </h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
              Due soon window (days)
            </span>
            <input
              type="number"
              data-testid="input-due-soon-days"
              min={1}
              max={30}
              value={dueSoonDays}
              onChange={(e) => setDueSoonDays(Number(e.target.value))}
              style={{
                padding: '8px 12px',
                borderRadius: 'var(--radius-small)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                width: '100px',
              }}
            />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
              Stale 1:1 threshold (days)
            </span>
            <input
              type="number"
              data-testid="input-stale-one-on-one-days"
              min={1}
              max={90}
              value={staleOneOnOneDays}
              onChange={(e) => setStaleOneOnOneDays(Number(e.target.value))}
              style={{
                padding: '8px 12px',
                borderRadius: 'var(--radius-small)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                width: '100px',
              }}
            />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
              Anniversary lookahead (days)
            </span>
            <input
              type="number"
              data-testid="input-anniversary-lookahead-days"
              min={1}
              max={90}
              value={anniversaryLookaheadDays}
              onChange={(e) => setAnniversaryLookaheadDays(Number(e.target.value))}
              style={{
                padding: '8px 12px',
                borderRadius: 'var(--radius-small)',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-elevated)',
                color: 'var(--color-text-primary)',
                fontSize: 'var(--text-body)',
                width: '100px',
              }}
            />
          </label>
        </div>
      </section>

      {/* Notifications Section */}
      <section
        data-testid="settings-section-notifications"
        style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-5)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
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
          Notifications
        </h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <ToggleRow
            testId="toggle-notify-overdue"
            label="Overdue action items"
            checked={notifyActionItemOverdue}
            onChange={setNotifyActionItemOverdue}
          />
          <ToggleRow
            testId="toggle-notify-due-soon"
            label="Due soon action items"
            checked={notifyActionItemDueSoon}
            onChange={setNotifyActionItemDueSoon}
          />
          <ToggleRow
            testId="toggle-notify-stale"
            label="Stale 1:1 reminders"
            checked={notifyStaleOneOnOne}
            onChange={setNotifyStaleOneOnOne}
          />
          <ToggleRow
            testId="toggle-notify-anniversary"
            label="Upcoming anniversaries"
            checked={notifyUpcomingAnniversary}
            onChange={setNotifyUpcomingAnniversary}
          />
        </div>
      </section>

      {/* Display Section */}
      <section
        data-testid="settings-section-display"
        style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-5)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
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
          Display
        </h2>
        <ToggleRow
          testId="toggle-show-achievements"
          label="Show achievements on dashboard"
          checked={showAchievements}
          onChange={setShowAchievements}
        />
      </section>

      {/* AI Assistant Section */}
      <section
        data-testid="settings-section-ai"
        style={{
          marginBottom: 'var(--space-6)',
          padding: 'var(--space-5)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-surface)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Subtle scan-line texture */}
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
          <h2
            style={{
              fontSize: 'var(--text-h3)',
              fontFamily: 'var(--font-heading)',
              fontWeight: 'var(--weight-semibold)',
              color: 'var(--color-primary)',
              margin: '0 0 var(--space-2) 0',
            }}
          >
            ✦ AI Assistant
          </h2>
          <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', margin: '0 0 var(--space-4) 0' }}>
            Configure an OpenAI-compatible API to generate 1:1 agenda suggestions.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
            <ToggleRow
              testId="toggle-ai-enabled"
              label="Enable AI Assistant"
              checked={aiEnabled}
              onChange={setAiEnabled}
            />
            {aiEnabled && (
              <>
                <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
                    API Base URL
                  </span>
                  <input
                    type="url"
                    data-testid="input-ai-api-base-url"
                    placeholder="http://ollama:11434/v1"
                    value={aiApiBaseUrl}
                    onChange={(e) => setAiApiBaseUrl(e.target.value)}
                    style={{
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-small)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-mono)',
                      width: '100%',
                    }}
                  />
                </label>
                <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
                    API Key
                  </span>
                  <input
                    type="password"
                    data-testid="input-ai-api-key"
                    placeholder="sk-... (leave empty for local models)"
                    value={aiApiKey}
                    onChange={(e) => setAiApiKey(e.target.value)}
                    autoComplete="off"
                    style={{
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-small)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-mono)',
                      width: '100%',
                    }}
                  />
                </label>
                <label style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-secondary)' }}>
                    Model Name
                  </span>
                  <input
                    type="text"
                    data-testid="input-ai-model-name"
                    placeholder="llama3, gpt-4o, etc."
                    value={aiModelName}
                    onChange={(e) => setAiModelName(e.target.value)}
                    style={{
                      padding: '8px 12px',
                      borderRadius: 'var(--radius-small)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-elevated)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-mono)',
                      width: '100%',
                    }}
                  />
                </label>
                <ToggleRow
                  testId="toggle-ai-privacy-mode"
                  label="Privacy Mode (exclude content marked as sensitive from AI)"
                  checked={aiPrivacyMode}
                  onChange={setAiPrivacyMode}
                />
                <ToggleRow
                  testId="toggle-ai-auto-execute"
                  label="Auto-Execute AI Commands (skip confirmation in Command Terminal)"
                  checked={aiAutoExecuteCommands}
                  onChange={setAiAutoExecuteCommands}
                />

                {/* AI Writing Style */}
                <div style={{ marginTop: 'var(--space-4)' }}>
                  <label
                    htmlFor="ai-writing-style"
                    style={{
                      display: 'block',
                      marginBottom: 'var(--space-1)',
                      fontSize: 'var(--text-small)',
                      color: 'var(--color-text-secondary)',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    AI Writing Style (for performance narratives)
                  </label>
                  <select
                    id="ai-writing-style"
                    data-testid="ai-writing-style-select"
                    value={aiWritingStyle}
                    onChange={(e) => setAiWritingStyle(e.target.value as AiWritingStyle)}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-medium)',
                      background: 'var(--color-bg-surface)',
                      color: 'var(--color-text-primary)',
                      fontSize: 'var(--text-body)',
                    }}
                  >
                    <option value="NARRATIVE">Narrative (3 paragraphs)</option>
                    <option value="BULLET_POINTS">Bullet Points (structured)</option>
                    <option value="CONCISE">Concise (1 paragraph)</option>
                  </select>
                </div>

                {/* AI Prompts Section */}
                <div style={{ marginTop: 'var(--space-5)', borderTop: '1px solid var(--color-border)', paddingTop: 'var(--space-4)' }}>
                  <h3
                    style={{
                      fontSize: 'var(--text-body)',
                      fontFamily: 'var(--font-heading)',
                      fontWeight: 'var(--weight-semibold)',
                      color: 'var(--color-primary)',
                      margin: '0 0 var(--space-2) 0',
                    }}
                  >
                    ✦ AI Prompts
                  </h3>
                  <p style={{ fontSize: 'var(--text-small)', color: 'var(--color-text-secondary)', margin: '0 0 var(--space-3) 0' }}>
                    Customize the system prompts used by AI features. Leave blank to use defaults.
                  </p>

                  <PromptTextarea
                    testId="input-kudos-refinement-prompt"
                    label="Kudos Refinement Prompt"
                    placeholder="You are a leadership coach. Refine the following kudos draft using the SBI framework..."
                    value={kudosRefinementPrompt}
                    onChange={setKudosRefinementPrompt}
                    onReset={() => setKudosRefinementPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-pdp-optimization-prompt"
                    label="PDP Goal Optimization Prompt"
                    placeholder="You are a career development expert. Evaluate the following goal and ensure it meets SMART criteria..."
                    value={pdpOptimizationPrompt}
                    onChange={setPdpOptimizationPrompt}
                    onReset={() => setPdpOptimizationPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-agenda-prep-prompt"
                    label="Agenda Prep Assistant Prompt"
                    placeholder="You are a leadership coach. Based on the provided context, suggest 3-5 high-impact agenda items..."
                    value={agendaPrepPrompt}
                    onChange={setAgendaPrepPrompt}
                    onReset={() => setAgendaPrepPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-narrative-prompt"
                    label="Generate AI Narrative Prompt"
                    placeholder="You are an expert Leadership Coach and People Manager. Draft a professional performance review narrative..."
                    value={narrativePrompt}
                    onChange={setNarrativePrompt}
                    onReset={() => setNarrativePrompt('')}
                  />

                  <PromptTextarea
                    testId="input-outcome-extractor-prompt"
                    label="Outcome Extractor Prompt"
                    placeholder="You are an executive assistant for a manager. Analyze the following 1:1 meeting notes. Extract a JSON object containing action_items and decisions..."
                    value={outcomeExtractorPrompt}
                    onChange={setOutcomeExtractorPrompt}
                    onReset={() => setOutcomeExtractorPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-trend-radar-prompt"
                    label="Trend Radar Prompt"
                    placeholder="You are a strategic people analytics advisor for a manager. Analyze the provided manager-report metadata and identify 3 potential trends or patterns..."
                    value={trendRadarPrompt}
                    onChange={setTrendRadarPrompt}
                    onReset={() => setTrendRadarPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-link-suggestions-prompt"
                    label="Link Suggestions Prompt"
                    placeholder="You are a strategic alignment advisor for a manager. Analyze strategy goals and team PDP goals, then suggest meaningful connections..."
                    value={linkSuggestionsPrompt}
                    onChange={setLinkSuggestionsPrompt}
                    onReset={() => setLinkSuggestionsPrompt('')}
                  />

                  <PromptTextarea
                    testId="input-triage-hint-prompt"
                    label="Triage Hint Prompt"
                    placeholder="You are a productivity coach for a people manager. Given a triage item, suggest a single short next-best-action..."
                    value={triageHintPrompt}
                    onChange={setTriageHintPrompt}
                    onReset={() => setTriageHintPrompt('')}
                  />

                  <PromptTextarea
                    testId="ai-command-terminal-prompt"
                    label="Command Terminal Prompt"
                    placeholder="You are a system command parser for a people management application. Parse the user's natural language input..."
                    value={commandTerminalPrompt}
                    onChange={setCommandTerminalPrompt}
                    onReset={() => setCommandTerminalPrompt('')}
                  />
                </div>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Save Button & Feedback */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
        <button
          type="button"
          data-testid="settings-save-btn"
          onClick={handleSave}
          disabled={saving}
          style={{
            padding: '10px 24px',
            borderRadius: 'var(--radius-medium)',
            border: '1px solid var(--color-border-glow)',
            backgroundColor: 'var(--color-primary-muted)',
            color: 'var(--color-primary)',
            cursor: saving ? 'not-allowed' : 'pointer',
            fontWeight: 'var(--weight-semibold)',
            fontSize: 'var(--text-body)',
            opacity: saving ? 0.6 : 1,
            transition: 'all 0.2s',
          }}
        >
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
        {success && (
          <span
            data-testid="settings-success"
            style={{ color: 'var(--color-success)', fontSize: 'var(--text-body)' }}
          >
            Settings saved successfully
          </span>
        )}
        {error && settings && (
          <span
            data-testid="settings-save-error"
            style={{ color: 'var(--color-alert)', fontSize: 'var(--text-body)' }}
          >
            {error}
          </span>
        )}
      </div>
    </div>
  );
}

function ToggleRow({
  testId,
  label,
  checked,
  onChange,
}: {
  testId: string;
  label: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label
      data-testid={testId}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '8px 0',
        cursor: 'pointer',
      }}
    >
      <span style={{ fontSize: 'var(--text-body)', color: 'var(--color-text-primary)' }}>
        {label}
      </span>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        style={{
          width: '18px',
          height: '18px',
          accentColor: 'var(--color-primary)',
          cursor: 'pointer',
        }}
      />
    </label>
  );
}

function PromptTextarea({
  testId,
  label,
  placeholder,
  value,
  onChange,
  onReset,
}: {
  testId: string;
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  onReset: () => void;
}) {
  return (
    <div style={{ marginBottom: 'var(--space-4)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
        <label
          htmlFor={testId}
          style={{
            fontSize: 'var(--text-small)',
            color: 'var(--color-text-secondary)',
            fontFamily: 'var(--font-mono)',
          }}
        >
          {label}
        </label>
        {value && (
          <button
            type="button"
            onClick={onReset}
            data-testid={`${testId}-reset-btn`}
            style={{
              padding: '2px 8px',
              fontSize: 'var(--text-caption)',
              fontFamily: 'var(--font-mono)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'transparent',
              color: 'var(--color-text-muted)',
              cursor: 'pointer',
            }}
          >
            Reset to Default
          </button>
        )}
      </div>
      <textarea
        id={testId}
        data-testid={testId}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        rows={3}
        style={{
          width: '100%',
          padding: '8px 12px',
          borderRadius: 'var(--radius-small)',
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-text-primary)',
          fontSize: 'var(--text-small)',
          fontFamily: 'var(--font-mono)',
          resize: 'vertical',
          boxSizing: 'border-box',
        }}
      />
    </div>
  );
}
