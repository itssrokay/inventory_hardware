# Dukaan — current state

Updated: 2026-11-06

Android 0.3.0 is published on GitHub with a direct APK download and checksum. The source includes an independent localhost web preview and project cockpit. The next gate is choosing actual carried variants and real use on the shop owner’s phone.

| Track | State | What works |
| --- | --- | --- |
| Android app | 0.3.0 published test build | Offline catalog plus a separate Prince reference library. Select carried variants, review blank selling prices and units, and retain source links through migration and backups. Existing image, search, price/history and backup flows remain. |
| Local web preview | Source published · runs locally | Independent browser catalog with a separate searchable reference library, source-page viewer and reviewed activation. Source list rates never become selling prices automatically. |
| Release | v0.3.0 public | The public GitHub release contains the 0.3.0 debug APK and SHA-256 file; the README’s first link downloads it. Updated Android, localhost preview and project workflow source is on main. |
| Shop validation | Not yet verified | Daily counter use, actual catalog quality, phone speech service and real-device performance still need a trial. |
| Project memory | Shared in the repository | One workflow for Codex/Claude, append-only outcomes and ADRs, generated Markdown and the read-only project cockpit. Source is published; a real Claude Code session remains untested. |

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
