import { fireEvent, render, screen } from '@testing-library/react';
import type { Planning } from '@/lib/types';
import { Timeline } from './Timeline';

const planning: Planning = {
  site: { id: 'LIL', name: 'Lille', city: 'Lille', opensAt: '08:00', closesAt: '20:00' },
  date: '2026-12-02',
  open: true,
  rooms: [
    {
      room: { id: 'LIL-VAU', siteId: 'LIL', name: 'Vauban', seats: 6, equipments: [], inService: true },
      bookings: [
        { id: '1', title: 'Revue de sprint', organizerName: 'Mckevin Kamguem', mine: true, start: '10:00', end: '11:00', status: 'CONFIRMED' },
        { id: '2', title: 'Comité', organizerName: 'Julie Dumont', mine: false, start: '14:00', end: '15:00', status: 'CHECKED_IN' },
      ],
    },
    {
      room: { id: 'LIL-BEF', siteId: 'LIL', name: 'Beffroi', seats: 8, equipments: [], inService: false, outOfServiceReason: 'Dégât des eaux' },
      bookings: [],
    },
  ],
};

function clickAt(element: HTMLElement, ratio: number) {
  element.getBoundingClientRect = () => ({ left: 0, width: 1200, top: 0, height: 64, right: 1200, bottom: 64, x: 0, y: 0, toJSON: () => ({}) });
  fireEvent.click(element, { clientX: ratio * 1200 });
}

describe('Timeline', () => {
  it('place chaque réservation sur la frise de la journée', () => {
    render(<Timeline planning={planning} onSlotClick={vi.fn()} />);
    const blocks = screen.getAllByTestId('planned-booking');
    expect(blocks[0]).toHaveStyle({ left: '16.666666666666664%' });
    expect(blocks[0]).toHaveTextContent('Vous');
    expect(blocks[1]).toHaveTextContent('Julie Dumont');
    expect(screen.getByText('Hors service : Dégât des eaux')).toBeInTheDocument();
  });

  it('propose de réserver le quart d’heure cliqué', () => {
    const onSlotClick = vi.fn();
    render(<Timeline planning={planning} onSlotClick={onSlotClick} />);
    const track = screen.getByTestId('row-LIL-VAU').children[1] as HTMLElement;

    clickAt(track, 0.75);

    expect(onSlotClick).toHaveBeenCalledWith(expect.objectContaining({ id: 'LIL-VAU' }), '17:00');
  });

  it('ignore les clics dans le passé et sur une salle hors service', () => {
    const onSlotClick = vi.fn();
    render(<Timeline planning={planning} now="12:00" onSlotClick={onSlotClick} />);

    clickAt(screen.getByTestId('row-LIL-VAU').children[1] as HTMLElement, 0.1);
    clickAt(screen.getByTestId('row-LIL-BEF').children[1] as HTMLElement, 0.9);

    expect(onSlotClick).not.toHaveBeenCalled();
  });
});
