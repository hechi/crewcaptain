import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProgressRing from '@/components/gamification/ProgressRing';

describe('ProgressRing', () => {
  it('should render with percentage value', () => {
    render(<ProgressRing percentage={75} />);
    expect(screen.getByTestId('progress-ring')).toBeInTheDocument();
    expect(screen.getByTestId('progress-ring-value')).toHaveTextContent('75%');
  });

  it('should render with 0 percentage', () => {
    render(<ProgressRing percentage={0} />);
    expect(screen.getByTestId('progress-ring-value')).toHaveTextContent('0%');
  });

  it('should render with 100 percentage', () => {
    render(<ProgressRing percentage={100} />);
    expect(screen.getByTestId('progress-ring-value')).toHaveTextContent('100%');
  });

  it('should clamp percentage above 100', () => {
    render(<ProgressRing percentage={150} />);
    expect(screen.getByTestId('progress-ring-value')).toHaveTextContent('150%');
  });

  it('should render label when provided', () => {
    render(<ProgressRing percentage={50} label="Achieved" />);
    expect(screen.getByTestId('progress-ring-label')).toHaveTextContent('Achieved');
  });

  it('should not render label when not provided', () => {
    render(<ProgressRing percentage={50} />);
    expect(screen.queryByTestId('progress-ring-label')).not.toBeInTheDocument();
  });

  it('should render SVG with correct aria-label', () => {
    render(<ProgressRing percentage={42} />);
    const svg = screen.getByRole('img');
    expect(svg).toHaveAttribute('aria-label', 'Progress: 42%');
  });

  it('should render progress arc element', () => {
    render(<ProgressRing percentage={60} />);
    expect(screen.getByTestId('progress-ring-arc')).toBeInTheDocument();
  });

  it('should accept custom size', () => {
    render(<ProgressRing percentage={50} size={80} />);
    const svg = screen.getByRole('img');
    expect(svg).toHaveAttribute('width', '80');
    expect(svg).toHaveAttribute('height', '80');
  });

  it('should accept custom color', () => {
    render(<ProgressRing percentage={50} color="var(--color-success)" />);
    const arc = screen.getByTestId('progress-ring-arc');
    expect(arc).toHaveAttribute('stroke', 'var(--color-success)');
  });
});
