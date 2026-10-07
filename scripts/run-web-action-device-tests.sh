#!/usr/bin/env bash
set -uo pipefail
result=0
./gradlew --no-daemon --max-workers=2 :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.webActionFixtureUrl=https://10.0.2.2:18443/   -Pandroid.testInstrumentationRunnerArguments.class=io.ethan.pushgo.data.db.PushGoDatabaseMigrationDeviceTest,io.ethan.pushgo.data.db.PushChannelMigrationDeviceTest,io.ethan.pushgo.ui.accessibility.TransportSelectorUiTest,io.ethan.pushgo.web.TaskMessageCardUiTest,io.ethan.pushgo.web.WebActionDeviceTest || result=$?
mkdir -p app/build/reports/web-action
# Gradle can uninstall the target after tests; UTP XML/logcat remain authoritative.
adb exec-out run-as io.ethan.pushgo cat files/web-action-failure.xml > app/build/reports/web-action/failure.xml 2>/dev/null || true
adb exec-out run-as io.ethan.pushgo cat files/web-action-failure.png > app/build/reports/web-action/failure.png 2>/dev/null || true
exit "$result"
