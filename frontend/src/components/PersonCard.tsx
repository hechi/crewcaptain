'use client';

import Link from 'next/link';
import { Person } from '@/types/person';
import MoraleIndicator from './MoraleIndicator';

interface PersonCardProps {
  person: Person;
}

export default function PersonCard({ person }: PersonCardProps) {
  return (
    <Link
      href={`/people/${person.id}`}
      data-testid="person-card"
      style={{
        display: 'block',
        padding: 'var(--space-4)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-medium)',
        textDecoration: 'none',
        color: 'inherit',
        cursor: 'pointer',
        backgroundColor: 'var(--color-bg-surface)',
        backdropFilter: 'var(--glass-blur)',
        transition: 'border-color 0.2s, box-shadow 0.2s',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{
            margin: 0,
            fontSize: '16px',
            fontWeight: 'var(--weight-semibold)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
            letterSpacing: '-0.2px',
          }}>
            {person.name}
          </h3>
          {person.roleTitle && (
            <p style={{
              margin: '4px 0 0',
              fontSize: 'var(--text-body)',
              color: 'var(--color-text-secondary)',
            }}>
              {person.roleTitle}
            </p>
          )}
        </div>
        <MoraleIndicator moraleStatus={person.moraleStatus} />
      </div>
    </Link>
  );
}
