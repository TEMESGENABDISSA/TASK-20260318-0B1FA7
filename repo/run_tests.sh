#!/usr/bin/env sh
set -u

echo "========================================"
echo " Anju Backend Test Runner"
echo "========================================"

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/pure_backend"

if [ ! -d "$BACKEND_DIR" ]; then
  echo "[FAIL] Backend directory not found: $BACKEND_DIR"
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "[FAIL] Maven (mvn) is not installed or not in PATH"
  exit 1
fi

TOTAL=3
PASSED=0
FAILED=0

run_step() {
  STEP_NAME="$1"
  STEP_CMD="$2"

  echo ""
  echo ">>> Running: $STEP_NAME"
  sh -c "$STEP_CMD"
  CODE=$?

  if [ $CODE -eq 0 ]; then
    echo "[PASS] $STEP_NAME"
    PASSED=$((PASSED + 1))
  else
    echo "[FAIL] $STEP_NAME (exit=$CODE)"
    FAILED=$((FAILED + 1))
  fi
}

run_step "Unit tests" "cd \"$BACKEND_DIR\" && mvn -q -Dtest='*ServiceTest,*StateMachineTest' test"
run_step "API tests" "cd \"$BACKEND_DIR\" && mvn -q -Dtest='*ApiTest' test"
run_step "Security RBAC tests" "cd \"$BACKEND_DIR\" && mvn -q -Dtest='*RbacTest' test"

echo ""
echo "========================================"
echo " Test Summary"
echo "----------------------------------------"
echo " Total : $TOTAL"
echo " Passed: $PASSED"
echo " Failed: $FAILED"
echo "========================================"

if [ $FAILED -eq 0 ]; then
  echo "FINAL RESULT: PASS"
  exit 0
else
  echo "FINAL RESULT: FAIL"
  exit 1
fi
