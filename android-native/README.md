# 새온은행 × TRACE — Native Android reference bank

An offline, fictional Korean bank app demonstrating transaction-context protection. Kotlin / Compose, not a WebView or generated-image interface. All money, banks, people, account identifiers and receipts are simulated. No real financial network is connected.

## Build

JDK 17; Gradle 8.11.1; AGP 8.9.2; Kotlin 2.1.20; SDK 35; minimum SDK 26. These are pinned reproducible dependencies, not claims about the newest releases.

The first build needs internet access for dependencies and the pinned Pretendard resources. The installed application does not. The resource preparation script verifies upstream Git blob hashes at commit `5c41199ea0024a9e0b2cb31735265056e5472d76`. Typeface resources and the SIL license are bundled in the APK. Standalone font binaries are generated only under `app/build/` and are not in the source archive.

```sh
# In this directory, bootstrap the wrapper when using a repository checkout:
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
./gradlew :core:test :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.saeon.trace.demo/app.saeon.trace.MainActivity
```

Python 3 is used only by build-time resource preparation. On Windows use `gradlew.bat -Ppython=python` when Python is installed as `python` rather than `python3`.

Outputs: debug `app/build/outputs/apk/debug/app-debug.apk`; optimized demo `app/build/outputs/apk/release/app-release.apk`. Both use a DEVELOPMENT signing identity. Release is optimized and non-debuggable, but is not a production/store release. Different CI runs can have different development keys; replacing a differently signed installation requires uninstalling it, which deletes local demo data.

## Screen design / R4

The deployed TRACE product is the visual reference. R4 uses its actual Korean typeface rather than an OEM Korean font fallback, a continuous warm-light canvas, a restrained text hierarchy and a single coral primary action. The account balance, transfer amount and required next action take precedence over explanatory text. Account lists and menus are not a grid of decorative cards.

Home, assets, recipient selection, amount input, review, completion, WARN, HOLD, VERIFY, UNKNOWN, the official route, safety center and demo center are native screens. HOLD keeps one transaction summary and three reasons; full explanations and the timeline are reached through a reason. Safety guidance now presents one step at a time in every reading mode.

## Demonstration center

Enter using Home → 시연, 전체 → 시연 센터, or 안전 → 시연 센터. The version-row five-tap shortcut remains compatible. Reading mode can be changed in the center itself. A scenario selection is a read-only preview. Only **이 상황으로 시작** resets the fictional ledger and returns to home. Reading preferences survive that reset.

- Normal: 이서연 / 32,000원 → ALLOW, balance 12,840,000 → 12,808,000원.
- Impersonation: 김○○ / 3,000,000원 → HOLD, no debit or receipt.
- Loan: 박○○ / 8,000,000원 → VERIFY. The independent fixture route returns 새온은행 대출상환센터 / 200-***-3014 / 생활안심대출. A NEW intent, review and authorization are required before ALLOW.
- Route unavailable: same personal-account loan request → UNKNOWN, no general-transfer fallback.
- Warning: 김○○ / 120,000원 → WARN. An independent check must be acknowledged before review and fresh authentication.
- Easy: the impersonation scenario with large-text guidance. Child mode uses simpler wording and asks for a trusted adult; it does not pretend an adult has authenticated or approved anything.

An entered but never-paid recipient is not described as a recent completed payment.

## Architecture and safety boundary

`core`: immutable Kotlin models, risk-policy evaluation, bounded local rule extraction and the pure transaction state machine. `app/ui`: Compose, Navigation Compose, Android ViewModels and lifecycle-aware StateFlow. `app/data`: Room atomic ledger snapshot, versioned codec, checksum, DataStore settings and demo clock. `app/security`: Android Keystore EC P-256, SHA256withECDSA and the local DemoBankGateway.

Balance, receipts and transaction states are written together inside one Room transaction under a mutex. Publications are revision-ordered. Submitted form data is captured atomically instead of trusting a queued draft autosave. An intent binds amount, recipient identity/account/kind, purpose, funding account, time and official-route identity. Editing invalidates old approval. Authorization and attestation bind the same transaction, current context digest, fresh nonce and expiration. A duplicate completed callback cannot debit again.

Only EVALUATING with a valid, matching, re-evaluated ALLOW packet can commit. HOLD, VERIFY, UNKNOWN, ROUTE, cancelled and superseded states cannot. Request-level evidence survives recipient/purpose edits. Loan-purpose evidence is independent of the UI dropdown. A verified route creates a new loan intent, never a blanket approval for the original personal account.

HOLD survives process death. Interrupted authorization/evaluation returns to review with approval cleared. A database failure does not silently reset money or finish a transfer. Risk events apply to new transactions for 15 minutes; their expiry does not release an existing held transfer. Official routes expire after five minutes; authorizations after two; attestations after one or sooner.

## Privacy

No INTERNET, SMS, contacts, call-log, phone-state or location permissions. ACTION_SEND `text/plain` previews need explicit consent. Raw input is stripped from the incoming intent, kept only in an in-memory ViewModel and cleared on completion/cancellation. It is not in Room, logs or signed packets. A newer share cannot receive an older analysis result. Other apps or the system clipboard can retain their own copies; this app cannot erase those.

Voice is user-initiated, foreground-only Android on-device recognition, available only with a suitable local speech model. No network fallback or call listening. Unsupported devices retain text input. BiometricPrompt is real when available; **시연 확인** is explicitly simulated authentication. Authentication never overrides fraud policy.

## Testing and evidence

```sh
./gradlew :core:test :app:connectedDebugAndroidTest
# Built debug/test/release APKs and a running dedicated API-35 emulator:
bash tools/run-device-suite.sh
```

The CI gate retains the two consecutive domain/UI/golden runs, separate force-stop/relaunch checks, 360×800 / 393×873 / 412×915 dp configurations at font scales 1.0/1.15/1.3/1.5/2.0, landscape and forced-light checks. Tests additionally verify inline demo-mode changes do not approve transactions, previews are read-only, never-sent recipients are labelled correctly, balance hiding and the actual bundled font family. Screenshots are Android UiAutomation captures. Stable frames and text/touch measurements are retained. Negative controls reject real clipping and ellipses; tests are not skipped to pass the gate.

The generated verification JSON, logs, actual screenshots and APK hash establish execution results. This README is a design/build description, not evidence that a run passed. Release installation/hash/cold-launch checks are a smoke test, distinct from full debug instrumentation.

## Explicit limitations

The signal extractor is a fixed local rule recognizer, not a trained model or a guarantee against all scams. The same-process gateway is a demonstration boundary, not a remote bank backend, hardware attestation or protection against a modified/rooted client. Card statements are fixed fixtures; upcoming payments are local presentation settings, not background payment execution. Real product opening, financial account lookup, bank credentials and customer-center calls are not implemented or represented as real services.

Automated text/semantics/target checks do not replace a human TalkBack usability session. CI timings are not physical-device performance guarantees. Real biometric success, offline Korean speech availability and manual assistive-technology review need suitable physical hardware.
