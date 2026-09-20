#!/usr/bin/env bash
set -euo pipefail
SDK="${ANDROID_HOME:?ANDROID_HOME is required}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="$ROOT/verification/logs"
mkdir -p "$LOG"
export ANDROID_SDK_ROOT="$SDK"
export ANDROID_USER_HOME="$ROOT/.android-user"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export PATH="$SDK/platform-tools:$SDK/emulator:$SDK/cmdline-tools/latest/bin:$PATH"
mkdir -p "$ANDROID_AVD_HOME"
if [ -n "${GITHUB_PATH:-}" ]; then
  printf '%s\n' "$SDK/platform-tools" "$SDK/emulator" "$SDK/cmdline-tools/latest/bin" >> "$GITHUB_PATH"
fi
if [ -n "${GITHUB_ENV:-}" ]; then
  printf 'ANDROID_USER_HOME=%s\nANDROID_EMULATOR_HOME=%s\nANDROID_AVD_HOME=%s\n' "$ANDROID_USER_HOME" "$ANDROID_EMULATOR_HOME" "$ANDROID_AVD_HOME" >> "$GITHUB_ENV"
fi
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
# Headless QEMU still dynamically links the audio/X11 libraries.
if [ "${GITHUB_ACTIONS:-false}" = true ]; then
  sudo apt-get update -qq
  sudo apt-get install -y --no-install-recommends libpulse0 libnss3 libx11-6 libxcb1 libxcomposite1 libxcursor1 libxi6 libxtst6 libxrandr2 libxkbcommon0 libasound2t64 libegl1 libgl1
fi
if [ ! -x "$SDK/platform-tools/adb" ]; then retry_install 'platform-tools'; fi
if [ ! -x "$SDK/emulator/emulator" ]; then retry_install 'emulator'; fi
if [ ! -f "$SDK/system-images/android-35/google_apis/x86_64/system.img" ]; then retry_install "$IMAGE"; fi
adb version | tee "$LOG/adb-version.txt"
LD_LIBRARY_PATH="$SDK/emulator/lib64:$SDK/emulator/lib64/qt/lib:${LD_LIBRARY_PATH:-}" ldd "$SDK/emulator/qemu/linux-x86_64/qemu-system-x86_64" > "$LOG/emulator-libraries.txt" 2>&1 || true
if ! emulator -version > "$LOG/emulator-version.txt" 2>&1; then cat "$LOG/emulator-version.txt" >&2; exit 1; fi
sdkmanager --list_installed > "$LOG/installed-sdk.txt"
# Never assume a runner's default AVD directory; SDK user-home overrides vary.
AVD_PATH="$ANDROID_AVD_HOME/saeon35.avd"
printf 'no\n' | avdmanager create avd --force --name saeon35 --package "$IMAGE" --device pixel_6 --path "$AVD_PATH" 2>&1 | tee "$LOG/avd-create.txt"
test -f "$AVD_PATH/config.ini"
cat >> "$AVD_PATH/config.ini" <<'AVD'
hw.ramSize=4096
vm.heapSize=512
hw.keyboard=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect
AVD
emulator -list-avds | tee "$LOG/avd-list.txt"
grep -qx 'saeon35' "$LOG/avd-list.txt"
nohup emulator -avd saeon35 -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot -camera-back none -camera-front none > "$LOG/emulator-boot.txt" 2>&1 &
echo "$!" > "$LOG/emulator.pid"
if ! timeout 180 adb wait-for-device; then cat "$LOG/emulator-boot.txt" >&2; exit 1; fi
booted=false
for attempt in $(seq 1 180); do
  if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = '1' ]; then booted=true; break; fi
  if ! kill -0 "$(cat "$LOG/emulator.pid")" 2>/dev/null; then cat "$LOG/emulator-boot.txt" >&2; exit 1; fi
  sleep 2
done
if [ "$booted" != true ]; then cat "$LOG/emulator-boot.txt" >&2; exit 1; fi
adb shell input keyevent 82
adb shell getprop ro.build.fingerprint | tee "$LOG/booted-device.txt"
