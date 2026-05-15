'use client';

import './LoadingScreen.css';

interface LoadingScreenProps {
  message?: string;
  fullScreen?: boolean;
}

export default function LoadingScreen({ message, fullScreen = false }: LoadingScreenProps) {
  return (
    <div
      data-testid="loading-screen"
      role="status"
      aria-label="Loading"
      className="loading-screen"
      style={{ minHeight: fullScreen ? '100vh' : '60vh' }}
    >
      {/* Scan-line overlay */}
      <div data-testid="loading-scanlines" className="loading-scanlines" aria-hidden="true" />

      {/* HUD Spinner */}
      <div data-testid="loading-spinner" className="loading-spinner">
        {/* Outer ring */}
        <div data-testid="loading-ring-outer" className="loading-ring loading-ring--outer" />
        {/* Inner ring */}
        <div data-testid="loading-ring-inner" className="loading-ring loading-ring--inner" />
        {/* Core pulse */}
        <div data-testid="loading-core" className="loading-core" />
      </div>

      {/* Glitch text */}
      <div data-testid="loading-glitch-text" className="loading-glitch-text" aria-hidden="true">
        <span className="loading-glitch-text__main">LOADING</span>
        <span className="loading-glitch-text__clone loading-glitch-text__clone--1">LOADING</span>
        <span className="loading-glitch-text__clone loading-glitch-text__clone--2">LOADING</span>
      </div>

      {/* Status message */}
      {message && (
        <p data-testid="loading-status" className="loading-status">
          {message}
        </p>
      )}
    </div>
  );
}
