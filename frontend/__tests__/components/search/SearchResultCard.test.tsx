import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import SearchResultCard from '@/components/search/SearchResultCard';
import { SearchResultItem } from '@/types/search';

// Mock next/link
jest.mock('next/link', () => {
  return ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  );
});

describe('SearchResultCard', () => {
  const baseResult: SearchResultItem = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    type: 'PERSON',
    title: 'John Doe',
    snippet: 'Software Engineer at Acme Corp',
    personId: '123e4567-e89b-12d3-a456-426614174000',
    personName: 'John Doe',
    sensitive: false,
    createdAt: '2026-05-10T10:00:00Z',
    relevanceScore: 0.85,
  };

  it('should render result title', () => {
    render(<SearchResultCard result={baseResult} />);
    expect(screen.getByTestId('search-result-title')).toHaveTextContent('John Doe');
  });

  it('should render type badge', () => {
    render(<SearchResultCard result={baseResult} />);
    expect(screen.getByTestId('search-result-type-badge')).toHaveTextContent('Person');
  });

  it('should render snippet for non-sensitive results', () => {
    render(<SearchResultCard result={baseResult} />);
    expect(screen.getByTestId('search-result-snippet')).toHaveTextContent('Software Engineer at Acme Corp');
  });

  it('should not render snippet for sensitive results', () => {
    const sensitiveResult: SearchResultItem = {
      ...baseResult,
      type: 'QUICK_NOTE',
      sensitive: true,
      snippet: 'This should be hidden',
    };
    render(<SearchResultCard result={sensitiveResult} />);
    expect(screen.queryByTestId('search-result-snippet')).not.toBeInTheDocument();
  });

  it('should render sensitive badge for sensitive results', () => {
    const sensitiveResult: SearchResultItem = {
      ...baseResult,
      sensitive: true,
    };
    render(<SearchResultCard result={sensitiveResult} />);
    expect(screen.getByTestId('search-result-sensitive-badge')).toHaveTextContent('Sensitive');
  });

  it('should not render sensitive badge for non-sensitive results', () => {
    render(<SearchResultCard result={baseResult} />);
    expect(screen.queryByTestId('search-result-sensitive-badge')).not.toBeInTheDocument();
  });

  it('should link to person page for PERSON type', () => {
    render(<SearchResultCard result={baseResult} />);
    const link = screen.getByTestId(`search-result-${baseResult.id}`);
    expect(link).toHaveAttribute('href', `/people/${baseResult.id}`);
  });

  it('should link to person page for ACTION_ITEM type', () => {
    const actionItemResult: SearchResultItem = {
      ...baseResult,
      type: 'ACTION_ITEM',
      title: 'Review PR',
      personId: 'person-123',
    };
    render(<SearchResultCard result={actionItemResult} />);
    const link = screen.getByTestId(`search-result-${actionItemResult.id}`);
    expect(link).toHaveAttribute('href', '/people/person-123');
  });

  it('should link to quick-notes page for QUICK_NOTE type', () => {
    const quickNoteResult: SearchResultItem = {
      ...baseResult,
      type: 'QUICK_NOTE',
      title: 'A quick note',
      personId: null,
    };
    render(<SearchResultCard result={quickNoteResult} />);
    const link = screen.getByTestId(`search-result-${quickNoteResult.id}`);
    expect(link).toHaveAttribute('href', '/quick-notes');
  });

  it('should render person name for non-PERSON types', () => {
    const actionItemResult: SearchResultItem = {
      ...baseResult,
      type: 'ACTION_ITEM',
      title: 'Review PR',
      personName: 'Alice Smith',
    };
    render(<SearchResultCard result={actionItemResult} />);
    expect(screen.getByTestId('search-result-person-name')).toHaveTextContent('Alice Smith');
  });

  it('should not render person name for PERSON type', () => {
    render(<SearchResultCard result={baseResult} />);
    expect(screen.queryByTestId('search-result-person-name')).not.toBeInTheDocument();
  });

  it('should render correct type badge for ONE_ON_ONE_ENTRY', () => {
    const entryResult: SearchResultItem = {
      ...baseResult,
      type: 'ONE_ON_ONE_ENTRY',
      title: '1:1 on 2026-05-10',
    };
    render(<SearchResultCard result={entryResult} />);
    expect(screen.getByTestId('search-result-type-badge')).toHaveTextContent('1:1 Entry');
  });

  it('should render correct type badge for PDP_GOAL', () => {
    const goalResult: SearchResultItem = {
      ...baseResult,
      type: 'PDP_GOAL',
      title: 'Leadership development',
    };
    render(<SearchResultCard result={goalResult} />);
    expect(screen.getByTestId('search-result-type-badge')).toHaveTextContent('PDP Goal');
  });

  it('should render correct type badge for KUDOS', () => {
    const kudosResult: SearchResultItem = {
      ...baseResult,
      type: 'KUDOS',
      title: 'Great presentation',
    };
    render(<SearchResultCard result={kudosResult} />);
    expect(screen.getByTestId('search-result-type-badge')).toHaveTextContent('Kudos');
  });

  it('should not render snippet when snippet is null', () => {
    const noSnippetResult: SearchResultItem = {
      ...baseResult,
      snippet: null,
    };
    render(<SearchResultCard result={noSnippetResult} />);
    expect(screen.queryByTestId('search-result-snippet')).not.toBeInTheDocument();
  });

  it('should link to person page for PDP_GOAL type', () => {
    const goalResult: SearchResultItem = {
      ...baseResult,
      type: 'PDP_GOAL',
      personId: 'person-456',
    };
    render(<SearchResultCard result={goalResult} />);
    const link = screen.getByTestId(`search-result-${goalResult.id}`);
    expect(link).toHaveAttribute('href', '/people/person-456');
  });

  it('should link to person page for KUDOS type', () => {
    const kudosResult: SearchResultItem = {
      ...baseResult,
      type: 'KUDOS',
      personId: 'person-789',
    };
    render(<SearchResultCard result={kudosResult} />);
    const link = screen.getByTestId(`search-result-${kudosResult.id}`);
    expect(link).toHaveAttribute('href', '/people/person-789');
  });
});
