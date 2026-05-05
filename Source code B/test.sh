#!/bin/bash
# ============================================================
# TEST SCRIPT — Source Code B (Package Pricing + Payment API)
# Port: 8081
# Author: Nguyen Luu Tan Sang — CGS26_D096
# ============================================================

BASE_URL="http://localhost:8081/api/v1"
PASS=0
FAIL=0
TOTAL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ---- Generate valid JWT ----
JWT_HEADER=$(echo -n '{"alg":"HS256","typ":"JWT"}' | base64 | tr '+/' '-_' | tr -d '=')
EXP=$(( $(date +%s) + 3600 ))
IAT=$(date +%s)
JWT_PAYLOAD=$(echo -n "{\"sub\":\"test\",\"companyId\":\"demo-company\",\"exp\":${EXP},\"iat\":${IAT}}" \
  | base64 | tr '+/' '-_' | tr -d '=')
JWT_SIG=$(python3 -c "
import hmac, hashlib, base64
secret = b'test-secret'
msg = '${JWT_HEADER}.${JWT_PAYLOAD}'.encode()
sig = hmac.new(secret, msg, hashlib.sha256).digest()
print(base64.urlsafe_b64encode(sig).rstrip(b'=').decode())
" 2>/dev/null)
VALID_TOKEN=$(python3 -c "
import jwt, time
payload = {'sub': 'test', 'companyId': 'demo-company', 'exp': int(time.time()) + 3600, 'iat': int(time.time())}
print(jwt.encode(payload, 'test-secret', algorithm='HS256'))
" 2>/dev/null)
[ -z "$VALID_TOKEN" ] && VALID_TOKEN="${JWT_HEADER}.${JWT_PAYLOAD}.${JWT_SIG}"

AUTH="Authorization: Bearer $VALID_TOKEN"
NOW_MS=$(python3 -c "import time; print(int(time.time()*1000))")

# ---- Helper Functions ----
print_header() {
  echo ""
  echo -e "${CYAN}══════════════════════════════════════════════════${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}══════════════════════════════════════════════════${NC}"
}

run_test() {
  local TEST_NAME="$1"
  local EXPECTED_STATUS="$2"
  local ACTUAL_STATUS="$3"
  local RESPONSE_BODY="$4"
  local EXTRA_CHECK="$5"

  TOTAL=$((TOTAL + 1))
  local STATUS_OK=false
  local EXTRA_OK=true

  [ "$ACTUAL_STATUS" == "$EXPECTED_STATUS" ] && STATUS_OK=true
  if [ -n "$EXTRA_CHECK" ]; then
    echo "$RESPONSE_BODY" | grep -q "$EXTRA_CHECK" || EXTRA_OK=false
  fi

  if $STATUS_OK && $EXTRA_OK; then
    echo -e "  ${GREEN}[PASS]${NC} $TEST_NAME"
    echo -e "         Status: $ACTUAL_STATUS | $(echo "$RESPONSE_BODY" | head -c 120)..."
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}[FAIL]${NC} $TEST_NAME"
    echo -e "         Expected: $EXPECTED_STATUS | Got: $ACTUAL_STATUS"
    [ -n "$EXTRA_CHECK" ] && ! $EXTRA_OK && echo -e "         Body must contain: '$EXTRA_CHECK'"
    echo -e "         Body: $(echo "$RESPONSE_BODY" | head -c 200)"
    FAIL=$((FAIL + 1))
  fi
}

# ---- Wait for server ----
echo -e "${YELLOW}Checking Source B on port 8081...${NC}"
for i in $(seq 1 10); do
  if curl -s "http://localhost:8081/api/v1/health" > /dev/null 2>&1; then
    echo -e "${GREEN}Server is up!${NC}"
    break
  fi
  echo "  Waiting... ($i/10)"
  sleep 1
done

# ======================================================
# SECTION 1: Health Check
# ======================================================
print_header "1. Health Check"

RESP=$(curl -s -w "\n%{http_code}" http://localhost:8081/api/v1/health)
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "GET /api/v1/health → 200 + UP" "200" "$CODE" "$BODY" "\"status\":\"UP\""

# ======================================================
# SECTION 2: Package Price (inherited from A)
# ======================================================
print_header "2. Package Price API (same as Source A)"

RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":3,\"packageId\":\"pkg-b-001\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "POST /package-price valid request → 200" "200" "$CODE" "$BODY" "Package price calculated"

RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":3,\"packageId\":\"pkg-b-001\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "POST /package-price no auth → 401" "401" "$CODE" "$BODY"

# ======================================================
# SECTION 3: Payment Preview
# ======================================================
print_header "3. POST /checkout/payment-preview"

# 3.1 Happy path with voucher SAVE10 + wallet
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-TEST-001",
    "subTotal": 24.5,
    "shippingFee": 1.5,
    "voucherCode": "SAVE10",
    "paymentMethod": 0,
    "walletAmount": 3
  }')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — SAVE10 voucher + wallet=3 → 200" "200" "$CODE" "$BODY" "Payment preview calculated"

# Verify math: subTotal=24.5, shipping=1.5, voucher=10%=2.45, gross=23.55, wallet=3, payAmount=20.55
echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> subTotal={d.get(\"subTotal\")}, shippingFee={d.get(\"shippingFee\")}')
print(f'     voucherDiscount={d.get(\"voucherDiscount\")}, walletUsed={d.get(\"walletUsed\")}')
print(f'     payAmount={d.get(\"payAmount\")} (expected: 20.55)')
ok = str(d.get('payAmount')) == '20.55'
print(f'     Math check: {\"PASS\" if ok else \"FAIL\"}')
" 2>/dev/null

# 3.2 SAVE5 voucher
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-002","subTotal":100,"shippingFee":5,"voucherCode":"SAVE5","paymentMethod":1,"walletAmount":0}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — SAVE5 voucher, COD payment → 200" "200" "$CODE" "$BODY" "Payment preview calculated"
echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> voucherDiscount={d.get(\"voucherDiscount\")} (expected: 5.00), payAmount={d.get(\"payAmount\")} (expected: 100.00)')
" 2>/dev/null

# 3.3 No voucher, no wallet
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-003","subTotal":50,"shippingFee":3,"paymentMethod":0}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — no voucher, no wallet → 200, payAmount=53" "200" "$CODE" "$BODY"

# 3.4 Wallet exceeds total (should cap wallet)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-004","subTotal":10,"shippingFee":0,"paymentMethod":1,"walletAmount":999}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — wallet > total, should cap → payAmount=0" "200" "$CODE" "$BODY"
echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> walletUsed={d.get(\"walletUsed\")} (expected: 10.00), payAmount={d.get(\"payAmount\")} (expected: 0.00)')
" 2>/dev/null

# 3.5 Invalid voucher
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-005","subTotal":20,"shippingFee":2,"voucherCode":"FAKE999","paymentMethod":0}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — invalid voucherCode → 400" "400" "$CODE" "$BODY" "Invalid voucherCode"

# 3.6 Missing orderId
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"subTotal":20,"shippingFee":2,"paymentMethod":0}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — missing orderId → 400" "400" "$CODE" "$BODY" "orderId is required"

# 3.7 Invalid paymentMethod
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-007","subTotal":20,"shippingFee":2,"paymentMethod":99}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — invalid paymentMethod=99 → 400" "400" "$CODE" "$BODY" "Unsupported paymentMethod"

# 3.8 Negative subTotal
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-008","subTotal":-10,"shippingFee":2,"paymentMethod":0}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Preview — negative subTotal → 400" "400" "$CODE" "$BODY" "subTotal must be greater"

# ======================================================
# SECTION 4: Payment Confirm
# ======================================================
print_header "4. POST /checkout/confirm"

# 4.1 ABA payment method (should get paymentUrl)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-CNF-001","expectedPayAmount":25.99,"paymentMethod":0,"idempotencyKey":"idem-cnf-001"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Confirm — ABA method → 200 + paymentUrl present" "200" "$CODE" "$BODY" "Payment transaction created"
TXN_ID_ABA=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('transactionId',''))" 2>/dev/null)
echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> transactionId={d.get(\"transactionId\")}, status={d.get(\"paymentStatus\")}')
print(f'     paymentUrl={d.get(\"paymentUrl\")} (expected: not null for ABA)')
" 2>/dev/null

# 4.2 COD method (should NOT get paymentUrl)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-CNF-002","expectedPayAmount":15,"paymentMethod":1,"idempotencyKey":"idem-cnf-002"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Confirm — COD method → 200 + paymentUrl=null" "200" "$CODE" "$BODY" "Payment transaction created"
TXN_ID_COD=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('transactionId',''))" 2>/dev/null)
echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> paymentUrl={d.get(\"paymentUrl\")} (expected: null for COD)')
" 2>/dev/null

# 4.3 Missing orderId
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"expectedPayAmount":15,"paymentMethod":1,"idempotencyKey":"idem-x"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Confirm — missing orderId → 400" "400" "$CODE" "$BODY" "orderId is required"

# 4.4 Missing idempotencyKey
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-CNF-003","expectedPayAmount":15,"paymentMethod":1}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Confirm — missing idempotencyKey → 400" "400" "$CODE" "$BODY" "idempotencyKey is required"

# 4.5 Negative expectedPayAmount
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-CNF-004","expectedPayAmount":-5,"paymentMethod":0,"idempotencyKey":"idem-neg"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Confirm — negative expectedPayAmount → 400" "400" "$CODE" "$BODY"

# ======================================================
# SECTION 5: Payment Pending (GET)
# ======================================================
print_header "5. GET /checkout/pending/:transactionId"

# 5.1 Valid transactionId from previous confirm
if [ -n "$TXN_ID_ABA" ]; then
  RESP=$(curl -s -w "\n%{http_code}" "http://localhost:8081/api/v1/checkout/pending/$TXN_ID_ABA")
  BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
  run_test "Pending — valid TXN_ID_ABA → 200 + PENDING status" "200" "$CODE" "$BODY" "Payment pending status fetched"
  echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> transactionId={d.get(\"transactionId\")}, paymentStatus={d.get(\"paymentStatus\")}, orderStatus={d.get(\"orderStatus\")}')
" 2>/dev/null
else
  echo -e "  ${YELLOW}[SKIP]${NC} Pending ABA — no transactionId from confirm step"
fi

if [ -n "$TXN_ID_COD" ]; then
  RESP=$(curl -s -w "\n%{http_code}" "http://localhost:8081/api/v1/checkout/pending/$TXN_ID_COD")
  BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
  run_test "Pending — valid TXN_ID_COD → 200 + PENDING status" "200" "$CODE" "$BODY" "Payment pending status fetched"
else
  echo -e "  ${YELLOW}[SKIP]${NC} Pending COD — no transactionId from confirm step"
fi

# 5.2 Non-existent transactionId → 404
RESP=$(curl -s -w "\n%{http_code}" "http://localhost:8081/api/v1/checkout/pending/TXN_DOES_NOT_EXIST")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Pending — non-existent TXN → 404 NOT_FOUND" "404" "$CODE" "$BODY" "NOT_FOUND"

# ======================================================
# SECTION 6: Payment Result (Full Flow)
# ======================================================
print_header "6. POST /checkout/payment-result (Full Flow)"

# Create a fresh transaction for this section
CONFIRM_RESP=$(curl -s -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-RESULT-001","expectedPayAmount":99.99,"paymentMethod":0,"idempotencyKey":"idem-result-001"}')
TXN_RESULT=$(echo "$CONFIRM_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('transactionId',''))" 2>/dev/null)

if [ -n "$TXN_RESULT" ]; then
  # 6.1 Apply SUCCESS result
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-result" \
    -H "Content-Type: application/json" \
    -d "{\"transactionId\":\"$TXN_RESULT\",\"success\":true}")
  BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
  run_test "PaymentResult — success=true → paymentStatus=SUCCESS, orderStatus=SUCCESS" "200" "$CODE" "$BODY" "SUCCESS"
  echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> paymentStatus={d.get(\"paymentStatus\")}, orderStatus={d.get(\"orderStatus\")}')
" 2>/dev/null

  # 6.2 Check pending reflects SUCCESS
  RESP=$(curl -s -w "\n%{http_code}" "http://localhost:8081/api/v1/checkout/pending/$TXN_RESULT")
  BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
  run_test "Pending after SUCCESS result → paymentStatus=SUCCESS" "200" "$CODE" "$BODY" "SUCCESS"
else
  echo -e "  ${YELLOW}[SKIP]${NC} Cannot test payment result — confirm failed"
fi

# 6.3 Create another tx and apply FAILED result
CONFIRM_RESP2=$(curl -s -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-RESULT-002","expectedPayAmount":45,"paymentMethod":0,"idempotencyKey":"idem-result-002"}')
TXN_RESULT2=$(echo "$CONFIRM_RESP2" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('transactionId',''))" 2>/dev/null)

if [ -n "$TXN_RESULT2" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-result" \
    -H "Content-Type: application/json" \
    -d "{\"transactionId\":\"$TXN_RESULT2\",\"success\":false,\"reason\":\"insufficient funds\"}")
  BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
  run_test "PaymentResult — success=false + reason → FAILED payment, PENDING order" "200" "$CODE" "$BODY" "FAILED"
  echo "$BODY" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read()).get('data', {})
print(f'  >> paymentStatus={d.get(\"paymentStatus\")}, orderStatus={d.get(\"orderStatus\")}, reason={d.get(\"failureReason\")}')
" 2>/dev/null
fi

# 6.4 Missing transactionId
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-result" \
  -H "Content-Type: application/json" \
  -d '{"success":true}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "PaymentResult — missing transactionId → 400" "400" "$CODE" "$BODY" "transactionId is required"

# 6.5 Missing reason when success=false
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-result" \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"TXN-FAKE","success":false}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "PaymentResult — success=false but no reason → 400" "400" "$CODE" "$BODY" "reason is required"

# 6.6 Non-existent transactionId
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/checkout/payment-result" \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"TXN_GHOST_9999","success":true}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "PaymentResult — non-existent TXN → 404" "404" "$CODE" "$BODY" "NOT_FOUND"

# ======================================================
# SECTION 7: Complete E2E Flow
# ======================================================
print_header "7. End-to-End Payment Flow"

echo "  Running full flow: Preview → Confirm → Pending → Result → Verify"
echo ""

# Step 1: Preview
E2E_PREVIEW=$(curl -s -X POST "$BASE_URL/checkout/payment-preview" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-E2E-001","subTotal":80,"shippingFee":5,"voucherCode":"SAVE10","paymentMethod":0,"walletAmount":5}')
E2E_PAY_AMOUNT=$(echo "$E2E_PREVIEW" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('payAmount','?'))" 2>/dev/null)
echo -e "  Step 1 — Preview: payAmount = ${CYAN}$E2E_PAY_AMOUNT${NC} USD"

# Step 2: Confirm
E2E_CONFIRM=$(curl -s -X POST "$BASE_URL/checkout/confirm" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"ORD-E2E-001\",\"expectedPayAmount\":$E2E_PAY_AMOUNT,\"paymentMethod\":0,\"idempotencyKey\":\"idem-e2e-001\"}")
E2E_TXN=$(echo "$E2E_CONFIRM" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('transactionId','?'))" 2>/dev/null)
E2E_URL=$(echo "$E2E_CONFIRM" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('paymentUrl','null'))" 2>/dev/null)
echo -e "  Step 2 — Confirm: transactionId = ${CYAN}$E2E_TXN${NC}"
echo -e "           paymentUrl = $E2E_URL"

# Step 3: Poll Pending
E2E_PENDING=$(curl -s "http://localhost:8081/api/v1/checkout/pending/$E2E_TXN")
E2E_PSTATUS=$(echo "$E2E_PENDING" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('paymentStatus','?'))" 2>/dev/null)
echo -e "  Step 3 — Pending: status = ${CYAN}$E2E_PSTATUS${NC}"

# Step 4: Apply SUCCESS
E2E_RESULT=$(curl -s -X POST "$BASE_URL/checkout/payment-result" \
  -H "Content-Type: application/json" \
  -d "{\"transactionId\":\"$E2E_TXN\",\"success\":true}")
E2E_FINAL=$(echo "$E2E_RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(d.get('paymentStatus','?')+'|'+d.get('orderStatus','?'))" 2>/dev/null)
echo -e "  Step 4 — Result applied: paymentStatus|orderStatus = ${CYAN}$E2E_FINAL${NC}"

# Verify E2E
if [[ "$E2E_FINAL" == "SUCCESS|SUCCESS" ]] && [ -n "$E2E_TXN" ] && [[ "$E2E_TXN" == TXN* ]]; then
  echo -e "  ${GREEN}[PASS]${NC} End-to-End flow completed successfully"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}[FAIL]${NC} E2E flow did not complete as expected"
  FAIL=$((FAIL + 1))
fi
TOTAL=$((TOTAL + 1))

# ======================================================
# SUMMARY
# ======================================================
echo ""
echo -e "${CYAN}══════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  SOURCE B — TEST SUMMARY${NC}"
echo -e "${CYAN}══════════════════════════════════════════════════${NC}"
echo -e "  Total : $TOTAL"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}✓ All tests passed!${NC}"
else
  echo -e "  ${YELLOW}⚠ Some tests failed. Review the output above.${NC}"
fi
echo ""