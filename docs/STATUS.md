# Dukaan — current state

Updated: 2026-11-06

The Android app has a public test APK. A separate localhost web preview is available for quick UI and workflow checks. The next gate is real use on the shop owner’s Android phone.

| Track | State | What works |
| --- | --- | --- |
| Android app | Local 0.3.0 test build | Offline catalog plus a separate Prince reference library. Select carried variants, review blank selling prices and units, and retain source links through migration and backups. Existing image, search, price/history and backup flows remain. |
| Local web preview | Local only | Independent browser catalog with a separate searchable reference library, source-page viewer and reviewed activation. Source list rates never become selling prices automatically. |
| Release | v0.1.0 public | Public release remains v0.1.0. Android 0.3.0, the local web preview and reference-library changes have not been published. |
| Shop validation | Not yet verified | Daily counter use, actual catalog quality, phone speech service and real-device performance still need a trial. |
| Project memory | Shared locally | One workflow for Codex/Claude, append-only outcomes and ADRs, generated Markdown and the read-only project cockpit. Claude runtime is not installed here. |

## Next priorities

- **Choose the products actually carried** — The imported reference library is hidden from the shop catalog with stock unconfirmed. The owner still needs to select carried variants and confirm selling prices and units.
- **Try it on the owner’s Android phone** — Check voice recognition, real product names, photos and lookup speed during daily counter use.
- **Capture photos inside the Android app** — The current build imports gallery photos; direct camera capture remains.
- **Add a weekly backup reminder** — Export and restore work, but regular backups still rely on the owner remembering.
- **Preserve unsaved drafts after process termination** — Drafts survive rotation but are not yet persisted if Android kills the process.

## Read next

- [Architecture](ARCHITECTURE.md)
- [Project journal](JOURNAL.md)
- [Instructions and logging guide](README.md)
