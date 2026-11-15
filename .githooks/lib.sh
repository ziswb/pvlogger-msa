# Shared by the leak-guard hooks (see README.md). Sourced, not run.

# have_gitleaks: true if gitleaks is installed. Without it the hook stops in a
# maintainer clone (pvlogger.blocklist configured) and only warns elsewhere.
have_gitleaks() {
  command -v gitleaks >/dev/null 2>&1 && return 0
  if [ -n "$PVLOGGER_BLOCKLIST" ] || git config --get pvlogger.blocklist >/dev/null 2>&1; then
    echo "$1: gitleaks not installed; refusing to go on without the pattern rules (brew install gitleaks)" >&2
    exit 1
  fi
  echo "$1: gitleaks not installed, pattern rules skipped (brew install gitleaks)" >&2
  return 1
}
