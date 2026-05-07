#!/usr/bin/env bash
set -uo pipefail

# Test script for dev.sh
# Validates Requirement 3.3: dev.sh displays usage when invoked without a valid argument

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEV_SCRIPT="$PROJECT_ROOT/dev.sh"

PASS_COUNT=0
FAIL_COUNT=0

pass() {
  echo "PASS: $1"
  ((PASS_COUNT++))
}

fail() {
  echo "FAIL: $1"
  ((FAIL_COUNT++))
}

# Test 1: ./dev.sh with no arguments exits non-zero and prints "Usage"
test_no_args() {
  local output exit_code

  output=$("$DEV_SCRIPT" 2>&1) && exit_code=0 || exit_code=$?

  if [[ $exit_code -ne 0 ]]; then
    pass "dev.sh with no arguments exits with non-zero status (exit code: $exit_code)"
  else
    fail "dev.sh with no arguments should exit non-zero but exited with 0"
  fi

  if echo "$output" | grep -qi "Usage"; then
    pass "dev.sh with no arguments prints Usage message"
  else
    fail "dev.sh with no arguments should print 'Usage' but output was: $output"
  fi
}

# Test 2: ./dev.sh invalid exits non-zero and prints "Usage"
test_invalid_arg() {
  local output exit_code

  output=$("$DEV_SCRIPT" invalid 2>&1) && exit_code=0 || exit_code=$?

  if [[ $exit_code -ne 0 ]]; then
    pass "dev.sh with invalid argument exits with non-zero status (exit code: $exit_code)"
  else
    fail "dev.sh with invalid argument should exit non-zero but exited with 0"
  fi

  if echo "$output" | grep -qi "Usage"; then
    pass "dev.sh with invalid argument prints Usage message"
  else
    fail "dev.sh with invalid argument should print 'Usage' but output was: $output"
  fi
}

# Run tests
echo "=== Testing dev.sh ==="
echo ""

test_no_args
echo ""
test_invalid_arg

echo ""
echo "=== Results: $PASS_COUNT passed, $FAIL_COUNT failed ==="

if [[ $FAIL_COUNT -gt 0 ]]; then
  exit 1
fi

exit 0
