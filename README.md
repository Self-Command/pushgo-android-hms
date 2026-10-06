# Signed PushGo update metadata

This branch contains version and checksum metadata only. APKs and application credentials are not stored here.

Public APKs are in Self-Command/pushgo-android-hms Releases. Personal HMS APKs are in the private Self-Command/pushgo-android-personal-build Releases and require GitHub login.

The workflows sign each feed with an ECDSA P-256 key. The public verification key is embedded in each distribution. Keep both feed files when publishing updates.
