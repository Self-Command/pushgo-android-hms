#!/usr/bin/env python3
"""Choose a monotonic Android version, preserving the version when CI is retried."""
import argparse
import json
import re

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("releases")
parser.add_argument("--commit", required=True)
parser.add_argument("--requested", default="")
args = parser.parse_args()
pattern = re.compile(r"hms-v(\d+)\.(\d+)\.(\d+)")
releases = []
with open(args.releases, encoding="utf-8") as stream:
    for line in stream:
        record = json.loads(line)
        match = pattern.fullmatch(record["tag_name"])
        if match:
            releases.append((tuple(map(int, match.groups())), record["target_commitish"]))

requested = args.requested.strip()
if requested:
    match = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", requested)
    if not match:
        raise SystemExit("Version must be vX.Y.Z")
    version = tuple(map(int, match.groups()))
    if version[1] > 99 or version[2] > 99:
        raise SystemExit("Minor and patch must be <= 99 for the Android version code")
    matching = [commit for existing, commit in releases if existing == version]
    if matching and any(commit != args.commit for commit in matching):
        raise SystemExit("Requested version belongs to another commit")
    if not matching and releases and version <= max(v for v, _ in releases):
        raise SystemExit("New APK version must be greater than every published APK")
else:
    current = [version for version, commit in releases if commit == args.commit]
    if current:
        version = max(current)
    else:
        major, minor, patch = max((v for v, _ in releases), default=(1, 3, 2))
        patch += 1
        if patch > 99:
            minor, patch = minor + 1, 0
        if minor > 99:
            major, minor = major + 1, 0
        version = (major, minor, patch)
print("v" + ".".join(map(str, version)))
