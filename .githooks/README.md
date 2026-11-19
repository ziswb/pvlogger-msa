# Git hooks: leak guard

This repository is public. Site specifics (hosts, network, location, accounts,
credentials) must not enter it, in files or in commit messages. Three layers:

1. **Pattern rules** in [`.gitleaks.toml`](../.gitleaks.toml): gitleaks' default secret
   rules plus rules for site data (`home.arpa` hosts, tailnet names, private IPs, MAC
   addresses, coordinates, FusionSolar station codes, e-mail addresses). They run in CI
   ([`.github/workflows/gitleaks.yml`](../.github/workflows/gitleaks.yml), full history
   and commit messages, always with the rules from `main`, so a branch cannot weaken its
   own scan) and in the hooks below.
2. **Private exact-value blocklist**, checked by the hooks. The list itself is kept
   outside the repo. A hit names the placeholder to use, never the value.
3. **Agent rules** in [`CLAUDE.md`](../CLAUDE.md).

Published images are outside the hooks' reach: they are built from a working tree.
Optionally, after a release or whenever an image is suspect, the maintainer scans them
by hand with the same blocklist: `scripts/scan-published-images.py` (see its header;
judged false positives go into a private allowlist, `git config pvlogger.imagescanAllow`).
Nothing runs it automatically.

The hooks run both checks:

| Hook | Scans |
|---|---|
| `pre-commit` | the staged additions; in a maintainer clone also that `user.email` is a GitHub noreply address |
| `commit-msg` | the message (not the `git commit -v` diff below the scissors) |
| `pre-push` | every commit being pushed: patches, file names, messages and author/committer identities (noreply only, in a maintainer clone), plus the message of a pushed annotated tag; also what the other hooks never saw (rebase, cherry-pick, `git am`, GUI clients). It fails closed: a push whose range it cannot compute is refused |

CI runs after the push, when the content is already public, so `pre-push` is the
last check that can still stop a leak.

## Setup (once per clone)

```bash
git config core.hooksPath .githooks
git config pvlogger.blocklist /path/to/private/blocklist.txt   # optional, maintainer only
brew install gitleaks                                           # or see gitleaks releases
```

Without `pvlogger.blocklist` the blocklist check is skipped with a notice. If it is
configured but the file is missing, the commit or push is refused, and so it is when
gitleaks is missing (a maintainer clone must run both checks; elsewhere a missing
gitleaks only prints a notice). Placeholders in use:
`<db-host>`, `<central-host>`, `<registry-host>`, `<pvlogger-host>`, `<tailnet>`,
`<lan-ip>`, `<lan-subnet>`, `<dongle-ip>`, `<site-lat>`, `<site-lon>`, `<station-code>`.
