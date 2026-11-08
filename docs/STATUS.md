# Dukaan — current state

Updated: 2026-11-08

Android v0.5.0 remains published. Newer source adds separate product/shelf albums, multi-selection with per-image crop/cover/removal, enlarged galleries and multi-image AI input. Version 3 backups retain both albums and restore versions 1–3. Android and localhost web storage remain independent.

| Track | State | What works |
| --- | --- | --- |
| Android app | 0.5.0 public test build | Newer local build adds up to six product and six shelf images per item, crop queue, cover selection and enlarged galleries. Additive Room schema 3 migration preserves old covers/prices/source links. AI batches hold six selected images; category suggestions exclude shelf photos. |
| Local web preview | Source published · runs locally | Local source adds product/shelf galleries and multi-image AI review with explicit reuse into product albums. Web backup format 3 retains both albums, accepts legacy formats and remains independent of Android. |
| Release | v0.5.0 public | GitHub hosts the 0.5.0 debug APK and SHA-256 file, including the latest category, search, price-view and related-product changes. The README’s first link downloads this APK. Source and project history are on main. |
| Shop validation | Not yet verified | Daily counter use, actual catalog quality, phone speech service and real-device performance still need a trial. |
| Project memory | Shared in the repository | One workflow for Codex/Claude, append-only outcomes and ADRs, generated Markdown and the read-only project cockpit. Source is published; a real Claude Code session remains untested. |

## Next priorities

- **Choose the products actually carried** — The imported reference library is hidden from the shop catalog with stock unconfirmed. The owner still needs to select carried variants and confirm selling prices and units.
- **Try it on the owner’s Android phone** — Check voice recognition, real product names, photos and lookup speed during daily counter use.
- **Pin frequent categories** — Optional shortcuts would reduce repeated category searches for the items sold most often at the counter.
- **Filter large families by brand and exact size** — Narrowing pipes and fittings should retain exact technical sizes and confirmed selling units.
- **Review duplicate category names before merging** — An explicit affected-item preview can clean up spelling variants without silently moving or renaming products.
- **Capture photos inside the Android app** — The current build imports gallery photos; direct camera capture remains.
- **Add a weekly backup reminder** — Export and restore work, but regular backups still rely on the owner remembering.
- **Preserve unsaved drafts after process termination** — Drafts survive rotation but are not yet persisted if Android kills the process.

## Read next

- [Architecture](ARCHITECTURE.md)
- [Project journal](JOURNAL.md)
- [Instructions and logging guide](README.md)
