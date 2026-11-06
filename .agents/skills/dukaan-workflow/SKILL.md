---
name: dukaan-workflow
description: Implement, fix, review or document Dukaan's Android app and local web preview with focused changes, relevant verification and readable project history. Use for meaningful project work and architecture decisions; skip journal updates for simple questions or status lookups.
---

# Dukaan workflow

Use this skill from the repository root. Read `AGENTS.md` first; it is shared across tools. Do not substitute another product or framework for the user's request.

## Understand and finish the change

Read `docs/STATUS.md` and the affected source. For a design change, check the relevant part of `docs/ARCHITECTURE.md` and its decision links. Treat historical checks as dated evidence, not proof of the current tree.

State assumptions that affect behavior. Choose ordinary reversible details yourself. Ask only when a missing fact changes correctness, scope, data handling or material cost. A skill invocation does not authorize external actions.

Describe the observable result the user needs, then make the smallest complete change that achieves it. Preserve unrelated edits. Keep speculative abstractions, unrelated refactors and new dependencies out unless the task needs them.

Verify the actual behavior at the right layer. For example: a price edit must persist and retain its previous value in history; an AI listing must reach editable review without silently saving. Use stubbed API tests for ordinary checks; make live calls only when relevant integration verification is within the task's authorization. Do not add ritual tests for wording or low-impact visual edits.

## Record outcomes, not a transcript

After meaningful work is actually complete, append one short entry:

```sh
python3 scripts/project_log.py done "Short outcome title" \
  --summary "What the user can do now and why it matters." \
  --changed "Concrete behavior or file boundary changed." \
  --check "Command or observed flow, and its actual result." \
  --next "A material remaining limit, if any."
```

Repeat `--changed`, `--check` and `--next` only when useful. Omit empty sections. Separate passed checks from not-run checks in the text. Never record credentials, customer data, private reasoning, raw tool output or unverified success. Use a `note` entry for an investigation or partial handoff rather than marking it done.

When a lasting design tradeoff changes, record it once:

```sh
python3 scripts/project_log.py decision "Choose a clear decision title" \
  --summary "The resulting boundary or behavior." \
  --context "What made a decision necessary." \
  --choice "What we chose." \
  --alternative "A plausible option and why it was not selected." \
  --consequence "The benefit, cost or limitation we accept."
```

Use `--status proposed` for an unsettled choice. To replace an accepted decision, add a new decision with `--supersedes ADR-001`; do not rewrite history. Routine implementation details do not need an ADR.

The helper appends `docs/events.jsonl` and regenerates `docs/JOURNAL.md` and the individual ADRs. Update `docs/project.json` only when the current snapshot or next priorities change, then run `python3 scripts/project_log.py render`. The same data appears in the read-only local cockpit at `http://localhost:5173/project`.

Finish with the result, relevant validation and any material limit. Keep logging proportional: one useful outcome entry, plus a decision only if one was made.

## Why these rules exist

Adapted for this project from the article's themes of clear assumptions, simple solutions, focused changes and observable verification. This is a project-specific adaptation, not an official Karpathy release or a promise that instructions guarantee behavior.

- [Reference article](https://aiparabellum.com/karpathy-claude-md-guidelines-tested/)
- [Project memory and invocation guide](../../../docs/README.md)
