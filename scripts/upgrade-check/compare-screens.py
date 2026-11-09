"""Compare two screenshots.sh captures: pixel diff per screenshot (a diff-*.png with the
changed pixels in red is written next to the newer capture) and the rendered DOM with
styles, scripts and per-build noise (random ids, class order) stripped.
Usage: python compare-screens.py <older/screens> <newer/screens>  (needs Pillow)"""
import difflib
import re
import sys
from pathlib import Path

from PIL import Image, ImageChops

a, b = Path(sys.argv[1]), Path(sys.argv[2])
for png in sorted(a.glob("dashboard-*.png")):
    ia, ib = Image.open(png).convert("RGB"), Image.open(b / png.name).convert("RGB")
    if ia.size != ib.size:
        print(f"{png.name}: size differs {ia.size} vs {ib.size}")
        continue
    diff = ImageChops.difference(ia, ib).convert("L").point(lambda v: 255 if v > 24 else 0)
    changed = diff.histogram()[255]
    print(f"{png.name}: {changed} px differ ({100 * changed / (ia.width * ia.height):.2f}%), region {diff.getbbox()}")
    if changed:
        Image.composite(Image.new("RGB", ia.size, (255, 0, 0)), ib, diff).save(b / f"diff-{png.name}")


def clean(p: Path) -> list[str]:
    s = p.read_text(errors="replace")
    s = re.sub(r"<style.*?</style>|<script.*?</script>|<link[^>]*>", "", s, flags=re.S)
    s = re.sub(r"_ngcontent-[a-z0-9-]+(=\"\")?|_nghost-[a-z0-9-]+(=\"\")?", "", s)
    s = re.sub(r'\s(ng-version|ng-reflect-[\w-]+|ng-server-context|id|aria-[\w-]+|for)="[^"]*"', "", s)
    s = re.sub(r"\d{2}:\d{2}:\d{2}", "hh:mm:ss", s)
    s = re.sub(r"highcharts-[a-z0-9]{7}-", "highcharts-ID-", s)
    s = re.sub(r"<!--bindings=\{.*?\}-->|<!--container-->", "", s, flags=re.S)
    s = re.sub(r'class="([^"]*)"', lambda m: 'class="' + " ".join(sorted(m.group(1).split())) + '"', s)
    s = re.sub(r"\s+>", ">", re.sub(r" {2,}", " ", s))
    return re.sub(r">\s*<", ">\n<", s).splitlines()


d = list(difflib.unified_diff(clean(a / "dom.html"), clean(b / "dom.html"), lineterm="", n=0))
print(f"DOM: {len(d)} diff lines")
print("\n".join(d))
