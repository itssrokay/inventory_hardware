# Project memory

Open the local [Project cockpit](http://localhost:5173/project) for the overview, work timeline, architecture and decisions. Start it with `npm --prefix web run dev` from the repository root.

For the imported manufacturer library and owner selection flow, read [Reference catalog](REFERENCE-CATALOG.md).

The same information is readable as plain files:

| Need | Read | Maintained from |
| --- | --- | --- |
| Current scope and next priorities | [STATUS.md](STATUS.md) | `project.json` |
| Components, data flows and boundaries | [ARCHITECTURE.md](ARCHITECTURE.md) | `project.json` |
| What changed and how it was checked | [JOURNAL.md](JOURNAL.md) | `events.jsonl` |
| Why a lasting choice was made | [Decision records](decisions/) | Decision entries in `events.jsonl` |
| Shared coding instructions | [AGENTS.md](../AGENTS.md) | Edit directly |
| Shared reusable workflow | [dukaan-workflow](../.agents/skills/dukaan-workflow/SKILL.md) | Edit the canonical skill |

## Codex, Claude Code and other agents

Codex discovers `.agents/skills/dukaan-workflow/`. Claude Code discovers `.claude/skills/dukaan-workflow/`, a relative symlink to that same folder. `CLAUDE.md` imports `@AGENTS.md`, so standing project instructions stay in one place, including for Claude sessions that do not load AGENTS.md directly. Other tools that understand AGENTS.md or standard SKILL.md folders can use the same content; they may need their own discovery configuration.

- Codex: invoke `$dukaan-workflow` or let it select the skill for a relevant task.
- Claude Code: invoke `/dukaan-workflow` or let it select the skill.
- Run a new session if your agent has not refreshed its project instructions or skill list.
- Edit `.agents/skills/dukaan-workflow/SKILL.md`, not the linked alias.
- On Windows checkouts that do not preserve symlinks, recreate the skill alias with Developer Mode enabled or copy the canonical skill into `.claude/skills/dukaan-workflow/` and keep it synchronized. The root `CLAUDE.md` import itself is a regular file.

The discovery paths and symlink target are validated locally. Claude Code is not installed on this machine, so no actual Claude session has been tested. The setup uses the documented discovery/import mechanisms rather than agent-specific hooks, global settings or model dependencies.

## Add a useful outcome

One entry per meaningful completed task is enough:

```sh
python3 scripts/project_log.py done "Made exact size lookup work offline" \
  --summary "A customer asking for a size sees that variant’s price first." \
  --changed "Kept the requested dimension in local matching." \
  --check "Focused size-search tests passed." \
  --next "Verify with actual product names on the owner’s phone."
```

Repeat `--changed`, `--check` or `--next` when needed. Describe the result and evidence; skip tool-call transcripts. For partially completed work, use `note` instead of `done`. Use `--date YYYY-MM-DD` only for an honest dated retrospective; do not claim its checks were rerun now.

## Record a design decision

Use an ADR when a lasting tradeoff changes persistence, architecture, API boundaries or product scope:

```sh
python3 scripts/project_log.py decision "A clear decision title" \
  --summary "The resulting boundary or behavior." \
  --context "Why a choice was needed." \
  --choice "The approach we selected." \
  --alternative "Another credible option and why it was not selected." \
  --consequence "The benefit, cost or limitation we accept."
```

`--status proposed` records an unsettled decision. An accepted new ADR can replace an old one with `--supersedes ADR-001`. The old record is retained and its generated view points to the new one.

The append operation locks the journal before assigning an ID and writing. This prevents two local writers from reusing an ID. Python 3.9+ and a POSIX environment are required for the helper's `fcntl` lock; both Codex and Claude Code can run it on this Mac/Linux. There are no third-party Python dependencies. Logging never commits, pushes, calls AI or publishes anything.

## Keep the current view current

Edit `project.json` when the current snapshot, architecture or next priorities change, then run:

```sh
python3 scripts/project_log.py render
python3 scripts/project_log.py check
python3 -m unittest discover -s scripts/tests
```

Append history through the helper; generated Markdown files are views. If a render is interrupted after an entry was recorded, run `render` again rather than appending the same outcome twice. Keep `events.jsonl` append-only. Never put API keys, real customer data or private reasoning into project memory; the files may be published with the repository.

The cockpit is a read-only local view. Its Refresh button reloads repository records; it does not edit files, touch either catalog or send the journal to OpenAI.

## Sources and adaptation

The shared instructions adapt the article’s ideas about stating assumptions, keeping solutions simple, focusing changes and verifying the result. Routine reversible choices remain autonomous. This is project-specific guidance, not an official Karpathy release or a guarantee of agent behavior.

- [Article supplied by the user](https://aiparabellum.com/karpathy-claude-md-guidelines-tested/)
- [Official Codex skill discovery](https://learn.chatgpt.com/docs/build-skills)
- [Official Claude Code skills](https://code.claude.com/docs/en/skills)
- [Official Claude project memory and imports](https://code.claude.com/docs/en/memory)
