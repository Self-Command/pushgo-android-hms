# PushGo Android HMS distribution

This is a modification of the official PushGo Android source. The package remains
`io.ethan.pushgo`; screens, Room schema version 30, message/event/thing protocol,
deduplication, expiry, ACK, pull recovery and local notification severity remain
the original application features.

## Build

Use Gradle 9.4.1, JDK 21, AGP 9.2.1, Kotlin 2.4.0, SDK 37.0, AGCP 1.9.6.300 and
HMS Push 6.13.0.301. Android 16 behavior is explicitly selected by target SDK 36;
the minimum remains 28. The HMS distribution does not package the private-stream
Rust JNI libraries, so Android builds do not require Rust/NDK. Native source is
retained for comparison with upstream.

Supply an ignored `app/agconnect-services.json` for the correct package and app
ID, with all `*secret*` and private-key properties removed. The Gateway application
OAuth secret is server-only. Configure the certificate fingerprint in AGC for
the key actually used to sign the APK.

An optional `PUSHGO_HMS_DEBUG_STORE_FILE` selects a local test keystore using the
standard Android debug alias/password. Release signing still uses the upstream
`PUSHGO_RELEASE_*` settings. Test signing does not grant the ability to replace an
official APK signed with a different key. Do not uninstall an existing app or
clear its data to work around this.

## Runtime

- HMS is the only push provider in this distribution. FCM dependencies are removed.
  HMS being unavailable or returning no token never selects the private channel.
- A single-flight synchronous token request survives individual caller timeouts;
  successful return values and both HMS callback forms persist the token and
  schedule registration/subscription synchronization. WorkManager retries token
  acquisition and route synchronization after failures.
- The encrypted `hms_push_token` key is separate from the old FCM token. Existing
  Room columns and some internal `Fcm` symbols remain to preserve upstream binding
  compatibility; they are not used to fetch/register FCM tokens.
- HMS data messages enqueue the original inbound worker. Android 12+ requests
  expedited execution with normal scheduling when quota is exhausted. Older
  devices use ordinary persistent work and do not need a new foreground service.
- Update checks require an explicitly configured HMS signed feed via the existing
  `PUSHGO_UPDATE_FEED_URL` and verification-key settings. The default official FCM
  update feed is not used by this build.

Deploy the compatible Huawei-enabled Gateway before installing this client.
Register the original device key with `channel_type: "huawei"`; an old server will
reject it. The separate `hms-probe` is only evidence that the handset can receive
NORMAL data messages and does not replace business-protocol UAT of this app.

SDK configuration reference: https://developer.android.com/build

## LAN testing

The `hmsLan` build type uses the original Release source set and R8 settings,
with the locally registered debug/test certificate. It sets the default gateway
to `http://192.168.1.6:6666` and permits HTTP for that exact host in both the URL
validator and Android network security configuration. Other hosts still require
HTTPS. Debug and Release have an empty LAN exception and retain upstream policy.
If the test PC address changes, update both the build-time host and XML whitelist.
Build with `:app:assembleHmsLan :app:testHmsLanUnitTest :app:lintHmsLan`.

AGConnect reads its generated `agc_*` strings by resource name at runtime.
`res/raw/hms_agc_keep.xml` preserves them in all resource-shrunk variants;
otherwise a build can succeed while `client/app_id` is missing on the handset.
APK verification checks that every generated AGC configuration resource remains
present, in addition to checking the package, certificate and HMS service.

## Verified on 2026-10-05

The LAN build passed 272 unit tests and lint with zero errors. Its APK retains all
64 generated AGConnect strings and uses the AGC-registered local test certificate.
The user confirmed the full 13-operation handset batch: four notification severity
levels, rich content/link/tags, oversized wakeup/pull, duplicate request suppression,
thing creation/update/archive, linked messages and event creation/update/close.
Device: REDMI Turbo 4 Pro, Android 16, HMS Core 6.16.4.352.
Encryption, offline recovery and long-term process/power-management tests remain
outside that batch. The repository does not contain local credentials or APKs.
