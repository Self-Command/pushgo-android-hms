# Optional Huawei HMS transport

This extension starts from official Android commit `995d0e1ae9ab867da82a4a35cc00c509bd64badc`. Firebase Messaging, private transports, Rust JNI, original screens, message/event/thing processing and the updater remain present. Huawei uses its own SDK service, token storage and `huawei` route. A device still has one active route.

## Generic and configured APKs

A generic build needs no AGC file. Its HMS option is visibly unconfigured, while FCM and private connections retain their original behavior. Do not put fake Huawei identities into a release.

A configured build requires a sanitized `app/agconnect-services.json` for `io.ethan.pushgo` and the APK certificate registered in AGC. The file is ignored by Git. It may contain required client SDK configuration, but **must not contain a sending App Secret, signing key or server credential**. Inject it and the keystore through Action Secrets. The original Firebase configuration and SDK remain independent.

The maintainer supplies the official AGC application and registered signing certificate for a configured release. Users who want a separate HMS application must build with their own configuration. Sending authorization remains on the Gateway.

## Runtime and migration

Room v31 adds a nullable `push_channel_type` field to v30. Official choices migrate from the original boolean; old HMS installations are identified by their separate secure token marker. A generic upgrade keeps that HMS identity and reports missing configuration instead of uploading its token as FCM. Device identity, gateway settings, subscriptions and business storage are retained.

FCM and HMS callbacks store only their own token. Route registration checks the selected provider and rejects inactive tokens. Route switching keeps the previous selection until acknowledgement; serializing settings writes prevents a late token update from reverting the choice. An uncertain provider switch is reconciled through the existing device identity.

The SDK message services share the original worker and ingress pipeline: decoding, expiry, deduplication, ACK, pull and notifications. Huawei transport urgency stays NORMAL; the original business notification levels continue to apply.

TaskNotes-specific address settings and notification actions are excluded. Standard message links and original rich text continue to work. HTTP gateway URLs are supported; only an automatic updater failure notice is suppressed after its first persistent notice, while evaluations, manual checks and successful update notifications continue.

## Updates and privacy

The original updater accepts signed feeds. Maintainer builds inject their own feed URL and ECDSA public key. Feeds may declare `distribution` and an authenticated `browserDownload` handoff. Public and personal feeds are separate; a mismatched distribution is rejected. Personal version/checksum metadata can be public, while the APK stays private and requires the user's GitHub login. No GitHub access token enters the APK.

Official contributions exclude personal release workflows and endpoints. The official maintainer supplies the official client configuration. Gateway sending authorization remains server-side; the official Token Service's future Huawei support is outside this implementation.
