<!-- 
Copyright (c) 2026 Ryan P. Walsh
All rights reserved.
This software is proprietary and confidential.
Unauthorized copying of this file, via any medium is strictly prohibited.
This is NOT open source software.
-->

# Receipt Wrangler Android (risklab)

A privacy-first, offline-first receipt management app for Android. Capture, organize, and export receipts with on-device OCR and zero cloud sync.

## Core Features

**Capture & Import**
- In-app camera capture with real-time preview
- Image and PDF import via file picker or share intent
- Batch import support

**Processing**
- On-device OCR with confidence scoring
- Duplicate detection with merge capability
- Scan cleanup (crop, rotate, contrast adjustment)
- Multi-select batch operations

**Organization**
- 9 workflow lanes (Business, Tax, Personal, Freelancer, Gig, Trucker, Practitioner, Tax Accountant, Group Travel)
- Category and status filtering
- Custom report grouping
- Project-based organization with quick switching

**Export**
- CSV export for spreadsheets
- JSON backup for portability
- Markdown packets for documentation
- PDF packets with receipt evidence
- Specialized gig-profit calculations

**Privacy & Security**
- No account required
- No cloud sync
- No analytics or tracking
- On-device storage only
- Explicit user-controlled exports

## Build & Test

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew testDebugUnitTest    # Run unit tests
./gradlew lintDebug            # Run lint analysis
```

**APKs**
- Debug: `app/build/outputs/apk/debug/app-debug.apk` (110 MB)
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk` (92 MB)
- sale/expense item counts

## Build and verify

Environment verification in this workspace currently passes:

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- `assembleRelease`

Debug APK and unsigned release APK are generated under [app/build/outputs](app/build/outputs).

## Release signing

Release signing is optional in local dev.

1. Copy `keystore.properties.example` to `keystore.properties`
2. Fill values
3. Build release artifact

## Notes

- The app intentionally avoids heavyweight cloud/AI dependencies.
- OCR fallback and cleanup are local-only and tuned for practical reliability, not perfect extraction.
