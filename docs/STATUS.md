# Dukaan — current state

Updated: 2026-11-06

Android v0.4.0 remains the published test release. Newer source adds responsive indexed reference search, read-only product details and discount previews, related-product browsing, and searchable categories. The independent localhost preview includes these changes; stored prices, stock status and product identities remain intact.

| Track | State | What works |
| --- | --- | --- |
| Android app | 0.4.0 public test build | Newer local build supports read-only product/discount views, explicit price editing and related-product links. Reference indexing/filtering runs off the UI thread. Source pictures, display modes, category browsing, backup and catalog boundaries remain intact. |
| Local web preview | Source update · runs locally | Stable reference input with a cached index and debounced result-only rendering. Read-only price/discount views, related-product browsing, searchable categories and batches of 36 cards. Independent browser storage with English/Hindi/Hinglish modes. |
| Release | v0.4.0 public | GitHub hosts the 0.4.0 debug APK and SHA-256 file. The README’s first link downloads the latest APK. Source pictures, multilingual Android/web changes and shared project history are on main. |
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
