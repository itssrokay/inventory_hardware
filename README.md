[Download the Android APK](https://github.com/itssrokay/inventory_hardware/releases/latest/download/Dukaan-debug.apk)

# Dukaan — hardware shop catalog

Native Android app built from `plan.md`. It keeps the catalog on one phone and puts selling prices first. No server or account is required for local features.

Implemented in this first working version:

- Room database, Unicode FTS4 index, Hindi/Roman aliases and typo-tolerant search.
- Size-specific prices and selling units; prices stored as integer paise to avoid rounding errors.
- Manual add/edit, gallery photos, optional shelf location, quick price changes and price history.
- Natural-language bulk entry using OpenAI Responses with a strict JSON schema. Every item is editable and must be confirmed before saving. Missing prices stay empty and block saving.
- Duplicate review: update an existing product's sizes or explicitly save a separate item. Bulk writes are transactional.
- Hindi/English voice entry using the phone's speech input activity. Availability and offline support depend on the installed speech service.
- Smart query rewriting on submitted searches with weak local results. Local search works without a key or internet.
- Missed-search review and one-tap alias learning.
- ZIP export/share and validated, transactional restore of items, photos, history and misses. Restore requires confirmation in the app.
- GST label selection: unspecified, included or excluded. No tax amount is invented or calculated.

Still to do before the daily-shop gate: camera capture inside the app, weekly backup reminders, and validation with the real catalog and the owner's phone. Background AI retries and embeddings are intentionally absent. Natural-language requests show a useful error when offline; manual entry stays available. Parsed drafts survive screen rotation, but are not persisted across process termination.

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

Default model: `gpt-4.1-mini`; change it in Settings if needed. Only submitted listing text, or search text plus category names, is sent to OpenAI. Requests use `store: false`. API usage is billed to the configured OpenAI account.

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
