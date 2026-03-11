# Testing Notes

## Spring Boot Test Stack

`spring-boot-starter-test` pulls in everything:
- **JUnit 5** (Jupiter) — test framework (`@Test`, assertions)
- **Mockito** — mocking (`@Mock`, `when()`, `verify()`)
- **Spring Test** — `@SpringBootTest`, `@MockBean`, `MockMvc`
- **AssertJ** — fluent assertions (`assertThat(x).isEqualTo(y)`)

## Three Levels of Spring Tests

### 1. Unit Test — no Spring, fastest

```java
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {
    @Mock PlayerRepository playerRepository;
    @InjectMocks PlayerService playerService;

    @Test
    void findById_returnsPlayer() {
        when(playerRepository.findById("aaronha01")).thenReturn(Optional.of(mockPlayer));
        Player result = playerService.getPlayerById("aaronha01");
        assertThat(result.getPlayerId()).isEqualTo("aaronha01");
    }
}
```

- `@ExtendWith(MockitoExtension.class)` — activates Mockito's annotation processing for JUnit 5. Without it, `@Mock` and `@InjectMocks` do nothing.
- `@Mock` — creates a fake. Mockito controls it. All methods return null/empty by default.
- `@InjectMocks` — creates a **real** instance and injects matching `@Mock` fields into it (like `@Autowired` but without Spring).
- `when(...).thenReturn(...)` — tells the mock what to return when called.
- `verify(repo).findById("x")` — asserts the mock was called with that argument.

### 2. Controller Test with MockMvc — tests HTTP layer

```java
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean PlayerService playerService;

    @Test
    void getPlayers_returns200() throws Exception {
        when(playerService.getPlayers()).thenReturn(mockPlayers);

        mockMvc.perform(get("/v1/players"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.players[0].playerId").value("aaronha01"));
    }
}
```

- `@WebMvcTest(PlayerController.class)` — boots only the web layer (controller + Jackson + routing). No DB, no services.
- `@MockBean` — replaces a real bean in Spring's context with a mock. Needed because the controller is Spring-managed.
- `MockMvc` — simulates HTTP requests without starting a real server.
- `jsonPath("$.players[0].playerId")` — JSONPath expressions to assert on response body.

### 3. Integration Test — full context, slowest

```java
@SpringBootTest
class PlayerServiceJavaApplicationTests {
    @Test
    void contextLoads() { }
}
```

Boots everything: Spring context, H2 database, real beans. Use sparingly.

### When to Use Which

| Test type | Speed | What it proves |
|---|---|---|
| Unit (`@ExtendWith`) | ~ms | Business logic is correct |
| Controller (`@WebMvcTest`) | ~1-2s | Routing, status codes, JSON serialization work |
| Integration (`@SpringBootTest`) | ~3-5s | Everything wires together |

### @Mock vs @MockBean

- `@Mock` — pure Mockito. Used in unit tests. Creates a standalone mock.
- `@MockBean` — Spring-aware. Used in `@WebMvcTest` / `@SpringBootTest`. Replaces the actual bean in Spring's context with a mock.

## React Test Stack

Create React App bundles:
- **Jest** — test runner and assertions
- **React Testing Library** (`@testing-library/react`) — renders components, queries the DOM
- **user-event** (`@testing-library/user-event`) — simulates user interactions

Run with: `npm test` (watch mode) or `npx react-scripts test --watchAll=false` (CI mode).

### Basic Pattern

```jsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MyComponent from '../components/MyComponent';

// Mock fetch globally
beforeEach(() => {
    global.fetch = jest.fn();
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('renders and handles click', async () => {
    global.fetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ data: 'result' }),
    });

    render(<MyComponent />);

    // Query the DOM like a user would
    fireEvent.click(screen.getByText("Submit"));

    // Wait for async updates
    await waitFor(() => {
        expect(screen.getByText("result")).toBeDefined();
    });
});
```

### Key Queries

```jsx
screen.getByText("Hello")            // exact text match (throws if not found)
screen.getByPlaceholderText("Search") // input placeholder
screen.getByRole("button")           // ARIA role
screen.getAllByText("Submit")         // returns array (multiple matches)
screen.queryByText("Hello")          // returns null if not found (doesn't throw)
```

### Mocking fetch

```jsx
// Success — JSON response
global.fetch.mockResolvedValueOnce({
    ok: true,
    json: () => Promise.resolve({ players: [] }),
});

// Success — text response
global.fetch.mockResolvedValueOnce({
    ok: true,
    text: () => Promise.resolve('plain text response'),
});

// Failure
global.fetch.mockResolvedValueOnce({
    ok: false,
    status: 500,
});

// Never resolves (test loading states)
global.fetch.mockReturnValueOnce(new Promise(() => {}));
```

### Common Assertions

```jsx
expect(element).toBeDefined();
expect(element.disabled).toBe(true);
expect(input.value).toBe('hello');
expect(screen.queryByText("gone")).toBeNull();
expect(global.fetch).toHaveBeenCalledWith('/v1/players');
```

## Test-Driven Development (TDD)

### The Cycle: Red → Green → Refactor

1. **Red** — Write a test for behavior that doesn't exist yet. Run it. Watch it fail.
2. **Green** — Write the minimum code to make the test pass. Nothing more.
3. **Refactor** — Clean up the code (and tests) while keeping all tests green.

Repeat. Each cycle should be small — minutes, not hours.

### TDD in Practice (Spring Boot)

Say the interviewer asks: "Add an endpoint to search players by country."

**Step 1: Red — write the controller test first**
```java
@Test
void searchByCountry_returns200WithMatches() throws Exception {
    Players players = new Players();
    players.getPlayers().add(makePlayer("suzukic01", "Ichiro", "Suzuki", "Japan"));

    when(playerService.getPlayersByCountry("Japan")).thenReturn(players);

    mockMvc.perform(get("/v1/players/search").param("country", "Japan"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players", hasSize(1)))
        .andExpect(jsonPath("$.players[0].birthCountry").value("Japan"));
}

@Test
void searchByCountry_returns200EmptyWhenNoMatches() throws Exception {
    when(playerService.getPlayersByCountry("Narnia")).thenReturn(new Players());

    mockMvc.perform(get("/v1/players/search").param("country", "Narnia"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players", hasSize(0)));
}
```

This won't even compile yet — `getPlayersByCountry` doesn't exist.

**Step 2: Green — implement just enough**
```java
// PlayerService
public Players getPlayersByCountry(String country) {
    Players players = new Players();
    playerRepository.findByBirthCountry(country)
        .forEach(players.getPlayers()::add);
    return players;
}

// PlayerRepository
List<Player> findByBirthCountry(String country);

// PlayerController
@GetMapping("/search")
public ResponseEntity<Players> searchByCountry(@RequestParam("country") String country) {
    return ResponseEntity.ok(playerService.getPlayersByCountry(country));
}
```

Run the tests. They pass.

**Step 3: Refactor** — maybe rename the method, add validation, extract patterns. Tests keep you safe.

### TDD in Practice (React)

Say the interviewer asks: "Add a search-by-country input to the player results."

**Step 1: Red — write the test first**
```jsx
test('search by country filters players', async () => {
    global.fetch
        .mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ players: allPlayers }),
        })
        .mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ players: [japanPlayer] }),
        });

    render(<PlayerResults />);

    await waitFor(() => expect(screen.getByText("aaronha01")).toBeDefined());

    const input = screen.getByPlaceholderText("Search by country...");
    fireEvent.change(input, { target: { value: 'Japan' } });
    fireEvent.click(screen.getByText("Search"));

    await waitFor(() => {
        expect(screen.getByText("suzukic01")).toBeDefined();
        expect(screen.queryByText("aaronha01")).toBeNull();
    });
});
```

Fails because the input/button don't exist yet.

**Step 2: Green — add the input, button, and handler**

**Step 3: Refactor — extract into a custom hook, clean up**

### Why TDD Works in Interviews

1. **Shows structured thinking** — you define the requirement before writing code
2. **Catches edge cases early** — you think about "what if empty?" before implementing
3. **Gives you a safety net** — when you refactor, you know instantly if you broke something
4. **Demonstrates confidence** — interviewers see you're not guessing

### The Pragmatic Version

Pure TDD can be slow in a 75-minute interview. A practical approach:

1. **Start by writing 2-3 tests** for the feature (happy path, empty case, error case)
2. **Implement the feature** to make them pass
3. **Add edge case tests** if time permits

Even writing tests *after* implementation is fine — the key is showing you know how to test. But writing them first signals seniority.

## Project-Specific Recommendations

### High-Value Tests to Have Ready

These are the features most likely to come up in the interview. Having tests ready (or knowing how to write them quickly) is a big advantage.

**Backend — PlayerController:**
- `GET /v1/players` — returns 200 with player list ✅ (done)
- `GET /v1/players/{id}` — returns 200 when found, 404 when not ✅ (done)
- `GET /v1/players/search?country=X` — search by country (TDD candidate)
- Pagination: `GET /v1/players?page=0&size=20` — returns Page object (TDD candidate)

**Backend — PlayerService:**
- `getPlayers()` — returns all players, handles empty ✅ (done)
- `getPlayerById()` — found, not found, exception ✅ (done)
- `getPlayersByCountry()` — TDD candidate

**Backend — ChatController:**
- `POST /v1/chat` with prompt → returns response
- `POST /v1/chat` with blank prompt → uses default
- Error when Ollama is down

**Backend — TeamController:**
- `GET /v1/team/generate?seedId=X&teamSize=5` → proxies to Python service
- Python service is down → appropriate error

**Frontend — Chat.jsx:**
- Renders input and send button ✅ (done)
- Sends message, shows response ✅ (done)
- Handles errors ✅ (done)
- Loading state disables input ✅ (done)

**Frontend — PlayersResults.jsx:**
- Fetches and displays on mount ✅ (done)
- Search by ID filters results (TDD candidate)
- Search by country filters results (TDD candidate)
- Pagination combo box changes page (TDD candidate)

### Test Naming Convention

Use the pattern: `methodName_conditionOrScenario` or `descriptive sentence`

```java
// Java
void getPlayerById_returnsPlayerWhenFound()
void getPlayerById_returns404WhenNotFound()
void getPlayerById_returnsEmptyOnException()
```

```jsx
// React
test('sends message and displays user bubble', ...)
test('displays error message when fetch fails', ...)
test('input and button are disabled while loading', ...)
```

### Interview Flow for a New Feature

1. "Let me start by writing a test for this." (shows TDD mindset)
2. Write the test — talk through what you expect
3. "This won't compile yet because the method doesn't exist."
4. Implement the method/endpoint/component
5. Run the test — "green, it passes"
6. "I'd also want to test the edge cases..." (add error/empty tests)
7. If time: "I'd refactor this into..." (extract hook, add pagination, etc.)

### Quick Reference: Running Tests

```bash
# Spring Boot (from player-service-backend/)
mvn test                          # run all tests
mvn test -Dtest=PlayerControllerTest   # run one test class
mvn test -Dtest="PlayerControllerTest#getPlayers*"  # run matching methods

# React (from players-ui-react/)
npm test                          # watch mode
npx react-scripts test --watchAll=false   # CI mode (run once)
npx react-scripts test Chat       # run tests matching "Chat"
```

Note: if Maven picks up the wrong Java version, prefix with:
```bash
JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home mvn test
```
