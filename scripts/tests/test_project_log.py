import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'project_log.py'
SPEC = importlib.util.spec_from_file_location('project_log', SCRIPT)
project_log = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(project_log)


class ProjectLogTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)

    def tearDown(self):
        self.directory.cleanup()

    def command(self, *args, success=True):
        result = subprocess.run([sys.executable, str(SCRIPT), '--root', str(self.root), *args], capture_output=True, text=True)
        self.assertEqual(result.returncode == 0, success, result.stderr)
        return result

    def entries(self):
        return project_log.read_events(self.root)

    def test_outcomes_append_without_rewriting_prior_history(self):
        self.command('done', 'First outcome', '--summary', 'Behavior one.', '--check', 'Focused check passed.')
        original = (self.root / 'docs/events.jsonl').read_bytes()
        self.command('note', 'Partial handoff', '--summary', 'Still investigating.', '--next', 'Reproduce on phone.')
        self.assertTrue((self.root / 'docs/events.jsonl').read_bytes().startswith(original))
        self.assertEqual([e['id'] for e in self.entries()], ['WORK-001', 'NOTE-001'])
        self.assertIn('Focused check passed.', (self.root / 'docs/JOURNAL.md').read_text())

    def test_decision_supersession_retains_both_originals(self):
        common = ['--summary', 'A boundary.', '--context', 'A real constraint.', '--choice', 'One approach.']
        self.command('decision', 'First decision', *common)
        self.command('decision', 'Changed decision', *common, '--supersedes', 'ADR-001')
        self.assertEqual(len(self.entries()), 2)
        self.assertIn('Superseded by ADR-002', (self.root / 'docs/decisions/ADR-001.md').read_text())
        self.assertIn('[ADR-001](ADR-001.md)', (self.root / 'docs/decisions/ADR-002.md').read_text())

    def test_proposed_or_unknown_decision_cannot_supersede(self):
        args = ['decision', 'New decision', '--summary', 'Boundary.', '--context', 'Constraint.', '--choice', 'Approach.']
        self.command(*args, '--supersedes', 'ADR-999', success=False)
        self.assertEqual(self.entries(), [])
        self.command(*args)
        before = (self.root / 'docs/events.jsonl').read_bytes()
        self.command(*args, '--supersedes', 'ADR-001', '--status', 'proposed', success=False)
        self.assertEqual((self.root / 'docs/events.jsonl').read_bytes(), before)

    def test_invalid_history_is_not_silently_overwritten(self):
        path = self.root / 'docs/events.jsonl'
        path.parent.mkdir()
        path.write_text('{not json}\n')
        self.command('done', 'Outcome', '--summary', 'Behavior.', success=False)
        self.assertEqual(path.read_text(), '{not json}\n')

    def test_render_failure_keeps_the_recorded_entry_recoverable(self):
        (self.root / 'docs').mkdir()
        (self.root / 'docs/project.json').write_text('{bad snapshot}')
        result = self.command('done', 'Recorded once', '--summary', 'The work is complete.', success=False)
        self.assertIn('Recorded WORK-001', result.stdout)
        self.assertIn('do not append this entry again', result.stderr)
        self.assertEqual(len(self.entries()), 1)
        (self.root / 'docs/project.json').unlink()
        self.command('render')
        self.assertEqual(len(self.entries()), 1)
        self.assertIn('Recorded once', (self.root / 'docs/JOURNAL.md').read_text())

    def test_credential_patterns_are_rejected_before_append(self):
        self.command('done', 'Outcome', '--summary', 'sk-proj-' + 'syntheticcredential' * 3, success=False)
        self.assertEqual(self.entries(), [])

    def test_parallel_writers_get_unique_ids(self):
        args = [sys.executable, str(SCRIPT), '--root', str(self.root), 'done', 'Parallel outcome', '--summary', 'Local isolated test.']
        jobs = [subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE) for _ in range(4)]
        for job in jobs:
            _, error = job.communicate(timeout=10)
            self.assertEqual(job.returncode, 0, error.decode())
        self.assertEqual({e['id'] for e in self.entries()}, {'WORK-001', 'WORK-002', 'WORK-003', 'WORK-004'})

    def test_architecture_diagram_only_references_declared_nodes(self):
        root = SCRIPT.parents[1]
        state = json.loads((root / 'docs/project.json').read_text())
        for lane in state['architecture']['lanes']:
            ids = {n['id'] for n in lane['nodes']}
            self.assertEqual(len(ids), len(lane['nodes']))
            for edge in lane['edges']:
                self.assertIn(edge['from'], ids)
                self.assertIn(edge['to'], ids)
            for node in lane['nodes']:
                if node.get('path'):
                    self.assertTrue((root / node['path']).exists(), node['path'])

    def test_codex_and_claude_skill_paths_resolve_to_one_source(self):
        root = SCRIPT.parents[1]
        codex = root / '.agents/skills/dukaan-workflow/SKILL.md'
        claude = root / '.claude/skills/dukaan-workflow/SKILL.md'
        self.assertEqual(codex.resolve(), claude.resolve())
        self.assertTrue(codex.is_file())
        self.assertTrue((root / 'CLAUDE.md').read_text().startswith('@AGENTS.md'))


if __name__ == '__main__':
    unittest.main()
