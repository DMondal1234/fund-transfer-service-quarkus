# Fund Transfer — changes required for HungerBox (Quarkus edition)

To let HungerBox pay vendors, Fund Transfer needs **merchant payment** support.
These changes are **additive** — the existing P2P transfer path is untouched.
This is the Quarkus change list; the equivalent Spring Boot list lived in the
original project. Terminology maps as in the main README's Spring→Quarkus table.

## New files (drop in)

| File | Purpose |
|------|---------|
| `domain/Merchant.java` | Merchant entity (`merchantCode`, `businessName`, `active`) |
| `domain/TransactionType.java` | enum `P2P`, `MERCHANT` |
| `repository/MerchantRepository.java` | Panache repository, `findByMerchantCode` |
| `dto/MerchantPaymentRequest.java` | `{sourceAccountNumber, merchantAccountNumber, amount, reference}` |
| `dto/MerchantPaymentResponse.java` | `{transactionId, status, sourceBalanceAfter, reference}` |
| `service/MerchantPaymentService.java` | interface |
| `service/impl/MerchantPaymentServiceImpl.java` | `@ApplicationScoped`; debit customer, credit merchant; audit-records a FAILED MERCHANT txn on insufficient funds |
| `controller/InternalPaymentController.java` | JAX-RS resource `POST /internal/payments/merchant`, checks `X-Service-Key` |
| `config/MerchantDataSeeder.java` | `@Observes StartupEvent` seeder for merchants + accounts MER5001–MER5010 |

## Modified files

**`domain/Account.java`** — an account may now be owned by a Customer OR a Merchant:
- `customer` relationship is **nullable** (`@ManyToOne` without `optional=false`, `@JoinColumn(name="customer_id")` without `nullable=false`).
- add a nullable `@ManyToOne Merchant merchant` (`@JoinColumn(name="merchant_id")`).
- add a second constructor `Account(String, AccountType, BigDecimal, Merchant)`.
- add `isMerchantAccount()` and `getOwnerName()` helpers.
- The existing customer constructor and all existing callers are unchanged. The P2P path never calls `getCustomer()` on the *destination*, so crediting a merchant account works with the existing transfer machinery. (Entity annotations are `jakarta.persistence.*` — identical to the Spring version.)

**`domain/Transaction.java`** — add `TransactionType type` (non-null) and `String reference`:
- keep the existing 6-arg constructor (it now delegates, defaulting `type=P2P`, `reference=null`), so `TransferServiceImpl` needs **no change**.
- add an 8-arg constructor for merchant payments; add getters; add a `reference` index.

**`domain/ErrorCode.java`** — add two constants. Note the Quarkus `ErrorCode` holds the status as an `int` + reason phrase (not Spring's `HttpStatus`):
```java
MERCHANT_ACCOUNT_NOT_FOUND("FT-MER-001", 404, "Not Found", "Merchant account not found"),
SERVICE_UNAUTHORIZED("FT-MER-002", 401, "Unauthorized", "Invalid or missing service key"),
```

**`repository/TransactionRepository.java`** — add the idempotency lookup as an explicit Panache query (import `TransactionStatus`, `Optional`):
```java
public Optional<Transaction> findFirstByReferenceAndStatus(String reference, TransactionStatus status) {
    return find("reference = ?1 and status = ?2", reference, status).firstResultOptional();
}
```

**`security/JwtAuthenticationFilter.java`** — permit the internal path. In Quarkus the path rules live in the JWT request filter (there is no `SecurityConfig`); `/internal` is already in its public prefixes:
```java
private static final List<String> PUBLIC_PREFIXES = List.of("/api/auth", "/internal", "/q");
```
(The `InternalPaymentResource` enforces the `X-Service-Key` itself.)

**`application.properties`** — add:
```properties
app.internal.api-key=hungerbox-to-fundtransfer-shared-key-2026
```
This must equal HungerBox's `app.fund-transfer.service-key`. The internal
controller reads it with `@ConfigProperty(name = "app.internal.api-key")`.

## Already handled (was "optional hardening" in the Spring version)

**`service/impl/BeneficiaryServiceImpl.java`** — `validateLinkedAccount` must not
call `getCustomer().getUsername()` on a merchant account (NPE). This port already
guards it, and additionally rejects merchant accounts as beneficiaries outright:
```java
if (account.isMerchantAccount()) {
    throw new BusinessException(ErrorCode.BENEFICIARY_MERCHANT_ACCOUNT, "...");
}
if (account.getCustomer() != null && account.getCustomer().getUsername().equals(username)) {
    throw new BusinessException(ErrorCode.BENEFICIARY_OWN_ACCOUNT, "...");
}
```

## New endpoint

`POST /internal/payments/merchant`  (header `X-Service-Key: <key>`)
```json
{ "sourceAccountNumber":"ACC1001", "merchantAccountNumber":"MER5001",
  "amount":126.00, "reference":"ORD-ABCD1234" }
```
→ `200 { "transactionId":.., "status":"SUCCESS", "sourceBalanceAfter":.., "reference":".." }`
· `422` insufficient funds (records a FAILED MERCHANT txn) · `404` unknown account
· `401` bad/missing service key.

> **Idempotency note:** as in the original, the idempotency-by-`reference` block in
> `MerchantPaymentServiceImpl` is present but commented out, so a repeated
> `reference` is **not** currently short-circuited. The `findFirstByReferenceAndStatus`
> query and the `reference` index are in place to enable it — uncomment the guard
> to make repeated references return the original result instead of charging again.
