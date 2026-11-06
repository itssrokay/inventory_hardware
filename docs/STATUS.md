# Dukaan — current state

Updated: 2026-11-06

Android 0.3.0 is published. A 0.4.0 update adds source product pictures and English/Hindi/Hinglish display modes; verification and release preparation are in progress. The web preview remains an independent localhost catalog.

| Track | State | What works |
| --- | --- | --- |
| Android app | 0.4.0 release preparation | Adds 520 matched source pictures for 2,404 reference variants, reviewed photo transfer, multilingual display and offline product aliases. Catalog facts, custom names, selling units and custom photos stay intact. |
| Local web preview | Source published · runs locally | Independent browser catalog with product pictures and an English/हिन्दी/Hinglish selector. Translated option labels retain canonical stored values; display preference persists in browser backups. |
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
