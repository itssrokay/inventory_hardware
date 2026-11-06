# Project journal

Generated from `events.jsonl` by `scripts/project_log.py`. Append outcomes with the helper; keep past entries intact. Dates use Asia/Kolkata.

## 2026-11-06 · DONE · Published product pictures and Hindi/Hinglish modes

`WORK-008` — Android v0.4.0 and the independent localhost preview show matched source pictures and offer English, Hindi and Hinglish display modes.

**Changed**

- Added 520 source-table pictures for 2,404 variants in a source-hash-bound manifest, reviewed photo transfer and custom-photo priority; unmatched products are explicitly labeled.
- Added a shared offline UI dictionary, familiar product aliases and display-language preferences preserved in Android/web backups. Original source details, custom names, prices and canonical units remain intact; no stock is inferred.
- Published v0.4.0 APK and checksum to public GitHub; README first link still directly downloads the latest APK. ADR-008 records presentation and source-image boundaries.

**Verification**

- Passed 21 Kotlin unit tests, 16 Android emulator tests, 27 web tests and 9 journal-helper tests. Android lint: zero errors, 16 warnings.
- Browser checks: Hindi/Hinglish switching and persistence, Hindi brand/product search, source picture in reviewed entry, unchanged piece unit under the Hindi display label and preserved prices. Original localhost catalog remains six samples with no activated reference variants.
- Verified all 520 bundled image hashes and shared source/dictionary assets in APK, matching previous signing certificate, source/APK credential scans and anonymous latest-download HTTP 200 with matching SHA-256 0da468c9383b7cb5a1f50452d23fb767b6e1754cefdf6e945e5d2ca57ce6f3c7. No live OpenAI call.

**Remaining / next**

- Physical-phone speech and daily counter use remain untested. Supplier technical text stays original; unmatched product groups have no substituted picture.

## 2026-11-06 · DECISION · Display languages and source pictures without changing catalog facts

`ADR-008` — English, Hindi and Hinglish are local presentation choices; manufacturer pictures are source-bound illustrations. Neither changes saved names, source IDs, units, prices or stock status.

[ADR-008: Display languages and source pictures without changing catalog facts](decisions/ADR-008.md) · Accepted

## 2026-11-06 · DONE · Published Dukaan 0.3.0 with a verified direct APK download

`WORK-007` — The latest Android test build and project source are now public on GitHub. The README’s first link downloads 0.3.0.

**Changed**

- Pushed the reference catalog, reviewed activation, image entry, natural filters, discount calculators, localhost preview and shared project workflow in source commit d817fa9.
- Published v0.3.0 with Dukaan-debug.apk and its portable SHA-256 checksum, then marked it latest. Updated the current release snapshot and README. No API key is bundled; the APK includes the original manufacturer PDF for its offline reference viewer.

**Verification**

- Anonymous download through the exact README URL returned HTTP 200 and redirected to v0.3.0. All 33,941,795 downloaded bytes matched the verified APK SHA-256; the downloaded checksum file matched too.
- Source and APK credential scans found no local key. APK signing certificate matches 0.1.0 for in-place updates. Gradle build/unit/lint commands succeeded with unchanged tasks cached; 23 web tests and 9 journal-helper tests passed before publication.
- The existing 19 Kotlin unit-test and 14 emulator-test results cover the released app inputs; emulator tests were not rerun solely for publication.

**Remaining / next**

- Real-phone counter validation, owner selection of carried variants, direct camera capture and weekly backup reminders remain. The browser preview runs locally; publishing source does not create a hosted web app.

## 2026-11-06 · DONE · Imported an unconfirmed reference library with reviewed activation

`WORK-006` — Android 0.3.0 and the local web preview now browse 702 Prince product groups and 4,105 reference variants separately from the shop catalog. Nothing is assumed stocked or automatically priced.

**Changed**

- Added a deterministic PDF importer, source-hash and page audit, exact source rows and rate bases, local original-PDF/page viewers and source-ambiguity flags. All 3,940 page-text code occurrences reconcile; 39 variants have no resolved source price.
- Added individual variant selection and editable review with blank selling price/unit fields. Unique source IDs prevent repeated activation and survive editing, Room migration and version 2 backups; legacy version 1 restores remain supported.
- Built a local 0.3.0 APK. Also fixed a renderer race where a recycled crop-preview bitmap could crash image rotation.

**Verification**

- 19 Kotlin unit tests, 14 Android emulator tests, 23 web tests and 9 journal-helper tests passed. Android debug build and lint passed with zero errors and 16 warnings.
- Emulator verified migration from the exported version 1 schema, one-variant review/save, unit/price confirmation, reload, duplicate prevention and backup source-link restoration.
- Browser verified original source page rendering and isolated fixture activation at a shop price different from the list rate. Reload retained the link and disabled repeated activation. The localhost user catalog retained six original samples and zero linked reference variants.
- Repeated import produced byte-identical JSON and all 92 browser source-page images were rendered. Packaged PDF and JSON match the local source assets; the local OpenAI key is absent from the APK. No live AI call was made for this import.

**Remaining / next**

- Owner selection of actual carried variants and shop selling prices is still pending. Quantities remain unknown; real-phone shop validation is not completed. Original PDF/page assets are local and Git-ignored; no new GitHub release was published.

## 2026-11-06 · DECISION · Keep manufacturer references separate from carried shop variants

`ADR-007` — The source PDF becomes an immutable reference library; only owner-reviewed variants with confirmed selling prices enter the shop catalog.

[ADR-007: Keep manufacturer references separate from carried shop variants](decisions/ADR-007.md) · Accepted

## 2026-11-06 · DONE · Added image entry, natural brand filters, AI categories and discount calculations

`WORK-005` — Both Android and the local web preview support image-assisted item creation with cropping, natural brand/product filtering, editable AI categorization and one-time discounted selling prices.

**Changed**

- Added local crop/position/zoom/rotation controls for product photos and image-based listing, shared strict category schemas, and explicit image submission to OpenAI review.
- Brand-aware offline filtering supports Prince only and sockets from Prince only, including plural item words and strict brand constraints during rewritten searches.
- Entry and quick-price forms accept optional discount percentages, display the net selling price, save integer paise and show actual last-price-update dates. Local Android build version is 0.2.0.

**Verification**

- 17 Kotlin unit tests, 11 Android emulator tests and 17 web tests passed; Android lint reported zero errors.
- The emulator verified crop/rotate/zoom output pixels and saved dimensions, plus discount persistence and price history without compounding.
- An isolated browser catalog passed live vision on a synthetic Prince socket label, category suggestions, image review/save, 25% discount, natural brand/product filters and quick-price editing. The existing localhost catalog was not changed by fixture writes.

**Remaining / next**

- Recognition quality on the owner’s real photos and real phone still needs shop validation. GitHub’s published APK remains 0.1.0; the new 0.2.0 APK is a local build.

## 2026-11-06 · DECISION · Use discounts as a one-time selling-price calculator

`ADR-006` — An optional percentage computes the price to save; existing storage and backups continue to hold one selling price per size and unit.

[ADR-006: Use discounts as a one-time selling-price calculator](decisions/ADR-006.md) · Accepted

## 2026-11-06 · DECISION · Route image entry and AI categories through editable review

`ADR-005` — Product photos, labels and lists can be cropped locally and understood by AI, with editable item/category suggestions before any catalog write.

[ADR-005: Route image entry and AI categories through editable review](decisions/ADR-005.md) · Accepted

## 2026-11-06 · DONE · Shared agent workflow and a readable project cockpit

`WORK-004` — Codex and Claude Code now share a project workflow, with an easy-to-browse record of outcomes, architecture and lasting design decisions.

**Changed**

- Added shared AGENTS.md instructions, the CLAUDE.md import and a single dukaan-workflow skill discovered through both agents’ project paths.
- Added an append-only journal helper, dated retrospective entries, four ADRs, a current-state snapshot and generated Markdown views.
- Added the read-only localhost /project cockpit with overview, searchable/filterable work log, Android/web architecture views, decisions and logging commands.

**Verification**

- 9 Python helper tests and 11 web/domain/server tests passed. The skill-creator validator reports the skill is valid.
- Browser verified the architecture switch, work-log filters/search, expanded decision rationale and both agent entry-point instructions. No browser console errors were observed.
- Android tests were not rerun: this change is project documentation, the logging helper and local web notebook routes.

**Remaining / next**

- Claude Code is not installed on this Mac; its documented discovery paths and shared target were validated, but an actual Claude session remains untested.

## 2026-11-06 · DECISION · Share one workflow and keep project memory in the repository

`ADR-004` — Codex and Claude Code use the same instructions and skill; the project history stays readable without either tool.

[ADR-004: Share one workflow and keep project memory in the repository](decisions/ADR-004.md) · Accepted

## 2026-11-06 · DECISION · Keep the web build as a separate local preview

`ADR-003` — The browser version is a quick way to inspect the interface and workflows, with its own data.

[ADR-003: Keep the web build as a separate local preview](decisions/ADR-003.md) · Accepted

## 2026-11-06 · DECISION · Use OpenAI only behind explicit review and key boundaries

`ADR-002` — Language understanding is online and produces suggestions; confirmed catalog changes remain local.

[ADR-002: Use OpenAI only behind explicit review and key boundaries](decisions/ADR-002.md) · Accepted

## 2026-11-06 · DECISION · Keep the shop catalog local on Android

`ADR-001` — The Android phone owns its catalog; Room and structured search serve the small offline inventory.

[ADR-001: Keep the shop catalog local on Android](decisions/ADR-001.md) · Accepted

## 2026-11-06 · DONE · Added the independent localhost web preview

`WORK-003` — A browser preview supports quick layout checks and real catalog workflows without changing Android data.

**Changed**

- Sample catalog, browser storage, Hindi/English search, price edits, history, reviewed AI entry and JSON backup controls.

**Verification**

- Previous task: 10 Node tests passed. Browser checks covered Hindi size search, persistence after reload and live OpenAI parse/review/update retaining existing sizes.

**Remaining / next**

- The web preview is still local and its changes have not been pushed to GitHub.

## 2026-11-06 · DONE · Published the Android test release

`WORK-002` — Created the public GitHub repository and v0.1.0 release with a direct APK download as the README’s first link.

**Changed**

- Initial Android source published in commit 881b436. APK and SHA-256 file attached to the release.

**Verification**

- At publication: repository visibility was public, asset upload was complete and the anonymous download URL returned HTTP 200.
- Credential scans before upload found no local OpenAI key in source or APK.

## 2026-11-06 · DONE · First usable Android build

`WORK-001` — Built the native catalog around fast offline price lookup, exact size prices and reviewed OpenAI entry.

**Changed**

- Compose screens, Room/FTS catalog, Hindi/Roman aliases, fuzzy search, price history, gallery photos, voice input and ZIP export/restore.

**Verification**

- Earlier in this chat: debug build, 12 unit tests, 9 Android emulator tests and lint with zero errors passed. Results recorded in build/verification/summary.json and README.md; not rerun while writing this retrospective entry.
- Earlier live OpenAI example preserved both lock sizes/prices and the per-packet screw price.

**Remaining / next**

- Direct camera capture, weekly backup reminders and the real-phone shop trial remain.
