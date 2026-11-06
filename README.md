[Download the Android APK](https://github.com/itssrokay/inventory_hardware/releases/latest/download/Dukaan-debug.apk)

# Dukaan — hardware shop catalog

Published Android test release: **0.4.0**. The first link above downloads this build.

Native Android app built from `plan.md`. It keeps the catalog on one phone and puts selling prices first. No server or account is required for local features.

Implemented in this first working version:

- Room database, Unicode FTS4 index, Hindi/Roman aliases and typo-tolerant search.
- Size-specific prices and selling units; prices stored as integer paise to avoid rounding errors.
- Manual add/edit, gallery photos, optional shelf location, quick price changes and price history.
- Natural-language or image-based bulk entry using OpenAI Responses with a strict JSON schema. Every item is editable and must be confirmed before saving. Missing prices stay empty and block saving.
- Duplicate review: update an existing product's sizes or explicitly save a separate item. Bulk writes are transactional.
- Hindi/English voice entry using the phone's speech input activity. Availability and offline support depend on the installed speech service.
- Smart query rewriting on submitted searches with weak local results. Local search works without a key or internet.
- Natural filters such as “Prince only” or “sockets from Prince only”, with strict matching against brands in your catalog. Missed-search review and one-tap alias learning.
- AI category/alias suggestions for manual entries and automatic editable categories for parsed entries.
- Optional discount calculators on item entry and quick price edit. Enter ₹100 and 25% to save ₹75; only the final selling price is stored, so reopening the editor does not compound the discount.
- ZIP export/share and validated, transactional restore of items, photos, history and misses. Restore requires confirmation in the app.
- GST label selection: unspecified, included or excluded. No tax amount is invented or calculated.

Still to do before the daily-shop gate: camera capture inside the app, weekly backup reminders, and validation with the real catalog and the owner's phone. Background AI retries and embeddings are intentionally absent. Natural-language requests show a useful error when offline; manual entry stays available. Parsed drafts survive screen rotation, but are not persisted across process termination.

## Project instructions and notebook

[Project cockpit](http://localhost:5173/project) · [Current status](docs/STATUS.md) · [Architecture](docs/ARCHITECTURE.md) · [Work journal](docs/JOURNAL.md) · [How to log work](docs/README.md)

Codex and Claude Code share `AGENTS.md` and the `dukaan-workflow` skill. Invoke `$dukaan-workflow` in Codex or `/dukaan-workflow` in Claude Code. The skill asks agents to make focused changes, verify the relevant behavior and leave a concise outcome or architecture decision. The cockpit is a read-only view of repository files; it stays separate from the shop catalog.

```sh
python3 scripts/project_log.py done "Short outcome" --summary "What is possible now" --check "Actual verification result"
python3 scripts/project_log.py render
```

## Local web preview

For a quick browser look, run this from the project root with Node.js 20 or newer:

```sh
npm --prefix web run dev
```

Open **http://localhost:5173**. No dependency installation is needed. The server binds to this computer only. It reads the existing ignored `.env.local` key on the server; credentials never reach browser JavaScript.

The preview includes six clearly labeled sample items, Hindi/English typo-tolerant search, category filters, manual item/size editing, gallery photos, quick price changes, price history, missed-search alias learning, and live OpenAI parsing with review before save. Data persists in this browser using IndexedDB, independently of Android. Clear sample items from Settings to start your own catalog. Web backups are JSON and do not import Android ZIP archives.

```sh
npm --prefix web test
```

Twenty-three tests cover search and strict natural brand filters, prices and discount rounding, crop geometry, duplicates, backup validation, vision/category proxy requests, credential isolation and the read-only project cockpit. Nine Python tests cover the journal helper and shared skill paths. Browser verification also covered Hindi size search, a price change surviving reload, live OpenAI parse → editable review → duplicate update while retaining existing sizes, and manual duplicate suggestions. Voice input depends on browser support and microphone permission.

## Build and run

Requires JDK 17 or 21, Android SDK 36, and Android Studio or `adb`. Minimum phone version: Android 8 (API 26).

Open this folder in Android Studio, let Gradle sync, and run `app`. Or:

```sh
./gradlew :app:assembleDebug
# With an emulator or USB-debugging phone connected:
./gradlew :app:installDebug
```

Set the SDK path in the ignored `local.properties`, or set `ANDROID_HOME`. The included Gradle wrapper downloads Gradle 8.13. Dependency versions are pinned for the SDK 36 toolchain.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## OpenAI configuration

The local development key lives in ignored `.env.local`. Gradle never reads it; the key is never included in an APK, generated source, backup, test report or catalog export.

For a phone installation, enter your key in **Settings → OpenAI**. It is encrypted using an Android Keystore AES-GCM key. To provision an existing debug installation from the local development key, first open the app once, then run:

```sh
python3 scripts/provision_debug_key.py --serial YOUR_ADB_SERIAL
```

The script asks for confirmation before transferring the key. It passes the key over stdin, without putting it in command-line arguments. On startup, the debug app encrypts it and deletes the temporary import. This import path is disabled in release builds. No key is preconfigured in the downloadable APK.

Default model: `gpt-4.1-mini`; change it in Settings if needed. Only submitted listing text/images, category-suggestion text/images, or search text plus category names are sent to OpenAI. Cropping is local; an image goes online only when Review items or Suggest category is requested. A single-item input image can become that item’s product photo; multi-item lists require individual product photos. Requests use `store: false`. API usage is billed to the configured OpenAI account.

Implementation follows the official [Structured Outputs documentation](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses). Schema and instructions are under `app/src/main/assets/` and shared with the host smoke check.

## Verify

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
# With an emulator or device connected; integration tests use isolated databases.
# The UI flow test replaces the debug app's catalog. Use a disposable emulator.
./gradlew :app:connectedDebugAndroidTest
# One live API request, using the local key; incurs a small API usage charge.
python3 scripts/smoke_openai.py
```

Unit tests cover Hindi, Hinglish, aliases, typos, size filtering, price validation, duplicate detection and search across 1,000 items. Emulator tests cover Room/FTS, price history, atomic batch writes, alias learning, backup round trips, malicious archive rejection, Keystore encryption, and add → search → price edit → activity recreation.

Backups use versioned `catalog.json` plus `photos/<item-id>.jpg`. Prices in the archive are integer paise. Restore validates entry names, sizes, schema, item relationships and prices before replacing the database in one transaction. Credentials are excluded and Android cloud backup is disabled.

## Validation on Nov 6, 2026

Debug APK built successfully. All 12 unit tests and 9 Android 17 emulator tests pass. Android lint reports zero errors (dependency-version/style advisories remain). The live OpenAI smoke request passed the two-item example, preserving both lock sizes/prices and the screw selling unit. A scan of the final APK confirmed that the local API key is absent. Physical-phone speech, camera capture and daily shop use still need validation.

## Image, category and discount verification — Nov 6, 2026

The local 0.2.0 APK builds, all 17 Kotlin unit tests and 11 Android emulator tests pass, and lint reports zero errors (dependency/style advisories remain). Emulator coverage includes crop → rotate → zoom → saved image dimensions and discounted-price persistence without compounding after reopening.

All 17 web tests pass. In an isolated browser catalog, a synthetic Prince PVC socket label was uploaded, cropped and read by live OpenAI: brand Prince, size 20 mm, selling price ₹100 and category Plumbing. The category suggestion button was also checked live. Entering 25% off previewed and saved ₹75; natural brand/product filters found it, and the quick discount editor saved a changed net price with history. The localhost user catalog was not used for these fixture writes. These checks do not replace real-shop photo and voice validation.

## Reference library · 0.3.0

The supplied Prince PDF is imported into a separate library: **702 groups and 4,105 reference variants**, all stock unconfirmed and hidden from the main catalog. Select only carried variants, then confirm blank selling-price and unit fields before saving. Source list rates/dates, pack details and page references remain separate; no stock quantity is invented. Repeated activation is prevented by stable reference IDs retained in backups.

Open **Reference** in Android or **Reference library** in the web preview. The [reference guide](docs/REFERENCE-CATALOG.md) explains source anomalies, the extraction audit and how to rebuild local PDF/source-page assets. Version 0.3.0 introduced the reference library. The web preview runs locally from the published source.

The 0.3.0 build passed **19 Kotlin unit tests, 14 Android emulator tests, 23 web tests and 9 journal-helper checks**. Android lint has zero errors (16 warnings). Migration was exercised against the exported version 1 schema; reference activation, source links and backup restoration were checked in the emulator. Browser checks covered source-page viewing, blank price/unit review, one confirmed variant, persistence and prevention of repeated activation. The localhost catalog stayed at its original six sample items, with no activated reference variants. A repeated PDF import produced byte-identical JSON. No OpenAI request was needed for this import.

## Product pictures and display languages · 0.4.0

Reference products now show **520 matched manufacturer pictures**, covering **2,404 variants**. Pictures come from each product’s own PDF table and are labeled as illustrations; size or colour can vary. Unmatched products show a missing-picture message. Reviewed entry carries the picture into the shop catalog when no custom photo exists, and photos remain in backups.

Choose **English**, **हिन्दी** or **Hinglish** in Android **Settings → App language**, or from the web preview’s language selector. Display language is separate from voice recognition. Controls and familiar source-product names use local translations and aliases; custom names, original source details, codes, sizes, prices and stored units stay intact. The choice persists after restart and in each platform’s own backup format. No translation API call is needed.

To regenerate the source-bound pictures after recreating the original PDF locally:

```sh
build/reference-import-env/bin/python scripts/extract_reference_images.py app/src/main/assets/reference/prince-2026.pdf
```

The 0.4.0 build passed **21 Kotlin unit tests, 16 Android emulator tests, 27 web tests and 9 journal-helper checks**. Android lint reports zero errors (16 warnings). Emulator and browser checks covered language persistence, Hindi product search, reviewed source-photo transfer, custom-photo priority, unchanged canonical units and photo/language backup round trips. The original localhost catalog remains at six samples with no activated reference variants. No live OpenAI call was used for these changes.
