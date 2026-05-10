import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Navigation from '@/components/Navigation';
import PersonCard from '@/components/PersonCard';
import EmptyState from '@/components/EmptyState';
import { Person } from '@/types/person';

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

describe('Cyberpunk Theme Design Tokens', () => {
  describe('Navigation', () => {
    it('should use dark surface background', () => {
      render(<Navigation />);
      const nav = screen.getByTestId('navigation');
      expect(nav).toHaveStyle({ backgroundColor: 'var(--color-bg-surface)' });
    });

    it('should use primary color for brand text', () => {
      render(<Navigation />);
      const brand = screen.getByTestId('nav-brand');
      expect(brand).toHaveStyle({ color: 'var(--color-primary)' });
    });

    it('should use monospace font for brand', () => {
      render(<Navigation />);
      const brand = screen.getByTestId('nav-brand');
      expect(brand).toHaveStyle({ fontFamily: 'var(--font-heading)' });
    });

    it('should use monospace font for user name', () => {
      render(<Navigation />);
      const userName = screen.getByTestId('nav-user-name');
      expect(userName).toHaveStyle({ fontFamily: 'var(--font-mono)' });
    });

    it('should use alert color for sign out button in user menu', () => {
      render(<Navigation />);
      fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
      const signOutBtn = screen.getByTestId('nav-signout');
      expect(signOutBtn).toHaveStyle({ color: 'var(--color-alert)' });
    });
  });

  describe('PersonCard', () => {
    const mockPerson: Person = {
      id: '1',
      name: 'Jane Doe',
      preferredName: null,
      roleTitle: 'Engineer',
      timezone: null,
      startDate: null,
      email: null,
      tags: [],
      moraleStatus: 'GREEN',
      moraleNote: null,
      pinnedRememberItems: [],
      atAGlance: { last1on1Date: null, openActionItemsCount: 0, activePdpGoalsSummary: null },
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01',
    };

    it('should use dark surface background for cards', () => {
      render(<PersonCard person={mockPerson} />);
      const card = screen.getByTestId('person-card');
      expect(card).toHaveStyle({ backgroundColor: 'var(--color-bg-surface)' });
    });

    it('should use dark border color', () => {
      render(<PersonCard person={mockPerson} />);
      const card = screen.getByTestId('person-card');
      expect(card).toHaveStyle({ border: '1px solid var(--color-border)' });
    });

    it('should use heading font for person name', () => {
      render(<PersonCard person={mockPerson} />);
      const name = screen.getByText('Jane Doe');
      expect(name).toHaveStyle({ fontFamily: 'var(--font-heading)' });
    });

    it('should use primary text color for person name', () => {
      render(<PersonCard person={mockPerson} />);
      const name = screen.getByText('Jane Doe');
      expect(name).toHaveStyle({ color: 'var(--color-text-primary)' });
    });

    it('should use secondary text color for role title', () => {
      render(<PersonCard person={mockPerson} />);
      const role = screen.getByText('Engineer');
      expect(role).toHaveStyle({ color: 'var(--color-text-secondary)' });
    });
  });

  describe('EmptyState', () => {
    it('should use dark surface background', () => {
      render(<EmptyState />);
      const empty = screen.getByTestId('empty-state');
      expect(empty).toHaveStyle({ backgroundColor: 'var(--color-bg-surface)' });
    });

    it('should use primary color for CTA button with glow', () => {
      render(<EmptyState onAction={() => {}} />);
      const cta = screen.getByTestId('empty-state-cta');
      expect(cta).toHaveStyle({ backgroundColor: 'var(--color-primary)' });
      expect(cta).toHaveStyle({ color: 'var(--color-bg-base)' });
      expect(cta).toHaveStyle({ boxShadow: 'var(--glow-primary)' });
    });

    it('should use monospace font for CTA button', () => {
      render(<EmptyState onAction={() => {}} />);
      const cta = screen.getByTestId('empty-state-cta');
      expect(cta).toHaveStyle({ fontFamily: 'var(--font-mono)' });
    });
  });
});
