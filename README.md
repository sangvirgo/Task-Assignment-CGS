# CGS Task Assignment — Source Code A & B
**Candidate:** Nguyen Luu Tan Sang | **Code:** CGS26_D096 | **Role:** Software Engineer

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Prerequisites](#2-prerequisites)
3. [Project Structure](#3-project-structure)
4. [Build Instructions](#4-build-instructions)
5. [Run Instructions](#5-run-instructions)
6. [API Endpoints](#6-api-endpoints)
7. [Running Unit Tests](#7-running-unit-tests)
8. [Running Shell Integration Tests](#8-running-shell-integration-tests)
9. [Environment Variables](#9-environment-variables)
10. [Known Limitations](#10-known-limitations)

---

## 1. Project Overview

| | Source Code A | Source Code B |
|---|---|---|
| **Purpose** | Package Pricing API | Package Pricing + Payment API |
| **Port** | 8080 | 8081 |
| **Framework** | Vert.x 5.0.8 / Java 17 | Vert.x 5.0.8 / Java 17 |
| **Build** | Gradle + Shadow JAR | Gradle + Shadow JAR |

Both projects are independent HTTP microservices. Source B extends Source A by adding a full payment checkout lifecycle (preview → confirm → pending → result).

---

## 2. Prerequisites

- **Java 17+**
- **Gradle** (wrapper included — `./gradlew`)
- **Bash** (for running `test.sh`)
- **Python 3** (optional — used inside `test.sh` for JSON parsing)
- **curl** (for `test.sh`)

---

## 3. Project Structure

```
Source code A/
├── build.gradle.kts
├── test.sh                          ← Shell integration test script
└── src/
    ├── main/java/com/demo/packing/
    │   ├── MainVerticle.java         ← HTTP server entry point
    │   ├── handler/
    │   │   ├── HealthHandler.java
    │   │   ├── PackageHandler.java
    │   │   └── RouterHandler.java    ← JWT middleware
    │   ├── service/
    │   │   ├── IPackagePricingService.java
    │   │   ├── PackagePricingServiceImpl.java
    │   │   ├── IItemRevenueConfigRepo.java
    │   │   └── ItemRevenueConfigRepoImpl.java  ← Mock (no DB)
    │   ├── dto/                      ← Request/Response objects
    │   ├── record/                   ← Immutable data records
    │   └── util/                     ← DateTimeUtil, PaymentOption
    └── test/java/com/demo/packing/
        ├── TestMainVerticle.java
        ├── handler/RouterHandlerTest.java
        ├── service/PackagePricingServiceImplTest.java
        └── util/DateTimeUtilTest.java

Source code B/
├── build.gradle.kts
├── test.sh                          ← Shell integration test script
└── src/
    ├── main/java/com/demo/
    │   ├── packing/                  ← Same as Source A
    │   └── payment/
    │       ├── handler/PaymentHandler.java
    │       ├── service/
    │       │   ├── IPaymentService.java
    │       │   └── PaymentServiceImpl.java
    │       ├── dto/                  ← Payment DTOs
    │       └── util/PaymentMethod.java
    └── test/java/com/demo/
        ├── packing/
        │   ├── TestMainVerticle.java
        │   └── service/PackagePricingServiceImplTest.java
        └── payment/
            ├── PaymentServiceImplTest.java
            └── PaymentApiIntegrationTest.java
```

---

## 4. Build Instructions

Run these commands from inside each project directory.

### Source Code A
```bash
cd "Source code A"
./gradlew shadowJar
```

### Source Code B
```bash
cd "Source code B"
./gradlew shadowJar
```

The fat JAR will be output to:
```
build/libs/*-fat.jar
```

---

## 5. Run Instructions

> **Important:** Start Source A first (port 8080), then Source B (port 8081).  
> Both must have `JWT_SECRET` set to the same value if you want JWT signature validation.

### Source Code A — Port 8080
```bash
cd "Source code A"

# Background (recommended)
nohup java -DPORT=8080 -DJWT_SECRET=test-secret \
  -jar build/libs/*-fat.jar com.demo.packing.MainVerticle \
  > logs-a.txt 2>&1 &

echo "Source A PID: $!"
```

### Source Code B — Port 8081
```bash
cd "Source code B"

# Background (recommended)
nohup java -DPORT=8081 -DJWT_SECRET=test-secret \
  -jar build/libs/*-fat.jar com.demo.packing.MainVerticle \
  > logs-b.txt 2>&1 &

echo "Source B PID: $!"
```

### Verify servers are up
```bash
curl http://localhost:8080/api/v1/health
# Expected: {"status":"UP","service":"package-api-service"}

curl http://localhost:8081/api/v1/health
# Expected: {"status":"UP","service":"package-api-service"}
```

### Stop servers
```bash
# Find and kill by port
kill $(lsof -t -i:8080)
kill $(lsof -t -i:8081)
```

---

## 6. API Endpoints

### Source Code A — Base URL: `http://localhost:8080/api/v1`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health` | None | Health check |
| POST | `/package-price` | JWT required | Calculate package rental price |

#### POST `/package-price` — Request Body
```json
{
  "fromDate": 1748000000000,
  "period": 1,
  "periodValue": 3,
  "packageId": "pkg-001",
  "list": [
    {
      "itemDesignId": "item-001",
      "spaceIdList": ["space-1", "space-2"]
    }
  ]
}
```
> `period`: `1` = monthly, `2` = yearly

---

### Source Code B — Base URL: `http://localhost:8081/api/v1`

Includes all Source A endpoints, plus:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/checkout/payment-preview` | None | Calculate final pay amount |
| POST | `/checkout/confirm` | None | Create payment transaction |
| GET | `/checkout/pending/:transactionId` | None | Poll transaction status |
| POST | `/checkout/payment-result` | None | Apply gateway result |

#### POST `/checkout/payment-preview`
```json
{
  "orderId": "ORD-001",
  "subTotal": 24.5,
  "shippingFee": 1.5,
  "voucherCode": "SAVE10",
  "paymentMethod": 0,
  "walletAmount": 3
}
```
> `paymentMethod`: `0` = ABA, `1` = Cash on Delivery  
> `voucherCode`: `SAVE10` (10% off) or `SAVE5` (5% off)

#### POST `/checkout/confirm`
```json
{
  "orderId": "ORD-001",
  "expectedPayAmount": 20.55,
  "paymentMethod": 0,
  "idempotencyKey": "unique-key-001"
}
```

#### GET `/checkout/pending/:transactionId`
```
GET /api/v1/checkout/pending/TXN1748000000000
```

#### POST `/checkout/payment-result`
```json
{
  "transactionId": "TXN1748000000000",
  "success": true
}
```
> On failure: add `"reason": "insufficient funds"`

---

## 7. Running Unit Tests

### Run all tests (with output)
```bash
# Source A
cd "Source code A"
./gradlew test --rerun-tasks

# Source B
cd "Source code B"
./gradlew test --rerun-tasks
```

### Expected results
```
# Source A
RouterHandlerTest          — 12 tests  PASSED
PackagePricingServiceImplTest — 14 tests PASSED
DateTimeUtilTest           — 9 tests   PASSED
TestMainVerticle           — 1 test    PASSED

# Source B
PaymentServiceImplTest     — 49 tests  PASSED
PaymentApiIntegrationTest  — 7 tests   PASSED
PackagePricingServiceImplTest — 14 tests PASSED
TestMainVerticle           — 1 test    PASSED
```

### View HTML test report
```bash
# After running tests, open in browser:
open "Source code A/build/reports/tests/test/index.html"
open "Source code B/build/reports/tests/test/index.html"
```

---

## 8. Running Shell Integration Tests

> **Requires:** Both servers must be running before executing `test.sh`.  
> See [Section 5](#5-run-instructions) to start the servers first.

### Source Code A — test.sh
Tests: Health check, Package Price API (valid/invalid JWT, various request combinations)
```bash
cd "Source code A"
chmod +x test.sh
./test.sh
```

### Source Code B — test.sh
Tests all payment endpoints end-to-end:
- Health check
- Package price (with JWT)
- Payment preview (vouchers, wallet, edge cases)
- Payment confirm (ABA vs COD)
- Payment pending (poll status)
- Payment result (success and failure)
- Full E2E flow: preview → confirm → pending → result

```bash
cd "Source code B"
chmod +x test.sh
./test.sh
```

### Expected test.sh output format
```
══════════════════════════════════════════════════
  1. Health Check
══════════════════════════════════════════════════
  [PASS] GET /api/v1/health → 200 + UP
  ...
══════════════════════════════════════════════════
  SOURCE B — TEST SUMMARY
══════════════════════════════════════════════════
  Total : 24
  Passed: 24
  Failed: 0

  ✓ All tests passed!
```

---

## 9. Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | HTTP server port (set via `-DPORT=xxxx`) |
| `JWT_SECRET` | *(none)* | HMAC-SHA256 secret for JWT signature validation. If not set, any JWT with a non-empty signature is accepted. |

### JWT Requirements
All requests to `/package-price` must include a valid JWT in the `Authorization` header:
```
Authorization: Bearer <jwt-token>
```
The JWT must:
- Use algorithm `HS256`
- Have a valid `exp` claim (not expired)
- Have a valid signature if `JWT_SECRET` is configured

### Generating a test JWT (Python)
```bash
pip install pyjwt
python3 -c "
import jwt, time
token = jwt.encode(
  {'sub': 'test', 'companyId': 'demo-company', 'exp': int(time.time()) + 3600},
  'test-secret', algorithm='HS256'
)
print(token)
"
```

---

## 10. Known Limitations

| # | Limitation | Impact |
|---|-----------|--------|
| 1 | **No database** — all data is mocked or in-memory | Payment transactions are lost on server restart |
| 2 | **Discount always 0%** — `ItemRevenueConfigRepoImpl` returns null | Discount fields are present but always zero |
| 3 | **Hardcoded vouchers** — only `SAVE10` and `SAVE5` supported | No voucher management system |
| 4 | **No idempotency deduplication** — `idempotencyKey` is accepted but not checked for duplicates | Same key can create multiple transactions |
| 5 | **No logging** — no structured logs | Debugging requires `nohup` output only |
| 6 | **Single-node only** — `ConcurrentHashMap` is not distributed | Cannot scale horizontally |

---

## Quick Start (Copy-paste)

```bash
# === BUILD BOTH ===
cd "Source code A" && ./gradlew shadowJar && cd ..
cd "Source code B" && ./gradlew shadowJar && cd ..

# === RUN BOTH ===
cd "Source code A"
nohup java -DPORT=8080 -DJWT_SECRET=test-secret -jar build/libs/*-fat.jar com.demo.packing.MainVerticle > /dev/null 2>&1 &
cd ..

cd "Source code B"
nohup java -DPORT=8081 -DJWT_SECRET=test-secret -jar build/libs/*-fat.jar com.demo.packing.MainVerticle > /dev/null 2>&1 &
cd ..

# === VERIFY ===
sleep 2
curl http://localhost:8080/api/v1/health
curl http://localhost:8081/api/v1/health

# === TEST ===
cd "Source code A" && ./test.sh && cd ..
cd "Source code B" && ./test.sh && cd ..
```

---

*Report: see `CGS26_D096_TechnicalReport.docx` for full architecture, flow diagrams, code quality analysis, and improvement proposals.*
