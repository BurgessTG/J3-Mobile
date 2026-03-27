#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

PROJECT_ROOT=$(project_root)

ensure_java_home

cd "$PROJECT_ROOT"
./gradlew -Pkotlin.native.cacheKind=none :composeApp:embedAndSignAppleFrameworkForXcode
