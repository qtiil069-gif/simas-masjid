#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_AVD_HOME="$PWD/avd-test"
mkdir -p "$ANDROID_AVD_HOME/Simas.avd"
cp avd/Simas.avd/config.ini "$ANDROID_AVD_HOME/Simas.avd/config.ini"
printf "avd.ini.encoding=UTF-8\npath=%s\ntarget=android-29\n" "$ANDROID_AVD_HOME/Simas.avd" > "$ANDROID_AVD_HOME/Simas.ini"
export ANDROID_SDK_ROOT="$PWD/sdk"
ADB="$PWD/sdk/platform-tools/adb"
rm -f avd-test/Simas.avd/*.lock
"$PWD/sdk/emulator/emulator" -avd Simas -accel off -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect > emulator-test.log 2>&1 &
SIMAS_EMULATOR_PID=$!
trap 'kill "$SIMAS_EMULATOR_PID" 2>/dev/null || true' EXIT
"$ADB" start-server
for i in $(seq 1 60); do
 status=$(timeout 8 "$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
 if [ "$status" = 1 ]; then break; fi
 if ! kill -0 "$SIMAS_EMULATOR_PID" 2>/dev/null; then tail -30 emulator-test.log; exit 1; fi
 echo "Waiting for Android boot ($i/60)"
 sleep 5
done
[ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = 1 ] || { echo "BOOT FAILED"; exit 1; }
"$ADB" shell svc wifi disable
"$ADB" shell svc data disable
echo "Letting Android settle before UI tests"
sleep 15
timeout 120 "$ADB" install --no-streaming -r ../deliverables/android/SIMASJID-2.4-Uji.apk
timeout 120 "$ADB" install --no-streaming -r tests/tests.apk
timeout 240 "$ADB" shell am instrument -w id.simas.mobile.modernpreview.tests/id.simas.mobile.modernpreview.NativeTests | tee tests/runtime-result.txt
grep -Eq "PASS [0-9]+ native checks" tests/runtime-result.txt || { echo "TESTS FAILED"; exit 1; }
mkdir -p tests/screenshots
"$ADB" pull /sdcard/Android/data/id.simas.mobile.modernpreview/files/. tests/screenshots/
"$ADB" logcat -d -s AndroidRuntime:E > tests/runtime-errors.txt
