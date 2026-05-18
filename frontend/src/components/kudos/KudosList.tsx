'use client';

import { useState } from 'react';
import { Kudos, CreateKudosRequest } from '@/types/kudos';
import KudosCard from './KudosCard';
import KudosForm from './KudosForm';
import EmptyState from '@/components/EmptyState';

interface KudosListProps {
  kudos: Kudos[];
  onCreateKudos: (data: CreateKudosRequest) => void;
  onDeleteKudos: (kudosId: string) => void;
  isSubmitting?: boolean;
  aiEnabled?: boolean;
}

/**
 * Displays a list of kudos entries with an inline create form.
 */
export default function KudosList({
  kudos,
  onCreateKudos,
  onDeleteKudos,
  isSubmitting = false,
  aiEnabled = false,
}: KudosListProps) {
  const [showCreateForm, setShowCreateForm] = useState(false);

  const handleCreate = (data: CreateKudosRequest) => {
    onCreateKudos(data);
    setShowCreateForm(false);
  };

  return (
    <div data-testid="kudos-list">
      {/* Header with create button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        {!showCreateForm ? (
          <button
            type="button"
            onClick={() => setShowCreateForm(true)}
            data-testid="kudos-create-btn"
            style={{
              padding: '10px 20px',
              backgroundColor: 'var(--color-primary)',
              color: 'var(--color-bg-base)',
              border: 'none',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              fontWeight: 'var(--weight-semibold)',
              fontFamily: 'var(--font-mono)',
              cursor: 'pointer',
              boxShadow: 'var(--glow-primary)',
            }}
          >
            + Give Kudos
          </button>
        ) : (
          <div />
        )}
      </div>

      {/* Create form */}
      {showCreateForm && (
        <div style={{ marginBottom: '16px' }}>
          <KudosForm
            onSubmit={handleCreate}
            onCancel={() => setShowCreateForm(false)}
            isSubmitting={isSubmitting}
            aiEnabled={aiEnabled}
          />
        </div>
      )}

      {/* Kudos list */}
      {kudos.length === 0 && !showCreateForm ? (
        <EmptyState
          message="No kudos yet — click '+ Give Kudos' to recognize this person's achievements."
        />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {kudos.map((item) => (
            <KudosCard
              key={item.id}
              kudos={item}
              onDelete={onDeleteKudos}
            />
          ))}
        </div>
      )}
    </div>
  );
}
