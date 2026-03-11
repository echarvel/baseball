import { render, screen, waitFor } from '@testing-library/react';
import PlayerResults from '../components/PlayersResults';

const mockPlayers = [
    { playerId: 'aaronha01', birthCountry: 'USA' },
    { playerId: 'ruthba01', birthCountry: 'USA' },
    { playerId: 'suzukic01', birthCountry: 'Japan' },
];

beforeEach(() => {
    global.fetch = jest.fn(() =>
        Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ players: mockPlayers }),
        })
    );
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('renders search inputs and submit buttons', () => {
    render(<PlayerResults />);
    expect(screen.getByText("Player id:")).toBeDefined();
    expect(screen.getByText("Player Country Code:")).toBeDefined();
    expect(screen.getAllByText("Submit").length).toBe(2);
});

test('fetches and displays players on mount', async () => {
    render(<PlayerResults />);

    await waitFor(() => {
        expect(screen.getByText("aaronha01")).toBeDefined();
        expect(screen.getByText("ruthba01")).toBeDefined();
        expect(screen.getByText("suzukic01")).toBeDefined();
    });

    expect(global.fetch).toHaveBeenCalledWith('/v1/players');
});

test('displays birth country for each player', async () => {
    render(<PlayerResults />);

    await waitFor(() => {
        expect(screen.getByText("Japan")).toBeDefined();
        // USA appears multiple times
        expect(screen.getAllByText("USA").length).toBe(2);
    });
});

test('handles empty player list', async () => {
    global.fetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ players: [] }),
    });

    render(<PlayerResults />);

    // Should render without errors, no player rows
    await waitFor(() => {
        expect(global.fetch).toHaveBeenCalled();
    });

    expect(screen.queryByText("aaronha01")).toBeNull();
});
