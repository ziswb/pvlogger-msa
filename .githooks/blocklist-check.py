#!/usr/bin/env python3
"""Refuse a commit whose staged additions or message contain a blocklisted value.

The blocklist is private (site hosts, network, location, accounts) and lives outside
this public repo. Point git at it once per clone:

    git config pvlogger.blocklist /path/to/blocklist.txt

Format (same as `git filter-repo --replace-text`): one entry per line,
`literal==>replacement` or `regex:<python regex>==>replacement`; `#` starts a comment.
Matches are reported with the suggested placeholder, never with the value itself.

Usage: blocklist-check.py --staged | --message <file>
"""
import os
import re
import subprocess
import sys


def blocklist_path():
    path = os.environ.get("PVLOGGER_BLOCKLIST") or subprocess.run(
        ["git", "config", "--get", "pvlogger.blocklist"], capture_output=True, text=True
    ).stdout.strip()
    return os.path.expanduser(path) if path else ""


def load(path):
    rules = []
    with open(path, encoding="utf-8") as fh:
        for raw in fh:
            line = raw.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#") or "==>" not in line:
                continue
            pattern, replacement = line.rsplit("==>", 1)
            if pattern.startswith("regex:"):
                rules.append((re.compile(pattern[len("regex:"):]), replacement))
            else:
                rules.append((re.compile(re.escape(pattern), re.IGNORECASE), replacement))
    return rules


def staged_additions():
    diff = subprocess.run(
        ["git", "diff", "--cached", "-U0", "--no-color", "--no-ext-diff", "--diff-filter=ACMR"],
        capture_output=True, text=True, errors="replace",
    ).stdout
    path, lineno = "?", 0
    for line in diff.splitlines():
        if line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else line[4:]
        elif line.startswith("@@"):
            m = re.search(r"\+(\d+)", line)
            lineno = int(m.group(1)) if m else 0
        elif line.startswith("+"):
            yield path, lineno, line[1:]
            lineno += 1


def message_lines(path):
    with open(path, encoding="utf-8", errors="replace") as fh:
        for i, line in enumerate(fh, 1):
            if not line.startswith("#"):
                yield "commit message", i, line


def main():
    if len(sys.argv) < 2 or sys.argv[1] not in ("--staged", "--message"):
        print(__doc__, file=sys.stderr)
        return 2
    path = blocklist_path()
    if not path:
        print("blocklist-check: pvlogger.blocklist not configured, skipping (see .githooks/README.md)", file=sys.stderr)
        return 0
    if not os.path.isfile(path):
        print(f"blocklist-check: blocklist {path} not found; refusing to commit unchecked", file=sys.stderr)
        return 1
    rules = load(path)
    lines = staged_additions() if sys.argv[1] == "--staged" else message_lines(sys.argv[2])
    hits = []
    for where, lineno, text in lines:
        for rx, replacement in rules:
            if rx.search(text):
                hits.append(f"  {where}:{lineno}: blocklisted value, use {replacement}")
                break
    if hits:
        print("blocklist-check: site-specific values in this commit (repo is public, placeholders only):", file=sys.stderr)
        print("\n".join(hits), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
