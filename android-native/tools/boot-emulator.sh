#!/usr/bin/env bash
set -euo pipefail
SDK="${ANDROID_HOME:?ANDROID_HOME is required}"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$SDK/cmdline-tools/latest/bin:$PATH"
mkdir -p android-native/verification/logs
LOG=android-native/verification/logs
IMAGE='system-images;android-35;google_apis;x86_64'
retry_install() {
  local package="$1"
  for attempt in 1 2 3 4; do
    if sdkmanager --install "$package"; then return 0; fi
    echo "SDK download failed for $package, attempt $attempt" >&2
    sleep $((attempt * 4))
  done
  return 1
}
# Preserve the runner's compatible platform-tools instead of upgrading them.
if [ ! -x "$SDK/platform-tools/adb" ]; then retry_install 'platform-tools'; fi
if [ ! -x "$SDK/emulator/emulator" ]; then retry_install 'emulator'; fi
if [ ! -f "$SDK/system-images/android-35/google_apis/x86_64/system.img" ]; then retry_install "$IMAGE"; fi
adb version | tee "$LOG/adb-version.txt"
emulator -version > "$LOG/emulator-version.txt" 2>&1
sdkmanager --list_installed > "$LOG/installed-sdk.txt"
echo no | avdmanager create avd --force --name saeon35 --package "$IMAGE" --device pixel_6
cat >> "$HOME/.android/avd/saeon35.avd/config.ini" <<'AVD'
hw.ramSize=4096
vm.heapSize=512
hw.keyboard=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect
AVD
nohup emulator -avd saeon35 -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > "$LOG/emulator-boot.txt" 2>&1 &
echo "$!" > "$LOG/emulator.pid"
timeout 180 adb wait-for-device
booted=false
for attempt in $(seq 1 180); do
  if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = '1' ]; then booted=true; break; fi
  if ! kill -0 "$(cat "$LOG/emulator.pid")" 2>/dev/null; then cat "$LOG/emulator-boot.txt"; exit 1; fi
  sleep 2
done
if [ "$booted" != true ]; then cat "$LOG/emulator-boot.txt"; exit 1; fi
adb shell input keyevent 82
adb shell getprop ro.build.fingerprint | tee "$LOG/booted-device.txt"
