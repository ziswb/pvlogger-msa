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

# is_maintainer_clone: true if the private blocklist is configured.
is_maintainer_clone() {
  [ -n "$PVLOGGER_BLOCKLIST" ] || git config --get pvlogger.blocklist >/dev/null 2>&1
}

# require_noreply_email: in a maintainer clone, refuse a commit whose identity
# is not a GitHub noreply address (a fresh clone or another machine may carry a
# personal or work user.email, and gitleaks does not scan commit headers).
require_noreply_email() {
  is_maintainer_clone || return 0
  case "$(git config user.email)" in
    *@users.noreply.github.com) ;;
    *) echo "$1: user.email is not a GitHub noreply address; set it for this clone (git config user.email ...)" >&2
       exit 1 ;;
  esac
}

# require_noreply_range <hook> <rev-list args...>: the same for every author and
# committer in the commits about to be pushed.
require_noreply_range() {
  hook=$1; shift
  is_maintainer_clone || return 0
  bad=$(git log --format='%ae%n%ce' "$@" | grep -v '@users\.noreply\.github\.com$' | sort -u | wc -l)
  if [ "$bad" -ne 0 ]; then
    echo "$hook: $bad author/committer address(es) are not GitHub noreply addresses; rewrite them before pushing" >&2
    exit 1
  fi
}
