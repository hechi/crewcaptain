import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import LoadingScreen from '@/components/LoadingScreen';

describe('LoadingScreen', () => {
  it('should render with default message', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-screen')).toBeInTheDocument();
    expect(screen.getByTestId('loading-glitch-text')).toHaveTextContent('LOADING');
  });

  it('should render with custom message', () => {
    render(<LoadingScreen message="Initializing systems" />);
    expect(screen.getByText('Initializing systems')).toBeInTheDocument();
  });

  it('should render the HUD spinner element', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should render outer and inner rings', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-ring-outer')).toBeInTheDocument();
    expect(screen.getByTestId('loading-ring-inner')).toBeInTheDocument();
  });

  it('should render the scan-line overlay', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-scanlines')).toBeInTheDocument();
  });

  it('should render the core pulse element', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-core')).toBeInTheDocument();
  });

  it('should apply fullscreen styles when fullScreen prop is true', () => {
    render(<LoadingScreen fullScreen />);
    const container = screen.getByTestId('loading-screen');
    expect(container).toHaveStyle({ minHeight: '100vh' });
  });

  it('should apply compact styles when fullScreen prop is false', () => {
    render(<LoadingScreen fullScreen={false} />);
    const container = screen.getByTestId('loading-screen');
    expect(container).toHaveStyle({ minHeight: '60vh' });
  });

  it('should default to fullScreen false', () => {
    render(<LoadingScreen />);
    const container = screen.getByTestId('loading-screen');
    expect(container).toHaveStyle({ minHeight: '60vh' });
  });

  it('should render the status text with monospace font', () => {
    render(<LoadingScreen message="Syncing data" />);
    const statusText = screen.getByTestId('loading-status');
    expect(statusText).toHaveTextContent('Syncing data');
  });

  it('should have accessible role and label', () => {
    render(<LoadingScreen />);
    const container = screen.getByTestId('loading-screen');
    expect(container).toHaveAttribute('role', 'status');
    expect(container).toHaveAttribute('aria-label', 'Loading');
  });

  it('should render the glitch text element', () => {
    render(<LoadingScreen />);
    expect(screen.getByTestId('loading-glitch-text')).toBeInTheDocument();
  });
});
