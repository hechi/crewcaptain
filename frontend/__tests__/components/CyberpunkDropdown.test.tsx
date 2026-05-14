import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Navigation from '@/components/Navigation';
import FilterBar from '@/components/FilterBar';
import WorkspaceSelector from '@/components/workspace/WorkspaceSelector';

// Mock next-auth
jest.mock('next-auth/react', () => ({
  useSession: () => ({
    data: { user: { name: 'Test User' }, accessToken: 'token' },
    status: 'authenticated',
  }),
  signOut: jest.fn(),
}));

// Mock next/link
jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  );
});

describe('Cyberpunk Dropdown Styling', () => {
  describe('Navigation User Menu', () => {
    it('should apply dropdown-trigger class to user menu button', () => {
      render(<Navigation />);
      const trigger = screen.getByTestId('nav-user-menu-trigger');
      expect(trigger).toHaveClass('dropdown-trigger');
    });

    it('should apply dropdown-panel class to opened menu', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const menu = screen.getByTestId('nav-user-menu');
      expect(menu).toHaveClass('dropdown-panel');
    });

    it('should apply dropdown-item class to menu links', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const settingsLink = screen.getByTestId('nav-settings');
      expect(settingsLink).toHaveClass('dropdown-item');
    });

    it('should apply dropdown-item--danger class to sign out button', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const signOutBtn = screen.getByTestId('nav-signout');
      expect(signOutBtn).toHaveClass('dropdown-item');
      expect(signOutBtn).toHaveClass('dropdown-item--danger');
    });

    it('should have dropdown-divider separator before sign out', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const menu = screen.getByTestId('nav-user-menu');
      const divider = menu.querySelector('.dropdown-divider');
      expect(divider).toBeInTheDocument();
    });

    it('should apply dropdown-item class to audit log link', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const auditLogLink = screen.getByTestId('nav-audit-log');
      expect(auditLogLink).toHaveClass('dropdown-item');
    });

    it('should apply dropdown-item class to workspaces link', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const workspacesLink = screen.getByTestId('nav-workspaces');
      expect(workspacesLink).toHaveClass('dropdown-item');
    });
  });

  describe('FilterBar Select', () => {
    it('should render morale select without conflicting inline styles', () => {
      render(<FilterBar onFilterChange={jest.fn()} />);
      const select = screen.getByLabelText('Filter by morale status');
      // Global CSS handles styling — no inline backgroundColor/border overrides
      expect(select).not.toHaveStyle({ backgroundColor: 'var(--color-bg-elevated)' });
      expect(select.tagName).toBe('SELECT');
    });

    it('should render tag input with cyberpunk glass styling', () => {
      render(<FilterBar onFilterChange={jest.fn()} />);
      const input = screen.getByLabelText('Filter by tag');
      expect(input).toHaveStyle({ fontFamily: 'var(--font-mono)' });
    });
  });

  describe('WorkspaceSelector', () => {
    const workspaces = [
      { id: '1', name: 'Engineering', description: null, displayOrder: 1, createdAt: '', updatedAt: '' },
      { id: '2', name: 'Design', description: null, displayOrder: 2, createdAt: '', updatedAt: '' },
    ];

    it('should render select element without conflicting inline styles', () => {
      render(
        <WorkspaceSelector
          workspaces={workspaces}
          selectedWorkspaceId={null}
          onWorkspaceChange={jest.fn()}
        />
      );
      const select = screen.getByTestId('workspace-selector');
      // Only minWidth should be set inline — rest handled by global CSS
      expect(select).toHaveStyle({ minWidth: '160px' });
      expect(select.tagName).toBe('SELECT');
    });
  });
});
