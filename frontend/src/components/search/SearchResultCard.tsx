'use client';

import Link from 'next/link';
import { SearchResultItem, SearchResultType } from '@/types/search';

interface SearchResultCardProps {
  result: SearchResultItem;
}

const TYPE_LABELS: Record<SearchResultType, string> = {
  PERSON: 'Person',
  ONE_ON_ONE_ENTRY: '1:1 Entry',
  QUICK_NOTE: 'Quick Note',
  ACTION_ITEM: 'Action Item',
  PDP_GOAL: 'PDP Goal',
  PDP_UPDATE: 'PDP Update',
  KUDOS: 'Kudos',
};

const TYPE_COLORS: Record<SearchResultType, string> = {
  PERSON: 'var(--color-primary)',
  ONE_ON_ONE_ENTRY: 'var(--color-secondary)',
  QUICK_NOTE: 'var(--color-warning)',
  ACTION_ITEM: 'var(--color-alert)',
  PDP_GOAL: '#10b981',
  PDP_UPDATE: '#10b981',
  KUDOS: '#f59e0b',
};

function getResultLink(result: SearchResultItem): string {
  switch (result.type) {
    case 'PERSON':
      return `/people/${result.id}`;
    case 'ONE_ON_ONE_ENTRY':
      return result.personId ? `/people/${result.personId}/one-on-ones/${result.id}` : '#';
    case 'QUICK_NOTE':
      return '/quick-notes';
    case 'ACTION_ITEM':
      return result.personId ? `/people/${result.personId}?tab=action-items` : '#';
    case 'PDP_GOAL':
    case 'PDP_UPDATE':
      return result.personId ? `/people/${result.personId}?tab=pdp-goals` : '#';
    case 'KUDOS':
      return result.personId ? `/people/${result.personId}?tab=kudos` : '#';
    default:
      return '#';
  }
}

export default function SearchResultCard({ result }: SearchResultCardProps) {
  const link = getResultLink(result);
  const typeColor = TYPE_COLORS[result.type] || 'var(--color-text-muted)';

  return (
    <Link
      href={link}
      data-testid={`search-result-${result.id}`}
      style={{
        display: 'block',
        padding: 'var(--space-4)',
        borderRadius: 'var(--radius-medium)',
        border: '1px solid var(--color-border)',
        backgroundColor: 'var(--color-bg-elevated)',
        textDecoration: 'none',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
        <span
          data-testid="search-result-type-badge"
          style={{
            fontSize: '11px',
            fontWeight: 600,
            padding: '2px 8px',
            borderRadius: '10px',
            backgroundColor: `color-mix(in srgb, ${typeColor} 15%, transparent)`,
            color: typeColor,
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
          }}
        >
          {TYPE_LABELS[result.type]}
        </span>
        {result.sensitive && (
          <span
            data-testid="search-result-sensitive-badge"
            style={{
              fontSize: '11px',
              fontWeight: 600,
              padding: '2px 8px',
              borderRadius: '10px',
              backgroundColor: 'rgba(255, 107, 107, 0.15)',
              color: 'var(--color-alert)',
            }}
          >
            Sensitive
          </span>
        )}
        {result.personName && result.type !== 'PERSON' && (
          <span
            data-testid="search-result-person-name"
            style={{
              fontSize: '12px',
              color: 'var(--color-text-muted)',
              marginLeft: 'auto',
            }}
          >
            {result.personName}
          </span>
        )}
      </div>
      <h3
        data-testid="search-result-title"
        style={{
          fontSize: '14px',
          fontWeight: 600,
          color: 'var(--color-text-primary)',
          margin: '0 0 4px 0',
          lineHeight: 1.4,
        }}
      >
        {result.title}
      </h3>
      {result.snippet && !result.sensitive && (
        <p
          data-testid="search-result-snippet"
          style={{
            fontSize: '13px',
            color: 'var(--color-text-secondary)',
            margin: 0,
            lineHeight: 1.5,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
          }}
        >
          {result.snippet}
        </p>
      )}
    </Link>
  );
}
