# Dukaan — current state

Updated: 2026-11-07

Android v0.5.0 is published with searchable categories, responsive indexed reference search, read-only product/discount views and related-product browsing. The README’s first link downloads the latest APK. The web preview remains an independent localhost catalog; stored prices, stock status and product identities remain intact.

| Track | State | What works |
| --- | --- | --- |
| Android app | 0.5.0 public test build | Published build supports read-only product/discount views, explicit price editing and related-product links. Reference indexing/filtering runs off the UI thread. Source pictures, display modes, category browsing, backup and catalog boundaries remain intact. |
| Local web preview | Source published · runs locally | Stable reference input with a cached index and debounced result-only rendering. Read-only price/discount views, related-product browsing, searchable categories and batches of 36 cards. Independent browser storage with English/Hindi/Hinglish modes. |
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
