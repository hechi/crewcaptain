import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ScreenshotShowcase from '@/components/ScreenshotShowcase';

describe('ScreenshotShowcase', () => {
  it('should render the showcase section', () => {
    render(<ScreenshotShowcase />);
    expect(screen.getByTestId('screenshot-showcase')).toBeInTheDocument();
  });

  it('should render the section title', () => {
    render(<ScreenshotShowcase />);
    expect(screen.getByText('See it in action.')).toBeInTheDocument();
  });

  it('should render the section subtitle', () => {
    render(<ScreenshotShowcase />);
    expect(
      screen.getByText(/A quick look at what awaits you/)
    ).toBeInTheDocument();
  });

  it('should render 11 tab buttons', () => {
    render(<ScreenshotShowcase />);
    const tabs = screen.getAllByRole('tab');
    expect(tabs).toHaveLength(11);
  });

  it('should render tab labels for each screenshot', () => {
    render(<ScreenshotShowcase />);
    expect(screen.getByRole('tab', { name: /Dashboard/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /1:1 with AI/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /1:1 Overview/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Kudos AI/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /PDP SMART Check/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Review Narrative/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Quick Capture/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /AI Settings/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Action Items/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Person Detail/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Search/i })).toBeInTheDocument();
  });

  it('should show the first screenshot (Dashboard) by default', () => {
    render(<ScreenshotShowcase />);
    const img = screen.getByRole('img', { name: /Dashboard overview/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/dashboard.png');
  });

  it('should have the first tab selected by default', () => {
    render(<ScreenshotShowcase />);
    const firstTab = screen.getByRole('tab', { name: /Dashboard/i });
    expect(firstTab).toHaveAttribute('aria-selected', 'true');
  });

  it('should switch to 1:1 with AI screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /1:1 with AI/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /1:1 session entry with AI assistant/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/one-on-one-entry-with-ai-assistant.png');
  });

  it('should switch to Kudos AI screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Kudos AI/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Kudos creation with AI-powered SBI/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/kudos-ai-refine.png');
  });

  it('should switch to PDP SMART Check screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /PDP SMART Check/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Personal development goal with AI SMART/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/personal-development-goal-ai-smart-check.png');
  });

  it('should switch to Review Narrative screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Review Narrative/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /AI-generated performance review/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/performance-review-ai-narrative.png');
  });

  it('should switch to Quick Capture screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Quick Capture/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Quick capture button available/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/quick-capture-button.png');
  });

  it('should switch to AI Settings screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /AI Settings/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /AI assistant settings page/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/settings-ai-assitant.png');
  });

  it('should switch to Action Items screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Action Items/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Adding an action item/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/action-item.png');
  });

  it('should switch to Person Detail screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Person Detail/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Person entry detail/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/person-entry.png');
  });

  it('should switch to Search screenshot when tab is clicked', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Search/i });
    fireEvent.click(tab);

    const img = screen.getByRole('img', { name: /Search results/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/screenshots/search.png');
  });

  it('should update aria-selected when switching tabs', () => {
    render(<ScreenshotShowcase />);
    const dashboardTab = screen.getByRole('tab', { name: /Dashboard/i });
    const searchTab = screen.getByRole('tab', { name: /Search/i });

    expect(dashboardTab).toHaveAttribute('aria-selected', 'true');
    expect(searchTab).toHaveAttribute('aria-selected', 'false');

    fireEvent.click(searchTab);

    expect(dashboardTab).toHaveAttribute('aria-selected', 'false');
    expect(searchTab).toHaveAttribute('aria-selected', 'true');
  });

  it('should render a caption for the active screenshot', () => {
    render(<ScreenshotShowcase />);
    expect(
      screen.getByText(/Get a quick overview of your crew/)
    ).toBeInTheDocument();
  });

  it('should update caption when switching tabs', () => {
    render(<ScreenshotShowcase />);
    const tab = screen.getByRole('tab', { name: /Search/i });
    fireEvent.click(tab);

    expect(
      screen.getByText(/Find anything across your workspace/)
    ).toBeInTheDocument();
  });

  it('should use proper tablist role for the tab container', () => {
    render(<ScreenshotShowcase />);
    expect(screen.getByRole('tablist')).toBeInTheDocument();
  });

  it('should use proper tabpanel role for the screenshot display', () => {
    render(<ScreenshotShowcase />);
    expect(screen.getByRole('tabpanel')).toBeInTheDocument();
  });

  it('should support keyboard navigation with arrow keys', () => {
    render(<ScreenshotShowcase />);
    const tablist = screen.getByRole('tablist');
    const firstTab = screen.getByRole('tab', { name: /Dashboard/i });

    firstTab.focus();
    fireEvent.keyDown(tablist, { key: 'ArrowRight' });

    const secondTab = screen.getByRole('tab', { name: /1:1 with AI/i });
    expect(secondTab).toHaveAttribute('aria-selected', 'true');
  });

  it('should wrap around when pressing ArrowRight on last tab', () => {
    render(<ScreenshotShowcase />);
    const tablist = screen.getByRole('tablist');
    const searchTab = screen.getByRole('tab', { name: /Search/i });

    fireEvent.click(searchTab);
    fireEvent.keyDown(tablist, { key: 'ArrowRight' });

    const firstTab = screen.getByRole('tab', { name: /Dashboard/i });
    expect(firstTab).toHaveAttribute('aria-selected', 'true');
  });

  it('should wrap around when pressing ArrowLeft on first tab', () => {
    render(<ScreenshotShowcase />);
    const tablist = screen.getByRole('tablist');
    const firstTab = screen.getByRole('tab', { name: /Dashboard/i });

    firstTab.focus();
    fireEvent.keyDown(tablist, { key: 'ArrowLeft' });

    const lastTab = screen.getByRole('tab', { name: /Search/i });
    expect(lastTab).toHaveAttribute('aria-selected', 'true');
  });
});
