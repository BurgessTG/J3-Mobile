#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

PROJECT_ROOT=$(project_root)

if [ -z "${APP_STORE_CONNECT_API_KEY_ID:-}" ] || [ -z "${APP_STORE_CONNECT_API_KEY_ISSUER_ID:-}" ]; then
    echo "APP_STORE_CONNECT_API_KEY_ID and APP_STORE_CONNECT_API_KEY_ISSUER_ID are required for TestFlight uploads." >&2
    exit 1
fi

AUTH_KEY_PATH=$(prepare_auth_key_path)

"$SCRIPT_DIR/archive.sh"

IPA_PATH=$(find "${PROJECT_ROOT}/ios-build/export" -maxdepth 1 -name "*.ipa" | head -n 1)

if [ -z "$IPA_PATH" ]; then
    echo "No IPA found after archive/export." >&2
    exit 1
fi

xcrun altool \
    --upload-package "$IPA_PATH" \
    --api-key "$APP_STORE_CONNECT_API_KEY_ID" \
    --api-issuer "$APP_STORE_CONNECT_API_KEY_ISSUER_ID" \
    --p8-file-path "$AUTH_KEY_PATH" \
    --wait
