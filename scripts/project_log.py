#!/usr/bin/env python3
"""Append project outcomes/decisions and render their Markdown views. Standard library only."""
import argparse
from contextlib import contextmanager
from datetime import date, datetime
import fcntl
import json
import os
from pathlib import Path
import re
import tempfile
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
KINDS = {'done': 'WORK', 'note': 'NOTE', 'decision': 'ADR'}
SECRET = re.compile(r'\b(?:sk-(?:proj-)?[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\b')


def text(value):
    return str(value).replace('\r', '').strip()


def read_events(root):
    path = root / 'docs/events.jsonl'
    if not path.exists():
        return []
    result = []
    ids = set()
    for number, line in enumerate(path.read_text().splitlines(), 1):
        if not line.strip():
            continue
        try:
            entry = json.loads(line)
            assert entry['kind'] in KINDS and entry['id'] not in ids
            assert re.fullmatch(KINDS[entry['kind']] + r'-\d{3,}', entry['id'])
            assert isinstance(entry['title'], str) and entry['title'].strip()
            assert isinstance(entry['summary'], str) and entry['summary'].strip()
            date.fromisoformat(entry['date'])
            for field in ('changes', 'checks', 'next', 'alternatives', 'consequences'):
                assert isinstance(entry.get(field, []), list) and all(isinstance(v, str) for v in entry.get(field, []))
            if entry['kind'] == 'decision':
                assert entry['status'] in ('accepted', 'proposed') and entry['context'].strip() and entry['choice'].strip()
                if entry.get('supersedes'):
                    assert any(old['id'] == entry['supersedes'] and old['kind'] == 'decision' for old in result) and entry['status'] == 'accepted'
            assert not SECRET.search(line)
        except (ValueError, KeyError, AssertionError, TypeError, AttributeError) as error:
            raise ValueError(f'Invalid journal entry on line {number}; original history was not changed.') from error
        result.append(entry)
        ids.add(entry['id'])
    return result


@contextmanager
def locked(root):
    directory = root / 'docs'
    directory.mkdir(parents=True, exist_ok=True)
    with (directory / '.journal.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(lock, fcntl.LOCK_UN)


def atomic_write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    handle, temporary = tempfile.mkstemp(dir=path.parent, prefix='.' + path.name + '-')
    try:
        with os.fdopen(handle, 'w') as output:
            output.write(content)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def bullets(label, values):
    return f'\n**{label}**\n\n' + ''.join(f'- {value}\n' for value in values) if values else ''


def render(root, entries=None):
    entries = read_events(root) if entries is None else entries
    superseded = {e['supersedes']: e['id'] for e in entries if e.get('supersedes')}
    journal = '# Project journal\n\nGenerated from `events.jsonl` by `scripts/project_log.py`. Append outcomes with the helper; keep past entries intact. Dates use Asia/Kolkata.\n\n'
    for entry in reversed(entries):
        tag = {'done': 'DONE', 'note': 'NOTE', 'decision': 'DECISION'}[entry['kind']]
        journal += f"## {entry['date']} · {tag} · {entry['title']}\n\n`{entry['id']}` — {entry['summary']}\n"
        if entry['kind'] == 'decision':
            status = f"Superseded by {superseded[entry['id']]}" if entry['id'] in superseded else entry['status'].capitalize()
            journal += f"\n[{entry['id']}: {entry['title']}](decisions/{entry['id']}.md) · {status}\n"
            adr = f"# {entry['id']}: {entry['title']}\n\n- Date: {entry['date']}\n- Status: {status}\n"
            if entry.get('supersedes'):
                adr += f"- Supersedes: [{entry['supersedes']}]({entry['supersedes']}.md)\n"
            adr += f"\n## Context\n\n{entry['context']}\n\n## Decision\n\n{entry['choice']}\n"
            adr += bullets('Alternatives considered', entry.get('alternatives', []))
            adr += bullets('Consequences', entry.get('consequences', []))
            atomic_write(root / f"docs/decisions/{entry['id']}.md", adr)
        for label, field in [('Changed', 'changes'), ('Verification', 'checks'), ('Remaining / next', 'next')]:
            journal += bullets(label, entry.get(field, []))
        journal += '\n'
    atomic_write(root / 'docs/JOURNAL.md', journal.rstrip() + '\n')
    state_file = root / 'docs/project.json'
    if not state_file.exists():
        return
    state = json.loads(state_file.read_text())
    status = f"# {state['name']} — current state\n\nUpdated: {state['updated']}\n\n{state['summary']}\n\n"
    status += '| Track | State | What works |\n| --- | --- | --- |\n'
    for track in state['tracks']:
        status += f"| {track['name']} | {track['status']} | {track['summary']} |\n"
    status += '\n## Next priorities\n\n'
    status += ''.join(f"- **{task['title']}** — {task['reason']}\n" for task in state['next'])
    status += '\n## Read next\n\n- [Architecture](ARCHITECTURE.md)\n- [Project journal](JOURNAL.md)\n- [Instructions and logging guide](README.md)\n'
    atomic_write(root / 'docs/STATUS.md', status)
    architecture = '# Architecture\n\nGenerated from `project.json`. The Android catalog and browser preview have separate storage and backup formats.\n\n'
    for lane in state['architecture']['lanes']:
        architecture += f"## {lane['title']}\n\n{lane['summary']}\n\n```mermaid\nflowchart LR\n"
        for node in lane['nodes']:
            label = node['title'].replace('"', "'")
            architecture += f'    {node["id"]}["{label}"]\n'
        for edge in lane['edges']:
            architecture += f'    {edge["from"]} -->|"{edge["label"]}"| {edge["to"]}\n'
        architecture += '```\n\n| Component | Responsibility | Source |\n| --- | --- | --- |\n'
        for node in lane['nodes']:
            source = f"[source](../{node['path']})" if node.get('path') else 'External service'
            architecture += f"| {node['title']} | {node['detail']} | {source} |\n"
        architecture += '\n'
    architecture += '## Boundaries worth keeping\n\n' + ''.join(f'- {note}\n' for note in state['architecture']['boundaries'])
    architecture += '\n## Decisions\n\n' + ''.join(f"- [{entry['id']}: {entry['title']}](decisions/{entry['id']}.md)\n" for entry in entries if entry['kind'] == 'decision')
    atomic_write(root / 'docs/ARCHITECTURE.md', architecture)


def append(root, args):
    entry = {'kind': args.command, 'title': text(args.title), 'summary': text(args.summary), 'date': args.date}
    if not entry['title'] or not entry['summary'] or '\n' in entry['title']:
        raise ValueError('Use a one-line title and a non-empty summary.')
    date.fromisoformat(entry['date'])
    for argument, field in [('changed', 'changes'), ('check', 'checks'), ('next', 'next'), ('alternative', 'alternatives'), ('consequence', 'consequences')]:
        values = [text(value) for value in getattr(args, argument, []) if text(value)]
        if values:
            entry[field] = values
    if args.command == 'decision':
        entry.update(context=text(args.context), choice=text(args.choice), status=args.status)
        if not entry['context'] or not entry['choice']:
            raise ValueError('A decision needs both context and a chosen approach.')
        if args.supersedes:
            entry['supersedes'] = args.supersedes
    if SECRET.search(json.dumps(entry)):
        raise ValueError('Possible credential detected. Write a redacted project outcome instead.')
    with locked(root):
        entries = read_events(root)
        if entry.get('supersedes') and (entry['status'] != 'accepted' or not any(e['id'] == entry['supersedes'] and e['kind'] == 'decision' for e in entries)):
            raise ValueError('Only an accepted decision can supersede an existing ADR.')
        prefix = KINDS[args.command]
        numbers = [int(e['id'].split('-')[1]) for e in entries if e['kind'] == args.command]
        entry['id'] = f'{prefix}-{max(numbers, default=0) + 1:03d}'
        with (root / 'docs/events.jsonl').open('a') as output:
            output.write(json.dumps(entry, ensure_ascii=False) + '\n')
            output.flush()
            os.fsync(output.fileno())
        print(f"Recorded {entry['id']}: {entry['title']}", flush=True)
        try:
            render(root, entries + [entry])
        except (ValueError, OSError, KeyError, TypeError) as error:
            raise ValueError(f"{entry['id']} is recorded, but a Markdown view could not be rendered. Fix the snapshot and run 'python3 scripts/project_log.py render'; do not append this entry again.") from error


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT, help='Repository root (also useful for isolated tests).')
    commands = parser.add_subparsers(dest='command', required=True)
    for command in ('done', 'note', 'decision'):
        child = commands.add_parser(command)
        child.add_argument('title')
        child.add_argument('--summary', required=True)
        child.add_argument('--date', default=datetime.now(ZoneInfo('Asia/Kolkata')).date().isoformat())
        for field in ('changed', 'check', 'next'):
            child.add_argument('--' + field, action='append', default=[])
        if command == 'decision':
            child.add_argument('--context', required=True)
            child.add_argument('--choice', required=True)
            child.add_argument('--alternative', action='append', default=[])
            child.add_argument('--consequence', action='append', default=[])
            child.add_argument('--status', choices=['accepted', 'proposed'], default='accepted')
            child.add_argument('--supersedes')
    commands.add_parser('render')
    commands.add_parser('check')
    args = parser.parse_args()
    try:
        if args.command in KINDS:
            append(args.root.resolve(), args)
        elif args.command == 'render':
            with locked(args.root.resolve()):
                render(args.root.resolve())
            print('Rendered journal, decisions, status and architecture.')
        else:
            entries = read_events(args.root.resolve())
            print(f'Journal valid: {len(entries)} entries; no duplicate IDs or credential patterns.')
    except (ValueError, OSError, json.JSONDecodeError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
