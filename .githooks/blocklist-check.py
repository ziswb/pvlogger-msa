#!/usr/bin/env python3
"""Refuse a commit or push whose additions or messages contain a blocklisted value.

The blocklist is private (site hosts, network, location, accounts) and lives outside
this public repo. Point git at it once per clone:

    git config pvlogger.blocklist /path/to/blocklist.txt

Format (same as `git filter-repo --replace-text`): one entry per line,
`literal`, `regex:<python regex>` or `glob:<pattern>`, each optionally followed by
`==>replacement` (default `***REMOVED***`); `#` starts a comment. Literals and globs
match case-insensitively. Matches are reported with the suggested placeholder,
never with the value itself.

Usage: blocklist-check.py --staged | --message <file> | --range <git rev range...>
"""
import fnmatch
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
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            pattern, replacement = line.rsplit("==>", 1) if "==>" in line else (line, "***REMOVED***")
            if pattern.startswith("regex:"):
                rules.append((re.compile(pattern[len("regex:"):]), replacement))
            elif pattern.startswith("glob:"):
                # fnmatch.translate anchors the whole string; strip that to search.
                rx = re.sub(r"^\(\?s:(.*)\)\\[zZ]$", r"(?s:\1)", fnmatch.translate(pattern[len("glob:"):]))
                rules.append((re.compile(rx, re.IGNORECASE), replacement))
            else:
                rules.append((re.compile(re.escape(pattern), re.IGNORECASE), replacement))
    return rules


def diff_additions(args):
    """Added lines of `git diff <args>`, with their file and line number."""
    diff = subprocess.run(
        ["git", "diff", "-U0", "--no-color", "--no-ext-diff", "--diff-filter=ACMR", *args],
        capture_output=True, text=True, errors="replace", check=True,
    ).stdout
    path, lineno, in_header = "?", 0, False
    for line in diff.splitlines():
        if line.startswith("diff --git "):
            in_header = True
        elif in_header and line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else line[4:]
        elif line.startswith("@@"):
            in_header = False
            m = re.search(r"\+(\d+)", line)
            lineno = int(m.group(1)) if m else 0
        elif not in_header and line.startswith("+"):
            # A content line may itself start with "++"; only the header's "+++" is a path.
            yield path, lineno, line[1:]
            lineno += 1


SCISSORS = "# ------------------------ >8 ------------------------"


def message_lines(path):
    with open(path, encoding="utf-8", errors="replace") as fh:
        for i, line in enumerate(fh, 1):
            if line.rstrip("\n") == SCISSORS:  # `git commit -v`: the diff below is not the message
                break
            if not line.startswith("#"):
                yield "commit message", i, line


def range_lines(rev_args):
    """Added lines and commit messages of every commit in the range (pre-push)."""
    commits = subprocess.run(
        ["git", "rev-list", "--reverse", *rev_args], capture_output=True, text=True, check=True,
    ).stdout.split()
    for sha in commits:
        message = subprocess.run(
            ["git", "log", "-1", "--format=%B", sha], capture_output=True, text=True, errors="replace", check=True,
        ).stdout
        for i, line in enumerate(message.splitlines(), 1):
            yield f"message of {sha[:10]}", i, line
        identity = subprocess.run(
            ["git", "log", "-1", "--format=%an <%ae>%n%cn <%ce>", sha],
            capture_output=True, text=True, errors="replace", check=True,
        ).stdout
        for i, line in enumerate(identity.splitlines(), 1):
            yield f"author/committer of {sha[:10]}", i, line
        parent = [f"{sha}^"] if subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", f"{sha}^"], capture_output=True,
        ).returncode == 0 else ["4b825dc642cb6eb9a060e54bf8d69288fbee4904"]  # the empty tree
        names = subprocess.run(
            ["git", "diff", "--name-only", "--no-renames", *parent, sha],
            capture_output=True, text=True, errors="replace", check=True,
        ).stdout
        for name in names.splitlines():
            yield f"{sha[:10]} file name", 0, name
        for path, lineno, text in diff_additions([*parent, sha]):
            yield f"{sha[:10]} {path}", lineno, text


def main():
    if len(sys.argv) < 2 or sys.argv[1] not in ("--staged", "--message", "--range"):
        print(__doc__, file=sys.stderr)
        return 2
    path = blocklist_path()
    if not path:
        print("blocklist-check: pvlogger.blocklist not configured, skipping (see .githooks/README.md)", file=sys.stderr)
        return 0
    if not os.path.isfile(path):
        print(f"blocklist-check: blocklist {path} not found; refusing to go on unchecked", file=sys.stderr)
        return 1
    rules = load(path)
    mode = sys.argv[1]
    if mode == "--staged":
        lines = diff_additions(["--cached"])
    elif mode == "--message":
        lines = message_lines(sys.argv[2])
    else:
        lines = range_lines(sys.argv[2:])
    hits = []
    for where, lineno, text in lines:
        for rx, replacement in rules:
            if rx.search(text):
                hits.append(f"  {where}:{lineno}: blocklisted value, use {replacement}")
                break
    if hits:
        print("blocklist-check: site-specific values (repo is public, placeholders only):", file=sys.stderr)
        print("\n".join(hits), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
