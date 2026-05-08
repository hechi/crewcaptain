import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import MarkdownEditor from '@/components/one-on-one/MarkdownEditor';

describe('MarkdownEditor', () => {
  const defaultProps = {
    value: '',
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders in edit mode by default', () => {
    render(<MarkdownEditor {...defaultProps} />);

    expect(screen.getByTestId('markdown-editor')).toBeInTheDocument();
    expect(screen.getByTestId('markdown-editor-textarea')).toBeInTheDocument();
    expect(screen.queryByTestId('markdown-editor-preview')).not.toBeInTheDocument();
  });

  it('displays the provided value in the textarea', () => {
    render(<MarkdownEditor {...defaultProps} value="Hello **world**" />);

    const textarea = screen.getByTestId('markdown-editor-textarea') as HTMLTextAreaElement;
    expect(textarea.value).toBe('Hello **world**');
  });

  it('calls onChange when text is typed', () => {
    const onChange = jest.fn();
    render(<MarkdownEditor {...defaultProps} onChange={onChange} />);

    const textarea = screen.getByTestId('markdown-editor-textarea');
    fireEvent.change(textarea, { target: { value: 'New content' } });

    expect(onChange).toHaveBeenCalledWith('New content');
  });

  it('shows placeholder text when provided', () => {
    render(<MarkdownEditor {...defaultProps} placeholder="Enter notes..." />);

    const textarea = screen.getByTestId('markdown-editor-textarea');
    expect(textarea).toHaveAttribute('placeholder', 'Enter notes...');
  });

  it('uses default placeholder when none provided', () => {
    render(<MarkdownEditor {...defaultProps} />);

    const textarea = screen.getByTestId('markdown-editor-textarea');
    expect(textarea).toHaveAttribute('placeholder', 'Write Markdown here...');
  });

  it('switches to preview mode when Preview tab is clicked', () => {
    render(<MarkdownEditor {...defaultProps} value="# Hello" />);

    const previewTab = screen.getByRole('tab', { name: /preview/i });
    fireEvent.click(previewTab);

    expect(screen.queryByTestId('markdown-editor-textarea')).not.toBeInTheDocument();
    expect(screen.getByTestId('markdown-editor-preview')).toBeInTheDocument();
  });

  it('switches back to edit mode when Edit tab is clicked', () => {
    render(<MarkdownEditor {...defaultProps} value="# Hello" />);

    // Switch to preview
    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));
    expect(screen.queryByTestId('markdown-editor-textarea')).not.toBeInTheDocument();

    // Switch back to edit
    fireEvent.click(screen.getByRole('tab', { name: /edit/i }));
    expect(screen.getByTestId('markdown-editor-textarea')).toBeInTheDocument();
  });

  it('renders Markdown headings in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="# Heading 1" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).toContain('<h1');
    expect(preview.innerHTML).toContain('Heading 1');
  });

  it('renders bold text in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="This is **bold** text" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).toContain('<strong>bold</strong>');
  });

  it('renders italic text in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="This is *italic* text" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).toContain('<em>italic</em>');
  });

  it('renders list items in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="- Item one\n- Item two" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).toContain('<li');
    expect(preview.innerHTML).toContain('Item one');
    expect(preview.innerHTML).toContain('Item two');
  });

  it('shows "Nothing to preview" when value is empty in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    expect(screen.getByText('Nothing to preview')).toBeInTheDocument();
  });

  it('has proper aria attributes for accessibility', () => {
    render(<MarkdownEditor {...defaultProps} label="Notes" />);

    const editTab = screen.getByRole('tab', { name: /edit/i });
    const previewTab = screen.getByRole('tab', { name: /preview/i });

    expect(editTab).toHaveAttribute('aria-selected', 'true');
    expect(previewTab).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByRole('tablist')).toBeInTheDocument();
  });

  it('uses label for aria-label on textarea', () => {
    render(<MarkdownEditor {...defaultProps} label="Meeting Notes" />);

    const textarea = screen.getByTestId('markdown-editor-textarea');
    expect(textarea).toHaveAttribute('aria-label', 'Meeting Notes');
  });

  it('renders checkbox list items in preview mode', () => {
    render(<MarkdownEditor {...defaultProps} value="- [x] Done\n- [ ] Todo" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).toContain('type="checkbox"');
    expect(preview.innerHTML).toContain('checked');
    expect(preview.innerHTML).toContain('Done');
    expect(preview.innerHTML).toContain('Todo');
  });

  it('escapes HTML in content to prevent XSS', () => {
    render(<MarkdownEditor {...defaultProps} value="<script>alert('xss')</script>" />);

    fireEvent.click(screen.getByRole('tab', { name: /preview/i }));

    const preview = screen.getByTestId('markdown-editor-preview');
    expect(preview.innerHTML).not.toContain('<script>');
    expect(preview.innerHTML).toContain('&lt;script&gt;');
  });
});
