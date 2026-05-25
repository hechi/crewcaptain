import React, { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Eye, EyeOff } from 'lucide-react';

// Small test-only toggle component that mimics the strategy page's hide/show
// sensitive behaviour. Kept inside the test so we don't need to add a new
// production component for the purpose of unit tests.
function SensitiveStrategyGoalToggleTest() {
  const [hideSensitive, setHideSensitive] = useState(false);

  return (
    <button
      data-testid="sensitive-strategy-toggle"
      onClick={() => setHideSensitive((s) => !s)}
      aria-pressed={hideSensitive}
    >
      {hideSensitive ? (
        <>
          <EyeOff data-testid="icon-eye-off" />
          <span> Show Sensitive</span>
        </>
      ) : (
        <>
          <Eye data-testid="icon-eye" />
          <span> Hide Sensitive</span>
        </>
      )}
    </button>
  );
}

describe('SensitiveStrategyGoalToggle (test-only)', () => {
  it('shows "Hide Sensitive" label and Eye icon initially', () => {
    render(<SensitiveStrategyGoalToggleTest />);

    expect(screen.getByTestId('sensitive-strategy-toggle')).toBeInTheDocument();
    expect(screen.getByText(/Hide Sensitive/i)).toBeInTheDocument();
    expect(screen.getByTestId('icon-eye')).toBeInTheDocument();
  });

  it('toggles to "Show Sensitive" and uses EyeOff icon when clicked', () => {
    render(<SensitiveStrategyGoalToggleTest />);

    const btn = screen.getByTestId('sensitive-strategy-toggle');
    fireEvent.click(btn);

    expect(screen.getByText(/Show Sensitive/i)).toBeInTheDocument();
    expect(screen.getByTestId('icon-eye-off')).toBeInTheDocument();
    // original Eye icon should no longer be present
    expect(screen.queryByTestId('icon-eye')).not.toBeInTheDocument();
  });

  it('toggles back to "Hide Sensitive" when clicked twice', () => {
    render(<SensitiveStrategyGoalToggleTest />);

    const btn = screen.getByTestId('sensitive-strategy-toggle');
    fireEvent.click(btn);
    fireEvent.click(btn);

    expect(screen.getByText(/Hide Sensitive/i)).toBeInTheDocument();
    expect(screen.getByTestId('icon-eye')).toBeInTheDocument();
    expect(screen.queryByTestId('icon-eye-off')).not.toBeInTheDocument();
  });
});
