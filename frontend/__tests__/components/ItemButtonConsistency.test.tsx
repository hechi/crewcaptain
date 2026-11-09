import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MyNoteCard from '@/components/quick-notes/MyNoteCard';
import ActionItemCard from '@/components/action-items/ActionItemCard';
import { itemButtonStyle } from '@/components/ui/itemButtonStyle';
import { QuickNote } from '@/types/quick-note';
import { ActionItem } from '@/types/action-item';

const note: QuickNote = {
  id: 'n1', personId: null, text: 'x', sensitive: false, selfAssigned: true, status: 'INBOX',
  attachedEntryId: null, tag: null, displayOrder: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
};
const item: ActionItem = {
  id: 'a1', personId: null, title: 'y', description: null, ownerType: 'MANAGER', dueDate: null, status: 'OPEN',
  originatingEntryId: null, originatingQuickNoteId: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
};

const SIZE_PROPS = [
  'padding', 'fontSize', 'fontFamily', 'borderRadius', 'height', 'lineHeight', 'boxSizing', 'display', 'whiteSpace',
] as const;

function sizeOf(el: HTMLElement) {
  return SIZE_PROPS.map((p) => el.style[p]);
}

describe('item button style consistency (notes, my-notes, action items)', () => {
  it('uses identical size/shape for every button on both cards', () => {
    render(
      <>
        <MyNoteCard
          note={note}
          onComplete={jest.fn()}
          onConvert={jest.fn()}
          onArchive={jest.fn()}
          onEdit={jest.fn()}
          onDelete={jest.fn()}
          onMove={jest.fn()}
        />
        <ActionItemCard
          item={item}
          onComplete={jest.fn()}
          onCancel={jest.fn()}
          onEdit={jest.fn()}
          onDelete={jest.fn()}
        />
      </>
    );
    const ids = [
      'my-note-move-up-btn', 'my-note-move-down-btn', 'my-note-due-date-input',
      'my-note-complete-btn', 'my-note-convert-btn', 'my-note-archive-btn', 'my-note-edit-btn', 'my-note-delete-btn',
      'action-item-complete-btn', 'action-item-cancel-btn', 'action-item-edit-btn', 'action-item-delete-btn',
    ];
    const expected = sizeOf(screen.getByTestId(ids[0]));
    expect(expected.every(Boolean)).toBe(true);
    ids.forEach((id) => expect(sizeOf(screen.getByTestId(id))).toEqual(expected));
  });

  it('exposes distinct colors per variant but the same size', () => {
    const a = itemButtonStyle('success');
    const b = itemButtonStyle('danger');
    expect(a.padding).toBe(b.padding);
    expect(a.borderRadius).toBe(b.borderRadius);
    expect(a.color).not.toBe(b.color);
  });
});
