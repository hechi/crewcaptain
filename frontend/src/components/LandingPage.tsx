'use client';

import { signIn } from 'next-auth/react';
import {
  Shield,
  Users,
  Target,
  MessageSquare,
  Zap,
  Lock,
  Server,
  BarChart3,
  CheckCircle2,
  ArrowRight,
  Compass,
  Sparkles,
  Brain,
  FileText,
  PenTool,
  Settings2,
  StickyNote,
} from 'lucide-react';
import ScreenshotShowcase from './ScreenshotShowcase';

export default function LandingPage() {
  return (
    <div data-testid="landing-page" className="landing-page">
      {/* Hero Section */}
      <section className="landing-hero" data-testid="hero-section">
        <div className="landing-hero__grid-overlay" aria-hidden="true" />
        <div className="landing-hero__content">
          <div className="landing-hero__badge">
            <Compass size={14} aria-hidden="true" />
            <span>Self-hosted &middot; Privacy-first &middot; Open Source</span>
          </div>
          <h1 className="landing-hero__title">
            Your private cockpit for{' '}
            <span className="landing-hero__title-accent">people context</span>
          </h1>
          <p className="landing-hero__subtitle">
            Track 1:1s, development goals, and action items in one self-hosted
            workspace. Your data. Your crew. Your rules.
          </p>
          <div className="landing-hero__cta-group">
            <button
              type="button"
              className="landing-btn landing-btn--primary"
              onClick={() => signIn('oidc', { callbackUrl: '/dashboard' })}
              data-testid="signin-button"
            >
              Get Started
              <ArrowRight size={16} aria-hidden="true" />
            </button>
            <a
              href="https://github.com/your-org/crewcaptain"
              className="landing-btn landing-btn--secondary"
              data-testid="github-link"
              target="_blank"
              rel="noopener noreferrer"
            >
              <Server size={16} aria-hidden="true" />
              Deploy with Docker
            </a>
          </div>
          <p className="landing-hero__slogan">
            Lead with memory. Act with clarity.
          </p>
        </div>
        <div className="landing-hero__visual" aria-hidden="true">
          <div className="landing-hero__hud">
            <div className="landing-hero__hud-ring landing-hero__hud-ring--outer" />
            <div className="landing-hero__hud-ring landing-hero__hud-ring--inner" />
            <div className="landing-hero__hud-center">
              <span className="landing-hero__hud-label">CC</span>
            </div>
            <div className="landing-hero__hud-line landing-hero__hud-line--1" />
            <div className="landing-hero__hud-line landing-hero__hud-line--2" />
            <div className="landing-hero__hud-line landing-hero__hud-line--3" />
            <div className="landing-hero__hud-line landing-hero__hud-line--4" />
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="landing-features" data-testid="features-section">
        <div className="landing-section__header">
          <h2 className="landing-section__title">
            Everything a manager needs.{' '}
            <span className="landing-section__title-muted">Nothing they don&apos;t.</span>
          </h2>
          <p className="landing-section__subtitle">
            A centralized command center for managers who believe great leadership
            starts with remembering what matters.
          </p>
        </div>
        <div className="landing-features__grid">
          <FeatureCard
            icon={<MessageSquare size={24} />}
            title="1:1 Management"
            description="Track meetings with agenda templates, notes, outcomes, and sensitive flags. Never lose context between sessions."
          />
          <FeatureCard
            icon={<Target size={24} />}
            title="PDP Goal Tracking"
            description="Set development goals, track progress with timestamped updates, and guide your team's growth over time."
          />
          <FeatureCard
            icon={<CheckCircle2 size={24} />}
            title="Action Items"
            description="Create follow-ups from 1:1s, assign owners, set due dates, and track completion across your entire team."
          />
          <FeatureCard
            icon={<Users size={24} />}
            title="People Directory"
            description="Organize your crew with morale tracking, pinned notes, tags, and workspaces. See everything at a glance."
          />
          <FeatureCard
            icon={<StickyNote size={24} />}
            title="Quick Capture"
            description="Capture thoughts from any page with the floating quick capture button. Assign to people, attach to 1:1s, or convert to action items — without leaving your current context."
          />
          <FeatureCard
            icon={<BarChart3 size={24} />}
            title="Dashboard & Insights"
            description="Overdue items, stale 1:1 reminders, upcoming anniversaries, and engagement streaks — all in one view."
          />
        </div>
      </section>

      {/* AI Features Section */}
      <section className="landing-ai" data-testid="ai-section">
        <div className="landing-section__header">
          <div className="landing-ai__badge">
            <Sparkles size={14} aria-hidden="true" />
            <span>AI-Powered</span>
          </div>
          <h2 className="landing-section__title">
            Your optional AI co-pilot.{' '}
            <span className="landing-section__title-muted">Privacy-first by design.</span>
          </h2>
          <p className="landing-section__subtitle">
            Bring your own model — local or cloud. Every AI feature is optional, and sensitive
            content can be excluded entirely. You stay in control.
          </p>
        </div>
        <div className="landing-ai__grid">
          <AiFeatureCard
            icon={<MessageSquare size={20} />}
            title="Generate Agenda"
            description="AI suggests agenda items based on open action items, PDP goals, and previous 1:1 context."
          />
          <AiFeatureCard
            icon={<Brain size={20} />}
            title="Extract Outcomes"
            description="After saving notes, AI extracts action items and decisions for review — no manual parsing needed."
          />
          <AiFeatureCard
            icon={<FileText size={20} />}
            title="Performance Narrative"
            description="Generate a review summary from your collected data. Copy-paste ready for any external tool."
          />
          <AiFeatureCard
            icon={<PenTool size={20} />}
            title="Kudos Refinement"
            description="Refine recognition using the SBI framework. AI helps you write more impactful, structured feedback."
          />
          <AiFeatureCard
            icon={<Target size={20} />}
            title="SMART Goal Check"
            description="Validate PDP goals against the SMART framework — Specific, Measurable, Achievable, Relevant, Time-bound."
          />
          <AiFeatureCard
            icon={<Settings2 size={20} />}
            title="Full Control"
            description="Choose your model, customize prompts, and exclude sensitive content. Works with Ollama, LiteLLM, OpenAI, or any compatible API."
          />
        </div>
      </section>

      {/* Screenshot Showcase Section */}
      <ScreenshotShowcase />

      {/* How It Works Section */}
      <section className="landing-how" data-testid="how-section">
        <div className="landing-section__header">
          <h2 className="landing-section__title">
            Deploy in minutes.{' '}
            <span className="landing-section__title-muted">Own it forever.</span>
          </h2>
        </div>
        <div className="landing-how__steps">
          <StepCard
            number="01"
            title="Clone & Configure"
            description="Pull the repo, set your OIDC provider credentials, and configure your environment."
          />
          <StepCard
            number="02"
            title="Docker Compose Up"
            description="One command spins up the full stack — API, frontend, and PostgreSQL database."
          />
          <StepCard
            number="03"
            title="Start Leading"
            description="Sign in with your identity provider and begin building your leadership memory."
          />
        </div>
      </section>

      {/* Privacy Section */}
      <section className="landing-privacy" data-testid="privacy-section">
        <div className="landing-privacy__content">
          <div className="landing-privacy__icon-wrapper">
            <Shield size={40} aria-hidden="true" />
          </div>
          <h2 className="landing-section__title">
            Your data stays{' '}
            <span className="landing-hero__title-accent">yours</span>
          </h2>
          <p className="landing-section__subtitle">
            CrewCaptain is fully self-hosted. No cloud dependency, no vendor lock-in,
            no third-party access to your people data. AES-256 encryption at rest
            for sensitive content.
          </p>
          <div className="landing-privacy__badges">
            <PrivacyBadge icon={<Lock size={16} />} text="AES-256 Encryption" />
            <PrivacyBadge icon={<Server size={16} />} text="Self-Hosted" />
            <PrivacyBadge icon={<Shield size={16} />} text="AGPL-3.0 Licensed" />
          </div>
        </div>
      </section>

      {/* Final CTA Section */}
      <section className="landing-cta" data-testid="cta-section">
        <h2 className="landing-cta__title">
          Ready to command your crew data?
        </h2>
        <p className="landing-cta__subtitle">
          Remember more. Lead better.
        </p>
        <div className="landing-hero__cta-group">
          <button
            type="button"
            className="landing-btn landing-btn--primary"
            onClick={() => signIn('oidc', { callbackUrl: '/dashboard' })}
            data-testid="cta-signin-button"
          >
            Get Started
            <ArrowRight size={16} aria-hidden="true" />
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer" data-testid="footer">
        <div className="landing-footer__content">
          <div className="landing-footer__brand">
            <span className="landing-footer__logo">CrewCaptain</span>
            <span className="landing-footer__tagline">
              The captain&apos;s log for modern managers.
            </span>
          </div>
          <div className="landing-footer__links">
            <a
              href="https://github.com/your-org/crewcaptain"
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </a>
            <a
              href="https://github.com/your-org/crewcaptain/blob/main/README.md"
              target="_blank"
              rel="noopener noreferrer"
            >
              Documentation
            </a>
            <a
              href="https://github.com/your-org/crewcaptain/blob/main/LICENSE"
              target="_blank"
              rel="noopener noreferrer"
            >
              License (AGPL-3.0)
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="landing-feature-card" data-testid="feature-card">
      <div className="landing-feature-card__icon">{icon}</div>
      <h3 className="landing-feature-card__title">{title}</h3>
      <p className="landing-feature-card__description">{description}</p>
    </div>
  );
}

function StepCard({
  number,
  title,
  description,
}: {
  number: string;
  title: string;
  description: string;
}) {
  return (
    <div className="landing-step-card" data-testid="step-card">
      <span className="landing-step-card__number">{number}</span>
      <h3 className="landing-step-card__title">{title}</h3>
      <p className="landing-step-card__description">{description}</p>
    </div>
  );
}

function PrivacyBadge({
  icon,
  text,
}: {
  icon: React.ReactNode;
  text: string;
}) {
  return (
    <div className="landing-privacy-badge" data-testid="privacy-badge">
      {icon}
      <span>{text}</span>
    </div>
  );
}

function AiFeatureCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="landing-ai-card" data-testid="ai-feature-card">
      <div className="landing-ai-card__icon">{icon}</div>
      <div className="landing-ai-card__content">
        <h3 className="landing-ai-card__title">{title}</h3>
        <p className="landing-ai-card__description">{description}</p>
      </div>
    </div>
  );
}
