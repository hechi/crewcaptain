import { render, screen, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import CompletionAnimation from '@/components/gamification/CompletionAnimation';

describe('CompletionAnimation', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should not render when show is false', () => {
    render(<CompletionAnimation show={false} />);
    expect(screen.queryByTestId('completion-animation')).not.toBeInTheDocument();
  });

  it('should render when show is true', () => {
    render(<CompletionAnimation show={true} />);
    expect(screen.getByTestId('completion-animation')).toBeInTheDocument();
  });

  it('should render checkmark SVG', () => {
    render(<CompletionAnimation show={true} />);
    expect(screen.getByTestId('completion-checkmark')).toBeInTheDocument();
  });

  it('should be aria-hidden for accessibility', () => {
    render(<CompletionAnimation show={true} />);
    expect(screen.getByTestId('completion-animation')).toHaveAttribute('aria-hidden', 'true');
  });

  it('should call onComplete after animation finishes', () => {
    const onComplete = jest.fn();
    render(<CompletionAnimation show={true} onComplete={onComplete} />);

    act(() => {
      jest.advanceTimersByTime(1000);
    });

    expect(onComplete).toHaveBeenCalledTimes(1);
  });

  it('should hide after animation completes', () => {
    render(<CompletionAnimation show={true} />);
    expect(screen.getByTestId('completion-animation')).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(1000);
    });

    expect(screen.queryByTestId('completion-animation')).not.toBeInTheDocument();
  });
});
