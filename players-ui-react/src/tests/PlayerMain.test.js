import { render, screen } from '@testing-library/react';
import PlayerMain from '../components/PlayerMain';

// Mock fetch so components that call the API don't fail
beforeEach(() => {
    global.fetch = jest.fn(() =>
        Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ players: [] }),
            text: () => Promise.resolve(''),
        })
    );
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('renders main header', () => {
    render(<PlayerMain />);
    const element = screen.getByText("Hello Players");
    expect(element).toBeDefined();
});

test('renders search labels', () => {
    render(<PlayerMain />);
    expect(screen.getByText("Player id:")).toBeDefined();
    expect(screen.getByText("Player Country Code:")).toBeDefined();
});

test('renders chat section', () => {
    render(<PlayerMain />);
    expect(screen.getByText("Player Chat")).toBeDefined();
});
