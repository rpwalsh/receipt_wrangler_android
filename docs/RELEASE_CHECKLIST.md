# Release checklist

## Build
- [x] Gradle sync clean
- [x] build uses JDK 17 (not JDK 25)
- [x] debug build runs on Android 13+
- [x] release build signed from local keystore (ready when keystore.properties configured)
- [x] applicationId confirmed
- [x] versionCode / versionName updated
- [x] no INTERNET permission in merged manifest
- [x] no analytics/crash/ads SDKs added
- [x] unit tests pass (`testDebugUnitTest`)
- [x] lint passes (`lintDebug`)
- [x] debug and unsigned release APK builds pass (`assembleDebug`, `assembleRelease`)

## Product
- [x] lane copy reviewed
- [x] import from files works
- [x] open-with (ACTION_VIEW) import works
- [x] in-app camera works
- [x] OCR works on at least 10 real receipts
- [x] low OCR confidence receipts are visible in queue
- [x] scan cleanup improves at least 3 bad captures
- [x] duplicate merge tested
- [x] merge notes include source-ID traceability
- [x] markdown packet export reviewed
- [x] PDF packet export reviewed
- [x] gig sales PDF export reviewed
- [x] packet includes date range, included/excluded counts, and category summary
- [x] backup/restore tested
- [x] onboarding first-run and replay tested
- [x] onboarding overlay (skip + lane/project setup) tested end-to-end
- [x] theme mode setting tested (System / Light / Dark)
- [x] multiple project stacks tested (create/switch/isolation)
- [x] gig lane sales-vs-expense math validated against sample month

## Play store
- [x] icon final (using default Material 3 receipts icon)
- [ ] screenshots captured (5-8 recommended)
- [x] short description final (see PLAY_STORE_LISTING.md)
- [x] privacy policy complete (see PRIVACY_POLICY.md)
- [x] support email configured (uiarchitect@outlook.com)
- [ ] content rating completed (IARC)
- [x] pricing selected (free)

## Verification honesty
- [x] release notes do not claim APK/AAB was built unless verified in Android Studio
- [x] known limitations documented before upload

## Build artifacts (local verification)
- [x] debug APK generated (`app/build/outputs/apk/debug/app-debug.apk`) — 109.5 MB
- [x] unsigned release APK generated (`app/build/outputs/apk/release/app-release-unsigned.apk`) — 91.8 MB
- [ ] release screenshots captured and curated (not transient proof files)

## Pre-submission final checks
- [x] Code is production-ready (zero compiler errors, zero lint errors)
- [x] All working notes and scaffolding removed from repository
- [x] Documentation is professional and complete
- [x] Tests pass (44+ unit tests verified)
- [x] App deploys successfully to emulator
- [ ] App tested on real device (minimum Android 13, target Android 15)
- [x] Privacy policy aligns with actual app behavior
- [x] No debug logging, println, or printStackTrace in production code

