# Interview Practice Tasks

These are tasks you're likely to get during the 75-minute live coding session, based on the original codebase. The original code has intentional gaps, broken tests, and stubbed features — the interview is about completing them.

Try to do these with only your NOTES files open. No AI. Time yourself.

---

## Section 1: Bug Fixes & Quick Wins (warm-up, 5-10 min each)

### Task 1.1: Fix the Broken React Test
The original `PlayerMain.test.js` has a broken test:
```jsx
const element = screen.getByRole("button");
expect(element).getByText("Player id");  // ← wrong: getByText is on screen, not element
```
**Fix it.** Make both tests pass.

### Task 1.2: Add Missing Key Props
`PlayersResults.jsx` renders a list with `.map()` but has no `key` prop — React warns about this.
**Add proper keys** using `player.playerId`.

### Task 1.3: Remove the Unused Import
`PlayersResults.jsx` imports `useDatatFetcher` (typo and unused).
**Remove it.** Clean up the import.

---

## Section 2: Wire Up the Search Inputs (15-20 min)

The search inputs in `PlayersResults.jsx` are completely broken:
- Inputs have no `value` or `onChange` (uncontrolled — can't read what user typed)
- "Search by ID" button calls `handleSearchById` but the handler does nothing inside the `if`
- "Search by Country" button has `onClick={()=>{}}` (empty)

### Task 2.1: Make Inputs Controlled
Add `useState` for `searchId` and `searchCountry`. Wire them to the inputs with `value` and `onChange`.

### Task 2.2: Implement Search by ID
- Add a `fetchPlayerById(id)` function to `DataFetcher.jsx`
- Call `GET /v1/players/{id}`
- In `handleSearchById`, call the fetcher and update `players` state with the result
- Handle the case where the player is not found (backend returns 404)

### Task 2.3: Implement Search by Country
This requires a NEW backend endpoint. Full-stack task:
- **Backend:** Add `GET /v1/players/search?country=USA` endpoint
- **Repository:** Add `List<Player> findByBirthCountry(String country)` to `PlayerRepository`
- **Service:** Add `getPlayersByCountry(String country)` method
- **Controller:** Add `@GetMapping("/search")` with `@RequestParam`
- **Frontend:** Add `fetchPlayersByCountry(country)` to `DataFetcher.jsx`
- Wire up the button to call it

---

## Section 3: Pagination (20-25 min)

The app loads ALL ~19,000 players and takes the first 10. That's terrible.

### Task 3.1: Backend Pagination
- Change `PlayerController.getPlayers()` to accept `page` and `size` parameters
- Use Spring Data's `Pageable` / `PageRequest.of(page, size)`
- Return `Page<Player>` instead of `Players` (or wrap it)
- The response should include `content`, `totalPages`, `totalElements`

### Task 3.2: Frontend Pagination
- Update `DataFetcher.jsx` to pass `page` and `size` to `/v1/players?page=0&size=20`
- Add a page selector (combo box / select dropdown) to `PlayersResults.jsx`
- `useEffect` should depend on `[page]` so changing the selector re-fetches
- Display the current page and total pages

### Task 3.3: Paginate the Country Search Too
- Change the country search endpoint to also accept `Pageable`
- Return `Page<Player>` from `findByBirthCountry(String country, Pageable pageable)`

---

## Section 4: Chat with User Prompts (15-20 min)

The original chat endpoint is hardcoded — `POST /v1/chat` always returns a haiku about recursion.

### Task 4.1: Accept Dynamic Prompts
- Modify `ChatController` to accept a `@RequestBody` with a `prompt` field
- Modify `ChatClientService.chat()` to accept a `String userPrompt` parameter
- Build the prompt with `PromptBuilder`: system instructions + user question
- If no prompt provided, fall back to the default haiku

### Task 4.2: Build a Chat Component in React
- Create `Chat.jsx` with:
  - Text input + Send button
  - Message history (array of `{role, text}` objects)
  - Loading state ("Thinking...")
  - Error handling
  - Enter key to send
- Add it to `PlayerMain.jsx`
- Call `POST /v1/chat` with `{ "prompt": userMessage }`

### Task 4.3: RAG — Ground Responses in Real Data
Instead of the LLM making up stats, retrieve real data first:
- Parse the user's question to extract a player name or ID
- Query the database for that player
- Inject the player's real stats into the prompt
- LLM generates a response based on actual data

Example prompt:
```
You are a baseball expert. Use ONLY the following data to answer:
Player: Hank Aaron
Birth: 1934, Mobile, USA
Debut: 1954-04-13
Height: 72, Weight: 180, Bats: R, Throws: R

User question: Tell me about Hank Aaron's career.
```

---

## Section 5: ML Service Integration (15-20 min)

The Python ML service exists but has NO connection to the Java backend.

### Task 5.1: Create Team Endpoints
- Create `TeamController` with:
  - `GET /v1/team/generate?seedId=aaronha01&teamSize=5` — calls Python's `/api/v1/team`
  - `POST /v1/team/feedback` — calls Python's `/api/v1/feedback`
- Create `TeamService` that uses `RestTemplate` to call the Python service
- Create request/response POJOs with `@JsonProperty` for snake_case mapping (Python uses `seed_id`, Java uses `seedId`)

### Task 5.2: Make the ML Service URL Configurable
- Add `ml-service.url: http://localhost:5001` to `application.yml`
- Create a `@ConfigurationProperties` class to bind it
- Inject it into `TeamService` instead of hardcoding the URL

### Task 5.3: Handle Python Service Being Down
- What happens if the Python service isn't running?
- Add error handling in `TeamService` — catch `RestClientException`
- Return a meaningful error response (503 Service Unavailable)

---

## Section 6: Testing (15-20 min)

The original project has one real test (`contextLoads`) and one broken test.

### Task 6.1: Write a Controller Test with MockMvc
```java
@WebMvcTest(PlayerController.class)
class PlayerControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean PlayerService playerService;

    // Test GET /v1/players returns 200 with mock data
    // Test GET /v1/players/{id} returns 200 when found
    // Test GET /v1/players/{id} returns 404 when not found
}
```

### Task 6.2: Write a Service Unit Test with Mockito
```java
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {
    @Mock PlayerRepository playerRepository;
    @InjectMocks PlayerService playerService;

    // Test getPlayers() returns all players
    // Test getPlayerById() when found
    // Test getPlayerById() when not found
    // Test getPlayerById() when exception thrown
}
```

### Task 6.3: Write React Component Tests
- Test that `PlayersResults` fetches and displays players on mount
- Test that search by ID calls the right endpoint and updates the list
- Test the Chat component: send message, loading state, error handling
- Mock `fetch` globally with `jest.fn()`

### Task 6.4: TDD a New Feature
Pick any feature from above that you haven't built yet. Write the tests FIRST, then implement.

---

## Section 7: Conceptual / Discussion Questions

These won't be coded but expect to explain:

### Spring Boot
- What does `@SpringBootApplication` do? (component scan + auto-config + configuration)
- What's the difference between `@Component`, `@Service`, `@Repository`, `@Controller`?
- What's the difference between `@Autowired` and `@Resource`?
- How does dependency injection work in Spring? What is a bean?
- What is `@Configuration` and `@Bean`? When would you use them?
- What does `@MockBean` do vs `@Mock`?
- Explain `ResponseEntity` — why use it instead of returning the object directly?
- What is `Optional` and why does `findById` return one?

### JPA / Database
- How does `JpaRepository` know how to query? (derived query methods from method names)
- How would you write a custom query? (`@Query` with JPQL or native SQL)
- How does pagination work with Spring Data? (`Pageable`, `Page<T>`, `PageRequest.of`)
- What is H2 and why is it used here? (in-memory DB, no setup needed)
- How does the schema get created? (`schema.sql` auto-executed on startup)

### React
- What is `useState` and how does it trigger re-renders?
- What is `useEffect`? What does the dependency array do?
- What is a controlled input?
- Why do lists need `key` props?
- What is a custom hook and when would you write one?
- How does the React dev server proxy work? (`"proxy"` in `package.json`)

### Architecture
- How do the three services (Spring Boot, React, Python) communicate?
- What is REST? What makes an API RESTful?
- What is Jackson? How does JSON serialization work in Spring?
- What is RAG and how is it different from plain LLM chat?
- Why use `RestTemplate` to call the Python service? (alternative: `WebClient`)

---

## Recommended Practice Order

If you have limited time, prioritize in this order:

1. **Section 2** (search inputs) — most likely to be asked, tests full-stack skills
2. **Section 3** (pagination) — very common interview ask, shows Spring Data knowledge
3. **Section 4.1-4.2** (chat) — demonstrates you understand the Ollama integration
4. **Section 6.1-6.2** (Spring tests) — shows testing maturity
5. **Section 5.1** (team endpoints) — shows RestTemplate / inter-service communication
6. **Section 4.3** (RAG) — stretch goal, impressive if you get to it
7. **Section 1** (bug fixes) — easy wins, do these to warm up

For each task, try to:
1. Write the test first (even a rough one)
2. Implement the feature
3. Verify the test passes
4. Check it works in the browser / curl

Good luck. You've got this.
