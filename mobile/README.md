# Mobile Release Notes

This app now includes an iOS host project under `iosApp/` and a shell-based TestFlight pipeline under `scripts/ios/`.

## One-Time Setup

1. Install Xcode and JDK 21 on the Mac that will build the app.
2. Create the iOS App ID and App Store Connect app for the bundle ID you want to ship.
3. Copy `iosApp/Configuration/Local.xcconfig.example` to `iosApp/Configuration/Local.xcconfig`.
4. Fill in `TEAM_ID`, `BUNDLE_ID`, and `APP_NAME`.

## Local Commands

```bash
make mobile-ios-open
make mobile-ios-archive IOS_MARKETING_VERSION=0.1.0 IOS_BUILD_NUMBER=2026032501
make mobile-ios-testflight IOS_MARKETING_VERSION=0.1.0 IOS_BUILD_NUMBER=2026032501
```

`mobile-ios-testflight` expects these environment variables when uploading:

- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_API_KEY_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64` or `APP_STORE_CONNECT_API_KEY_PATH`

If `Local.xcconfig` does not exist, the archive/upload scripts can generate it from:

- `IOS_TEAM_ID`
- `IOS_BUNDLE_ID`
- `IOS_APP_NAME`

## GitHub Actions

`.github/workflows/ios-testflight.yml` provides a manual TestFlight upload flow.

Required repository secrets:

- `APPLE_TEAM_ID`
- `IOS_BUNDLE_ID`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_API_KEY_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`

Optional repository variable:

- `IOS_APP_NAME`

## Notes

- The Kotlin/Native iOS path currently uses `-Pkotlin.native.cacheKind=none` to avoid a known cache build failure in the current toolchain.
- The checked-in app icon is a placeholder and should be replaced before shipping beyond internal testing.
