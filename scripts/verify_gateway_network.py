#!/usr/bin/env python3
"""Verify HTTP gateway support in the actual, resource-shrunk release APK."""

import argparse
import json
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    parser.add_argument("--aapt2", required=True)
    args = parser.parse_args()

    def dump(*options):
        return subprocess.check_output(
            [args.aapt2, "dump", *options], text=True, encoding="utf-8", errors="replace"
        )

    manifest = dump("xmltree", args.apk, "--file", "AndroidManifest.xml")
    if not re.search(r"usesCleartextTraffic\([^)]*\)=true", manifest):
        raise SystemExit("Release APK does not permit HTTP gateways")
    network_id = re.search(r"networkSecurityConfig\([^)]*\)=@(0x[0-9a-fA-F]+)", manifest)
    if not network_id:
        raise SystemExit("Release APK is missing its gateway network security configuration")
    resources = dump("resources", args.apk)
    config_file = re.search(
        r"resource " + re.escape(network_id.group(1))
        + r" xml/gateway_network_security_config\s*\n\s*\(\) \(file\) (\S+) type=XML",
        resources,
    )
    if not config_file:
        raise SystemExit("Manifest must reference the common gateway network policy")
    config = dump("xmltree", args.apk, "--file", config_file.group(1))
    if not re.search(r"E: base-config[^\n]*\n\s*A: cleartextTrafficPermitted(?:\([^)]*\))?=true", config):
        raise SystemExit("Release APK network policy blocks custom HTTP hosts")
    print(json.dumps({"apk": args.apk, "http_gateway_support": True, "network_policy": config_file.group(1)}))


if __name__ == "__main__":
    main()
