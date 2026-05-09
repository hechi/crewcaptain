'use client';

import { useState } from 'react';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  label?: string;
}

/**
 * Lightweight Markdown editor with edit/preview toggle.
 * Renders basic Markdown formatting in preview mode without external dependencies.
 * Styled with cyberpunk-lite dark theme.
 */
export default function MarkdownEditor({ value, onChange, placeholder, label }: MarkdownEditorProps) {
  const [mode, setMode] = useState<'edit' | 'preview'>('edit');

  const editorId = label ? `markdown-editor-${label.toLowerCase().replace(/\s+/g, '-')}` : 'markdown-editor';

  return (
    <div data-testid="markdown-editor" style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-medium)', overflow: 'hidden' }}>
      {/* Toolbar */}
      <div
        style={{
          display: 'flex',
          gap: '0',
          borderBottom: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg-elevated)',
        }}
        role="tablist"
        aria-label={label ? `${label} editor mode` : 'Editor mode'}
      >
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'edit'}
          aria-controls={`${editorId}-edit-panel`}
          onClick={() => setMode('edit')}
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-small)',
            fontWeight: mode === 'edit' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderBottom: mode === 'edit' ? '2px solid var(--color-primary)' : '2px solid transparent',
            backgroundColor: 'transparent',
            color: mode === 'edit' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            transition: 'color 0.2s',
          }}
        >
          Edit
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={mode === 'preview'}
          aria-controls={`${editorId}-preview-panel`}
          onClick={() => setMode('preview')}
          style={{
            padding: '8px 16px',
            fontSize: 'var(--text-small)',
            fontWeight: mode === 'preview' ? 'var(--weight-semibold)' : 'var(--weight-regular)',
            fontFamily: 'var(--font-mono)',
            border: 'none',
            borderBottom: mode === 'preview' ? '2px solid var(--color-primary)' : '2px solid transparent',
            backgroundColor: 'transparent',
            color: mode === 'preview' ? 'var(--color-primary)' : 'var(--color-text-muted)',
            cursor: 'pointer',
            transition: 'color 0.2s',
          }}
        >
          Preview
        </button>
      </div>

      {/* Edit panel */}
      {mode === 'edit' && (
        <div
          id={`${editorId}-edit-panel`}
          role="tabpanel"
          aria-label={label ? `${label} edit area` : 'Edit area'}
        >
          {label && (
            <label htmlFor={editorId} className="sr-only" style={{ position: 'absolute', width: '1px', height: '1px', overflow: 'hidden', clip: 'rect(0,0,0,0)' }}>
              {label}
            </label>
          )}
          <textarea
            id={editorId}
            data-testid="markdown-editor-textarea"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder || 'Write Markdown here...'}
            aria-label={label || 'Markdown content'}
            style={{
              width: '100%',
              minHeight: '200px',
              padding: '12px',
              border: 'none',
              outline: 'none',
              resize: 'vertical',
              fontSize: 'var(--text-body)',
              fontFamily: 'var(--font-mono)',
              lineHeight: '1.6',
              boxSizing: 'border-box',
              backgroundColor: 'var(--color-bg-surface)',
              color: 'var(--color-text-primary)',
            }}
          />
        </div>
      )}

      {/* Preview panel */}
      {mode === 'preview' && (
        <div
          id={`${editorId}-preview-panel`}
          role="tabpanel"
          aria-label={label ? `${label} preview` : 'Markdown preview'}
          data-testid="markdown-editor-preview"
          style={{
            minHeight: '200px',
            padding: '12px',
            fontSize: 'var(--text-body)',
            lineHeight: '1.6',
            backgroundColor: 'var(--color-bg-surface)',
            color: 'var(--color-text-primary)',
          }}
        >
          {value ? (
            <div dangerouslySetInnerHTML={{ __html: renderMarkdown(value) }} />
          ) : (
            <p style={{ color: 'var(--color-text-muted)', fontStyle: 'italic' }}>Nothing to preview</p>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * Simple Markdown-to-HTML renderer for preview mode.
 * Supports: headings, bold, italic, inline code, code blocks, lists, links, and line breaks.
 */
function renderMarkdown(markdown: string): string {
  let html = escapeHtml(markdown);

  // Code blocks (``` ... ```)
  html = html.replace(/```([\s\S]*?)```/g, '<pre style="background:var(--color-bg-elevated);padding:12px;border-radius:var(--radius-medium);overflow-x:auto;font-size:13px;border:1px solid var(--color-border);"><code>$1</code></pre>');

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code style="background:var(--color-bg-elevated);padding:2px 6px;border-radius:var(--radius-small);font-size:13px;color:var(--color-primary);">$1</code>');

  // Headings (h1-h3)
  html = html.replace(/^### (.+)$/gm, '<h3 style="font-size:16px;font-weight:600;margin:16px 0 8px;color:var(--color-text-primary);font-family:var(--font-heading);">$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2 style="font-size:18px;font-weight:600;margin:16px 0 8px;color:var(--color-text-primary);font-family:var(--font-heading);">$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1 style="font-size:20px;font-weight:700;margin:16px 0 8px;color:var(--color-text-primary);font-family:var(--font-heading);">$1</h1>');

  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

  // Italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');

  // Unordered lists
  html = html.replace(/^- \[x\] (.+)$/gm, '<li style="list-style:none;margin-left:0;"><input type="checkbox" checked disabled /> $1</li>');
  html = html.replace(/^- \[ \] (.+)$/gm, '<li style="list-style:none;margin-left:0;"><input type="checkbox" disabled /> $1</li>');
  html = html.replace(/^- (.+)$/gm, '<li style="margin-left:20px;">$1</li>');

  // Links [text](url)
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" style="color:var(--color-primary);text-decoration:underline;">$1</a>');

  // Line breaks (double newline = paragraph break)
  html = html.replace(/\n\n/g, '</p><p style="margin:8px 0;">');
  html = html.replace(/\n/g, '<br />');

  // Wrap in paragraph
  html = '<p style="margin:8px 0;">' + html + '</p>';

  return html;
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
