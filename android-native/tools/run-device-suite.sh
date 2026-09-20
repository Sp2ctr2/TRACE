#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PACKAGE=app.saeon.trace.demo
RUNNER="$PACKAGE.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p verification/logs verification/apk
collect() {
  rm -rf verification/screens.next
  if adb pull "/sdcard/Android/data/$PACKAGE/files/verification" verification/screens.next >/dev/null 2>&1; then
    rm -rf verification/screens
    mv verification/screens.next verification/screens
  fi
  adb logcat -b crash -d > verification/logs/crash-buffer.txt 2>/dev/null || true
  adb shell dumpsys gfxinfo "$PACKAGE" framestats > verification/logs/frame-stats.txt 2>/dev/null || true
  adb shell dumpsys accessibility > verification/logs/accessibility-services.txt 2>/dev/null || true
  adb shell settings get system font_scale > verification/logs/font-scale-final.txt 2>/dev/null || true
  cp app/build/outputs/apk/debug/app-debug.apk verification/apk/saeon-trace-demo.apk 2>/dev/null || true
  (cd verification/apk && sha256sum *.apk > SHA256SUMS.txt) 2>/dev/null || true
}
finish_suite() {
  local status=$?
  trap - EXIT
  set +e
  collect
  python3 tools/summarize-verification.py
  local report_status=$?
  if [ "$status" -ne 0 ]; then exit "$status"; fi
  exit "$report_status"
}
trap finish_suite EXIT
adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put system haptic_feedback_enabled 0
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put secure show_ime_with_hard_keyboard 1
adb shell wm density 320
adb shell wm size 786x1746
adb shell settings put system font_scale 1.0
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear "$PACKAGE"
adb shell am start -W -n "$PACKAGE/app.saeon.trace.MainActivity" | tee verification/logs/cold-start.txt
adb shell getprop ro.build.fingerprint > verification/logs/device-fingerprint.txt
adb shell getprop ro.build.version.sdk > verification/logs/api-level.txt
adb shell wm size > verification/logs/baseline-size.txt
adb shell wm density > verification/logs/baseline-density.txt
adb shell dumpsys package "$PACKAGE" > verification/logs/package.txt
run_test() {
  local name="$1" classes="$2"
  adb shell am instrument -w -r -e class "$classes" -e pass "$name" "$RUNNER" | tee "verification/logs/$name.txt"
  grep -Eq '^OK \([0-9]+ tests?\)' "verification/logs/$name.txt"
  ! grep -q 'FAILURES!!!' "verification/logs/$name.txt"
}
SUITE=app.saeon.trace.RepositoryDeviceTest,app.saeon.trace.BankUiFlowTest,app.saeon.trace.GoldenScreensTest,app.saeon.trace.PrivacyLifecycleTest,app.saeon.trace.TextBoundsAuditTest
for pass in pass-1 pass-2; do
  run_test "$pass" "$SUITE"
  run_test "$pass-seed" app.saeon.trace.SeedHoldProcessTest
  adb shell am force-stop "$PACKAGE"
  adb shell am start -W -n "$PACKAGE/app.saeon.trace.MainActivity" | tee "verification/logs/$pass-process-relaunch.txt"
  run_test "$pass-restore" app.saeon.trace.RestoreHoldProcessTest
  touch "verification/$pass.passed"
done
for dimensions in 720x1600 786x1746 824x1830; do
  for scale in 1.0 1.15 1.3 1.5 2.0; do
    adb shell am force-stop "$PACKAGE"
    adb shell wm size "$dimensions"
    adb shell settings put system font_scale "$scale"
    run_test "matrix-${dimensions}-${scale}" app.saeon.trace.LayoutMatrixTest
  done
done
for scale in 1.0 2.0; do
  adb shell am force-stop "$PACKAGE"
  adb shell wm size 1600x720
  adb shell settings put system font_scale "$scale"
  run_test "matrix-landscape-${scale}" app.saeon.trace.LayoutMatrixTest
done
adb shell settings put system font_scale 1.0
adb shell wm size 786x1746
adb shell cmd uimode night yes
run_test dark-system-forced-light app.saeon.trace.LayoutMatrixTest
adb shell cmd uimode night no
