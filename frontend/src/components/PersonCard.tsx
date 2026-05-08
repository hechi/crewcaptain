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
        border: '1px solid var(--color-neutral-border)',
        borderRadius: 'var(--radius-medium)',
        textDecoration: 'none',
        color: 'inherit',
        cursor: 'pointer',
        backgroundColor: 'var(--color-neutral-surface)',
        boxShadow: 'var(--shadow-sm)',
        transition: 'box-shadow var(--transition-normal), border-color var(--transition-normal), transform var(--transition-normal)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ margin: 0, fontSize: '16px', fontFamily: 'var(--font-heading)', fontWeight: 'var(--weight-semibold)', color: 'var(--color-primary)' }}>{person.name}</h3>
          {person.roleTitle && (
            <p style={{ margin: '4px 0 0', fontSize: 'var(--text-body)', color: 'var(--color-neutral-text-muted)' }}>
              {person.roleTitle}
            </p>
          )}
        </div>
        <MoraleIndicator moraleStatus={person.moraleStatus} />
      </div>
    </Link>
  );
}
