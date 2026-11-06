# Bookwise

Native Android EPUB reader with a BYOK (Bring Your Own Key) AI tutor.

The reader works fully offline with no account and no AI key. The optional AI tutor talks directly to a provider you configure with your own credentials — there is no Bookwise backend.

## Status

**Phase 2 — Library & import.** Phase 1 app shell plus document picking (SAF), EPUB validation/metadata via Readium, Room persistence, app-private file copy, duplicate detection, and library list with covers.

Reader rendering, text selection, and AI are intentionally not implemented yet — those are Phases 3+.

## Architecture & rationale

### Rendering & metadata — Readium Kotlin Toolkit 3.4.0

`org.readium.kotlin-toolkit:readium-shared` + `readium-streamer` were selected for Phase 2 metadata extraction because they are the maintained, BSD-3-Clause reference implementation for EPUB and expose a stable reading-position model (Locator) equivalent to EPUB CFI for Phase 3, which the product spec requires over a raw percentage. Verified from the upstream POMs: `readium-shared` depends only on Kotlin stdlib, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization-json, and jsoup — all permissive — and `readium-streamer` layers on `readium-shared`. Phase 2 uses only OPF/container parsing + ZIP validation and does not depend on the heavier `readium-navigator` UI, which is deferred to Phase 3 selection verification where the spec mandates stopping if the renderer cannot expose selected text + locator.

### Storage

- **Files:** `filesDir/books/` (EPUB copy) and `filesDir/covers/` — app-private via `Context.filesDir`, needs no `READ_EXTERNAL_STORAGE`, removed on uninstall. SAF URI is used only as the import source; the app never relies on its temporary grant.
- **Structured data:** Room `books` + `reading_progress` (`bookwise.db`, version 1). `books.sha256` is unique for dedupe. `reading_progress.bookId` is FK CASCADE to `books.id` so deleting a book wipes its progress row atomically. `exportSchema = true` so Phase 7 migrations can be tested.
- **Preferences:** DataStore `app_preferences` for non-secrets (font scale, theme, provider/model). API keys are never stored here and are not implemented yet.

### Import safety

1. SAF `ACTION_OPEN_DOCUMENT` picker → `takePersistableUriPermission` (best-effort).
2. Streaming read capped at 100 MB, SHA-256 computed in-memory.
3. ZIP structure checks: `PK` signature, `mimetype` == `application/epub+zip`, `META-INF/container.xml`, Zip-Slip rejection (`..` / absolute paths), OPF rootfile resolution, title presence.
4. Duplicate check by SHA-256 before copy; dedupe is deliberate, not a silent overwrite.
5. Copy to private dir, then Room insert. On failure the copied file is not rolled back — Room remains the source of truth; orphan files are reclaimable.

### DI & threading

No DI framework. `BookwiseApp` wires `BookwiseDatabase` → `BookRepository`/`ReadingRepository` → `BookImportHandler`/`EpubValidator`. All disk/network off the main thread via `viewModelScope` + `Dispatchers.IO` inside the repository/handler.

## Prerequisites

- JDK 17+
- Android SDK installed (platform `android-36`, build-tools `35.0.0`)
- Gradle 9.5.1 (via wrapper)

## Setup (Windows / PowerShell)

```powershell
cd C:\Users\kerry\Desktop\bookwise

# Point Gradle at your Android SDK (already set on this machine; adjust if needed)
Get-Content local.properties

# Build the debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat testDebugUnitTest
```

Debug APK: `app\build\outputs\apk\debug\app-debug.apk`

## Dependency versions (Phase 2)

| Component | Version | Source |
|---|---|---|
| AGP | 8.13.0 | Gradle plugin portal |
| Kotlin | 2.4.20 | Maven Central |
| Compose BOM | 2025.09.00 | Google Maven |
| Navigation Compose | 2.9.5 | Google Maven |
| Activity Compose | 1.10.1 | Google Maven |
| Lifecycle | 2.9.4 | Google Maven |
| Core KTX | 1.17.0 | Google Maven |
| Coroutines | 1.10.2 | Maven Central |
| Room | 2.8.5 | Google Maven |
| DataStore | 1.2.0 | Google Maven |
| Readium | 3.4.0 | Maven Central |
| KSP | 2.0.20-1.0.25 | Maven Central |

`compileSdk 36`, `targetSdk 36`, `minSdk 26`, JVM 17.

## Roadmap

1. Phase 1 — Project foundation ✅
2. Phase 2 — Library & import ✅
3. Phase 3 — Reader (renderer, TOC, locator-based progress)
4. Phase 4 — Text selection (verified against real EPUB content)
5. Phase 5 — BYOK (secure key storage, provider, error handling)
6. Phase 6 — Tutor (actions, follow-ups, saved explanations)
7. Phase 7 — Hardening (tests, migrations, licenses, accessibility)

## License

Bookwise is an original implementation. Readium Kotlin Toolkit 3.4.0 (BSD-3-Clause) is used as a dependency only — no source copied. All other Phase 2 deps are permissive (Apache-2.0 / MIT / BSD-3).
