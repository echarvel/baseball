# Interview Prep Notes

## Java Refresher (Java 8+ features used in this codebase)

**`Optional<T>`** (Java 8) — Used in `PlayerService.java:27` and `PlayerController.java:30`
A container that may or may not hold a value. Replaces returning `null` to avoid NullPointerExceptions.
```java
Optional<Player> player = playerRepository.findById(playerId);
if (player.isPresent()) {       // is there a value?
    return player.get();         // unwrap it
}
```

**Method references `::`** (Java 8) — Used in `PlayerService.java:23`
Shorthand for a lambda that just calls one method:
```java
// These are equivalent:
.forEach(p -> players.getPlayers().add(p));
.forEach(players.getPlayers()::add);
```

**Lambda expressions** (Java 8) — Not heavily used here yet, but you'll use them when adding features. Quick reminder:
```java
list.stream().filter(p -> p.getBirthCountry().equals("USA")).collect(Collectors.toList());
```

---

## Spring Boot — The 60-Second Version

Spring Boot is an **opinionated framework** that eliminates boilerplate. Instead of writing XML configs, servlets, and connection pools, you annotate classes and Spring wires everything together at startup.

**The core idea: Dependency Injection (DI)**

You never do `new PlayerService()`. Instead, Spring creates objects (called **beans**) and injects them where needed. When the app starts, Spring scans for annotations and builds a graph of dependencies.

### Key Annotations in this Codebase

**`@SpringBootApplication`** — `PlayerServiceJavaApplication.java:6`
The entry point. This single annotation is actually three annotations combined:

1. **`@ComponentScan`** — Scans the package this class lives in (`com.app.playerservicejava`) and **all sub-packages** for annotated classes (`@Controller`, `@Service`, `@Repository`, `@Configuration`, etc.) and creates bean instances of them. The package structure matters — if you put a controller in `com.other.package`, Spring won't find it.

2. **`@EnableAutoConfiguration`** — Reads your `pom.xml` dependencies and auto-configures things. `spring-boot-starter-data-jpa` in the pom → JPA, DataSource, and H2 get set up automatically. `spring-boot-starter-web` → embedded Tomcat starts on port 8080. No manual config needed.

3. **`@SpringConfiguration`** — Marks this class as a source of bean definitions.

**How `SpringApplication.run()` works:**

`SpringApplication.run()` is a **static method on the `SpringApplication` class** (from `org.springframework.boot`), not a method on your app. Your class is passed **as an argument** — Spring reads its annotations to figure out what to do:

```java
// You're calling this:
SpringApplication.run(PlayerServiceJavaApplication.class, args);
// NOT: playerServiceJavaApplication.run()
```

Under the hood, `run()` roughly does:
```
1. Create application context (the bean container)
2. Read annotations on the class you passed in (@SpringBootApplication)
3. Component scan from that class's package
4. Auto-configure based on pom.xml dependencies
5. Create and wire all beans:
     PlayerController needs PlayerService -> inject it
     PlayerService needs PlayerRepository -> inject it
     ChatController needs ChatClientService -> inject it
     ChatClientService needs OllamaAPI -> inject it (from @Bean in config)
6. Run schema.sql to load CSV into H2
7. Start embedded Tomcat on port 8080, register controller routes
8. App is ready
```

### Stereotypes

In Spring, a **stereotype** is a label annotation that tells Spring two things:
1. **Create a bean** of this class (same as `@Component`)
2. **What role** this class plays in the architecture

They're all functionally identical under the hood — they all do what `@Component` does. The only difference is semantic (for humans reading the code):

```
@Component         — generic bean, no specific role
  @Service         — "I'm business logic"        (PlayerService)
  @Repository      — "I'm data access"           (PlayerRepository)
  @Controller      — "I'm an HTTP handler"        (ChatController)
  @RestController  — "I'm an HTTP handler + auto-serialize to JSON" (PlayerController)
  @Configuration   — "I define @Bean methods"     (ChatClientConfiguration)
```

You could replace every `@Service` with `@Component` and the app would work exactly the same. Spring just provides specific names so your architecture is self-documenting.

**`@Component` vs `@Configuration`** — key distinction:
- `@Component` — "I **am** a bean. Create an instance of me." (e.g., `AppProperties` holds config values)
- `@Configuration` — "I **define** beans. My `@Bean` methods create other objects." (e.g., `ChatClientConfiguration` creates `OllamaAPI`)

Using `@Configuration` on a value-holding class would work but would be misleading — someone reading the code would expect `@Bean` methods inside.

### Configuration Properties

`@ConfigurationProperties(prefix = "app")` binds YAML properties to a Java class by matching names to setters:

```
application.yml                    AppProperties.java
───────────────                    ──────────────────
app:                               @ConfigurationProperties(prefix = "app")
  environment: dev        ->       setEnvironment("dev")
  name: Player Service    ->       setName("Player Service")
```

Convention: YAML key name -> `set` + capitalized key name. `environment` -> `setEnvironment()`.

Full chain:
1. **YAML defines values** -> `app.environment: dev`
2. **`@ConfigurationProperties` binds them** -> calls setters on `AppProperties`
3. **`@Component` makes it a bean** -> Spring manages the instance
4. **`@Autowired` injects it** -> any class can use the values

Override at runtime without changing code:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.environment=production"
```

### @Component vs @Configuration — When to Use Which

Both are stereotypes that create beans. The difference is their purpose:

- **`@Component`** → Spring creates the bean and that's it. The bean itself is what you use.
- **`@Configuration`** → Spring creates the bean, then **scans it for `@Bean` methods** and calls each one to create additional beans.

`ChatClientConfiguration` produces **three** beans:
```
1. ChatClientConfiguration itself (from @Configuration)
2. RestTemplate (from @Bean restTemplate() method)
3. OllamaAPI (from @Bean ollamaAPI() method)
```

Nobody injects `ChatClientConfiguration` directly — it's just a factory.

| Pattern | Use when |
|---------|----------|
| `@Component` | You own the class, Spring can create it with a no-arg constructor |
| `@Component` + `@ConfigurationProperties` | Same, but also bind YAML values to fields |
| `@Configuration` + `@Bean` | Third-party class, or needs custom construction logic |

You use `@Bean` when you can't add `@Component` to a class (e.g., `OllamaAPI` is from the ollama4j library — you don't own the source).

**Why `@Configuration` over `@Component` for `@Bean` methods:**
`@Configuration` classes are **proxied** — if one `@Bean` method calls another, you get the same singleton. `@Component` classes aren't proxied, so calling between `@Bean` methods creates new instances. Always use `@Configuration` for `@Bean` methods.

### RestTemplate

Spring's built-in HTTP client for calling other services (like `fetch()` in JS):
```java
// GET — second arg is what type to deserialize the response into
String response = restTemplate.getForObject(url, String.class);
Player player = restTemplate.getForObject(url, Player.class);

// POST — sends playerData as JSON body, parses response as Player
Player created = restTemplate.postForObject(url, playerData, Player.class);
```

Defined as a `@Bean` in `ChatClientConfiguration.java` but **not used anywhere** in the codebase — probably there in case you need it during the interview (e.g., calling the Python ML service on port 5000).

Note: `RestTemplate` is in maintenance mode — Spring recommends `WebClient` for new projects. But it's simpler and fine for interviews.

### Jackson (JSON Serialization)

Jackson is included automatically via `spring-boot-starter-web` — you never see it in the pom or configure it. It works both directions:
- **Outgoing** (Java -> JSON): calls getters on the object
- **Incoming** (JSON -> Java): calls setters on the object

Null fields are included by default. Customize with annotations:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)  // skip null fields in output
public class Player { ... }

@JsonIgnore           // hide a field entirely from JSON
private String retroId;
```

### pom.xml vs application.yml

`pom.xml` is **not** the app's configuration — it's just the Maven build/dependency file that tells Maven what JARs to download. But Spring Boot is clever about it:

- **`pom.xml`** → "download these libraries" (what capabilities you want)
- **`application.yml`** → "configure the app" (how you want them configured)
- **`@SpringBootApplication`** → "wire it all up at startup" (when)

The magic is **auto-configuration**: when Spring Boot starts, it checks what's on the classpath (what `pom.xml` downloaded). If it sees `spring-boot-starter-data-jpa` and `h2` JARs present, it thinks: "they want JPA with H2 — I'll set up a DataSource, EntityManager, and connection pool with sensible defaults." Then `application.yml` **overrides** defaults where needed. If you removed `application.yml` entirely, the app would still start with Spring's default values.

### How does it know it's a Spring app?

The `<parent>` in `pom.xml` inherits from Spring Boot's parent POM, which pre-defines dependency versions and plugin configs. But `pom.xml` alone doesn't make it a Spring app — Maven just downloads JARs. What actually makes it a Spring app is `main()` calling `SpringApplication.run()`. Without that call, you'd just have annotated classes sitting there doing nothing.

### The Boot Chain

```
mvn spring-boot:run
    |
spring-boot-maven-plugin (defined in pom.xml <build> section)
    |
Plugin scans for a class with public static void main(String[] args)
    |
Finds PlayerServiceJavaApplication.main()
    |
main() calls SpringApplication.run(PlayerServiceJavaApplication.class, args)
    |
Spring Boot takes over
```

**`@RestController`** — `PlayerController.java:18`
Marks a class as an HTTP endpoint handler. Every method return value is automatically serialized to JSON (no need to manually call `objectMapper.writeValueAsString()`).

**`@Controller`** — `ChatController.java:21`
Same as `@RestController` but doesn't auto-serialize responses — that's why `ChatController` needs `@ResponseBody` on its methods while `PlayerController` doesn't.

**`@Service`** — `PlayerService.java:13`
Marks a class as business logic. Functionally identical to `@Component` — it's just a semantic label so humans know "this is the service layer."

**`@Configuration`** + **`@Bean`** — `ChatClientConfiguration.java:8,19,25`
`@Configuration` says "this class defines beans manually." Each `@Bean` method returns an object that Spring manages. Here it creates the `OllamaAPI` and `RestTemplate` instances:
```java
@Bean
public OllamaAPI ollamaAPI() {
    OllamaAPI api = new OllamaAPI("http://127.0.0.1:11434/");
    api.setRequestTimeoutSeconds(120);
    return api;  // Spring now manages this — anyone can @Autowired it
}
```

**`@Autowired`** — `PlayerService.java:17`, `ChatController.java:27`, etc.
"Spring, inject the bean of this type here." When `PlayerService` says `@Autowired private PlayerRepository playerRepository`, Spring finds the `PlayerRepository` bean and plugs it in.

**`@Resource`** — `PlayerController.java:19`
Same as `@Autowired`, just an older Jakarta (Java EE) annotation. Functionally identical here.

### HTTP Mapping Annotations

All in `PlayerController.java` and `ChatController.java`:

- **`@RequestMapping("v1/players")`** — Base URL path for all methods in this controller
- **`@GetMapping("/{id}")`** — Handles `GET /v1/players/someId`
- **`@PostMapping`** — Handles `POST /v1/chat`
- **`@PathVariable("id")`** — Extracts `someId` from the URL path

**`ResponseEntity<T>`** — A wrapper that lets you control the HTTP status code:
```java
return new ResponseEntity<>(player.get(), HttpStatus.OK);      // 200
return new ResponseEntity<>(HttpStatus.NOT_FOUND);              // 404
```

### REST Annotations — How Requests Get Routed

**Class-level:**
```java
@RestController  // handles HTTP requests + auto-serializes return values to JSON
@RequestMapping(value = "v1/players", produces = { MediaType.APPLICATION_JSON_VALUE })
```
Sets the base path. Every method is relative to `/v1/players`.

**Method-level mapping (all shortcuts for @RequestMapping):**
```
@GetMapping("/path")       →  GET    (read)
@PostMapping("/path")      →  POST   (create)
@PutMapping("/path")       →  PUT    (update/replace)
@DeleteMapping("/path")    →  DELETE (remove)
@PatchMapping("/path")     →  PATCH  (partial update)
```

**Getting data from requests — three ways:**
```java
// 1. Path variables — part of the URL
@GetMapping("/{id}")
public Player get(@PathVariable("id") String id) { }
// GET /v1/players/aaronha01 → id = "aaronha01"

// 2. Query parameters — after the ? in the URL
@GetMapping
public List<Player> search(@RequestParam("country") String country) { }
// GET /v1/players?country=USA → country = "USA"

// 3. Request body — JSON sent in POST/PUT
@PostMapping
public Player create(@RequestBody Player player) { }
// POST /v1/players with JSON body → Spring deserializes JSON into Player
```

**Controlling the response with ResponseEntity:**
```java
return new ResponseEntity<>(player, HttpStatus.OK);       // 200 with data
return ResponseEntity.ok(player);                          // 200 shorthand
return new ResponseEntity<>(HttpStatus.NOT_FOUND);         // 404 no body
return new ResponseEntity<>(newPlayer, HttpStatus.CREATED); // 201 for POST
return new ResponseEntity<>(HttpStatus.NO_CONTENT);         // 204 for DELETE
```

You can also return the object directly (Spring defaults to 200), but you lose status code control:
```java
public Player getPlayer(@PathVariable String id) {
    return playerService.getPlayerById(id);  // always 200
}
```

To return proper errors without ResponseEntity, throw an exception:
```java
public Player getPlayer(@PathVariable String id) {
    return playerService.getPlayerById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
}
```

**How Jackson serializes:** Spring uses Jackson (included via `spring-boot-starter-web`) to convert Java objects to JSON. Jackson uses **reflection** to discover getters and builds the JSON from them. Any Java object with getters can be serialized — `ResponseEntity<T>` is generic, so `T` can be any type.

**`@RestController` vs `@Controller`:**
- `@RestController` = `@Controller` + `@ResponseBody` on every method (auto-serializes)
- `@Controller` requires `@ResponseBody` per method (ChatController does this)

### @Resource vs @Autowired

Both inject beans. The difference is how they find the bean:

- **`@Autowired`** (Spring) — matches by **type**
- **`@Resource`** (Jakarta) — matches by **field name** first, then type

Only matters with multiple beans of the same type:
```java
@Bean
public RestTemplate restTemplateA() { return new RestTemplate(); }
@Bean
public RestTemplate restTemplateB() { return new RestTemplate(); }

@Autowired
private RestTemplate restTemplate;    // ambiguous — two RestTemplate beans!

@Resource
private RestTemplate restTemplateA;   // works — field name matches bean name
```

The `@Bean` method name defines the bean's name in Spring's registry. `@Resource` matches field name against bean name. The method runs once at startup; after that it's just a named object in a registry.

**`@Bean` only works on methods, not fields:**
```java
@Bean
private RestTemplate rt = new RestTemplate();  // does NOT work

@Bean
public RestTemplate rt() { return new RestTemplate(); }  // works
```

Spring needs a method to control when/how the object is created. For simple beans, use `@Component` on the class instead — Spring creates it automatically via component scanning.

### Three Ways to Inject Dependencies

```java
// 1. Field injection (what this codebase uses) — simple but harder to test
@Autowired
private PlayerRepository playerRepository;

// 2. Constructor injection (recommended) — explicit, testable
private final PlayerRepository playerRepository;
public PlayerService(PlayerRepository playerRepository) {
    this.playerRepository = playerRepository;
}
// No @Autowired needed if only one constructor.

// 3. Setter injection (rare)
@Autowired
public void setPlayerRepository(PlayerRepository playerRepository) {
    this.playerRepository = playerRepository;
}
```

Field injection works on private fields via **reflection** (Spring bypasses access modifiers). Constructor injection is preferred because fields can be `final`, and you can test without Spring by passing mocks.

### Beans as Collections

You can define a bean that's a list, and Spring can **auto-collect** all beans of a type into a list:

```java
// Multiple beans of the same type
@Bean
public RestTemplate restTemplateA() { return new RestTemplate(); }
@Bean
public RestTemplate restTemplateB() { return new RestTemplate(); }

// Spring collects both automatically
@Autowired
private List<RestTemplate> allRestTemplates;  // contains both
```

Useful with interfaces — define multiple implementations, inject them all:
```java
public interface SearchStrategy { List<Player> search(String query); }

@Component
public class NameSearch implements SearchStrategy { ... }
@Component
public class CountrySearch implements SearchStrategy { ... }

@Autowired
private List<SearchStrategy> strategies;  // [NameSearch, CountrySearch]
```

### Optional<T> Methods (Java 8)

```java
Optional<Player> result = playerService.getPlayerById("aaronha01");

result.isPresent()                    // true if contains a value
result.get()                          // unwrap (throws if empty!)
result.orElse(defaultPlayer)          // return value, or default if empty
result.orElseThrow(() -> new Ex())    // return value, or throw if empty
```

---

## Tomcat — How HTTP Requests Get Into Your App

In the old Java days, you'd install Tomcat separately, configure it with XML, build a WAR file, and deploy it. Spring Boot **embeds Tomcat inside your app**. When you add `spring-boot-starter-web` to the pom, Spring Boot bundles a Tomcat server that starts automatically. You never touch Tomcat directly.

### Request Flow

```
Browser: GET http://localhost:8080/v1/players/aaronha01
    |
Embedded Tomcat (listening on port 8080)
    |
Spring's DispatcherServlet (single entry point for ALL requests)
    |
Scans @RequestMapping annotations across all controllers
Finds: PlayerController has @RequestMapping("v1/players")
       and @GetMapping("/{id}") matches "/aaronha01"
    |
Calls PlayerController.getPlayerById("aaronha01")
    |
Return value (ResponseEntity<Player>) gets serialized to JSON
    |
HTTP response sent back to browser
```

**DispatcherServlet** is a single servlet that Tomcat runs, acting as a **front controller** — every request goes through it, and it routes to the right controller method based on annotations. You never create or configure it — Spring Boot does it automatically.

### What Tomcat does vs what Spring does

**Tomcat (network plumbing):**
- Listens on the port for TCP connections
- Parses raw HTTP bytes into request objects
- Manages threads (one per request by default)
- Passes requests to DispatcherServlet
- Sends response bytes back over the wire

**Spring (application logic):**
- Routes requests to the right controller method (via annotations)
- Deserializes request bodies (JSON -> Java objects)
- Serializes return values (Java objects -> JSON)
- Handles content negotiation, error responses, CORS, etc.

**The only Tomcat config you touch** is in `application.yml`:
```yaml
server:
  port: 8080    # change this to run on a different port
```

---

## JPA + H2 + Service Layer

### H2 — The Database

H2 is an **in-memory database** written in Java. No installation, no server process — it lives inside your app's JVM. When the app starts, it's empty. When the app stops, everything is gone.

On startup, Spring automatically runs `schema.sql`:
```sql
DROP TABLE IF EXISTS PLAYERS;
CREATE TABLE PLAYERS AS SELECT * FROM CSVREAD('Player.csv');
```

This reads all ~19,000 rows from the CSV directly into a table. Every restart reloads from scratch — you can't corrupt the data.

**Why `DROP TABLE` if it's in-memory?** Safety net for DevTools — it auto-restarts the app without fully killing the JVM, so H2 can survive the restart. Without the drop, `CREATE TABLE` would fail because the table already exists. The H2 console can also hold connections that keep the DB alive during restarts.

`application.yml` configures the connection:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:playerdb    # "mem" = in-memory
    driverClassName: org.h2.Driver
    username: sa                  # default H2 user, password is blank
  h2:
    console:
      enabled: true              # web UI at /h2-console
```

H2 console at http://localhost:8080/h2-console — use JDBC URL `jdbc:h2:mem:playerdb`, username `sa`, password blank.

### JPA — The ORM Layer

JPA sits between your Java code and the database. Instead of writing SQL, you define an **entity** class and JPA maps it to a table.

**The Entity** (`Player.java`):
```java
@Entity                              // "this class = a DB table"
@Table(name="PLAYERS")               // which table
public class Player {
    @Id                              // primary key
    @Column(name = "PLAYERID")       // which column
    private String playerId;

    @Column(name = "BIRTHCOUNTRY")
    private String birthCountry;
    // ... 20 more fields with getters/setters
}
```

Each `@Column` maps a Java field to a database column. The field name and column name don't have to match — that's what `name = "PLAYERID"` is for.

**Lombok** — already in the pom but not used. Could reduce 276-line Player.java to:
```java
@Entity
@Table(name="PLAYERS")
@Data                           // generates getters, setters, toString, equals, hashCode
@NoArgsConstructor              // generates empty constructor
public class Player {
    @Id @Column(name = "PLAYERID") private String playerId;
    @Column(name = "BIRTHCOUNTRY") private String birthCountry;
    // ... just fields, no getters/setters needed
}
```

### The Repository — `PlayerRepository.java`

```java
public interface PlayerRepository extends JpaRepository<Player, String> {
}
```

**Zero implementation code.** By extending `JpaRepository<Player, String>` (entity type, primary key type), Spring generates these methods for free:

- `findAll()` → `SELECT * FROM PLAYERS`
- `findById("aaronha01")` → `SELECT * FROM PLAYERS WHERE PLAYERID = 'aaronha01'`
- `save(player)` → `INSERT` or `UPDATE` (checks if ID exists)
- `deleteById("aaronha01")` → `DELETE FROM PLAYERS WHERE PLAYERID = 'aaronha01'`
- `count()` → `SELECT COUNT(*) FROM PLAYERS`

**Custom queries by method name.** Add method signatures and Spring parses the name to generate SQL:

```java
public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findByBirthCountry(String country);                          // WHERE BIRTHCOUNTRY = ?
    List<Player> findByLastName(String lastName);                             // WHERE NAMELAST = ?
    List<Player> findByBirthYearAndBirthCountry(String year, String country); // WHERE BIRTHYEAR = ? AND BIRTHCOUNTRY = ?
    List<Player> findByLastNameContaining(String partial);                    // WHERE NAMELAST LIKE '%value%'
    List<Player> findByBirthYearGreaterThan(String year);                     // WHERE BIRTHYEAR > ?
}
```

**Method name keywords:**
```
findBy          → SELECT ... WHERE
And / Or        → AND / OR
Containing      → LIKE '%value%'
StartingWith    → LIKE 'value%'
GreaterThan / LessThan  → > / <
OrderBy...Desc  → ORDER BY ... DESC
```

Field names must match the Java field names in `Player.java` (e.g., `birthCountry` not `BIRTHCOUNTRY`).

**Complex queries with @Query** — when method naming isn't enough:

```java
// JPQL — uses Java field names, not column names
@Query("SELECT p FROM Player p WHERE p.birthYear > :startYear AND p.birthCountry = :country ORDER BY p.lastName")
List<Player> findVeteransByCountry(@Param("startYear") String startYear, @Param("country") String country);

// Native SQL — uses actual table/column names
@Query(value = "SELECT * FROM PLAYERS WHERE BIRTHYEAR BETWEEN :start AND :end AND WEIGHT > :minWeight", nativeQuery = true)
List<Player> findByBirthYearRangeAndWeight(@Param("start") String start, @Param("end") String end, @Param("minWeight") String minWeight);

// JPQL with LIKE
@Query("SELECT p FROM Player p WHERE LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
List<Player> searchByName(@Param("name") String name);

// Updates
@Modifying
@Query("UPDATE Player p SET p.birthCountry = :country WHERE p.playerId = :id")
int updateCountry(@Param("id") String id, @Param("country") String country);
```

**JPQL vs native SQL:**
- **JPQL** — queries against Java entities (`Player p`, `p.birthYear`). Portable, validated at startup.
- **Native SQL** (`nativeQuery = true`) — raw SQL against actual tables/columns. Use for DB-specific features or complex joins.

### Dynamic Queries (Optional Filters)

When you have multiple optional search fields (e.g., ID and country, both optional), static approaches break down:
- Method naming → need a method for every combination
- `@Query` → static, can't conditionally add WHERE clauses (hack: `(:id IS NULL OR p.playerId = :id)` gets ugly fast)

**Option 1: JPA Specifications (recommended)** — clean, reusable:

Repository needs to extend one more interface:
```java
public interface PlayerRepository extends JpaRepository<Player, String>, JpaSpecificationExecutor<Player> {
}
```

Controller builds the spec dynamically:
```java
@GetMapping
public ResponseEntity<Page<Player>> getPlayers(
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String country,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Specification<Player> spec = Specification.where(null);
    if (id != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("playerId"), id));
    }
    if (country != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("birthCountry"), country));
    }
    return ResponseEntity.ok(playerRepository.findAll(spec, PageRequest.of(page, size)));
}
```

The lambda `(root, query, cb)`:
- `root` — the entity (access fields with `root.get("fieldName")`)
- `query` — the query being built
- `cb` — CriteriaBuilder (creates conditions: `equal`, `like`, `greaterThan`, etc.)

Each `.and()` adds a WHERE clause. Both provided: `WHERE PLAYERID = ? AND BIRTHCOUNTRY = ?`. One: just that clause. Neither: no WHERE.

**Option 2: Native SQL with EntityManager** — if you're more comfortable with SQL:

```java
@Autowired
private EntityManager entityManager;

public List<Player> search(String id, String country) {
    StringBuilder sql = new StringBuilder("SELECT * FROM PLAYERS WHERE 1=1");
    if (id != null) sql.append(" AND PLAYERID = :id");
    if (country != null) sql.append(" AND BIRTHCOUNTRY = :country");

    Query query = entityManager.createNativeQuery(sql.toString(), Player.class);
    if (id != null) query.setParameter("id", id);
    if (country != null) query.setParameter("country", country);
    return query.getResultList();
}
```

`Player.class` as second arg tells JPA to map result rows into Player objects automatically.

**Option 3: JPQL with EntityManager** — same but uses entity field names:

```java
StringBuilder jpql = new StringBuilder("SELECT p FROM Player p WHERE 1=1");
if (id != null) jpql.append(" AND p.playerId = :id");
TypedQuery<Player> query = entityManager.createQuery(jpql.toString(), Player.class);
```

**Full query approach comparison:**

| Approach | Pros | Cons |
|----------|------|------|
| Method naming | Zero code | Fixed filters only |
| `@Query` | Readable SQL/JPQL | Static, no conditionals |
| Native SQL + EntityManager | Full SQL power, dynamic | String building, less safe |
| Criteria API | Type-safe, dynamic | Verbose |
| `Specification` | Clean, reusable, dynamic | Slight learning curve |

### The Service Layer — Business Logic

The service sits between controller and repository. In this codebase it's thin — mostly delegates:

```java
@Service
public class PlayerService {
    @Autowired
    private PlayerRepository playerRepository;

    public Players getPlayers() {
        Players players = new Players();
        playerRepository.findAll()
                .forEach(players.getPlayers()::add);
        return players;
    }

    public Optional<Player> getPlayerById(String playerId) {
        player = playerRepository.findById(playerId);
        Thread.sleep((long)(Math.random() * 2000));   // simulated delay
        return player;
    }
}
```

**Why not call the repository directly from the controller?** Separation of concerns:
- **Controller** — HTTP concerns (request parsing, status codes, routing)
- **Service** — business logic (validation, transformations, orchestration)
- **Repository** — data access (SQL queries)

During the interview, you'd add logic here — validation, combining data from multiple sources, calling the LLM with player context, etc.

**The `Players` wrapper** (`Players.java`):
```java
public class Players implements Serializable {
    private List<Player> players;
}
```
Wraps the list so JSON is `{"players": [...]}` instead of a raw array `[...]`. Lets you add metadata later (like pagination) without breaking the API.

### Design Note: Use `Players` Wrapper, Not Raw `List<Player>`

Always return `Players` (the wrapper) instead of `List<Player>` for consistency:
- Existing `getPlayers()` already returns `Players` → `{"players": [...]}`
- Returning `List<Player>` gives a raw array `[...]` — inconsistent API
- The wrapper is where you'd add pagination metadata later:

```java
public class Players implements Serializable {
    private List<Player> players;
    private int totalResults;
    private int page;
    private int pageSize;
}
// Response: {"players": [...], "totalResults": 523, "page": 1, "pageSize": 20}
```

This is impossible with a raw array. Use the wrapper everywhere for consistency and future-proofing.

### Wiring It End-to-End (Example: Search by Country)

```java
// 1. Repository — add method signature
public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findByBirthCountry(String country);
}

// 2. Service — add business logic method
public List<Player> getPlayersByCountry(String country) {
    return playerRepository.findByBirthCountry(country);
}

// 3. Controller — add endpoint
@GetMapping("/search")
public ResponseEntity<List<Player>> searchByCountry(@RequestParam("country") String country) {
    return ResponseEntity.ok(playerService.getPlayersByCountry(country));
}
// GET /v1/players/search?country=USA
```

---

## Request Flow (How a GET /v1/players/{id} works)

```
Browser hits GET /v1/players/aaronha01
        |
PlayerController.getPlayerById("aaronha01")    <-- Spring routes the request
        |
PlayerService.getPlayerById("aaronha01")       <-- Business logic layer
        |
PlayerRepository.findById("aaronha01")         <-- JPA auto-generated SQL
        |
H2 Database: SELECT * FROM PLAYERS WHERE PLAYERID = 'aaronha01'
        |
Returns Optional<Player> -> Controller wraps in ResponseEntity -> JSON response
```

---

## Ollama — LLM Integration

Ollama runs large language models locally on your machine instead of calling a remote API. Installed via `brew install ollama`, listens on port 11434.

### Available Models

- **tinyllama** (1B params) — fast, low quality responses
- **llama3.1** (8B params) — slower, much better responses

Pull models with `ollama pull <name>`. List with `ollama list`.

### How the Code Connects

Three files, same Controller -> Service -> Config pattern as player data:

**1. `ChatClientConfiguration.java`** — creates the API client bean:
```java
@Configuration
public class ChatClientConfiguration {
    @Bean
    public OllamaAPI ollamaAPI() {
        OllamaAPI api = new OllamaAPI("http://127.0.0.1:11434/");
        api.setRequestTimeoutSeconds(120);
        return api;
    }
}
```
Creates a single `OllamaAPI` bean pointing at local Ollama server. 120-second timeout because LLM responses can be slow.

**2. `ChatClientService.java`** — the business logic:
```java
public String chat() {
    String model = "tinyllama";
    PromptBuilder promptBuilder = new PromptBuilder()
            .addLine("Recite a haiku about recursion.");
    OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), false, new OptionsBuilder().build());
    return response.getResponse();
}
```
- **`ollamaAPI.generate()`** — sends a prompt to the model and waits for full response
- **`PromptBuilder`** — builds multi-line prompts with `.addLine()` and `.addSeparator()`
- **`OptionsBuilder`** — model parameters (temperature, top_p, etc.). Empty = defaults
- Prompt is hardcoded — the stub you'd replace during the interview
- The commented-out code (lines 39-57) shows a sophisticated prompt pattern with system instructions and few-shot examples — hint for interview

**3. `ChatController.java`** — exposes endpoints:
- `GET /v1/chat/list-models` — shows installed models
- `POST /v1/chat` — returns hardcoded response (no user input yet)

### The ollama4j SDK

```
OllamaAPI          — the client: connect, generate, listModels
PromptBuilder      — build prompts: .addLine(), .addSeparator()
OptionsBuilder     — model params: temperature, topP, etc.
OllamaResult       — response wrapper: .getResponse() gets the text
Model              — model info: name, size, modified date
```

### Gotchas

- **Don't run Ollama in Docker AND natively at the same time** — they fight over port 11434. Native install takes priority.
- **Zscaler proxy blocks Docker pulls** — mount the Zscaler CA cert into the container if using Docker.
- **ollama4j SDK version matters** — v1.0.79 may not work with very new Ollama server versions. Test with curl first: `curl -s http://localhost:11434/api/tags`
- **tinyllama is fast but dumb** — good for testing, switch to llama3.1 for demo quality.

### What They'll Likely Ask You to Build

**1. Dynamic prompts** — accept user input:
```java
public String chat(String userPrompt) {
    PromptBuilder promptBuilder = new PromptBuilder().addLine(userPrompt);
    OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), false, new OptionsBuilder().build());
    return response.getResponse();
}

// Controller accepts request body
@PostMapping
public @ResponseBody String chat(@RequestBody Map<String, String> request) {
    return chatClientService.chat(request.get("prompt"));
}
```

**2. RAG (Retrieval-Augmented Generation)** — query player data, inject into prompt:
```java
public String chatAboutPlayer(String playerId, String question) {
    Player player = playerRepository.findById(playerId).orElseThrow();
    PromptBuilder promptBuilder = new PromptBuilder()
            .addLine("You are a baseball expert. Answer using this player data:")
            .addLine("Name: " + player.getFirstName() + " " + player.getLastName())
            .addLine("Born: " + player.getBirthYear() + ", " + player.getBirthCity())
            .addLine("Debut: " + player.getDebut())
            .addSeparator()
            .addLine("Question: " + question);
    OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), false, new OptionsBuilder().build());
    return response.getResponse();
}
```
RAG = **retrieve** data from DB, **augment** the prompt with it, **generate** a response. Most likely AI story because it ties both halves of the app together.

**3. Model parameters:**
```java
new OptionsBuilder()
        .setTemperature(0.7f)    // 0 = deterministic, 1 = creative
        .setTopP(0.9f)           // nucleus sampling
        .build()
```

**4. Unified chat — orchestrating DB + ML + LLM:**

The chat service becomes an orchestrator that routes user messages to the right backend:
- "Tell me about Hank Aaron" → DB lookup + LLM (RAG)
- "Find players similar to Hank Aaron" → ML service (KNN)
- General question → LLM directly

Simple approach (good for interview speed):
```java
@Service
public class ChatService {
    @Autowired private PlayerRepository playerRepository;
    @Autowired private TeamService teamService;
    @Autowired private OllamaAPI ollamaAPI;

    public String chat(String userMessage) {
        if (userMessage.toLowerCase().contains("similar") || userMessage.contains("team")) {
            return handleTeamRequest(userMessage);
        }
        return handlePlayerQuestion(userMessage);
    }

    private String handlePlayerQuestion(String userMessage) {
        // find player, build RAG prompt, call LLM
    }

    private String handleTeamRequest(String userMessage) {
        // extract player, call ML service, optionally narrate with LLM
    }
}
```

Staff-level approach (mention verbally, implement if time allows) — strategy pattern:
```java
public interface ChatHandler {
    boolean canHandle(String message);
    String handle(String message);
}

@Component
public class PlayerInfoHandler implements ChatHandler {
    public boolean canHandle(String msg) { return !msg.contains("similar"); }
    public String handle(String msg) { /* RAG lookup */ }
}

@Component
public class SimilarPlayersHandler implements ChatHandler {
    public boolean canHandle(String msg) { return msg.contains("similar"); }
    public String handle(String msg) { /* ML service call */ }
}

@Service
public class ChatService {
    @Autowired
    private List<ChatHandler> handlers;  // Spring auto-collects both

    public String chat(String message) {
        return handlers.stream()
                .filter(h -> h.canHandle(message))
                .findFirst()
                .orElseThrow()
                .handle(message);
    }
}
```

Extensible without modifying existing code. But for a 75-minute interview, the simple if/else with private methods is fine — mention the pattern verbally as a trade-off.

---

## Project Structure Summary

```
controller/          <-- HTTP endpoints (routes requests)
  PlayerController   <-- GET /v1/players, GET /v1/players/{id}
  chat/ChatController <-- POST /v1/chat, GET /v1/chat/list-models

service/             <-- Business logic (called by controllers)
  PlayerService      <-- getPlayers(), getPlayerById()
  chat/ChatClientService <-- chat(), listModels()

model/               <-- Data objects
  Player             <-- JPA entity (maps to PLAYERS table)
  Players            <-- Wrapper (holds List<Player>)

repository/          <-- Database access (auto-implemented by Spring)
  PlayerRepository   <-- extends JpaRepository, zero code

config/              <-- Bean definitions
  ChatClientConfiguration <-- OllamaAPI + RestTemplate beans

resources/
  application.yml    <-- DB config, port, settings
  schema.sql         <-- Creates table from CSV on startup
```
