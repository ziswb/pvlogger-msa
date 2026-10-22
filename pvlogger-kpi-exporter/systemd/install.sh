#!/usr/bin/env bash
# Installs (or updates) the PVLogger KPI relay as a systemd service on the
# receiver's server (infrastructure ADR-0024, pvlogger-msa ADR-0021).
#
# Run it as root from the unpacked bundle, after filling in relay.env:
#
#   sudo ./install.sh -d /srv/pvlogger/export -p 8780
#   sudo ./install.sh -d /srv/pvlogger/export -p 8780 -g tomcat   # his reader may also delete/move files
#   sudo ./install.sh -u                                           # remove everything but the export directory
#
# Running it again with a new bundle updates the binary and unit. If relay.env
# in the bundle is still the empty template, the installed one is kept.
set -euo pipefail

NAME=pvlogger-relay
BIN=/usr/local/bin/pvlogger-kpi-exporter
UNIT=/etc/systemd/system/$NAME.service
DROPIN_DIR=/etc/systemd/system/$NAME.service.d
DROPIN=$DROPIN_DIR/site.conf
ENV_DIR=/etc/$NAME
ENV_FILE=$ENV_DIR/relay.env
STATE_DIR=/var/lib/$NAME
SECRETS=(UPSTREAM_URL UPSTREAM_TOKEN TS_AUTHKEY)

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
export_dir="" port="" reader_group="" env_src="$here/relay.env" uninstall=0

die()  { echo "ERROR: $*" >&2; exit 1; }
info() { echo "==> $*"; }

usage() {
	cat <<EOF
Usage: sudo $0 -d EXPORT_DIR -p PORT [-g GROUP] [-e ENV_FILE]
       sudo $0 -u

  -d DIR    directory for the export files; must be empty or already the relay's.
            The relay deletes EVERY file older than 48 h in it.
  -p PORT   port his platform calls (1024-65535), e.g. 8780
  -g GROUP  optional: existing group that may also delete/rename the files
  -e FILE   filled-in env file (default: relay.env next to this script)
  -u        uninstall (keeps the export directory and its files)
EOF
	exit 2
}

while getopts ":d:p:g:e:uh" opt; do
	case $opt in
		d) export_dir=${OPTARG%/} ;;
		p) port=$OPTARG ;;
		g) reader_group=$OPTARG ;;
		e) env_src=$OPTARG ;;
		u) uninstall=1 ;;
		*) usage ;;
	esac
done

[[ $EUID -eq 0 ]] || die "run as root (sudo)"
command -v systemctl >/dev/null || die "systemd not found"

# --- uninstall -------------------------------------------------------------
if (( uninstall )); then
	info "Removing $NAME"
	systemctl disable --now "$NAME" 2>/dev/null || true
	rm -rf "$UNIT" "$DROPIN_DIR" "$ENV_DIR" "$STATE_DIR" "$BIN"
	systemctl daemon-reload
	if id -u "$NAME" >/dev/null 2>&1; then userdel "$NAME"; fi
	if getent group "$NAME" >/dev/null; then groupdel "$NAME"; fi
	info "Done. The export directory and its files were kept."
	info "Also remove the device '$NAME' in the Tailscale admin console."
	exit 0
fi

# --- check the arguments ---------------------------------------------------
[[ -n $export_dir && -n $port ]] || usage

[[ $port =~ ^[0-9]+$ ]] && (( port >= 1024 && port <= 65535 )) \
	|| die "port must be 1024-65535 (the service has no privilege to bind below 1024)"

[[ $export_dir =~ ^/[A-Za-z0-9._/-]+$ ]] || die "export dir must be an absolute path of letters, digits, . _ - /"
[[ $export_dir != *..* ]] || die "export dir must not contain '..'"
[[ $(tr -cd / <<<"$export_dir" | wc -c) -ge 2 ]] || die "export dir must be at least two levels deep (e.g. /srv/pvlogger/export)"
case $export_dir/ in
	/home/*|/root/*|/run/*|/tmp/*|/var/tmp/*|/proc/*|/sys/*|/dev/*|/boot/*|/usr/*|/etc/*|/bin/*|/sbin/*|/lib*|$STATE_DIR/*)
		die "export dir $export_dir is in a location the service sandbox hides or must not write to; use e.g. /srv/pvlogger/export" ;;
esac

if [[ -n $reader_group ]]; then
	getent group "$reader_group" >/dev/null || die "group '$reader_group' does not exist"
fi

# --- check the bundle ------------------------------------------------------
for f in pvlogger-kpi-exporter pvlogger-relay.service SHA256SUMS BUNDLE; do
	[[ -f $here/$f ]] || die "bundle incomplete: $f missing in $here"
done
(cd "$here" && sha256sum --quiet -c SHA256SUMS) || die "checksum mismatch, copy the bundle again"

read -r version arch < "$here/BUNDLE"
case $(uname -m) in
	x86_64)  host_arch=amd64 ;;
	aarch64) host_arch=arm64 ;;
	*)       host_arch=$(uname -m) ;;
esac
[[ $arch == "$host_arch" ]] || die "bundle is for $arch, this host is $host_arch"

# --- check the env file ----------------------------------------------------
# Read KEY=VALUE without sourcing; strip optional surrounding quotes.
env_get() {
	local v
	v=$(sed -n "s/^[[:space:]]*${1}[[:space:]]*=//p" "$2" | tail -n1)
	v=${v%\"}; v=${v#\"}; v=${v%\'}; v=${v#\'}
	printf '%s' "$v"
}

install_env=1
[[ -f $env_src ]] || die "env file $env_src not found"
filled=0
for k in "${SECRETS[@]}"; do [[ -n $(env_get "$k" "$env_src") ]] && filled=$((filled + 1)); done

# The URL is pre-filled in the template, so "untouched" means token and key empty.
if [[ -z $(env_get UPSTREAM_TOKEN "$env_src") && -z $(env_get TS_AUTHKEY "$env_src") ]]; then
	[[ -f $ENV_FILE ]] || die "fill in UPSTREAM_TOKEN and TS_AUTHKEY in $env_src first"
	install_env=0
	info "relay.env not filled in, keeping the installed $ENV_FILE"
else
	(( filled == ${#SECRETS[@]} )) || die "$env_src: all of ${SECRETS[*]} must be set"
	url=$(env_get UPSTREAM_URL "$env_src")
	[[ $url == https://* && $url != *'<'* ]] || die "UPSTREAM_URL must be the https URL of the export endpoint"
	[[ $(env_get TS_AUTHKEY "$env_src") == tskey-auth-* ]] || die "TS_AUTHKEY does not look like a Tailscale auth key (tskey-auth-...)"
	for k in HTTP_PORT KPI_EXPORT_PATH; do
		grep -Eq "^[[:space:]]*${k}[[:space:]]*=" "$env_src" \
			&& die "$env_src must not set $k (it would override -d/-p); remove the line"
	done
fi

# --- install ---------------------------------------------------------------
info "Installing $NAME $version ($arch): dir $export_dir, port $port${reader_group:+, group $reader_group}"

if systemctl is-active --quiet "$NAME"; then
	info "Stopping the running service for the update"
	systemctl stop "$NAME"
fi

if ss -Hltn "sport = :$port" | grep -q .; then
	die "port $port is already in use:
$(ss -ltnp "sport = :$port")"
fi

if ! id -u "$NAME" >/dev/null 2>&1; then
	info "Creating system user $NAME (UID chosen by the system)"
	useradd --system --user-group --no-create-home --home-dir /nonexistent \
		--shell /usr/sbin/nologin --comment "PVLogger KPI relay" "$NAME"
fi

# Export directory: refuse a foreign, non-empty one, because the 48-hour
# cleanup deletes every regular file in it that is older than 48 hours.
if [[ -L $export_dir ]]; then
	die "$export_dir is a symlink; give the real path"
elif [[ -e $export_dir ]]; then
	[[ -d $export_dir ]] || die "$export_dir exists and is not a directory"
	owner=$(stat -c %U "$export_dir")
	if [[ $owner != "$NAME" && -n $(ls -A "$export_dir") ]]; then
		die "$export_dir is not empty and belongs to $owner. The relay deletes every file older than 48 h there; use a directory of its own."
	fi
fi
install -d -m 0755 "$(dirname "$export_dir")"
if [[ -n $reader_group ]]; then
	install -d -o "$NAME" -g "$reader_group" -m 2775 "$export_dir"
else
	install -d -o "$NAME" -g "$NAME" -m 0755 "$export_dir"
fi

install -m 0755 "$here/pvlogger-kpi-exporter" "$BIN"
install -m 0644 "$here/pvlogger-relay.service" "$UNIT"

install -d -m 0700 "$ENV_DIR"
if (( install_env )); then
	install -m 0600 -o root -g root "$env_src" "$ENV_FILE"
fi

# Site settings go into a drop-in so the unit stays the one from the repo.
# The empty ReadWritePaths= resets the unit's default path.
install -d -m 0755 "$DROPIN_DIR"
{
	echo "# Written by install.sh on $(date -Is). Re-run install.sh to change."
	echo "[Service]"
	echo "Environment=KPI_EXPORT_PATH=$export_dir"
	echo "Environment=HTTP_PORT=$port"
	echo "ReadWritePaths="
	echo "ReadWritePaths=$export_dir"
	if [[ -n $reader_group ]]; then echo "UMask=0002"; fi
} > "$DROPIN"
chmod 0644 "$DROPIN"

systemctl daemon-reload
systemctl enable --quiet "$NAME"
systemctl start "$NAME"

# --- verify ----------------------------------------------------------------
info "Waiting for the relay to join the tailnet and listen (up to 60 s)"
inv=$(systemctl show -p InvocationID --value "$NAME")
ok=0
for _ in $(seq 60); do
	logs=$(journalctl -q -o cat _SYSTEMD_INVOCATION_ID="$inv" 2>/dev/null || true)
	if grep -q listening <<<"$logs"; then ok=1; break; fi
	systemctl is-active --quiet "$NAME" || break
	sleep 1
done
if (( !ok )); then
	journalctl -u "$NAME" -n 30 --no-pager >&2 || true
	die "the service did not come up, see the log above"
fi
grep -q 'TS_AUTHKEY not set' <<<"$logs" && die "the relay runs WITHOUT Tailscale (TS_AUTHKEY empty)"
info "Relay is up"

if command -v curl >/dev/null; then
	info "Test call: GET http://127.0.0.1:$port/export"
	body=$(mktemp)
	code=$(curl -s -o "$body" -w '%{http_code}' --max-time 30 "http://127.0.0.1:$port/export" || echo 000)
	if [[ $code == 200 ]]; then
		info "200 OK, newest file: $(ls -t "$export_dir" | head -n1)"
		rm -f "$body"
	else
		echo "Test call returned $code:" >&2; cat "$body" >&2; echo >&2; rm -f "$body"
		die "the relay runs, but the export call failed (endpoint down, token or tailnet policy?)"
	fi
else
	info "curl not installed, skipped the test call"
fi

# The secrets now live only in the root-only file.
if (( install_env )) && [[ $(realpath "$env_src") != "$ENV_FILE" ]]; then
	rm -f "$env_src"
	info "Removed $env_src; the secrets are now only in $ENV_FILE (root, 0600)"
fi

cat <<EOF

Done. $NAME $version runs as user $NAME, files in $export_dir, port $port.
  status:  systemctl status $NAME
  log:     journalctl -u $NAME -f
  call:    curl -s http://<this host>:$port/export
The port listens on all interfaces; close it with ufw if only this host calls it.
EOF
