import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Chat from '../components/Chat';

beforeEach(() => {
    global.fetch = jest.fn();
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('renders chat input and send button', () => {
    render(<Chat />);
    expect(screen.getByPlaceholderText("Ask about a player...")).toBeDefined();
    expect(screen.getByText("Send")).toBeDefined();
});

test('renders placeholder message when no messages', () => {
    render(<Chat />);
    expect(screen.getByText("Ask me about a player...")).toBeDefined();
});

test('send button is disabled when input is empty', () => {
    render(<Chat />);
    const button = screen.getByText("Send");
    expect(button.disabled).toBe(true);
});

test('send button is enabled when input has text', () => {
    render(<Chat />);
    const input = screen.getByPlaceholderText("Ask about a player...");
    fireEvent.change(input, { target: { value: 'Who is Babe Ruth?' } });
    const button = screen.getByText("Send");
    expect(button.disabled).toBe(false);
});

test('sends message and displays user bubble', async () => {
    global.fetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve('Babe Ruth was a legendary player.'),
    });

    render(<Chat />);
    const input = screen.getByPlaceholderText("Ask about a player...");
    fireEvent.change(input, { target: { value: 'Who is Babe Ruth?' } });
    fireEvent.click(screen.getByText("Send"));

    // User message appears immediately
    expect(screen.getByText("Who is Babe Ruth?")).toBeDefined();

    // Input clears after send
    expect(input.value).toBe('');

    // Loading indicator appears
    expect(screen.getByText("Thinking...")).toBeDefined();

    // Assistant response appears after fetch resolves
    await waitFor(() => {
        expect(screen.getByText("Babe Ruth was a legendary player.")).toBeDefined();
    });
});

test('displays error message when fetch fails', async () => {
    global.fetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
    });

    render(<Chat />);
    const input = screen.getByPlaceholderText("Ask about a player...");
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.click(screen.getByText("Send"));

    await waitFor(() => {
        expect(screen.getByText("Error: HTTP 500")).toBeDefined();
    });
});

test('Enter key sends message', async () => {
    global.fetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve('response'),
    });

    render(<Chat />);
    const input = screen.getByPlaceholderText("Ask about a player...");
    fireEvent.change(input, { target: { value: 'hello' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(screen.getByText("hello")).toBeDefined();
});

test('input and button are disabled while loading', async () => {
    // Never-resolving promise to keep loading state
    global.fetch.mockReturnValueOnce(new Promise(() => {}));

    render(<Chat />);
    const input = screen.getByPlaceholderText("Ask about a player...");
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.click(screen.getByText("Send"));

    expect(input.disabled).toBe(true);
    expect(screen.getByText("Send").disabled).toBe(true);
});
