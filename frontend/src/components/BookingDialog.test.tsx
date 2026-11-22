import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api, ApiError } from '@/lib/api';
import type { Booking, Room } from '@/lib/types';
import { BookingDialog } from './BookingDialog';

const vauban: Room = {
  id: 'LIL-VAU',
  siteId: 'LIL',
  name: 'Vauban',
  floor: '3e étage',
  seats: 6,
  equipments: [],
  inService: true,
};

const intent = { room: vauban, date: '2026-12-02', start: '10:00', end: '11:00', attendees: 4 };

describe('BookingDialog', () => {
  afterEach(() => vi.restoreAllMocks());

  it('réserve la salle avec l’objet saisi', async () => {
    const book = vi.spyOn(api, 'book').mockResolvedValue({ id: 'b1', roomName: 'Vauban' } as Booking);
    const onBooked = vi.fn();
    render(<BookingDialog intent={intent} onClose={vi.fn()} onBooked={onBooked} />);

    expect(screen.getByRole('button', { name: 'Réserver' })).toBeDisabled();
    await userEvent.type(screen.getByTestId('booking-title'), '  Revue de sprint ');
    await userEvent.click(screen.getByRole('button', { name: 'Réserver' }));

    expect(book).toHaveBeenCalledWith({ roomId: 'LIL-VAU', date: '2026-12-02', start: '10:00', end: '11:00', title: 'Revue de sprint', attendees: 4 });
    expect(onBooked).toHaveBeenCalledWith(expect.objectContaining({ id: 'b1' }));
  });

  it('affiche la règle métier refusée par l’API', async () => {
    vi.spyOn(api, 'book').mockRejectedValue(new ApiError(409, 'La salle Vauban est déjà réservée sur ce créneau.', 'SLOT_TAKEN'));
    render(<BookingDialog intent={intent} onClose={vi.fn()} onBooked={vi.fn()} />);

    await userEvent.type(screen.getByTestId('booking-title'), 'Point');
    await userEvent.click(screen.getByRole('button', { name: 'Réserver' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('déjà réservée');
  });

  it('permet de modifier le créneau depuis le planning', async () => {
    const book = vi.spyOn(api, 'book').mockResolvedValue({ id: 'b2' } as Booking);
    render(<BookingDialog intent={intent} hours={{ opensAt: '08:00', closesAt: '20:00' }} onClose={vi.fn()} onBooked={vi.fn()} />);

    await userEvent.selectOptions(screen.getByTestId('booking-end'), '11:30');
    await userEvent.type(screen.getByTestId('booking-title'), 'Atelier');
    await userEvent.click(screen.getByRole('button', { name: 'Réserver' }));

    expect(book).toHaveBeenCalledWith(expect.objectContaining({ start: '10:00', end: '11:30' }));
    expect(screen.getByText(/1 h 30/)).toBeInTheDocument();
  });
});
