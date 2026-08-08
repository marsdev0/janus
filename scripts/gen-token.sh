#!/usr/bin/env bash
#
# Generate a Janus virtual API key. Prints the plaintext key (shown ONCE — save it),
# its SHA256 (for token.key_hash), the display prefix, and a ready-to-run INSERT.
#
# Usage:
#   ./scripts/gen-token.sh [name] [models] [quota_limit]
#   ./scripts/gen-token.sh                        # name=demo, models="" (all), quota=1000000
#   ./scripts/gen-token.sh alice "gpt-4o,qwen2.5" 500000
#
# key_hash = SHA256(plaintext key) hex — identical to AuthFilter#sha256, so it is safe
# to write the printed hash directly into token.key_hash.
#
# ⚠️ The plaintext key is printed only here; it is never stored in the DB or logs.
#    Lose it and you must regenerate.
#
set -euo pipefail

NAME="${1:-demo}"
MODELS="${2:-}"                 # empty = all models (written as NULL)
QUOTA="${3:-1000000}"

# 256-bit random -> urlsafe base64 without padding (~43 chars), high entropy
RAND=$(openssl rand -base64 32 | tr '+/' '-_' | tr -d '=' | tr -d '\n')
KEY="sk-janus-${RAND}"

# SHA-256 lowercase hex (macOS shasum / Linux sha256sum)
if command -v shasum >/dev/null 2>&1; then
    HASH=$(printf '%s' "$KEY" | shasum -a 256 | awk '{print $1}')
else
    HASH=$(printf '%s' "$KEY" | sha256sum | awk '{print $1}')
fi

PREFIX="${KEY:0:12}"            # display prefix (token.key_prefix VARCHAR(16))

echo "================================================================"
echo "  Plaintext key is shown ONCE - save it now (cannot be recovered)"
echo "================================================================"
echo "plaintext key : $KEY"
echo "key_prefix    : $PREFIX"
echo "key_hash      : $HASH"
echo "================================================================"
echo
echo "-- Run this SQL to insert the token --"
if [ -z "$MODELS" ]; then
    MODELS_SQL="NULL"
else
    MODELS_SQL="'$MODELS'"
fi
echo "INSERT INTO \`token\` (key_hash, key_prefix, name, models, quota_limit, status)"
echo "VALUES ('$HASH', '$PREFIX', '$NAME', $MODELS_SQL, $QUOTA, 1);"
