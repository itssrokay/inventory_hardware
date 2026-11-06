# Dukaan project instructions

These instructions are shared by Codex, Claude Code and other agents working in this repository. The user's current request takes priority over project conventions.

## Start with the relevant context

- Read `docs/STATUS.md` for current scope and remaining work. Use `docs/ARCHITECTURE.md` when changing a boundary or data flow; consult only relevant decisions under `docs/decisions/`.
- Inspect the working tree before editing. Preserve unrelated local work. The Android app is the shop product; `web/` is an independent localhost preview.
- Use the `dukaan-workflow` skill for implementation, fixes, architecture work and recording meaningful project progress. Canonical source: `.agents/skills/dukaan-workflow/SKILL.md`.

## Work deliberately, without ceremony

- State material assumptions before they affect the result. Resolve safe, reversible details yourself; ask only when an unknown could materially change correctness, scope, data or cost.
- Make the smallest complete change that meets the request. Avoid speculative features, new frameworks and unrelated cleanup.
- Define an observable success check. For a bug, reproduce it and use a focused regression check where useful. Documentation and cosmetic changes do not need invented tests.
- Run the checks relevant to the change. Report what passed, what was not run, and the practical limits. Do not call a feature verified because it merely compiles.

## Keep these product boundaries

- Selling prices belong to size/unit variants and use integer paise. Preserve exact sizes and selling units. Never infer GST; label the selected convention.
- Search and manual entry stay local. AI parsing produces an editable review; it never saves catalog changes on its own.
- Android Room storage and browser IndexedDB are separate. Their backup formats are different. Do not imply they sync.
- Keep API keys out of source, browser responses, APKs, backups, logs and screenshots. `.env.local` is server-side development configuration; phone keys use Android Keystore.
- The README's first link remains the direct APK download. Publishing or creating another release requires a user request; writing a journal entry does not authorize publishing.

## Useful commands

- Local preview and project cockpit: `npm --prefix web run dev` → `http://localhost:5173/` and `/project`.
- Web behavior/proxy checks: `npm --prefix web test`.
- Android: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Android device tests: `./gradlew :app:connectedDebugAndroidTest` on a disposable emulator; the tests replace its debug catalog and key settings.
- Journal helper checks: `python3 -m unittest discover -s scripts/tests`.

## Leave useful project memory

After meaningful completed work, append one outcome entry using `python3 scripts/project_log.py done` with the actual changes and verification. Do not log every tool call, narrate private reasoning, or claim historical checks were rerun.

Record a decision when a lasting tradeoff changes architecture, persistence, API boundaries or scope. Explain the context, chosen approach, alternatives and consequences. Keep historical decisions; supersede one with a new record when necessary.

`docs/project.json` holds the current project snapshot; `docs/events.jsonl` is the append-only history. The helper renders the readable Markdown views. Update the snapshot only when current facts or next priorities change; regenerate with `python3 scripts/project_log.py render`. Never put secrets or real customer data in project memory.
