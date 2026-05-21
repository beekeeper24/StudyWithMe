#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env. Create it from .env.example and fill in OAuth client values." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-oauth}"

required_vars=(
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  KAKAO_CLIENT_ID
  KAKAO_CLIENT_SECRET
)

missing_vars=()
for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    missing_vars+=("$var_name")
  fi
done

if (( ${#missing_vars[@]} > 0 )); then
  printf 'Missing required env vars: %s\n' "${missing_vars[*]}" >&2
  exit 1
fi

./gradlew bootRun --no-daemon --console=plain
