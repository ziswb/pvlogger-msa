# Git hooks: leak guard

This repository is public. Site specifics (hosts, network, location, accounts,
credentials) must not enter it, in files or in commit messages. Three layers:

1. **Pattern rules** in [`.gitleaks.toml`](../.gitleaks.toml): gitleaks' default secret
   rules plus rules for site data (`home.arpa` hosts, tailnet names, private IPs, MAC
   addresses, coordinates, FusionSolar station codes, e-mail addresses). They run in CI
   and in the `pre-commit` hook.
2. **Private exact-value blocklist**, checked by `pre-commit` (staged additions) and
   `commit-msg`. The list itself is kept outside the repo. A hit names the placeholder
   to use, never the value.
3. **Agent rules** in [`CLAUDE.md`](../CLAUDE.md).

## Setup (once per clone)

```bash
git config core.hooksPath .githooks
git config pvlogger.blocklist /path/to/private/blocklist.txt   # optional, maintainer only
brew install gitleaks                                           # or see gitleaks releases
```

Without `pvlogger.blocklist` the blocklist check is skipped with a notice. If it is
configured but the file is missing, the commit is refused. Placeholders in use:
`<db-host>`, `<central-host>`, `<registry-host>`, `<pvlogger-host>`, `<tailnet>`,
`<lan-ip>`, `<lan-subnet>`, `<dongle-ip>`, `<site-lat>`, `<site-lon>`, `<station-code>`.
