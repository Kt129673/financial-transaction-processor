# Financial Transaction Processor

A **high-throughput financial transaction processing system** built with Spring Boot 3.4.4 and Java 21. Designed to accept batches of up to 10,000 transactions via a REST API, validate and deduplicate them, and process money transfers asynchronously using a configurable thread pool.

Built as a production-grade coding assignment demonstrating **Clean Architecture**, **SOLID principles**, and **enterprise-level Java development practices**.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        REST API Layer                               │
│  POST /api/v1/transactions/process → HTTP 202 Accepted              │
│  TransactionController → @Valid → TransactionService                │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ Returns immediately with batchId
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Service Layer (Orchestrator)                    │
│  TransactionServiceImpl → generates UUID → delegates to async      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ @Async("transactionExecutor")
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  Async Processing Pipeline                          │
│                                                                     │
│  1. Validate (Stream API) ──→ FAILED records persisted              │
│  2. Deduplicate (2s window) ─→ FAILED records persisted             │
│  3. Group by sourceAccountId (Collectors.groupingBy)                │
│  4. Process each transaction:                                       │
│     ├─ Lock accounts (PESSIMISTIC_WRITE, ordered by ID)             │
│     ├─ Check balance ──→ FAILED if insufficient                     │
│     ├─ Debit source, Credit target                                  │
│     └─ Save as SUCCESS                                              │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Persistence Layer                               │
│  Spring Data JPA → Hibernate → H2 Database                         │
│  AccountRepository (PESSIMISTIC_WRITE lock)                         │
│  TransactionRepository (batch queries)                              │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Separate `@Async` bean** | Spring's `@Async` requires proxy interception — self-invocation bypasses the proxy |
| **Deadlock prevention** | Always lock accounts in ascending ID order to break circular wait |
| **`BigDecimal` for money** | `double`/`float` have rounding errors (`0.1 + 0.2 ≠ 0.3`) |
| **`EnumType.STRING`** | Ordinal storage breaks when new enum values are inserted |
| **Constructor injection** | No `@Autowired` on fields — all dependencies via `@RequiredArgsConstructor` |
| **Interface → Impl pattern** | Dependency Inversion Principle — controllers depend on abstractions |

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language runtime |
| Spring Boot | 3.4.4 | Application framework |
| Spring Data JPA | 3.4.x | ORM and repository abstraction |
| Hibernate | 6.6.x | JPA implementation |
| H2 Database | 2.3.x | In-memory SQL database |
| Lombok | Latest | Boilerplate reduction |
| Jakarta Validation | 3.1.x | Request validation |
| JUnit 5 | 5.11.x | Testing framework |
| Mockito | 5.x | Mock framework |
| JaCoCo | 0.8.12 | Code coverage |
| Maven | 3.9+ | Build tool |

---

## Folder Structure

```
src/main/java/com/assignment/transaction/
├── FinancialTransactionProcessorApplication.java   # Entry point (@EnableAsync)
├── async/
│   └── TransactionProcessorAsync.java              # @Async batch processing pipeline
├── config/
│   └── AsyncConfig.java                            # ThreadPoolTaskExecutor (10/20/1000)
├── controller/
│   └── TransactionController.java                  # REST endpoint (POST → 202)
├── dto/
│   ├── BatchTransactionRequest.java                # Request wrapper (@Valid, @Size)
│   └── TransactionRequestDTO.java                  # Single transaction DTO
├── entity/
│   ├── Account.java                                # JPA entity (BigDecimal, @Version)
│   └── Transaction.java                            # JPA entity (status, batchId)
├── exception/
│   ├── AccountNotFoundException.java               # 404 exception
│   ├── GlobalExceptionHandler.java                 # @RestControllerAdvice
│   └── InsufficientBalanceException.java           # 422 exception
├── mapper/
│   └── TransactionMapper.java                      # DTO ↔ Entity conversion
├── model/
│   ├── DeduplicationResult.java                    # Unique/duplicate partition
│   ├── TransactionStatus.java                      # Enum: SUCCESS, FAILED, PENDING
│   └── ValidationResult.java                       # Valid/invalid partition
├── repository/
│   ├── AccountRepository.java                      # PESSIMISTIC_WRITE lock
│   └── TransactionRepository.java                  # Batch and status queries
├── response/
│   ├── ApiErrorResponse.java                       # Standardized error format
│   └── BatchTransactionResponse.java               # 202 response (batchId, status)
├── service/
│   ├── TransactionService.java                     # Interface
│   └── impl/
│       └── TransactionServiceImpl.java             # Orchestrator
├── util/                                           # Utility classes
└── validator/
    ├── TransactionDeduplicator.java                # Duplicate detection (2s window)
    └── TransactionValidator.java                   # Stream-based validation (6 rules)

src/test/java/com/assignment/transaction/
├── FinancialTransactionProcessorApplicationTests.java  # Smoke test
├── TransactionPerformanceTest.java                     # 1K/5K benchmarks
├── controller/
│   └── TransactionControllerTest.java                  # MockMvc (7 tests)
├── repository/
│   ├── AccountRepositoryTest.java                      # @DataJpaTest (6 tests)
│   └── TransactionRepositoryTest.java                  # @DataJpaTest (6 tests)
├── service/
│   └── TransactionServiceImplTest.java                 # Mockito (4 tests)
├── util/
│   └── TestDataGenerator.java                          # Test data factory
└── validator/
    ├── TransactionDeduplicatorTest.java                # Unit tests (10 tests)
    └── TransactionValidatorTest.java                   # Unit tests (10 tests)
```

---

## How to Run

### Prerequisites

- **Java 21** (verify: `java -version`)
- **Maven 3.9+** (verify: `mvn -version`)

### Start the Application

```bash
# Clone the repository
git clone <repository-url>
cd financial-transaction-processor

# Build and run
mvn clean install
mvn spring-boot:run
```

The application starts at **http://localhost:8080**.

### H2 Console

Access the database UI at **http://localhost:8080/h2-console**:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:transactiondb` |
| Username | `sa` |
| Password | *(empty)* |

---

## API Endpoint

### POST `/api/v1/transactions/process`

Accepts a batch of financial transactions for asynchronous processing.

**Request:**

```json
{
  "transactions": [
    {
      "sourceAccountId": 1,
      "targetAccountId": 2,
      "amount": 500,
      "timestamp": "2026-07-20T10:00:00"
    },
    {
      "sourceAccountId": 3,
      "targetAccountId": 4,
      "amount": 1000.50,
      "timestamp": "2026-07-20T10:01:00"
    }
  ]
}
```

**Response (HTTP 202 Accepted):**

```json
{
  "batchId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "Processing Started"
}
```

### Sample cURL

```bash
curl -X POST http://localhost:8080/api/v1/transactions/process \
  -H "Content-Type: application/json" \
  -d '{
    "transactions": [
      {
        "sourceAccountId": 1,
        "targetAccountId": 2,
        "amount": 500,
        "timestamp": "2026-07-20T10:00:00"
      }
    ]
  }'
```

### Error Responses

**400 Bad Request — Validation Error:**

```json
{
  "timestamp": "2026-07-20T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/transactions/process",
  "validationErrors": [
    "sourceAccountId: Source account ID must not be null",
    "amount: Amount must be positive"
  ]
}
```

**400 Bad Request — Malformed JSON:**

```json
{
  "timestamp": "2026-07-20T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Malformed JSON request body",
  "path": "/api/v1/transactions/process"
}
```

---

## Seed Data

The application ships with 10 pre-configured accounts (loaded via `data.sql`):

| ID | Owner | Balance | Currency |
|---|---|---|---|
| 1 | Alice Johnson | 10,000.00 | USD |
| 2 | Bob Smith | 25,000.00 | USD |
| 3 | Charlie Brown | 50,000.00 | USD |
| 4 | Diana Prince | 100,000.00 | USD |
| 5 | Edward Norton | 15,000.00 | EUR |
| 6 | Fiona Apple | 30,000.00 | EUR |
| 7 | George Martin | 8,000.00 | EUR |
| 8 | Hannah Lee | 20,000.00 | GBP |
| 9 | Ivan Petrov | 45,000.00 | GBP |
| 10 | Julia Roberts | 500.00 | GBP |

---

## How to Run Tests

```bash
# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=TransactionValidatorTest

# Run tests with verbose output
mvn test -Dsurefire.useFile=false
```

### Test Summary (46 tests)

| Test Class | Count | Type | Focus |
|---|---|---|---|
| `AccountRepositoryTest` | 6 | `@DataJpaTest` | CRUD, pessimistic lock, seed data |
| `TransactionRepositoryTest` | 6 | `@DataJpaTest` | Queries, bulk save, failure reason |
| `TransactionValidatorTest` | 10 | JUnit 5 | All 6 validation rules + edge cases |
| `TransactionDeduplicatorTest` | 10 | JUnit 5 | 2s boundary, amounts, sources |
| `TransactionServiceImplTest` | 4 | Mockito | UUID generation, async delegation |
| `TransactionControllerTest` | 7 | MockMvc | HTTP 202, 400 errors, malformed JSON |
| `TransactionPerformanceTest` | 2 | `@SpringBootTest` | 1,000 and 5,000 transaction batches |
| `FinancialTransactionProcessorApplicationTests` | 1 | `@SpringBootTest` | Context smoke test |

---

## JaCoCo Report

Generate the code coverage report:

```bash
mvn clean test jacoco:report
```

View the report at: `target/site/jacoco/index.html`

---

## Async Thread Pool Configuration

| Parameter | Value | Rationale |
|---|---|---|
| Core Pool Size | 10 | Matches HikariCP's default connection pool |
| Max Pool Size | 20 | Burst capacity when queue fills up |
| Queue Capacity | 1000 | Buffers tasks before scaling to max |
| Thread Prefix | `txn-processor-` | Identifiable in logs and thread dumps |
| Await Termination | 60 seconds | Graceful shutdown on application stop |

---

## Validation Rules (Stream API)

| # | Rule | Failure Reason |
|---|---|---|
| 1 | Amount is null | "Amount is null" |
| 2 | Amount ≤ 0 | "Negative or zero amount" |
| 3 | Source account ID is null | "Source account ID is null" |
| 4 | Target account ID is null | "Target account ID is null" |
| 5 | Source = Target | "Source and target accounts are the same" |
| 6 | Timestamp is null | "Timestamp is null" |

## Deduplication Rules

A transaction is a **duplicate** if another transaction exists with:
- Same source account ID
- Same target account ID
- Same amount
- Timestamp within **2 seconds**

Duplicates are persisted as `FAILED` with reason `"Duplicate Transaction"`.

---

## License

This project is a coding assignment for educational purposes.
