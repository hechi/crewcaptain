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
    label: '1:1 Session',
    src: '/screenshots/one-on-one-entry.png',
    alt: '1:1 session entry with notes and agenda',
    caption:
      'Capture session notes, agenda items, and outcomes during your 1:1s. Everything stays in context for next time.',
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
