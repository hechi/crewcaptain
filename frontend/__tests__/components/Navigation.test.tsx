import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Navigation from '@/components/Navigation';

jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
});

jest.mock('next-auth/react', () => ({
  useSession: jest.fn(),
  signOut: jest.fn(),
}));

import { useSession, signOut } from 'next-auth/react';

const mockUseSession = useSession as jest.MockedFunction<typeof useSession>;
const mockSignOut = signOut as jest.MockedFunction<typeof signOut>;

describe('Navigation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not render when unauthenticated', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'unauthenticated',
      update: jest.fn(),
    });

    const { container } = render(<Navigation />);
    expect(container.innerHTML).toBe('');
  });

  it('should not render when loading', () => {
    mockUseSession.mockReturnValue({
      data: null,
      status: 'loading',
      update: jest.fn(),
    });

    const { container } = render(<Navigation />);
    expect(container.innerHTML).toBe('');
  });

  it('should render navigation when authenticated', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByTestId('navigation')).toBeInTheDocument();
  });

  it('should display brand name with link to people', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const brandLink = screen.getByTestId('nav-brand');
    expect(brandLink).toHaveTextContent('CrewCaptain');
    expect(brandLink).toHaveAttribute('href', '/people');
  });

  it('should display People nav link', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const peopleLink = screen.getByTestId('nav-people');
    expect(peopleLink).toHaveTextContent('People');
    expect(peopleLink).toHaveAttribute('href', '/people');
  });

  it('should display Dashboard nav link', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const dashboardLink = screen.getByTestId('nav-dashboard');
    expect(dashboardLink).toHaveTextContent('Dashboard');
    expect(dashboardLink).toHaveAttribute('href', '/dashboard');
  });

  it('should display user name in the menu trigger', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Jane Manager' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByTestId('nav-user-name')).toHaveTextContent('Jane Manager');
  });

  it('should show fallback name when user name is not available', () => {
    mockUseSession.mockReturnValue({
      data: { user: {}, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByTestId('nav-user-name')).toHaveTextContent('User');
  });

  it('should not show user menu dropdown by default', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.queryByTestId('nav-user-menu')).not.toBeInTheDocument();
  });

  it('should open user menu dropdown when trigger is clicked', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.getByTestId('nav-user-menu')).toBeInTheDocument();
  });

  it('should close user menu dropdown when trigger is clicked again', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.getByTestId('nav-user-menu')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.queryByTestId('nav-user-menu')).not.toBeInTheDocument();
  });

  it('should display Settings link in user menu', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));

    const settingsLink = screen.getByTestId('nav-settings');
    expect(settingsLink).toHaveTextContent('Settings');
    expect(settingsLink).toHaveAttribute('href', '/settings');
  });

  it('should display My Notes link in user menu', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));

    const myNotesLink = screen.getByTestId('nav-my-notes');
    expect(myNotesLink).toHaveTextContent('My Notes');
    expect(myNotesLink).toHaveAttribute('href', '/my-notes');
  });

  it('should display Sign out button in user menu', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));

    expect(screen.getByTestId('nav-signout')).toHaveTextContent('Sign out');
  });

  it('should call signOut when sign out is clicked in menu', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    fireEvent.click(screen.getByTestId('nav-signout'));
    expect(mockSignOut).toHaveBeenCalledWith({ callbackUrl: '/' });
  });

  it('should close menu when clicking outside', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.getByTestId('nav-user-menu')).toBeInTheDocument();

    fireEvent.mouseDown(document.body);
    expect(screen.queryByTestId('nav-user-menu')).not.toBeInTheDocument();
  });

  it('should close menu on Escape key', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.getByTestId('nav-user-menu')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByTestId('nav-user-menu')).not.toBeInTheDocument();
  });

  it('should have proper aria-label for accessibility', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    expect(screen.getByLabelText('Main navigation')).toBeInTheDocument();
  });

  it('should have aria-expanded on menu trigger', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    const trigger = screen.getByTestId('nav-user-menu-trigger');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
  });

  it('should have role=menu on the dropdown', () => {
    mockUseSession.mockReturnValue({
      data: { user: { name: 'Test User' }, expires: '', accessToken: 'test-token' },
      status: 'authenticated',
      update: jest.fn(),
    });

    render(<Navigation />);
    fireEvent.click(screen.getByTestId('nav-user-menu-trigger'));
    expect(screen.getByTestId('nav-user-menu')).toHaveAttribute('role', 'menu');
  });
});
