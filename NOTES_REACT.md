# React Frontend Notes

## How It Connects to the Backend

The key line is in `package.json`:
```json
"proxy": "http://127.0.0.1:8080"
```

This tells the React dev server: "any request that isn't a static file, forward it to the Spring Boot backend." So `fetch('/v1/players')` on `localhost:3000` gets proxied to `localhost:8080/v1/players`. No CORS issues, no full URLs needed.

## File Structure

```
src/
  index.js              <-- Entry point, renders PlayerMain
  components/
    PlayerMain.jsx      <-- Shell: header + logo + renders PlayerResults
    PlayersResults.jsx  <-- The actual app: search inputs, player list
  utils/
    DataFetcher.jsx     <-- API call to backend
    index.js            <-- Validation stubs (all return true)
  styling/
    index.css           <-- Global styles
    PlayersMain.css     <-- Component styles
```

## The Flow

**`index.js`** — boots React, renders `<PlayerMain />` into the DOM:
```jsx
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<PlayerMain />);
```

**`PlayerMain.jsx`** — just a shell with header and logo, renders `<PlayerResults />`.

**`PlayersResults.jsx`** — where everything happens:
```jsx
const [players, setPlayers] = useState([]);   // state: array of players

useEffect(() => {
    fetchData()
        .then(data => {
            const subsetOfPlayers = data.players.slice(0,10);
            setPlayers(subsetOfPlayers)
        })
}, []);   // empty array = run once on mount
```

- `useState([])` — creates a state variable `players` (starts empty) and `setPlayers` to update it
- `useEffect(..., [])` — runs once when component first renders. Fetches all players, takes first 10, stores in state
- When `setPlayers` is called, React re-renders the component with the new data

The render section has two search inputs (both stubbed) and maps over the players array:
```jsx
{players.map((player) => (
    <div>
        <div>{player.playerId}</div>
        <div>{player.birthCountry}</div>
    </div>
))}
```

## What's Broken / Stubbed

1. **Search by ID** — `handleSearchById` calls `validateId()` but does nothing inside the `if`
2. **Search by Country** — button's `onClick` is `() => {}` (empty function), doesn't even call `handleSearchByCountry`
3. **Input values aren't captured** — the `<input>` elements have no `value` or `onChange`, so there's no way to read what the user typed
4. **Validation** — all three functions return `true` / pass-through
5. **Unused import** — `useDatatFetcher` is imported but never used (also has a typo)
6. **No key prop** — the `.map()` doesn't have a `key` on the outer div (React warning)
7. **Only shows 2 fields** — `playerId` and `birthCountry` out of 22 available

## DataFetcher.jsx — The API Layer

One function, one endpoint:
```jsx
export default async function fetchData() {
    return fetch('/v1/players')
        .then(response => response.json())
        .then(data => data)
        .catch(error => console.log('oops there was an error', error))
}
```

For interview features, add more:
```jsx
export async function fetchPlayerById(id) {
    return fetch(`/v1/players/${id}`).then(r => r.json());
}

export async function fetchPlayersByCountry(country) {
    return fetch(`/v1/players/search?country=${country}`).then(r => r.json());
}

export async function generateTeam(seedId, teamSize) {
    return fetch(`/v1/team/generate?seedId=${seedId}&teamSize=${teamSize}`).then(r => r.json());
}

export async function sendChat(prompt) {
    return fetch('/v1/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt })
    }).then(r => r.json());
}
```

## Fixing the Search Inputs (Interview Ready)

The inputs need state to capture what the user types:
```jsx
const [searchId, setSearchId] = useState('');
const [searchCountry, setSearchCountry] = useState('');

// Input with controlled value
<input value={searchId} onChange={(e) => setSearchId(e.target.value)} />
<button onClick={() => handleSearchById(searchId)}>Submit</button>

// Handler that actually does something
const handleSearchById = (id) => {
    if (validateId(id)) {
        fetchPlayerById(id)
            .then(data => setPlayers([data]))
            .catch(() => setPlayers([]));
    }
}
```

## React Concepts Quick Reference

**`useState`** — creates reactive state. When you call the setter, React re-renders:
```jsx
const [count, setCount] = useState(0);    // initial value = 0
setCount(5);                                // triggers re-render
```

**`useEffect`** — runs side effects (API calls, timers, etc.):
```jsx
useEffect(() => { /* runs once on mount */ }, []);
useEffect(() => { /* runs when 'query' changes */ }, [query]);
useEffect(() => { /* runs every render */ });  // no array = every render (avoid)
```

**Controlled inputs** — React manages the input value via state:
```jsx
const [name, setName] = useState('');
<input value={name} onChange={(e) => setName(e.target.value)} />
// 'name' always reflects what's in the input
```

**Conditional rendering:**
```jsx
{loading && <p>Loading...</p>}
{error && <p>Error: {error}</p>}
{players.length === 0 && <p>No results found</p>}
```

**Lists need keys** — React uses `key` to track which items changed:
```jsx
{players.map((player) => (
    <div key={player.playerId}>    // unique key per item
        {player.firstName} {player.lastName}
    </div>
))}
```

**JSX gotchas:**
- `className` instead of `class`
- `{}` for JavaScript expressions: `<div>{player.firstName}</div>`
- Self-closing tags required: `<img />`, `<input />`
- Style objects use camelCase: `style={{backgroundColor: 'red'}}`

## Pagination (Full Stack)

### Backend — Spring Data does it for free

No custom SQL or `@Query` needed. `JpaRepository` already supports `Pageable`:

```java
// Controller — accept page and size as query params
@GetMapping
public ResponseEntity<Page<Player>> getPlayers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(playerService.getPlayers(page, size));
}

// Service
public Page<Player> getPlayers(int page, int size) {
    return playerRepository.findAll(PageRequest.of(page, size));
}
```

Or even simpler — Spring auto-parses `Pageable` from query params:
```java
@GetMapping
public ResponseEntity<Page<Player>> getPlayers(Pageable pageable) {
    return ResponseEntity.ok(playerRepository.findAll(pageable));
}
// GET /v1/players?page=0&size=20&sort=lastName,asc
```

`Page<Player>` returns:
```json
{
    "content": [ ... 20 players ... ],
    "totalElements": 19000,
    "totalPages": 950,
    "number": 0,
    "size": 20
}
```

`totalPages` is calculated automatically from `totalElements / size`.

Pagination also works with custom query methods — just add `Pageable` parameter:
```java
Page<Player> findByBirthCountry(String country, Pageable pageable);
// Usage: playerRepository.findByBirthCountry("USA", PageRequest.of(0, 20));
```

With sorting:
```java
PageRequest.of(0, 20, Sort.by("lastName").ascending());
```

### Frontend — combo box with page selection

```jsx
const [players, setPlayers] = useState([]);
const [page, setPage] = useState(0);
const [totalPages, setTotalPages] = useState(0);

useEffect(() => {
    fetch(`/v1/players?page=${page}&size=20`)
        .then(r => r.json())
        .then(data => {
            setPlayers(data.content);
            setTotalPages(data.totalPages);
        });
}, [page]);  // re-fetches when page changes

// Combo box
<select value={page} onChange={(e) => setPage(Number(e.target.value))}>
    {Array.from({ length: totalPages }, (_, i) => (
        <option key={i} value={i}>Page {i + 1}</option>
    ))}
</select>
```

`useEffect` depends on `[page]` — when user changes the combo box, `setPage` fires, effect re-runs, fetches new page, re-renders.

**React 18 batching:** `setPlayers` and `setTotalPages` in the same `.then()` only triggers **one** re-render, not two. React 18 batches all state updates in the same callback.

## Custom Hooks — Reusable Fetch Logic

Custom hooks extract repeated patterns (loading, error, fetching) out of components. They can contain `useState`, `useEffect`, etc. inside them. Must start with `use` — tells React "this function contains hooks."

### Approach 1: Return `execute` — imperative

```jsx
function useFetch(initialUrl) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(!!initialUrl);
    const [error, setError] = useState(null);

    const execute = (url) => {
        setLoading(true);
        setError(null);
        fetch(url)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => setData(data))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        if (initialUrl) execute(initialUrl);
    }, []);

    return { data, loading, error, execute };
}

// Component — no useEffect, no .then() chains
function PlayerResults() {
    const { data, loading, error, execute } = useFetch('/v1/players?page=0&size=20');

    const handleSearchById = (id) => execute(`/v1/players?id=${id}`);
    const handleSearchByCountry = (country) => execute(`/v1/players?country=${country}`);
    const handlePageChange = (page) => execute(`/v1/players?page=${page}&size=20`);

    if (loading) return <p>Loading...</p>;
    if (error) return <p>Error: {error}</p>;
    // render data...
}
```

Component explicitly calls `execute` when it wants to fetch. Returns `execute` so user interactions (search, pagination) can trigger new fetches.

### Approach 2: React to URL state — declarative (more "React-y")

```jsx
function useFetch(url) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        setError(null);
        fetch(url)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => setData(data))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    }, [url]);  // re-fetches whenever url changes

    return { data, loading, error };  // no execute needed
}

// Component — state change drives everything
function PlayerResults() {
    const [url, setUrl] = useState('/v1/players?page=0&size=20');
    const { data, loading, error } = useFetch(url);

    // Search button changes URL state -> re-render ->
    // hook sees url changed -> useEffect re-runs -> new fetch
    const handleSearchById = (id) => setUrl(`/v1/players?id=${id}`);
    const handlePageChange = (page) => setUrl(`/v1/players?page=${page}&size=20`);

    if (loading) return <p>Loading...</p>;
    if (error) return <p>Error: {error}</p>;
    // render data...
}
```

No imperative "go fetch now" call. State changes drive fetching automatically.

### Domain-specific hook

Can also make a hook specific to player fetching:

```jsx
function usePlayerFetch() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchPlayers = (params = {}) => {
        setLoading(true);
        setError(null);
        const query = new URLSearchParams(params).toString();
        fetch(`/v1/players?${query}`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => setData(data))
            .catch(err => setError(err.message))
            .finally(() => setLoading(false));
    };

    return { data, loading, error, fetchPlayers };
}

// Usage — clean and descriptive
const { data, loading, error, fetchPlayers } = usePlayerFetch();
fetchPlayers({ page: 0, size: 20 });
fetchPlayers({ id: 'aaronha01' });
fetchPlayers({ country: 'USA', page: 2, size: 20 });
```

### Why custom hooks?
- **Reusable** — same loading/error pattern everywhere
- **Encapsulated** — component doesn't deal with fetch plumbing
- **Testable** — mock the hook in tests
- **Composable** — use multiple hooks in one component:
```jsx
const players = useFetch('/v1/players?page=0&size=20');
const team = useFetch('/v1/team/generate?seedId=aaronha01&teamSize=5');
// each has its own data, loading, error state
```

## Component Patterns for Interview

**Loading state:**
```jsx
const [loading, setLoading] = useState(false);

const handleSearch = (id) => {
    setLoading(true);
    fetchPlayerById(id)
        .then(data => setPlayers([data]))
        .finally(() => setLoading(false));
}

// In JSX:
{loading ? <p>Loading...</p> : <PlayerList players={players} />}
```

**Extracting a reusable component:**
```jsx
// PlayerCard.jsx
function PlayerCard({ player }) {
    return (
        <div className="player-card">
            <h3>{player.firstName} {player.lastName}</h3>
            <p>Born: {player.birthYear}, {player.birthCity}, {player.birthCountry}</p>
            <p>Debut: {player.debut}</p>
        </div>
    );
}

// Usage in PlayersResults.jsx
{players.map((player) => (
    <PlayerCard key={player.playerId} player={player} />
))}
```

**Chat component (with message history):**

The actual `Chat.jsx` keeps a full conversation history instead of just one response. Key patterns:

```jsx
function Chat() {
    const [prompt, setPrompt] = useState('');
    const [messages, setMessages] = useState([]);   // array of {role, text}
    const [loading, setLoading] = useState(false);

    const handleSend = () => {
        if (!prompt.trim()) return;

        const userMessage = prompt;
        setMessages(prev => [...prev, { role: 'user', text: userMessage }]);
        setPrompt('');          // clear input immediately
        setLoading(true);

        fetch('/v1/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt: userMessage })
        })
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.text();    // backend returns plain text, not JSON
            })
            .then(response => {
                setMessages(prev => [...prev, { role: 'assistant', text: response }]);
            })
            .catch(err => {
                setMessages(prev => [...prev, { role: 'assistant', text: `Error: ${err.message}` }]);
            })
            .finally(() => setLoading(false));
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !loading) handleSend();
    };
    // ... render with messages.map(), loading indicator, input + button
}
```

Key things to notice:
- **`r.text()` not `r.json()`** — the backend returns a plain string, not JSON
- **Append pattern** — `setMessages(prev => [...prev, newMsg])` uses the functional form of setState to safely append
- **Optimistic UI** — user message appears instantly, input clears, then we wait for the response
- **Enter key** — `onKeyDown` handler checks for Enter key to submit
- **Error as message** — catch block adds error as an assistant message so user sees it in the chat

## Plain LLM vs RAG

**Current implementation: Plain LLM** — the user prompt goes straight to tinyllama with a system prompt ("You are a baseball expert"). The model answers from its training data only.

**RAG (Retrieval-Augmented Generation)** — interview likely asks for this:
1. User asks about a player (e.g., "Tell me about Hank Aaron")
2. Backend queries the database for that player's stats
3. Backend builds a prompt: system instructions + player data from DB + user question
4. LLM generates a response grounded in real data

The difference: plain LLM can hallucinate stats. RAG grounds the response in actual database records. The backend `ChatClientService.chat()` method would need to:
- Parse the user's question to identify a player
- Query `PlayerRepository` for that player's data
- Inject the data into the prompt via `PromptBuilder`

This is a natural interview extension — combines JPA, REST, and the Ollama integration.
