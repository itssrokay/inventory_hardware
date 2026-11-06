# Project journal

Generated from `events.jsonl` by `scripts/project_log.py`. Append outcomes with the helper; keep past entries intact. Dates use Asia/Kolkata.

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
