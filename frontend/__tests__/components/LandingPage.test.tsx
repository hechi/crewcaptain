import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import LandingPage from '@/components/LandingPage';

jest.mock('next-auth/react', () => ({
  signIn: jest.fn(),
}));

import { signIn } from 'next-auth/react';

const mockSignIn = signIn as jest.MockedFunction<typeof signIn>;

describe('LandingPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the landing page container', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('landing-page')).toBeInTheDocument();
  });

  it('should render the hero section with headline', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('hero-section')).toBeInTheDocument();
    expect(screen.getByText('people context')).toBeInTheDocument();
  });

  it('should render the hero badge with self-hosted messaging', () => {
    render(<LandingPage />);
    expect(screen.getByText(/Self-hosted/)).toBeInTheDocument();
    expect(screen.getByText(/Privacy-first/)).toBeInTheDocument();
    expect(screen.getByText(/Open Source/)).toBeInTheDocument();
  });

  it('should render the hero subtitle', () => {
    render(<LandingPage />);
    expect(
      screen.getByText(/Track 1:1s, development goals, and action items/)
    ).toBeInTheDocument();
  });

  it('should render the Get Started button that triggers sign in', () => {
    render(<LandingPage />);
    const button = screen.getByTestId('signin-button');
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent('Get Started');

    button.click();
    expect(mockSignIn).toHaveBeenCalledWith('oidc', { callbackUrl: '/dashboard' });
  });

  it('should render the Deploy with Docker link', () => {
    render(<LandingPage />);
    const link = screen.getByTestId('github-link');
    expect(link).toBeInTheDocument();
    expect(link).toHaveTextContent('Deploy with Docker');
    expect(link).toHaveAttribute('href', 'https://github.com/your-org/crewcaptain');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('should render the tagline', () => {
    render(<LandingPage />);
    expect(screen.getByText('Lead with memory. Act with clarity.')).toBeInTheDocument();
  });

  it('should render the features section with 6 feature cards', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('features-section')).toBeInTheDocument();
    const featureCards = screen.getAllByTestId('feature-card');
    expect(featureCards).toHaveLength(6);
  });

  it('should render feature titles', () => {
    render(<LandingPage />);
    expect(screen.getByText('1:1 Management')).toBeInTheDocument();
    expect(screen.getByText('PDP Goal Tracking')).toBeInTheDocument();
    expect(screen.getByText('Action Items')).toBeInTheDocument();
    expect(screen.getByText('People Directory')).toBeInTheDocument();
    expect(screen.getByText('Quick Notes Inbox')).toBeInTheDocument();
    expect(screen.getByText('Dashboard & Insights')).toBeInTheDocument();
  });

  it('should render the how it works section with 3 steps', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('how-section')).toBeInTheDocument();
    const stepCards = screen.getAllByTestId('step-card');
    expect(stepCards).toHaveLength(3);
  });

  it('should render step titles', () => {
    render(<LandingPage />);
    expect(screen.getByText('Clone & Configure')).toBeInTheDocument();
    expect(screen.getByText('Docker Compose Up')).toBeInTheDocument();
    expect(screen.getByText('Start Leading')).toBeInTheDocument();
  });

  it('should render the privacy section', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('privacy-section')).toBeInTheDocument();
    expect(screen.getByText(/fully self-hosted/)).toBeInTheDocument();
  });

  it('should render privacy badges', () => {
    render(<LandingPage />);
    const badges = screen.getAllByTestId('privacy-badge');
    expect(badges).toHaveLength(3);
    expect(screen.getByText('AES-256 Encryption')).toBeInTheDocument();
    expect(screen.getByText('Self-Hosted')).toBeInTheDocument();
    expect(screen.getByText('AGPL-3.0 Licensed')).toBeInTheDocument();
  });

  it('should render the final CTA section', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('cta-section')).toBeInTheDocument();
    expect(screen.getByText('Ready to command your crew data?')).toBeInTheDocument();
    expect(screen.getByText('Remember more. Lead better.')).toBeInTheDocument();
  });

  it('should render the CTA sign in button that triggers sign in', () => {
    render(<LandingPage />);
    const ctaButton = screen.getByTestId('cta-signin-button');
    expect(ctaButton).toBeInTheDocument();

    ctaButton.click();
    expect(mockSignIn).toHaveBeenCalledWith('oidc', { callbackUrl: '/dashboard' });
  });

  it('should render the footer', () => {
    render(<LandingPage />);
    expect(screen.getByTestId('footer')).toBeInTheDocument();
    expect(screen.getByText('CrewCaptain')).toBeInTheDocument();
    expect(
      screen.getByText("The captain's log for modern managers.")
    ).toBeInTheDocument();
  });

  it('should render footer links', () => {
    render(<LandingPage />);
    expect(screen.getByText('GitHub')).toBeInTheDocument();
    expect(screen.getByText('Documentation')).toBeInTheDocument();
    expect(screen.getByText('License (AGPL-3.0)')).toBeInTheDocument();
  });

  it('should have accessible HUD visual with aria-hidden', () => {
    render(<LandingPage />);
    const heroSection = screen.getByTestId('hero-section');
    const hiddenElements = heroSection.querySelectorAll('[aria-hidden="true"]');
    // Grid overlay + HUD visual
    expect(hiddenElements.length).toBeGreaterThanOrEqual(2);
  });
});
