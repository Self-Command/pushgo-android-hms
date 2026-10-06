"""Create signed update metadata only. Private APKs remain in private Releases."""
import base64
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

version = os.environ['RELEASE_VERSION']
match = re.fullmatch(r'v(\d+)\.(\d+)\.(\d+)', version)
assert match
major, minor, patch = map(int, match.groups())
version_code = major * 1_000_000 + minor * 10_000 + patch * 100 + 99
distribution = os.environ.get('PUSHGO_DISTRIBUTION', 'public')
apk = Path(os.environ['UPDATE_APK_FILE'])
entry = {
    'versionCode': version_code,
    'versionName': version,
    'apkUrl': os.environ['UPDATE_DOWNLOAD_URL'],
    'apkSha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
    'releaseNotesUrl': os.environ['UPDATE_RELEASE_URL'],
    'browserDownload': distribution == 'personal',
    'minSdk': 28,
    'allowedAbis': ['arm64-v8a', 'armeabi-v7a', 'x86_64'],
    'notes': 'Official FCM and private transports retained; optional independent Huawei HMS.',
}
payload = {'schemaVersion': 1, 'distribution': distribution, 'entries': [entry]}
canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()
key = Path('.ci/update-key.pem')
key.write_text(os.environ['PUSHGO_UPDATE_FEED_SIGNING_KEY_PEM'], encoding='utf-8')
key.chmod(0o600)
try:
    signature = subprocess.run(['openssl', 'dgst', '-sha256', '-sign', str(key)], input=canonical, capture_output=True, check=True).stdout
finally:
    key.unlink(missing_ok=True)
document = {'payload': payload, 'signatures': {'ecdsa-p256-sha256': base64.b64encode(signature).decode()}}
out = Path(os.environ['UPDATE_METADATA_FILE'])
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(document, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Signed update metadata created:', distribution, version, os.environ['SOURCE_SHA'])
