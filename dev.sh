#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# --- Usage ---
usage() {
  echo "Usage: ./dev.sh <command>"
  echo ""
  echo "Commands:"
  echo "  backend    Start the Kotlin Spring Boot backend (hot-reload)"
  echo "  frontend   Start the Next.js frontend (HMR)"
  echo ""
  echo "Examples:"
  echo "  ./dev.sh backend"
  echo "  ./dev.sh frontend"
  exit 1
}

# --- Load .env file ---
load_env() {
  local env_file="$1"
  if [[ -f "$env_file" ]]; then
    echo "Loading environment from $env_file"
    set -a
    # shellcheck disable=SC1090
    source "$env_file"
    set +a
  else
    echo "Warning: $env_file not found — using system environment variables"
  fi
}

# --- Backend command ---
run_backend() {
  # Check Java 21+
  if ! command -v java &>/dev/null; then
    echo "Error: Java is not installed."
    echo "Install Java 21+ (e.g., via SDKMAN: sdk install java 21-tem)"
    exit 1
  fi

  local java_version
  java_version=$(java -version 2>&1 | head -n1 | sed -E 's/.*"([0-9]+).*/\1/')
  if [[ "$java_version" -lt 21 ]]; then
    echo "Error: Java 21+ is required (found Java $java_version)."
    echo "Install Java 21+ (e.g., via SDKMAN: sdk install java 21-tem)"
    exit 1
  fi

  # Load backend environment
  load_env "$SCRIPT_DIR/.env"

  # Change to api directory
  cd "$SCRIPT_DIR/api"

  echo "Building backend (skipping tests)..."
  ./gradlew build -x test

  echo "Starting backend with hot-reload (continuous build)..."
  ./gradlew bootRun --continuous
}

# --- Frontend command ---
run_frontend() {
  # Check Node.js
  if ! command -v node &>/dev/null; then
    echo "Error: Node.js is not installed."
    echo "Install Node.js 20+ (https://nodejs.org/)"
    exit 1
  fi

  # Check npm
  if ! command -v npm &>/dev/null; then
    echo "Error: npm is not installed."
    echo "Install Node.js 20+ which includes npm (https://nodejs.org/)"
    exit 1
  fi

  # Load frontend environment
  load_env "$SCRIPT_DIR/.env.local"

  # Change to frontend directory
  cd "$SCRIPT_DIR/frontend"

  echo "Installing frontend dependencies..."
  npm install

  echo "Starting frontend dev server..."
  npm run dev
}

# --- Main ---
if [[ $# -eq 0 ]]; then
  usage
fi

case "${1}" in
  backend)
    run_backend
    ;;
  frontend)
    run_frontend
    ;;
  *)
    usage
    ;;
esac
