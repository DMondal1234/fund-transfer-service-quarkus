# Code Walkthrough — Fund Transfer Service (Quarkus edition)

A file-by-file tour of the Quarkus port, in the order a request flows through it:
configuration → bootstrap/security → domain → repositories → DTOs → services →
resources → exception mapping. Where a class behaves differently from the Spring
Boot original, the difference is called out.

## Contents

- [Configuration](#configuration) — `application.properties`, `pom.xml`
- [Bootstrap](#bootstrap) — entry point, seeders
- [Security](#security) — `JwtService`, `PasswordEncoder`, `JwtAuthenticationFilter`
- [Domain](#domain-entities)
- [Repositories](#repositories)
- [DTOs](#dtos)
- [Services](#services)
- [Resources (controllers)](#resources-controllers)
- [Exceptions](#exceptions)

---

# Configuration

## application.properties

Quarkus config keys replace the Spring `spring.*` keys:

```properties
quarkus.http.port=8080
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:fundtransfer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
quarkus.hibernate-orm.database.generation=drop-and-create   # == Spring's create-drop
app.security.jwt.secret=...        # custom keys, read via @ConfigProperty
app.security.jwt.expiration-minutes=60
app.internal.api-key=...
```

`quarkus.hibernate-orm.database.generation=drop-and-create` rebuilds the schema
from the entities on each boot, exactly like `ddl-auto=create-drop`. The three
`app.*` keys are plain MicroProfile Config properties injected where needed.

## pom.xml

Key dependencies and why:

- `quarkus-rest` + `quarkus-rest-jackson` — the Quarkus REST engine and its
  Jackson JSON binding. This is the extension that provides the JAX-RS
  annotations (`@Path`, `@GET`, …) you see on the resources.
- `quarkus-hibernate-orm-panache` — Hibernate ORM with the Panache repository
  pattern.
- `quarkus-jdbc-h2` — the H2 JDBC driver.
- `quarkus-hibernate-validator` — Bean Validation (`@Valid`, `@NotBlank`, …).
- `jjwt-api/impl/jackson` — the same JWT library as the original.
- `at.favre.lib:bcrypt` — BCrypt hashing (replaces Spring Security's encoder).
- `quarkus-junit5` + `rest-assured` — testing.

The `quarkus-maven-plugin` wires in `build`, `generate-code` and
`generate-code-tests`. A `native` profile is included for GraalVM/Mandrel builds.

---

# Bootstrap

## No main class

There is **no `FundTransferApplication`**. Quarkus owns the entry point and the
lifecycle; you just provide beans. (You can add an `io.quarkus.runtime.QuarkusMain`
class if you want custom startup args, but it is not needed here.)

## config/DataSeeder.java

Seeds 20 customers with a spread of balances and beneficiary links.

- `@ApplicationScoped` CDI bean; the seeding runs from
  `void seed(@Observes StartupEvent event)` — the Quarkus equivalent of Spring's
  `CommandLineRunner`.
- `@UnlessBuildProfile("test")` excludes the bean under the test profile, exactly
  as Spring's `@Profile("!test")` did, so tests own their fixtures.
- `@Transactional` wraps the insert; it guards on `customerRepository.count() > 0`
  so it only seeds an empty DB.
- **Deterministic IDs:** Bob is persisted before Alice, so Alice's single
  beneficiary is always id `1` (the Postman collection relies on it). With H2
  IDENTITY generation, `persist` inserts immediately in call order, preserving it.

## config/MerchantDataSeeder.java

Same shape — an `@Observes StartupEvent`, `@UnlessBuildProfile("test")`,
`@Transactional` bean — that seeds merchants and their settlement accounts
(MER5001–MER5010). Additive and independent of the customer seeder.

---

# Security

## security/JwtService.java

Unchanged jjwt logic, now a CDI bean.

- `@ApplicationScoped`; config comes in through the constructor with
  `@ConfigProperty(name = "app.security.jwt.secret")` and
  `...expiration-minutes` (replacing Spring's `@Value`).
- `generateToken` builds an HS256 token whose subject is the username;
  `extractUsername` / `isValid` parse and verify it. API is jjwt 0.12
  (`Jwts.builder()…signWith(key)`, `Jwts.parser().verifyWith(key)…`).

## security/PasswordEncoder.java + BcryptPasswordEncoder.java

A small interface + `@ApplicationScoped` implementation replacing Spring
Security's `PasswordEncoder` bean. `BcryptPasswordEncoder` uses
`at.favre.lib:bcrypt` at strength 10 (matching `BCryptPasswordEncoder`'s default)
with `encode` and `matches`.

## security/UsernamePrincipal.java

A tiny `java.security.Principal` record whose `getName()` is the username. This is
what resources read via `SecurityContext.getUserPrincipal()`.

## security/JwtAuthenticationFilter.java

The heart of the security port. One `@Provider @Priority(AUTHENTICATION)`
`ContainerRequestFilter` does the job of **both** the Spring
`JwtAuthenticationFilter` and `SecurityConfig`:

1. Reads `Authorization: Bearer <token>`, validates it with `JwtService`, and on
   success installs a custom `SecurityContext` (`requestContext.setSecurityContext`)
   whose principal is a `UsernamePrincipal`.
2. Enforces the path rules: public prefixes `("/api/auth", "/internal", "/q")` are
   open; any other path without a valid principal is aborted with a bare `401`
   (matching the original stateless `HttpStatusEntryPoint`).

There is no `SecurityContextHolder`, no `UserDetailsService`, and no
`OncePerRequestFilter` — the JAX-RS `SecurityContext` carries the identity, and
resources read it with `@Context SecurityContext`.

---

# Domain (entities)

The entities are **identical** to the Spring version — all `jakarta.persistence.*`,
which Quarkus's Hibernate ORM uses unchanged.

- `Customer` — `@OneToMany` accounts + beneficiaries (cascade all).
- `Account` — owned by a `Customer` **or** a `Merchant` (exactly one); `@Version`
  for optimistic locking; `debit`/`credit`/`hasSufficientFunds`/`isMerchantAccount`.
- `Beneficiary` — nickname + account number + owning customer; unique on
  `(customer, accountNumber)`.
- `Transaction` — immutable ledger row (source/dest account numbers, amount,
  timestamp, `TransactionStatus`, `TransactionType`, optional `reference`,
  description); indexes on source, timestamp, reference.
- `AccountType`, `TransactionStatus`, `TransactionType` — enums.
- `ErrorCode` — the one Spring-touching file that changed: it now carries the HTTP
  status as an `int` + reason phrase instead of `org.springframework.http.HttpStatus`
  (which also makes 422 straightforward).

---

# Repositories

Spring Data interfaces became **Panache repository classes** (`@ApplicationScoped`,
`implements PanacheRepository<T>`), with each derived query written explicitly.

- `CustomerRepository` — `findByUsername` → `find("username", u).firstResultOptional()`;
  `existsByUsername` → `count("username", u) > 0`.
- `AccountRepository` — `findByAccountNumber`, `findByCustomerUsername`
  (`list("customer.username", u)`), and the locked read
  `findByAccountNumberForUpdate` →
  `find("accountNumber", n).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional()`.
- `BeneficiaryRepository` — `findByCustomerUsername`, `findByIdAndCustomerUsername`,
  `existsByCustomerUsernameAndAccountNumber`.
- `MerchantRepository` — `findByMerchantCode`.
- `TransactionRepository` — `findFirstByReferenceAndStatus`, and `findHistory`
  which returns a `PanacheQuery<Transaction>` built from a named-parameter query
  (`:accountNumbers in … and (:from is null or …) and (:to is null or …)`) so the
  service can page it and read the count. This mirrors the original JPQL one-to-one.

---

# DTOs

All request/response DTOs are Java `record`s and are unchanged, **except**:

- `PageResponse` — no longer wraps Spring's `Page`; it is a neutral record built
  with `PageResponse.of(content, page, size, totalElements)` that computes
  `totalPages` itself.
- `AccountResponse`, `TransferResponse`, `MerchantPaymentResponse`,
  `TransactionResponse`, `BeneficiaryResponse` — static `from(...)` factories map
  entity → DTO (Streams + method references in the services).

---

# Services

Interfaces in `service/`, implementations in `service/impl/` (the original's
layout is preserved). Implementations are `@ApplicationScoped`; transactions use
`jakarta.transaction.Transactional`.

- **AuthServiceImpl** — the biggest change. With no `AuthenticationManager`, it
  looks the customer up via `CustomerRepository` and verifies the password with
  `PasswordEncoder.matches`; on failure it throws `InvalidCredentialsException`
  (replacing Spring's `BadCredentialsException`), then mints a JWT.
- **AccountServiceImpl** — lists / fetches the caller's own accounts, scoping by
  username and 404-ing otherwise.
- **BeneficiaryServiceImpl** — full CRUD enforcing the "logical link": the account
  must exist, must not be a merchant account, and must not be the caller's own;
  no duplicates. Uses `persist`/`delete` (Panache) instead of `save`.
- **TransferServiceImpl** (the core) — `@Transactional(rollbackOn = Exception.class)`:
  loads the beneficiary (scoped to caller), loads the source account **under a
  pessimistic write lock**, checks ownership, same-account and balance rules,
  then debits/credits and persists a SUCCESS `Transaction`. On insufficient funds
  it persists a FAILED row and throws `422`. (As in the original, that FAILED row
  is rolled back with the throw — behaviour preserved.)
- **MerchantPaymentServiceImpl** — the same locked debit/credit pattern from a
  customer account to a merchant settlement account; records a FAILED MERCHANT txn
  on insufficient funds. Idempotency-by-reference is present but commented out
  (see `FUND_TRANSFER_CHANGES.md`).
- **TransactionHistoryServiceImpl** — resolves the caller's account numbers, then
  pages the `findHistory` query with `io.quarkus.panache.common.Page.of(page, size)`
  and maps to `TransactionResponse`.

---

# Resources (controllers)

JAX-RS resource classes (still in the `controller` package). Constructor injection
of services; `@Context SecurityContext` supplies the authenticated username via
`getUserPrincipal().getName()` (replacing `@AuthenticationPrincipal UserDetails`).

- **AuthController** — `POST /api/auth/login`, `@Valid LoginRequest` → `LoginResponse`.
- **AccountController** — `GET /api/accounts`, `GET /api/accounts/{accountNumber}`.
- **BeneficiaryController** — GET list, GET one, POST (`201` + `Location`), PUT,
  DELETE (`204`). Specific status codes use `jakarta.ws.rs.core.Response`.
- **TransferController** — `POST /api/transfers` → `201 Created`.
- **TransactionController** — `GET /api/transactions` with `month` **or**
  `from`/`to` (parsed from strings; a bad format → `400` via a `BusinessException`)
  plus `page`/`size` query params (defaults `0`/`20`, replacing Spring's `Pageable`).
- **InternalPaymentController** — `POST /internal/payments/merchant`; reads the
  shared key with `@HeaderParam("X-Service-Key")` and the expected key via
  `@ConfigProperty`, throwing `SERVICE_UNAUTHORIZED` on mismatch.

---

# Exceptions

Custom exceptions (`ApiException` base + `ResourceNotFoundException`,
`InsufficientBalanceException`, `BusinessException`, and the new
`InvalidCredentialsException`) are unchanged in spirit — each carries an
`ErrorCode`.

The Spring `@RestControllerAdvice` (`GlobalExceptionHandler`) is replaced by three
JAX-RS `@Provider ExceptionMapper`s producing the identical `ErrorResponse` shape:

- **ApiExceptionMapper** — maps any `ApiException`, taking code + HTTP status from
  its `ErrorCode`.
- **ValidationExceptionMapper** — maps `ConstraintViolationException` to a `400`
  with one `details` line per invalid field (the `@Valid` equivalent of Spring's
  `MethodArgumentNotValidException` handler). Registered explicitly so it overrides
  Quarkus' built-in validation mapper and keeps the API's error shape.
- **GenericExceptionMapper** — catch-all `500`, but passes `WebApplicationException`s
  (e.g. a 404 for an unknown route) through unchanged.
