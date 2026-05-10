import React from 'react';
import { render, screen, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import ThemeProvider, { useTheme } from '@/components/ThemeProvider';

function TestConsumer() {
  const { theme, setTheme } = useTheme();
  return (
    <div>
      <span data-testid="current-theme">{theme}</span>
      <button data-testid="set-light" onClick={() => setTheme('LIGHT')}>Light</button>
      <button data-testid="set-dark" onClick={() => setTheme('DARK')}>Dark</button>
    </div>
  );
}

describe('ThemeProvider', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
  });

  it('should provide default dark theme', () => {
    render(
      <ThemeProvider>
        <TestConsumer />
      </ThemeProvider>
    );
    expect(screen.getByTestId('current-theme')).toHaveTextContent('DARK');
  });

  it('should not set data-theme attribute for dark theme', () => {
    render(
      <ThemeProvider>
        <TestConsumer />
      </ThemeProvider>
    );
    expect(document.documentElement.getAttribute('data-theme')).toBeNull();
  });

  it('should set data-theme attribute to light when light theme is selected', () => {
    render(
      <ThemeProvider initialTheme="LIGHT">
        <TestConsumer />
      </ThemeProvider>
    );
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('should update theme when setTheme is called', () => {
    render(
      <ThemeProvider>
        <TestConsumer />
      </ThemeProvider>
    );

    act(() => {
      screen.getByTestId('set-light').click();
    });

    expect(screen.getByTestId('current-theme')).toHaveTextContent('LIGHT');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('should remove data-theme attribute when switching back to dark', () => {
    render(
      <ThemeProvider initialTheme="LIGHT">
        <TestConsumer />
      </ThemeProvider>
    );

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    act(() => {
      screen.getByTestId('set-dark').click();
    });

    expect(document.documentElement.getAttribute('data-theme')).toBeNull();
  });

  it('should accept initialTheme prop', () => {
    render(
      <ThemeProvider initialTheme="LIGHT">
        <TestConsumer />
      </ThemeProvider>
    );
    expect(screen.getByTestId('current-theme')).toHaveTextContent('LIGHT');
  });

  it('should render children', () => {
    render(
      <ThemeProvider>
        <div data-testid="child">Hello</div>
      </ThemeProvider>
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
  });
});
