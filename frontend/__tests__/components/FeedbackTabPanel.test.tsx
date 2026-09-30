import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

// Sub-panels do their own data fetching; stub them so this test focuses on the
// tab header styling/behaviour.
jest.mock('@/components/feedback/FeedbackResponsesPanel', () => () => <div data-testid="responses-stub" />);
jest.mock('@/components/feedback/FeedbackLinksPanel', () => () => <div data-testid="links-stub" />);
jest.mock('@/components/feedback/FeedbackAnalyticsPanel', () => () => <div data-testid="analytics-stub" />);
jest.mock('@/components/feedback/FeedbackSummaryPanel', () => () => <div data-testid="summary-stub" />);

import FeedbackTabPanel from '@/components/feedback/FeedbackTabPanel';

describe('FeedbackTabPanel header', () => {
  it('renders the four sub-tabs and switches panels', () => {
    render(<FeedbackTabPanel personId="p1" />);
    ['responses', 'links', 'analytics', 'summary'].forEach((key) => {
      expect(screen.getByTestId(`feedback-subtab-${key}`)).toBeInTheDocument();
    });
    // Default is responses.
    expect(screen.getByTestId('responses-stub')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('feedback-subtab-analytics'));
    expect(screen.getByTestId('analytics-stub')).toBeInTheDocument();
  });

  it('uses the segmented-control style (like the app filter/scope toggles), not page-level underline tabs', () => {
    render(<FeedbackTabPanel personId="p1" />);

    const active = screen.getByTestId('feedback-subtab-responses');
    const inactive = screen.getByTestId('feedback-subtab-links');

    // Active segment: muted-primary fill + primary text (segmented control, not an underline tab).
    expect(active.style.backgroundColor).toBe('var(--color-primary-muted)');
    expect(active.style.color).toBe('var(--color-primary)');
    // It is not the page-level underline tab style.
    expect(active.style.borderBottom).toBe('');

    // Inactive segment: transparent fill + secondary text.
    expect(inactive.style.backgroundColor).toBe('transparent');
    expect(inactive.style.color).toBe('var(--color-text-secondary)');

    // Non-first segments have a divider border on the left (connected control).
    expect(inactive.style.borderLeft).toBe('1px solid var(--color-border)');
    // The first segment has no left divider.
    expect(active.style.borderLeft).toBe('');

    // Switching updates the active segment fill.
    fireEvent.click(inactive);
    expect(screen.getByTestId('feedback-subtab-links').style.backgroundColor).toBe('var(--color-primary-muted)');
  });

  it('wraps the segments in a single bordered, rounded container', () => {
    render(<FeedbackTabPanel personId="p1" />);
    const tablist = screen.getByRole('tablist', { name: 'Feedback sections' });
    expect(tablist.style.border).toBe('1px solid var(--color-border)');
    expect(tablist.style.borderRadius).toBe('var(--radius-medium)');
    expect(tablist.style.overflow).toBe('hidden');
  });

  it('renders the create link action as a filled primary button when onCreateLink is provided', () => {
    const onCreateLink = jest.fn();
    render(<FeedbackTabPanel personId="p1" onCreateLink={onCreateLink} />);
    const btn = screen.getByTestId('feedback-create-link-button');
    expect(btn.style.background).toBe('var(--color-primary)');
    expect(btn.style.borderRadius).toBe('var(--radius-medium)');
    fireEvent.click(btn);
    expect(onCreateLink).toHaveBeenCalledTimes(1);
  });

  it('omits the create link action when no callback is given', () => {
    render(<FeedbackTabPanel personId="p1" />);
    expect(screen.queryByTestId('feedback-create-link-button')).not.toBeInTheDocument();
  });
});
