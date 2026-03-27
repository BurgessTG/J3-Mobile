#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

PROJECT_ROOT=$(project_root)
IOS_PROJECT="${IOS_PROJECT:-iosApp/iosApp.xcodeproj}"
IOS_SCHEME="${IOS_SCHEME:-iosApp}"
IOS_CONFIGURATION="${IOS_CONFIGURATION:-Release}"
IOS_MARKETING_VERSION="${IOS_MARKETING_VERSION:-$(default_marketing_version)}"
IOS_BUILD_NUMBER="${IOS_BUILD_NUMBER:-$(default_build_number)}"
ARCHIVE_BASENAME="${IOS_ARCHIVE_BASENAME:-J3Mobile}"
ARCHIVE_DIR="${PROJECT_ROOT}/ios-build/archive"
EXPORT_DIR="${PROJECT_ROOT}/ios-build/export"
ARCHIVE_PATH="${ARCHIVE_DIR}/${ARCHIVE_BASENAME}.xcarchive"
EXPORT_OPTIONS_PLIST="${EXPORT_DIR}/ExportOptions.plist"
INTERNAL_ONLY_VALUE=$(internal_only_plist_value)

mkdir -p "$ARCHIVE_DIR" "$EXPORT_DIR"

ensure_java_home
ensure_local_xcconfig

cat > "$EXPORT_OPTIONS_PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>destination</key>
    <string>export</string>
    <key>manageAppVersionAndBuildNumber</key>
    <false/>
    <key>method</key>
    <string>app-store-connect</string>
    <key>signingStyle</key>
    <string>automatic</string>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>testFlightInternalTestingOnly</key>
    <${INTERNAL_ONLY_VALUE}/>
    <key>uploadSymbols</key>
    <true/>
</dict>
</plist>
EOF

cd "$PROJECT_ROOT"
rm -rf "$ARCHIVE_PATH" "$EXPORT_DIR"/*.ipa

AUTH_KEY_PATH=""
if [ -n "${APP_STORE_CONNECT_API_KEY_ID:-}" ] || [ -n "${APP_STORE_CONNECT_API_KEY_ISSUER_ID:-}" ] || [ -n "${APP_STORE_CONNECT_API_KEY_PATH:-}" ] || [ -n "${APP_STORE_CONNECT_API_KEY_BASE64:-}" ]; then
    if [ -z "${APP_STORE_CONNECT_API_KEY_ID:-}" ] || [ -z "${APP_STORE_CONNECT_API_KEY_ISSUER_ID:-}" ]; then
        echo "APP_STORE_CONNECT_API_KEY_ID and APP_STORE_CONNECT_API_KEY_ISSUER_ID are required when using App Store Connect authentication." >&2
        exit 1
    fi
    AUTH_KEY_PATH=$(prepare_auth_key_path)
fi

if [ -n "$AUTH_KEY_PATH" ]; then
    xcodebuild \
        -project "$IOS_PROJECT" \
        -scheme "$IOS_SCHEME" \
        -configuration "$IOS_CONFIGURATION" \
        -destination "generic/platform=iOS" \
        -derivedDataPath "${PROJECT_ROOT}/ios-build/DerivedData" \
        -archivePath "$ARCHIVE_PATH" \
        -allowProvisioningUpdates \
        -authenticationKeyPath "$AUTH_KEY_PATH" \
        -authenticationKeyID "$APP_STORE_CONNECT_API_KEY_ID" \
        -authenticationKeyIssuerID "$APP_STORE_CONNECT_API_KEY_ISSUER_ID" \
        MARKETING_VERSION="$IOS_MARKETING_VERSION" \
        CURRENT_PROJECT_VERSION="$IOS_BUILD_NUMBER" \
        clean archive

    xcodebuild \
        -exportArchive \
        -archivePath "$ARCHIVE_PATH" \
        -exportPath "$EXPORT_DIR" \
        -exportOptionsPlist "$EXPORT_OPTIONS_PLIST" \
        -allowProvisioningUpdates \
        -authenticationKeyPath "$AUTH_KEY_PATH" \
        -authenticationKeyID "$APP_STORE_CONNECT_API_KEY_ID" \
        -authenticationKeyIssuerID "$APP_STORE_CONNECT_API_KEY_ISSUER_ID"
else
    xcodebuild \
        -project "$IOS_PROJECT" \
        -scheme "$IOS_SCHEME" \
        -configuration "$IOS_CONFIGURATION" \
        -destination "generic/platform=iOS" \
        -derivedDataPath "${PROJECT_ROOT}/ios-build/DerivedData" \
        -archivePath "$ARCHIVE_PATH" \
        -allowProvisioningUpdates \
        MARKETING_VERSION="$IOS_MARKETING_VERSION" \
        CURRENT_PROJECT_VERSION="$IOS_BUILD_NUMBER" \
        clean archive

    xcodebuild \
        -exportArchive \
        -archivePath "$ARCHIVE_PATH" \
        -exportPath "$EXPORT_DIR" \
        -exportOptionsPlist "$EXPORT_OPTIONS_PLIST" \
        -allowProvisioningUpdates
fi

IPA_PATH=$(find "$EXPORT_DIR" -maxdepth 1 -name "*.ipa" | head -n 1)

if [ -z "$IPA_PATH" ]; then
    echo "No IPA was exported to $EXPORT_DIR." >&2
    exit 1
fi

printf "Exported IPA: %s\n" "$IPA_PATH"
