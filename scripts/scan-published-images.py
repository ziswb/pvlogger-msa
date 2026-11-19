#!/usr/bin/env python3
"""Scan published images for blocklisted values (optional, run by hand).

The git hooks keep site values out of the repository, but an image is built from
a working tree and can carry a value that never reached git, or one that was
removed from the history later: the 2026-11-19 pre-launch review found a site
name in three published versions (SECURITY-FINDINGS 65). Nothing runs it
automatically (CI never sees the private blocklist); run it after a release or
whenever an image is suspect. The git hooks remain the main guard. This pulls the images
anonymously from the registry, as anyone can, unpacks the layers (nested jars
too) and checks every file against the private blocklist the hooks use
(`git config pvlogger.blocklist`, same format and matching as
.githooks/blocklist-check.py).

A hit prints the image, platform, layer, path and the placeholder of the rule,
with the matched value masked. Library code gives false positives (Go's Unicode
tables, example addresses in help texts). Once judged, a false positive goes
into a private allowlist (`git config pvlogger.imagescanAllow <file>`, kept next
to the blocklist, never in this repo: the text around a masked match can give
part of the value away). One entry per line, `<placeholder><TAB><fragment>`,
where the fragment is a piece of the printed context, masked part included;
a hit whose context contains it is reported as known and does not fail the
scan.

Usage (from the repo root):
  scripts/scan-published-images.py              # the images values.yaml pins
  scripts/scan-published-images.py --all        # every tag of those images
  options: --layers N (top N layers, default 3) | --all-layers
           --registry ghcr.io/<owner> (default: global.imageRegistry)
Exit status: 0 clean (or only known false positives), 1 hits, 2 usage or
setup error. Expect several minutes per run.
"""
import argparse
import gzip
import importlib.util
import io
import json
import os
import re
import subprocess
import sys
import tarfile
import urllib.error
import urllib.parse
import urllib.request
import warnings
import zipfile

VALUES = "deploy/helm/pvlogger/values.yaml"
INDEX_TYPES = (
    "application/vnd.oci.image.index.v1+json",
    "application/vnd.docker.distribution.manifest.list.v2+json",
)
MANIFEST_TYPES = (
    "application/vnd.oci.image.manifest.v1+json",
    "application/vnd.docker.distribution.manifest.v2+json",
)
CONTEXT = 24  # characters shown on each side of a (masked) match


def load_rules():
    sys.dont_write_bytecode = True  # no __pycache__ in .githooks/
    spec = importlib.util.spec_from_file_location("blocklist_check", ".githooks/blocklist-check.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    path = mod.blocklist_path()
    if not path or not os.path.isfile(path):
        sys.exit("scan-published-images: pvlogger.blocklist is not configured or missing (see .githooks/README.md)")
    return mod.load(path)


def load_allow():
    """(placeholder, fragment) pairs of judged false positives, or none."""
    path = os.environ.get("PVLOGGER_IMAGESCAN_ALLOW") or subprocess.run(
        ["git", "config", "--get", "pvlogger.imagescanAllow"], capture_output=True, text=True
    ).stdout.strip()
    if not path:
        return []
    with open(os.path.expanduser(path), encoding="utf-8") as fh:
        return [tuple(line.rstrip("\n").split("\t", 1)) for line in fh
                if "\t" in line and not line.startswith("#")]


def pinned_images():
    """(module, tag) pairs and the default registry from values.yaml."""
    pairs, repo, registry = [], None, None
    with open(VALUES, encoding="utf-8") as fh:
        for line in fh:
            m = re.match(r'^\s+imageRegistry:\s*"?([^"\s]+)"?', line)
            if m and registry is None:
                registry = m.group(1)
            m = re.match(r'^\s+repository:\s*"([a-z0-9-]+)"', line)
            if m:
                repo = m.group(1)
                continue
            m = re.match(r'^\s+tag:\s*"?([^"\s]+)"?', line)
            if m and repo:
                pairs.append((repo, m.group(1)))
                repo = None
    return pairs, registry


class Registry:
    def __init__(self, ref):
        self.host, _, self.owner = ref.partition("/")
        self.tokens = {}

    def _token(self, name):
        if name not in self.tokens:
            scope = urllib.parse.quote(f"repository:{self.owner}/{name}:pull")
            with urllib.request.urlopen(f"https://{self.host}/token?scope={scope}") as r:
                self.tokens[name] = json.load(r)["token"]
        return self.tokens[name]

    def get(self, name, path, accept=None):
        url = f"https://{self.host}/v2/{self.owner}/{name}/{path}"
        headers = {"Authorization": f"Bearer {self._token(name)}"}
        if accept:
            headers["Accept"] = ", ".join(accept)

        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, *args, **kwargs):
                return None

        opener = urllib.request.build_opener(NoRedirect)
        try:
            with opener.open(urllib.request.Request(url, headers=headers)) as r:
                return r.read(), r.headers.get("Content-Type", "")
        except urllib.error.HTTPError as e:
            if e.code in (301, 302, 303, 307, 308):  # blob storage: no registry token there
                with urllib.request.urlopen(e.headers["Location"]) as r:
                    return r.read(), r.headers.get("Content-Type", "")
            raise

    def tags(self, name):
        body, _ = self.get(name, "tags/list")
        return [t for t in json.loads(body).get("tags") or [] if not t.startswith("sha256-")]

    def platform_manifests(self, name, tag):
        """(platform, manifest) for every runnable platform of the tag."""
        body, ctype = self.get(name, f"manifests/{tag}", INDEX_TYPES + MANIFEST_TYPES)
        doc = json.loads(body)
        if doc.get("mediaType", ctype) in MANIFEST_TYPES or "layers" in doc:
            return [("single", doc)]
        out = []
        for m in doc.get("manifests", []):
            p = m.get("platform") or {}
            if p.get("os") in (None, "unknown"):  # attestations
                continue
            body, _ = self.get(name, f"manifests/{m['digest']}", MANIFEST_TYPES)
            out.append((f"{p['os']}/{p.get('architecture')}", json.loads(body)))
        return out


def files_in(data, prefix):
    """(path, bytes) of every file in a layer blob, descending into zip/jar entries."""
    try:
        raw = gzip.decompress(data)
    except OSError:
        raw = data
    with tarfile.open(fileobj=io.BytesIO(raw)) as tar:
        for member in tar:
            if member.isfile():
                yield from expand(f"{prefix}{member.name}", tar.extractfile(member).read())


def expand(path, data, depth=0):
    yield path, data
    if depth < 3 and data[:4] == b"PK\x03\x04":
        try:
            with zipfile.ZipFile(io.BytesIO(data)) as z:
                for info in z.infolist():
                    if not info.is_dir():
                        yield from expand(f"{path}!/{info.filename}", z.read(info), depth + 1)
        except (zipfile.BadZipFile, NotImplementedError, OSError):
            pass


def scan(data, rules):
    """(placeholder, masked context) for each rule that matches."""
    text = data.decode("utf-8", errors="replace")
    for rx, replacement in rules:
        m = rx.search(text)
        if m:
            before = text[max(0, m.start() - CONTEXT):m.start()]
            after = text[m.end():m.end() + CONTEXT]
            ctx = re.sub(r"[^\x20-\x7e]", ".", f"{before}[{replacement}]{after}")
            yield replacement, ctx


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--all", action="store_true", help="every published tag of the pinned images")
    ap.add_argument("--layers", type=int, default=3, help="top N layers per image (default 3)")
    ap.add_argument("--all-layers", action="store_true")
    ap.add_argument("--registry", help="host/owner, default: global.imageRegistry in values.yaml")
    args = ap.parse_args()
    if not os.path.isfile(VALUES):
        print(f"run from the repo root ({VALUES} not found)", file=sys.stderr)
        return 2
    rules = load_rules()
    allow = load_allow()
    warnings.simplefilter("ignore", UserWarning)  # zipfile: duplicate entries in fat jars
    pairs, default_registry = pinned_images()
    reg = Registry(args.registry or default_registry)
    if args.all:
        pairs = [(name, tag) for name in dict.fromkeys(n for n, _ in pairs) for tag in reg.tags(name)]
    hits = 0
    for name, tag in pairs:
        ref = f"{reg.host}/{reg.owner}/{name}:{tag}"
        try:
            platforms = reg.platform_manifests(name, tag)
        except urllib.error.HTTPError as e:
            print(f"{ref}: not published ({e.code})")
            continue
        found, known = [], 0
        for platform, manifest in platforms:
            layers = manifest["layers"] if args.all_layers else manifest["layers"][-args.layers:]
            for layer in layers:
                blob, _ = reg.get(name, f"blobs/{layer['digest']}")
                for path, data in files_in(blob, f"{layer['digest'][7:19]}:/"):
                    for replacement, ctx in scan(data, rules):
                        if any(p == replacement and frag in ctx for p, frag in allow):
                            known += 1
                        else:
                            found.append(f"  {platform} {path}: {replacement}  …{ctx}…")
        note = f" ({known} known false positive(s))" if known else ""
        if found:
            hits += 1
            print(f"{ref}: {len(found)} hit(s){note}")
            print("\n".join(found))
        else:
            print(f"{ref}: clean{note}")
    return 1 if hits else 0


if __name__ == "__main__":
    sys.exit(main())
