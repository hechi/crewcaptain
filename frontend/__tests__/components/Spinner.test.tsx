import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import Spinner from '@/components/feedback/Spinner';

describe('Spinner', () => {
  it('renders a status role with an accessible label', () => {
    render(<Spinner label="Generating template" />);
    const status = screen.getByTestId('ai-spinner');
    expect(status).toBeInTheDocument();
    expect(status).toHaveAttribute('role', 'status');
    expect(screen.getByText('Generating template')).toBeInTheDocument();
  });

  it('renders the animated spinner icon', () => {
    const { container } = render(<Spinner />);
    // The lucide icon carries the class the CSS animation (and reduced-motion override) target.
    expect(container.querySelector('.feedback-spinner-icon')).toBeInTheDocument();
  });
});
