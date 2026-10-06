# Fund Transfer Service — Quarkus edition

A retail-banking fund-transfer service (REST API demo), **ported from
Spring Boot 3.2 to Quarkus 3.15**. Functionality, REST contract, error shape,
JWT scheme and seeded data are preserved one-to-one; only the framework
plumbing changed.

Stack: **Java 17 · Quarkus 3.15 · Quarkus REST (JAX-RS) · Hibernate ORM with
Panache · H2 (in-memory) · jjwt · BCrypt**.

---

## Running

Dev mode (live reload):

```bash
mvn quarkus:dev
```

Packaged JAR:

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

Native image (optional, GraalVM / Mandrel):

```bash
mvn package -Dnative
./target/fund-transfer-1.0.0-runner
```

The service listens on `http://localhost:8080`. The schema is rebuilt on every
boot (`drop-and-create`) and the startup seeders load 20 customers and 10
merchant accounts (skipped under the `test` profile).

> **Build note:** the Quarkus platform is resolved from Maven Central on the
> first build, so an initial build needs internet access to your Maven
> repository/mirror.

---

## Authentication

1. `POST /api/auth/login` with `{"username","password"}` → `{ "token", "tokenType":"Bearer", "expiresInSeconds" }`.
2. Send `Authorization: Bearer <token>` on every `/api/**` endpoint.

Sample logins (same as the Spring Boot original):

| Username | Password      | Key account(s)            | Notes                               |
|----------|---------------|---------------------------|-------------------------------------|
| `alice`  | `password1`   | ACC1001 (5000), ACC1002   | Beneficiary **id 1** → Bob (ACC2001)|
| `bob`    | `password2`   | ACC2001 (800)             | No beneficiaries                    |
| `dave`   | `password123` | ACC4001 (50)              | Low balance → insufficient-funds    |
| `karl`   | `password123` | ACC11001 (0)              | Zero balance                        |

All other seeded customers use `password123`. See `config/DataSeeder.java` for
the full set.

---

## Endpoints

| Method | Path                           | Auth            | Purpose                                 |
|--------|--------------------------------|-----------------|-----------------------------------------|
| POST   | `/api/auth/login`              | public          | Obtain a JWT                            |
| GET    | `/api/accounts`                | Bearer          | List the caller's accounts              |
| GET    | `/api/accounts/{accountNumber}`| Bearer          | Fetch one owned account                 |
| GET    | `/api/beneficiaries`           | Bearer          | List beneficiaries                      |
| GET    | `/api/beneficiaries/{id}`      | Bearer          | Fetch one beneficiary                   |
| POST   | `/api/beneficiaries`           | Bearer          | Add a beneficiary                       |
| PUT    | `/api/beneficiaries/{id}`      | Bearer          | Update a beneficiary                    |
| DELETE | `/api/beneficiaries/{id}`      | Bearer          | Delete a beneficiary                    |
| POST   | `/api/transfers`               | Bearer          | P2P transfer to a saved beneficiary     |
| GET    | `/api/transactions`            | Bearer          | History (`month=YYYY-MM` or `from`/`to`, `page`, `size`) |
| POST   | `/internal/payments/merchant`  | `X-Service-Key` | Service-to-service merchant payment     |

The bundled Postman collections under `postman/` work unchanged.

---

## Spring Boot → Quarkus mapping

| Concern                     | Spring Boot (original)                                   | Quarkus (this project)                                              |
|-----------------------------|----------------------------------------------------------|--------------------------------------------------------------------|
| Web layer                   | `spring-boot-starter-web`, `@RestController`, `@GetMapping`… | `quarkus-rest`, JAX-RS `@Path`/`@GET`/`@POST`…                      |
| JSON                        | Jackson (auto)                                           | `quarkus-rest-jackson`                                              |
| Request/response            | `ResponseEntity`, `@RequestBody`, `@PathVariable`, `@RequestParam`, `@RequestHeader` | `Response`, method body param, `@PathParam`, `@QueryParam`, `@HeaderParam` |
| Persistence                 | `spring-boot-starter-data-jpa`, `JpaRepository` + derived queries | `quarkus-hibernate-orm-panache`, `PanacheRepository` + explicit queries |
| Pessimistic lock            | `@Lock(PESSIMISTIC_WRITE)` on a repo method             | `find(...).withLock(LockModeType.PESSIMISTIC_WRITE)`               |
| Paging                      | `Pageable` / `Page`                                     | `io.quarkus.panache.common.Page` + a neutral `PageResponse`        |
| Entities                    | `jakarta.persistence.*`                                  | **unchanged**                                                      |
| DI + beans                  | `@Service`, `@Component`, `@Configuration`/`@Bean`, `@Autowired` | `@ApplicationScoped`, constructor injection (ArC/CDI)              |
| Transactions                | Spring `@Transactional(readOnly, rollbackFor)`          | `jakarta.transaction.Transactional(rollbackOn=…)`                  |
| Config injection            | `@Value("${...}")`                                      | `@ConfigProperty(name="...")` (MicroProfile Config)                |
| Validation                  | `@Valid` + `MethodArgumentNotValidException` handler    | `quarkus-hibernate-validator` + `ExceptionMapper<ConstraintViolationException>` |
| Exception handling          | `@RestControllerAdvice` + `@ExceptionHandler`           | JAX-RS `@Provider ExceptionMapper<…>` (Api / Validation / Generic) |
| Security config             | `SecurityConfig` + `SecurityFilterChain`                | path rules folded into the JWT `ContainerRequestFilter`            |
| JWT auth                    | `OncePerRequestFilter` + `SecurityContextHolder`        | `ContainerRequestFilter` + a custom `SecurityContext`              |
| Authenticated principal     | `@AuthenticationPrincipal UserDetails`                  | `@Context SecurityContext` → `getUserPrincipal().getName()`         |
| Login auth                  | `AuthenticationManager` + `UserDetailsService`          | direct lookup in `AuthServiceImpl` via `CustomerRepository` + `PasswordEncoder` |
| Password hashing            | `BCryptPasswordEncoder` bean                            | `PasswordEncoder` interface + `BcryptPasswordEncoder` (`at.favre.lib:bcrypt`, strength 10) |
| Startup seeding             | `CommandLineRunner` bean, `@Profile("!test")`           | `@Observes StartupEvent`, `@UnlessBuildProfile("test")`            |
| Error status codes          | `org.springframework.http.HttpStatus` in `ErrorCode`    | plain `int` + reason phrase in `ErrorCode`                         |
| App entry point             | `@SpringBootApplication` main class                     | provided by the Quarkus runtime (no main class needed)             |
| Config file                 | `application.properties` (`spring.*`)                   | `application.properties` (`quarkus.*`)                             |

### Behavioural notes

- **Error body** is byte-for-byte the same shape: `{ timestamp, status, error, code, message, details }`, with the same `FT-*` codes and HTTP statuses.
- **Unauthenticated** requests to protected paths get a bare `401` (matching the original stateless entry point); business errors get the JSON error body.
- **Failed-transfer audit record:** as in the original, the failed `Transaction` is written and then the method throws, so the surrounding transaction rolls the record back — behaviour is preserved exactly (`@Transactional(rollbackOn = Exception.class)`).
- **No H2 web console:** Spring's `/h2-console` has no Quarkus equivalent; use Quarkus Dev Services / Dev UI (`/q/dev`) in dev mode instead.
- **Tests:** a Quarkus integration test (`FundTransferIntegrationTest`, `@QuarkusTest` + REST Assured) covers login, scoping, transfer, insufficient-funds and merchant payment. The original Spring unit tests (MockMvc / `@WebMvcTest` / `spring-security-test`) were framework-specific and are not carried over verbatim.
