# baseball

A baseball project.

## Local Dev Setup (macOS)

### Prerequisites

Install Java 17 and Maven via Homebrew:

```bash
brew install openjdk@17
brew install maven
```

After installing Java 17, symlink it so the system can find it:

```bash
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

Add to your shell profile (`~/.zshrc`):

```bash
export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

Then reload:

```bash
source ~/.zshrc
```

Verify:

```bash
java --version    # should show 17.x
mvn --version     # should show Maven and Java 17
```

### Other dependencies (should already be installed)

- **Node.js**: `brew install node@18` (or any 18+ version)
- **Docker**: Install from [docker.com](https://www.docker.com/) or `brew install --cask docker`

### Running the app

```bash
# Backend
cd player-service-backend
mvn clean install -DskipTests
mvn spring-boot:run
# Visit http://localhost:8080/v1/players

# Frontend (in a separate terminal)
cd players-ui-react
npm install
npm start
# Visit http://localhost:3000

# LLM (optional - in a separate terminal)
docker pull ollama/ollama
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
docker exec -it ollama ollama run tinyllama
```

## Interview Prep Strategy

### Codebase Overview

The app is a Spring Boot + React fullstack service for baseball player data with LLM integration:

| Layer | Tech | Status |
|-------|------|--------|
| Backend API | Spring Boot 3.3.4, Java 17, Maven | 2 GET endpoints working |
| Database | H2 in-memory, loaded from Player.csv (~19k rows) | Working |
| LLM | Ollama (tinyllama) via ollama4j SDK | Stubbed (hardcoded prompt) |
| Frontend | React 18, plain CSS | Displays 10 players, search is stubbed |
| ML Model | Python Flask + scikit-learn | Separate service, team recommendations |
| Tests | JUnit 5 / React Testing Library | Minimal/broken |

### Architecture

```
React UI (:3000) --> Spring Boot API (:8080) --> H2 DB (Player.csv)
                                             --> Ollama LLM (:11434)
                                             --> Python ML Service (:5000) [optional]
```

### What's Intentionally Incomplete (Likely User Story Targets)

These are the gaps in the codebase that are clearly set up for interview-day features:

1. **Search by ID** - Frontend has the input/button wired up, handler is empty, backend endpoint exists
2. **Search by Country** - Frontend has input/button, no handler, no backend endpoint
3. **Validation** - `validateId()`, `validateCountryCode()`, `sanitizeInput()` all return `true`
4. **CRUD operations** - No POST/PUT/DELETE endpoints (only GET)
5. **Dynamic LLM prompts** - Chat endpoint has a hardcoded haiku prompt, no user input accepted
6. **Pagination** - Frontend hardcodes first 10 results, no pagination support
7. **Custom queries** - PlayerRepository has zero custom methods (only JpaRepository defaults)
8. **Tests** - Backend has only a context-load test; frontend tests have assertion bugs
9. **Error handling** - Minimal try/catch, no global exception handler, no user-facing error states
10. **Loading/empty states** - No spinners, no "no results found" UI

### Recommended Prep: Features to Practice Building

Ordered by likelihood of being asked and interview impact:

#### 1. Search by Player ID (end-to-end, ~15 min)
**Why:** Demonstrates full-stack fluency. The scaffolding is already there — just connect the dots.
- Frontend: Implement `handleSearchById` in PlayersResults.jsx, call new fetch function
- Backend: Endpoint already exists (`GET /v1/players/{id}`)
- Add validation for player ID format
- Add error handling for 404

#### 2. Search/Filter by Field (e.g., country, name, birth year) (~20 min)
**Why:** Shows you can add a new endpoint end-to-end and write custom JPA queries.
- Backend: Add `findByBirthCountry(String country)` to PlayerRepository, new service method, new controller endpoint
- Frontend: Wire up the country search handler, add new DataFetcher function
- Shows: JPA derived queries, REST design, React state management

#### 3. Pagination (~15 min)
**Why:** Returning 19k rows is a red flag in any real app. Shows operational maturity.
- Backend: Use Spring Data's `Pageable` — add `Page<Player> findAll(Pageable pageable)` and query params `?page=0&size=20`
- Frontend: Add next/prev buttons, track page state
- Shows: Performance awareness, Spring Data knowledge

#### 4. CRUD - Add/Update/Delete Players (~20 min)
**Why:** Standard REST competency expected at Staff level.
- Backend: POST/PUT/DELETE endpoints with proper HTTP status codes (201, 204)
- Add `@RequestBody` parsing, `@Valid` annotations
- Shows: REST conventions, input validation, idempotency awareness

#### 5. LLM Integration - Dynamic Chat (~15 min)
**Why:** The interview explicitly includes an "AI-focused story." This is almost certainly it.
- Backend: Accept user prompt via POST body, make model configurable
- Consider RAG: query player DB, inject context into LLM prompt (e.g., "tell me about Hank Aaron" pulls player data first)
- Frontend: Add a chat input component
- Shows: AI/LLM integration skills, RAG pattern awareness

#### 6. Global Exception Handler (~10 min)
**Why:** Quick win that shows production mindset.
- Add `@ControllerAdvice` with `@ExceptionHandler` methods
- Return consistent error response DTOs
- Shows: Operational excellence, clean error contracts

#### 7. Tests (~15 min)
**Why:** Staff engineers are expected to write tests without being asked.
- Backend: Controller integration tests with `@WebMvcTest`, service unit tests with Mockito
- Frontend: Fix existing broken tests, add search interaction tests
- Shows: Quality standards, TDD capability

### Key Files to Know Cold

**Backend:**
- `PlayerController.java` - REST endpoints
- `PlayerService.java` - Business logic
- `PlayerRepository.java` - JPA queries (extend this)
- `Player.java` - Entity model (22 fields from CSV)
- `ChatController.java` / `ChatClientService.java` - LLM integration
- `application.yml` - Config (H2, port 8080)
- `schema.sql` - DB init from CSV

**Frontend:**
- `PlayersResults.jsx` - Main component with stubbed handlers
- `DataFetcher.jsx` - API calls (add new fetch functions here)
- `utils/index.js` - Validation stubs (implement these)

### Interview Day Tips

- **Ask clarifying questions** before coding (scope, edge cases, priority)
- **Talk through your approach** before writing code — they want to see your thought process
- **Start with the backend** — get the endpoint working first, then wire up the frontend
- **Write a test** for at least one feature — unprompted testing signals Staff-level maturity
- **Mention trade-offs** — "I'd add caching here in production" or "this should be paginated"
- The AI story will likely involve making the LLM chat dynamic or adding RAG with player data
