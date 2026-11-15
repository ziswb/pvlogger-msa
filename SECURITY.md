# Security policy

Please report a vulnerability privately, through GitHub's
[private vulnerability reporting](../../security/advisories/new) for this repository,
not in a public issue.

This is a personal homelab project, maintained on a best-effort basis. Its security
model is written down: in-cluster calls are trusted and the ingress exposes an
allowlist only ([ADR-0020](docs/adr/0020-trust-boundary-ingress-allowlist.md)); known
findings are tracked in the open in [docs/SECURITY-FINDINGS.md](docs/SECURITY-FINDINGS.md).
