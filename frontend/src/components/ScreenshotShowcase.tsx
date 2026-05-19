'use client';

import { useState, useCallback } from 'react';

interface Screenshot {
  id: string;
  label: string;
  src: string;
  alt: string;
  caption: string;
}

const screenshots: Screenshot[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    src: '/screenshots/dashboard.png',
    alt: 'Dashboard overview showing crew summary and key metrics',
    caption:
      'Get a quick overview of your crew — overdue items, upcoming 1:1s, anniversaries, and engagement streaks at a glance.',
  },
  {
    id: 'one-on-one-entry',
    label: '1:1 with AI',
    src: '/screenshots/one-on-one-entry-with-ai-assistant.png',
    alt: '1:1 session entry with AI assistant for agenda generation and outcome extraction',
    caption:
      'Run your 1:1s with AI-powered agenda generation and automatic outcome extraction. Notes, action items, and decisions — captured effortlessly.',
  },
  {
    id: 'one-on-one-overview',
    label: '1:1 Overview',
    src: '/screenshots/one-on-one-overview.png',
    alt: '1:1 overview showing history of past sessions',
    caption:
      'See the full timeline of past 1:1 sessions with a team member. Never lose track of what was discussed.',
  },
  {
    id: 'kudos-ai',
    label: 'Kudos AI',
    src: '/screenshots/kudos-ai-refine.png',
    alt: 'Kudos creation with AI-powered SBI framework refinement',
    caption:
      'Write better recognition with AI. Refine your kudos using the SBI (Situation-Behavior-Impact) framework for more meaningful feedback.',
  },
  {
    id: 'pdp-smart-check',
    label: 'PDP SMART Check',
    src: '/screenshots/personal-development-goal-ai-smart-check.png',
    alt: 'Personal development goal with AI SMART framework check',
    caption:
      'Validate development goals against the SMART framework. AI checks if goals are Specific, Measurable, Achievable, Relevant, and Time-bound.',
  },
  {
    id: 'performance-review',
    label: 'Review Narrative',
    src: '/screenshots/performance-review-ai-narrative.png',
    alt: 'AI-generated performance review narrative summary',
    caption:
      'Generate a performance review narrative from your collected data. Copy-paste ready for any external review tool — no file downloads needed.',
  },
  {
    id: 'ai-insights',
    label: 'AI Insights',
    src: '/screenshots/person-ai-insights.png',
    alt: 'AI Strategic Trend Radar showing insights with confidence scores',
    caption:
      'Surface long-term patterns with the AI Trend Radar. Analyzes 90 days of data across morale, growth, recognition, and meeting efficacy — each insight scored by confidence level.',
  },
  {
    id: 'quick-capture',
    label: 'Quick Capture',
    src: '/screenshots/quick-capture-button.png',
    alt: 'Quick capture button available on every screen',
    caption:
      'Capture thoughts instantly from any page. The floating quick capture button lets you jot down notes without leaving your current context.',
  },
  {
    id: 'ai-settings',
    label: 'AI Settings',
    src: '/screenshots/settings-ai-assitant.png',
    alt: 'AI assistant settings page with privacy controls and model configuration',
    caption:
      'Full control over your AI assistant. Choose your own model (local or cloud), customize prompts, and exclude sensitive content from AI processing.',
  },
  {
    id: 'action-item',
    label: 'Action Items',
    src: '/screenshots/action-item.png',
    alt: 'Adding an action item to a person',
    caption:
      'Create action items directly from a person\u2019s profile. Set due dates, assign owners, and track completion.',
  },
  {
    id: 'person-entry',
    label: 'Person Detail',
    src: '/screenshots/person-entry.png',
    alt: 'Person entry detail view with profile and history',
    caption:
      'Everything about a team member in one place — morale, notes, 1:1 history, goals, and kudos.',
  },
  {
    id: 'search',
    label: 'Search',
    src: '/screenshots/search.png',
    alt: 'Search results across the workspace',
    caption:
      'Find anything across your workspace — people, notes, action items, and 1:1 entries with full-text search.',
  },
];

export default function ScreenshotShowcase() {
  const [activeIndex, setActiveIndex] = useState(0);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'ArrowRight') {
        setActiveIndex((prev) => (prev + 1) % screenshots.length);
      } else if (e.key === 'ArrowLeft') {
        setActiveIndex(
          (prev) => (prev - 1 + screenshots.length) % screenshots.length
        );
      }
    },
    []
  );

  const active = screenshots[activeIndex];

  return (
    <section
      className="landing-showcase"
      data-testid="screenshot-showcase"
      aria-labelledby="showcase-title"
    >
      <div className="landing-section__header">
        <h2 className="landing-section__title" id="showcase-title">
          See it in action.{' '}
          <span className="landing-section__title-muted">
            Your crew command center.
          </span>
        </h2>
        <p className="landing-section__subtitle">
          A quick look at what awaits you inside CrewCaptain.
        </p>
      </div>

      <div className="landing-showcase__container">
        {/* Tab buttons */}
        <div
          className="landing-showcase__tabs"
          role="tablist"
          aria-label="Screenshot tabs"
          onKeyDown={handleKeyDown}
        >
          {screenshots.map((shot, index) => (
            <button
              key={shot.id}
              role="tab"
              type="button"
              aria-selected={index === activeIndex}
              aria-controls={`tabpanel-${shot.id}`}
              id={`tab-${shot.id}`}
              className={`landing-showcase__tab ${
                index === activeIndex ? 'landing-showcase__tab--active' : ''
              }`}
              onClick={() => setActiveIndex(index)}
              tabIndex={index === activeIndex ? 0 : -1}
            >
              {shot.label}
            </button>
          ))}
        </div>

        {/* Screenshot display */}
        <div
          className="landing-showcase__panel"
          role="tabpanel"
          id={`tabpanel-${active.id}`}
          aria-labelledby={`tab-${active.id}`}
        >
          <div className="landing-showcase__image-wrapper">
            <img
              src={active.src}
              alt={active.alt}
              className="landing-showcase__image"
              loading="lazy"
            />
          </div>
          <p className="landing-showcase__caption">{active.caption}</p>
        </div>
      </div>
    </section>
  );
}
