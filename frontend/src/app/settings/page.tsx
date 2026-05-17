'use client';

import { useEffect, useState, useCallback } from 'react';
import { UserSettings, UpdateUserSettingsRequest, Theme } from '@/types/settings';
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
                  label="Privacy Mode (exclude sensitive content from AI)"
                  checked={aiPrivacyMode}
                  onChange={setAiPrivacyMode}
                />
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
