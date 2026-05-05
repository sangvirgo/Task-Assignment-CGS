#!/bin/bash
# ============================================================
# TEST SCRIPT — Source Code A (Package Pricing API)
# Port: 8080
# Author: Nguyen Luu Tan Sang — CGS26_D096
# ============================================================

BASE_URL="http://localhost:8080/api/v1"
PASS=0
FAIL=0
TOTAL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ---- Generate a valid JWT (no secret mode — just needs valid format + exp) ----
# Header: {"alg":"HS256","typ":"JWT"}
# Payload: {"sub":"test","companyId":"demo-company","exp":<far future>,"iat":<now>}
JWT_HEADER=$(echo -n '{"alg":"HS256","typ":"JWT"}' | base64 | tr '+/' '-_' | tr -d '=')
EXP=$(( $(date +%s) + 3600 ))
IAT=$(date +%s)
JWT_PAYLOAD=$(echo -n "{\"sub\":\"test\",\"companyId\":\"demo-company\",\"exp\":${EXP},\"iat\":${IAT}}" \
  | base64 | tr '+/' '-_' | tr -d '=')
# Use python3 to compute proper HMAC-SHA256 signature
JWT_SIG=$(python3 -c "
import hmac, hashlib, base64
secret = b'test-secret'
msg = '${JWT_HEADER}.${JWT_PAYLOAD}'.encode()
sig = hmac.new(secret, msg, hashlib.sha256).digest()
print(base64.urlsafe_b64encode(sig).rstrip(b'=').decode())
" 2>/dev/null)

# Fallback: use python3-jwt if available
VALID_TOKEN=$(python3 -c "
import jwt, time
payload = {'sub': 'test', 'companyId': 'demo-company', 'exp': int(time.time()) + 3600, 'iat': int(time.time())}
print(jwt.encode(payload, 'test-secret', algorithm='HS256'))
" 2>/dev/null)

# If JWT lib not available, build manually (no-secret mode — just needs a signature segment)
if [ -z "$VALID_TOKEN" ]; then
  VALID_TOKEN="${JWT_HEADER}.${JWT_PAYLOAD}.${JWT_SIG}"
fi

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
  local EXTRA_CHECK="$5"   # optional: a string that must appear in body

  TOTAL=$((TOTAL + 1))

  local STATUS_OK=false
  local EXTRA_OK=true

  [ "$ACTUAL_STATUS" == "$EXPECTED_STATUS" ] && STATUS_OK=true
  if [ -n "$EXTRA_CHECK" ]; then
    echo "$RESPONSE_BODY" | grep -q "$EXTRA_CHECK" || EXTRA_OK=false
  fi

  if $STATUS_OK && $EXTRA_OK; then
    echo -e "  ${GREEN}[PASS]${NC} $TEST_NAME"
    echo -e "         Status: $ACTUAL_STATUS | Body snippet: $(echo "$RESPONSE_BODY" | head -c 120)..."
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}[FAIL]${NC} $TEST_NAME"
    echo -e "         Expected status: $EXPECTED_STATUS | Got: $ACTUAL_STATUS"
    if ! $EXTRA_OK; then
      echo -e "         Expected body to contain: '$EXTRA_CHECK'"
    fi
    echo -e "         Body: $(echo "$RESPONSE_BODY" | head -c 200)"
    FAIL=$((FAIL + 1))
  fi
}

# ---- Wait for server ----
echo -e "${YELLOW}Checking Source A on port 8080...${NC}"
for i in $(seq 1 10); do
  if curl -s "http://localhost:8080/api/v1/health" > /dev/null 2>&1; then
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

RESP=$(curl -s -w "\n%{http_code}" http://localhost:8080/api/v1/health)
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
run_test "GET /api/v1/health — should return 200 + UP status" "200" "$CODE" "$BODY" "\"status\":\"UP\""

# ======================================================
# SECTION 2: Package Price — Auth Validation
# ======================================================
print_header "2. Package Price — Authentication"

# 2.1 Missing Authorization header
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -d '{"fromDate":1748736000000,"period":1,"periodValue":3,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
run_test "POST /package-price — no auth header → 401" "401" "$CODE" "$BODY" "Missing Authorization"

# 2.2 Invalid JWT format
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer not.a.valid.token.here" \
  -d '{"fromDate":1748736000000,"period":1,"periodValue":3,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
run_test "POST /package-price — malformed JWT → 401" "401" "$CODE" "$BODY"

# 2.3 Expired JWT
EXP_PAST=$(($(date +%s) - 3600))
JWT_P=$(echo -n "{\"sub\":\"test\",\"exp\":${EXP_PAST}}" | base64 | tr '+/' '-_' | tr -d '=')
EXPIRED_TOKEN="${JWT_HEADER}.${JWT_P}.fakesig"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${EXPIRED_TOKEN}" \
  -d '{"fromDate":1748736000000,"period":1,"periodValue":3,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1)
CODE=$(echo "$RESP" | tail -n 1)
run_test "POST /package-price — expired JWT → 401" "401" "$CODE" "$BODY"

# ======================================================
# SECTION 3: Package Price — Request Validation
# ======================================================
print_header "3. Package Price — Request Validation (400 cases)"

AUTH="Authorization: Bearer $VALID_TOKEN"

# 3.1 Missing fromDate
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{"period":1,"periodValue":3,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Missing fromDate → 400" "400" "$CODE" "$BODY" "fromDate is required"

# 3.2 Missing period
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{"fromDate":1748736000000,"periodValue":3,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Missing period → 400" "400" "$CODE" "$BODY" "period is required"

# 3.3 periodValue = 0
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{"fromDate":1748736000000,"period":1,"periodValue":0,"packageId":"pkg-001"}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "periodValue=0 → 400" "400" "$CODE" "$BODY" "periodValue must be greater than 0"

# 3.4 Missing packageId
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '{"fromDate":1748736000000,"period":1,"periodValue":3}')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Missing packageId → 400" "400" "$CODE" "$BODY" "packageId is required"

# 3.5 Empty body
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d '')
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Empty body → 400" "400" "$CODE" "$BODY"

# ======================================================
# SECTION 4: Package Price — Happy Path
# ======================================================
print_header "4. Package Price — Successful Calculations"

# 4.1 Minimal valid request (monthly, 3 months)
NOW_MS=$(python3 -c "import time; print(int(time.time()*1000))")
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":3,\"packageId\":\"pkg-001\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Valid request — period=MONTH, periodValue=3 → 200" "200" "$CODE" "$BODY" "Package price calculated"

# Check price math: officialPrice=80, periodValue=3 → packageTotalPrice=240
echo "$BODY" | python3 -c "
import sys, json
d = json.load(sys.stdin).get('data', {})
total = d.get('packageTotalPrice')
price = d.get('packagePrice')
print(f'  >> packagePrice={price}, packageTotalPrice={total}')
" 2>/dev/null

# 4.2 Yearly period
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":2,\"periodValue\":1,\"packageId\":\"pkg-002\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Valid request — period=YEAR, periodValue=1 → 200" "200" "$CODE" "$BODY" "Package price calculated"

# 4.3 With custom item list
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{
    \"fromDate\":${NOW_MS},
    \"period\":1,
    \"periodValue\":6,
    \"packageId\":\"pkg-003\",
    \"list\":[
      {\"itemDesignId\":\"item-001\",\"spaceIdList\":[\"space-1\",\"space-2\"]},
      {\"rackDesignId\":\"item-002\",\"spaceIdList\":[\"space-3\"]}
    ]
  }")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "With item list (customizeItem) → 200" "200" "$CODE" "$BODY" "Package price calculated"

# Verify customizePlanogram is present
echo "$BODY" | python3 -c "
import sys, json
d = json.load(sys.stdin).get('data', {})
cp = d.get('customizePlanogram')
dp = d.get('defaultPlanogram')
print(f'  >> customizePlanogram present: {cp is not None}, defaultPlanogram present: {dp is not None}')
if cp:
    print(f'     totalCustomPrice={cp.get(\"totalPrice\")}, items={len(cp.get(\"itemInfoList\",[]))}')
" 2>/dev/null

# 4.4 Verify toDate is calculated correctly (period=1, periodValue=2 → +2 months)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":2,\"packageId\":\"pkg-date-test\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "toDate calculation — period=MONTH, periodValue=2 → 200" "200" "$CODE" "$BODY"
echo "$BODY" | python3 -c "
import sys, json, datetime
d = json.load(sys.stdin).get('data', {})
fd = d.get('fromDate'); td = d.get('toDate')
if fd and td:
    diff_days = (td - fd) / 86400000
    print(f'  >> fromDate→toDate diff ≈ {diff_days:.1f} days (~60 days expected for 2 months)')
" 2>/dev/null

# ======================================================
# SECTION 5: Payment Option fields verification
# ======================================================
print_header "5. Response Fields Verification"

RESP=$(curl -s -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":3,\"packageId\":\"pkg-verify\"}")

python3 -c "
import json, sys
body = '''$RESP'''
try:
    d = json.loads(body).get('data', {})
    fields = ['packageId','packageName','fromDate','toDate','packagePrice',
              'packageTotalPrice','subTotal','packageFullPaymentPrice',
              'fullPaymentOptionDiscount','packagePayMonthlyPrice',
              'payMonthlyOptionDiscount','payMonthlyPrice','taxPercent','tax']
    missing = [f for f in fields if f not in d]
    present = [f for f in fields if f in d]
    print(f'  Fields present ({len(present)}/{len(fields)}): {present}')
    if missing:
        print(f'  Missing fields: {missing}')
    else:
        print('  All expected fields present!')
except Exception as e:
    print(f'  Could not parse: {e}')
" 2>/dev/null

# ======================================================
# SECTION 6: Edge Cases
# ======================================================
print_header "6. Edge Cases"

# 6.1 Invalid period value (period=3 is not valid)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":3,\"periodValue\":1,\"packageId\":\"pkg-edge\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
# DateTimeUtil throws IllegalArgumentException → caught by catch(IllegalArgumentException) → 400
run_test "Invalid period=3 (not 1 or 2) → 400 BAD_REQUEST" "400" "$CODE" "$BODY" "Unexpected period value"

# 6.2 Large periodValue
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":120,\"packageId\":\"pkg-large\"}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Large periodValue=120 months → 200" "200" "$CODE" "$BODY" "Package price calculated"

# 6.3 Empty list array (should behave same as no list)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/package-price" \
  -H "Content-Type: application/json" \
  -H "$AUTH" \
  -d "{\"fromDate\":${NOW_MS},\"period\":1,\"periodValue\":3,\"packageId\":\"pkg-empty-list\",\"list\":[]}")
BODY=$(echo "$RESP" | head -n -1); CODE=$(echo "$RESP" | tail -n 1)
run_test "Empty list array → 200 (no customize items)" "200" "$CODE" "$BODY" "Package price calculated"

# ======================================================
# SUMMARY
# ======================================================
echo ""
echo -e "${CYAN}══════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  SOURCE A — TEST SUMMARY${NC}"
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