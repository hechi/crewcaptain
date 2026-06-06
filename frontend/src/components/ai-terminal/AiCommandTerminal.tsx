'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { parseAiCommand, getPersonDirectory, createQuickNote, createActionItem, createKudos, createOneOnOneEntry } from '@/lib/api-client';
import { getUserSettings } from '@/lib/api-client';
import { useStableToken } from '@/lib/useStableToken';
import { AiCommandResponse, PersonDirectoryEntry, UserSettings } from '@/types/settings';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  parsedCommand?: AiCommandResponse;
  executed?: boolean;
  undoTimeout?: ReturnType<typeof setTimeout>;
}

/**
 * AI Command Terminal — chat overlay for natural language commands.
 * Accessible via Cmd/Ctrl+K or floating action button.
 * Only visible when AI is enabled and configured in User Settings.
 */
export default function AiCommandTerminal() {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [settingsLoaded, setSettingsLoaded] = useState(false);
  const [personDirectory, setPersonDirectory] = useState<PersonDirectoryEntry[]>([]);
  const [undoToast, setUndoToast] = useState<{ id: string; message: string } | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { getToken, isAuthenticated } = useStableToken();

  // Fetch user settings to check if AI is enabled
  useEffect(() => {
    const fetchSettings = async () => {
      const token = getToken();
      if (!token || !isAuthenticated) return;
      try {
        const s = await getUserSettings(token);
        setSettings(s);
      } catch {
        // silently fail — terminal will be hidden
      } finally {
        setSettingsLoaded(true);
      }
    };
    fetchSettings();
  }, [getToken, isAuthenticated]);

  // Fetch person directory when terminal opens
  useEffect(() => {
    if (!isOpen) return;
    const fetchDirectory = async () => {
      const token = getToken();
      if (!token) return;
      try {
        const dir = await getPersonDirectory(token);
        setPersonDirectory(dir);
      } catch {
        // non-critical
      }
    };
    fetchDirectory();
  }, [isOpen, getToken]);

  const open = useCallback(() => {
    if (!isAuthenticated || !settings?.aiEnabled) return;
    setIsOpen(true);
    setInput('');
  }, [isAuthenticated, settings]);

  const close = useCallback(() => {
    setIsOpen(false);
  }, []);

  // Keyboard shortcut: Cmd/Ctrl+K to toggle
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        if (isOpen) {
          close();
        } else {
          open();
        }
      }
      if (e.key === 'Escape' && isOpen) {
        e.preventDefault();
        close();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, open, close]);

  // Auto-focus input when overlay opens
  useEffect(() => {
    if (isOpen && inputRef.current) {
      const timer = setTimeout(() => inputRef.current?.focus(), 50);
      return () => clearTimeout(timer);
    }
  }, [isOpen]);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const token = getToken();
    if (!token) return;

    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: input.trim(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const result = await parseAiCommand(token, userMessage.content);

      if (result.error) {
        const errorMsg: ChatMessage = {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: result.error,
        };
        setMessages(prev => [...prev, errorMsg]);
      } else {
        // Privacy mode check
        if (settings?.aiPrivacyMode && result.sensitive) {
          const warnMsg: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'system',
            content: '⚠️ Privacy Mode is ON. This command involves sensitive content. Please confirm you want to proceed.',
            parsedCommand: result,
          };
          setMessages(prev => [...prev, warnMsg]);
        } else if (settings?.aiAutoExecuteCommands) {
          // Auto-execute mode
          await executeCommand(token, result);
        } else {
          // Standard mode — show preview card
          const previewMsg: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: formatPreview(result),
            parsedCommand: result,
          };
          setMessages(prev => [...prev, previewMsg]);
        }
      }
    } catch (err) {
      const errorMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: err instanceof Error ? err.message : 'An error occurred. Please try again.',
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const executeCommand = async (token: string, command: AiCommandResponse) => {
    try {
      const personName = command.targetPersonId
        ? personDirectory.find(p => p.id === command.targetPersonId)?.preferredName || 'Unknown'
        : null;

      switch (command.intent) {
        case 'create_action_item':
          if (!command.targetPersonId) {
            setMessages(prev => [...prev, {
              id: crypto.randomUUID(),
              role: 'assistant',
              content: '⚠️ Action items require a target person. Please specify who this is for.',
            }]);
            return;
          }
          await createActionItem(token, command.targetPersonId, {
            title: command.content!,
            dueDate: command.dueDate || undefined,
          });
          break;
        case 'create_kudo':
          if (!command.targetPersonId) {
            setMessages(prev => [...prev, {
              id: crypto.randomUUID(),
              role: 'assistant',
              content: '⚠️ Kudos require a target person. Please specify who this is for.',
            }]);
            return;
          }
          await createKudos(token, command.targetPersonId, {
            text: command.content!,
            date: new Date().toISOString().split('T')[0],
            tags: command.tags,
          });
          break;
        case 'create_quick_note':
          await createQuickNote(token, {
            text: command.content!,
            personId: command.targetPersonId || undefined,
            sensitive: command.sensitive,
          });
          break;
        case 'create_one_on_one_entry':
          if (!command.targetPersonId) {
            setMessages(prev => [...prev, {
              id: crypto.randomUUID(),
              role: 'assistant',
              content: '⚠️ 1:1 entries require a target person. Please specify who the meeting was with.',
            }]);
            return;
          }
          const meetingDate = command.meetingDate || new Date().toISOString().split('T')[0];
          await createOneOnOneEntry(token, command.targetPersonId, {
            meetingDate: `${meetingDate}T00:00:00Z`,
            notesMarkdown: command.content!,
            sensitive: command.sensitive,
          });
          break;
      }

      const successMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: `✓ ${formatIntentLabel(command.intent!)} created${personName ? ` for ${personName}` : ''}.`,
        executed: true,
      };
      setMessages(prev => [...prev, successMsg]);

      // Show undo toast for auto-execute
      if (settings?.aiAutoExecuteCommands) {
        const toastId = crypto.randomUUID();
        setUndoToast({ id: toastId, message: `${formatIntentLabel(command.intent!)} created` });
        setTimeout(() => {
          setUndoToast(prev => prev?.id === toastId ? null : prev);
        }, 10000);
      }
    } catch (err) {
      setMessages(prev => [...prev, {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: `Failed to execute: ${err instanceof Error ? err.message : 'Unknown error'}`,
      }]);
    }
  };

  const handleConfirm = async (messageId: string) => {
    const token = getToken();
    if (!token) return;

    const msg = messages.find(m => m.id === messageId);
    if (!msg?.parsedCommand) return;

    setLoading(true);
    await executeCommand(token, msg.parsedCommand);
    setMessages(prev => prev.map(m =>
      m.id === messageId ? { ...m, executed: true } : m
    ));
    setLoading(false);
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      close();
    }
  };

  // Don't render if not authenticated or AI not enabled
  if (!isAuthenticated || !settingsLoaded || !settings?.aiEnabled) return null;

  return (
    <>
      {/* Floating Action Button */}
      <button
        type="button"
        onClick={open}
        data-testid="ai-terminal-fab"
        aria-label="AI Command Terminal (Ctrl+K)"
        title="AI Command Terminal (Ctrl+K)"
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '84px',
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          border: '1px solid var(--color-border-glow)',
          backgroundColor: 'var(--color-bg-elevated)',
          color: 'var(--color-secondary, #A855F7)',
          fontSize: '20px',
          fontFamily: 'var(--font-mono)',
          fontWeight: '700',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 12px rgba(168, 85, 247, 0.2)',
          transition: 'box-shadow 0.2s, border-color 0.2s, transform 0.2s',
          zIndex: 900,
        }}
      >
        ⌘
      </button>

      {/* Overlay */}
      {isOpen && (
        <div
          data-testid="ai-terminal-overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby="ai-terminal-title"
          onClick={handleBackdropClick}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'flex-end',
            justifyContent: 'center',
            zIndex: 1100,
            animation: 'ai-terminal-fade-in 0.25s ease-out forwards',
          }}
        >
          {/* Backdrop */}
          <div
            data-testid="ai-terminal-backdrop"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: 'rgba(0, 0, 0, 0.7)',
              backdropFilter: 'blur(4px)',
            }}
          />

          {/* Terminal panel */}
          <div
            data-testid="ai-terminal-panel"
            style={{
              position: 'relative',
              width: '100%',
              maxWidth: '640px',
              maxHeight: '70vh',
              margin: '0 16px 24px',
              display: 'flex',
              flexDirection: 'column',
              backgroundColor: 'var(--glass-elevated-bg, rgba(30, 35, 48, 0.95))',
              border: '1px solid rgba(168, 85, 247, 0.2)',
              borderRadius: 'var(--radius-large, 12px)',
              backdropFilter: 'var(--glass-elevated-blur, blur(16px))',
              boxShadow: '0 0 30px rgba(168, 85, 247, 0.1), 0 20px 60px rgba(0, 0, 0, 0.4)',
              animation: 'ai-terminal-slide-up 0.3s ease-out forwards',
              overflow: 'hidden',
            }}
          >
            {/* Scan-line decoration */}
            <div
              aria-hidden="true"
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                borderRadius: 'var(--radius-large, 12px)',
                pointerEvents: 'none',
                background: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(168, 85, 247, 0.01) 2px, rgba(168, 85, 247, 0.01) 4px)',
                opacity: 0.5,
              }}
            />

            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px 12px', position: 'relative', borderBottom: '1px solid var(--color-border, #2A3040)' }}>
              <h2
                id="ai-terminal-title"
                style={{
                  margin: 0,
                  fontSize: '16px',
                  fontWeight: '600',
                  fontFamily: 'var(--font-heading, monospace)',
                  color: 'var(--color-secondary, #A855F7)',
                  letterSpacing: '-0.2px',
                }}
              >
                ✦ AI Command Terminal
              </h2>
              <span
                style={{
                  fontSize: '11px',
                  fontFamily: 'var(--font-mono, monospace)',
                  color: 'var(--color-text-muted, #4A5568)',
                  padding: '2px 8px',
                  border: '1px solid var(--color-border, #2A3040)',
                  borderRadius: 'var(--radius-small, 4px)',
                }}
              >
                Ctrl+K
              </span>
            </div>

            {/* Messages area */}
            <div
              data-testid="ai-terminal-messages"
              style={{
                flex: 1,
                overflowY: 'auto',
                padding: '12px 20px',
                minHeight: '200px',
                maxHeight: '50vh',
              }}
            >
              {messages.length === 0 && (
                <div
                  data-testid="ai-terminal-empty"
                  style={{
                    textAlign: 'center',
                    padding: '32px 16px',
                    color: 'var(--color-text-muted, #4A5568)',
                    fontSize: '13px',
                    fontFamily: 'var(--font-mono, monospace)',
                  }}
                >
                  <p style={{ margin: '0 0 8px' }}>Type a command in natural language.</p>
                  <p style={{ margin: 0, fontSize: '12px' }}>e.g. &quot;Create an action item for Alice to update the docs by Friday&quot;</p>
                </div>
              )}

              {messages.map((msg) => (
                <div
                  key={msg.id}
                  style={{
                    marginBottom: '12px',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start',
                  }}
                >
                  {/* Message bubble */}
                  <div
                    style={{
                      maxWidth: '90%',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-medium, 8px)',
                      fontSize: '13px',
                      fontFamily: msg.role === 'user' ? 'var(--font-ui, sans-serif)' : 'var(--font-mono, monospace)',
                      backgroundColor: msg.role === 'user'
                        ? 'var(--color-primary-muted, rgba(0, 240, 255, 0.15))'
                        : msg.role === 'system'
                          ? 'var(--color-warning-muted, rgba(255, 214, 0, 0.15))'
                          : 'var(--color-bg-elevated, #1E2330)',
                      color: msg.role === 'system'
                        ? 'var(--color-warning, #FFD600)'
                        : 'var(--color-text-primary, #E8ECF0)',
                      border: msg.role === 'user'
                        ? '1px solid rgba(0, 240, 255, 0.2)'
                        : msg.role === 'system'
                          ? '1px solid rgba(255, 214, 0, 0.2)'
                          : '1px solid var(--color-border, #2A3040)',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {msg.content}
                  </div>

                  {/* Confirm button for preview cards */}
                  {msg.parsedCommand && !msg.executed && (
                    <button
                      type="button"
                      data-testid="ai-terminal-confirm"
                      onClick={() => handleConfirm(msg.id)}
                      disabled={loading}
                      style={{
                        marginTop: '8px',
                        padding: '6px 14px',
                        border: 'none',
                        borderRadius: 'var(--radius-medium, 8px)',
                        backgroundColor: 'var(--color-primary, #00F0FF)',
                        color: 'var(--color-bg-base, #0D0F14)',
                        fontSize: '12px',
                        fontWeight: '600',
                        fontFamily: 'var(--font-mono, monospace)',
                        cursor: loading ? 'not-allowed' : 'pointer',
                        boxShadow: 'var(--glow-primary, 0 0 12px rgba(0, 240, 255, 0.2))',
                        transition: 'all 0.2s',
                      }}
                    >
                      {loading ? 'Executing...' : 'Confirm & Save'}
                    </button>
                  )}

                  {/* Executed indicator */}
                  {msg.executed && (
                    <span
                      data-testid="ai-terminal-executed"
                      style={{
                        marginTop: '4px',
                        fontSize: '11px',
                        fontFamily: 'var(--font-mono, monospace)',
                        color: 'var(--color-success, #39FF85)',
                      }}
                    >
                      ✓ Saved
                    </span>
                  )}
                </div>
              ))}

              <div ref={messagesEndRef} />
            </div>

            {/* Input area */}
            <form onSubmit={handleSubmit} style={{ padding: '12px 20px 16px', borderTop: '1px solid var(--color-border, #2A3040)', position: 'relative' }}>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  ref={inputRef}
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Type a command..."
                  data-testid="ai-terminal-input"
                  aria-label="AI command input"
                  disabled={loading}
                  style={{
                    flex: 1,
                    padding: '10px 14px',
                    border: '1px solid var(--color-border-glow, rgba(0, 240, 255, 0.2))',
                    borderRadius: 'var(--radius-medium, 8px)',
                    fontSize: '14px',
                    fontFamily: 'var(--font-mono, monospace)',
                    backgroundColor: 'var(--color-bg-elevated, #1E2330)',
                    color: 'var(--color-text-primary, #E8ECF0)',
                    outline: 'none',
                    transition: 'border-color 0.2s, box-shadow 0.2s',
                  }}
                />
                <button
                  type="submit"
                  disabled={!input.trim() || loading}
                  data-testid="ai-terminal-submit"
                  style={{
                    padding: '10px 16px',
                    border: 'none',
                    borderRadius: 'var(--radius-medium, 8px)',
                    backgroundColor: input.trim() && !loading
                      ? 'var(--color-secondary, #A855F7)'
                      : 'var(--color-bg-elevated, #1E2330)',
                    color: input.trim() && !loading
                      ? '#FFFFFF'
                      : 'var(--color-text-muted, #4A5568)',
                    fontSize: '13px',
                    fontWeight: '600',
                    fontFamily: 'var(--font-mono, monospace)',
                    cursor: input.trim() && !loading ? 'pointer' : 'not-allowed',
                    boxShadow: input.trim() && !loading
                      ? '0 0 12px rgba(168, 85, 247, 0.2)'
                      : 'none',
                    transition: 'all 0.2s',
                  }}
                >
                  {loading ? '...' : '▶'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Undo toast */}
      {undoToast && (
        <div
          data-testid="ai-terminal-undo-toast"
          style={{
            position: 'fixed',
            bottom: '84px',
            left: '50%',
            transform: 'translateX(-50%)',
            padding: '10px 20px',
            backgroundColor: 'var(--color-bg-elevated, #1E2330)',
            border: '1px solid var(--color-border-glow, rgba(0, 240, 255, 0.2))',
            borderRadius: 'var(--radius-medium, 8px)',
            color: 'var(--color-text-primary, #E8ECF0)',
            fontSize: '13px',
            fontFamily: 'var(--font-mono, monospace)',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)',
            zIndex: 1200,
            animation: 'ai-terminal-fade-in 0.2s ease-out',
          }}
        >
          ✓ {undoToast.message}
        </div>
      )}

      {/* Keyframe animations */}
      <style>{`
        @keyframes ai-terminal-fade-in {
          from { opacity: 0; }
          to { opacity: 1; }
        }

        @keyframes ai-terminal-slide-up {
          from {
            opacity: 0;
            transform: translateY(40px) scale(0.97);
          }
          to {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
        }

        @media (prefers-reduced-motion: reduce) {
          @keyframes ai-terminal-fade-in {
            from { opacity: 1; }
            to { opacity: 1; }
          }
          @keyframes ai-terminal-slide-up {
            from { opacity: 1; transform: none; }
            to { opacity: 1; transform: none; }
          }
        }
      `}</style>
    </>
  );
}

// Helper functions

function formatPreview(command: AiCommandResponse): string {
  const lines: string[] = [];
  lines.push(`Action: ${formatIntentLabel(command.intent!)}`);
  lines.push(`Content: ${command.content}`);
  if (command.targetPersonId) {
    lines.push(`Person: ${command.targetPersonId}`);
  }
  if (command.dueDate) {
    lines.push(`Due: ${command.dueDate}`);
  }
  if (command.meetingDate) {
    lines.push(`Meeting Date: ${command.meetingDate}`);
  }
  if (command.tags.length > 0) {
    lines.push(`Tags: ${command.tags.join(', ')}`);
  }
  if (command.sensitive) {
    lines.push(`⚠️ Sensitive: Yes`);
  }
  return lines.join('\n');
}

function formatIntentLabel(intent: string): string {
  switch (intent) {
    case 'create_action_item': return 'Action Item';
    case 'create_kudo': return 'Kudos';
    case 'create_quick_note': return 'Quick Note';
    case 'create_one_on_one_entry': return '1:1 Entry';
    default: return intent;
  }
}
