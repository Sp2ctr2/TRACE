# 새온은행 × TRACE — Native Android reference bank

Warm-light, offline Korean banking reference implementation. The app is a fictional bank shell containing TRACE's transaction-context protection layer, not a landing page or WebView wrapper.

**No real money moves.** All banks, people, account identifiers, balances, customer-support content and receipts are fictional. There is no external banking integration or network permission.

## Build and run

Pinned build: JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2, Kotlin/Compose compiler 2.1.20, compile/target SDK 35, minimum SDK 26. Dependency versions are deliberately pinned for reproducibility; they are not advertised as the latest versions.

Open this directory in Android Studio with SDK 35 installed. The delivery source archive contains a generated Gradle Wrapper. A repository checkout can bootstrap it using an installed Gradle 8.11.1:

```sh
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
./gradlew :core:test :app:assembleDebug :app:assembleRelease :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.saeon.trace.demo/app.saeon.trace.MainActivity
```

Windows uses `gradlew.bat`. Initial build/dependency downloads require a network; **installed application flows do not**. No Google account, SIM, server, remote image or downloaded app font is needed.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release-demo APK: `app/build/outputs/apk/release/app-release.apk`. Both use a development signing identity; the release variant is optimized with R8, not a production/store signing certificate. Different CI machines may generate different demo signing keys. Uninstall an older differently signed build before installing, understanding that uninstalling clears local demo state.

## App navigation

Five bottom tabs: 홈, 자산, 송금, 안전, 전체. Additional destinations cover account details, savings/imports, card statement, loan, completed history with search/type/period filters, receipts, recipient entry, amount editing, review, evaluation, all policy states, reasons, safety guide, timeline, consent-based sharing/manual checks, privacy, profile/security/transfer settings, favorite accounts, upcoming-payment display, notifications, accessibility, help and app information.

To open **Demo Lab**, go to 전체 → 앱 정보 and tap the version row five times. Scenario selection explicitly resets only the fictional local state.

| Scenario | Recipient / amount | Expected result |
|---|---|---|
| 정상 송금 | 이서연 / 32,000원 | ALLOW; 12,840,000 → 12,808,000원 |
| 기관 사칭 | 김○○ / 3,000,000원 | HOLD; no debit, no completed receipt |
| 대출 상환 | 박○○ / 8,000,000원 | VERIFY; independent fixture route; new intent and authorization; ALLOW |
| 공식 경로 조회 실패 | 박○○ / 8,000,000원 | UNKNOWN; fail closed; unchanged balance |
| 보내기 전 확인 | 김○○ / 120,000원 | WARN; explicit independent check, review and new authorization |
| 쉬운 모드 | 김○○ / 3,000,000원 | Simplified HOLD; no bypass action |

The verified loan route is 새온은행 대출상환센터 / 새온은행 200-***-3014 / 생활안심대출. Routing does not authorize or send money by itself. A successful full 8,000,000원 repayment leaves a 4,840,000원 checking balance and zero fictional loan balance.

## Architecture

- `core`: pure Kotlin immutable domain models, policy evaluator, bounded local signal extractor, transaction state machine, 43 JVM regression tests.
- `app/ui`: native Compose design system, Navigation Compose destinations, lifecycle-aware StateFlow observation and Android ViewModels. Input state flows into the repository; navigation never authorizes a transaction.
- `app/data`: Room ledger, explicit versioned JSON snapshot codec, DataStore non-financial preferences and a presentation-date clock that advances with elapsed time.
- `app/security`: Android Keystore EC P-256 handle, SHA256withECDSA attestation signing, pinned local verifier and deterministic DemoBankGateway.

A single small Room snapshot is the atomic ledger unit. Its balance, receipts, intents, challenges and held states are committed together inside `RoomDatabase.withTransaction`, serialized by a repository mutex. This avoids balance/receipt split writes. The design is intentionally sized for an offline reference application rather than a large distributed bank ledger.

## Transaction invariants

Transaction bindings cover the intent ID, amount, recipient identity/name/bank/account/kind, purpose, funding account, creation time, original intent and official route ID. Changing a reviewed transaction creates a new intent and invalidates old authorization.

An authorization binds the exact transaction, relevant live-context digest, fresh nonce and expiration. A risk attestation additionally binds policy decision, reason codes, key identity and the fixed `rawContentExported=false` field. The bank fixture verifies the signature against its provisioned Keystore public key, re-evaluates current policy, checks freshness and rejects nonce reuse before committing.

HOLD, VERIFY, UNKNOWN, ROUTE and cancelled/superseded states are not committable. Only EVALUATING with a matching valid ALLOW attestation can debit. Repeated callbacks for an already completed intent return the existing durable state. Eight simultaneous repository submissions are covered by device tests.

On process restoration, HOLD remains HOLD. Interrupted authorization/evaluation returns to REVIEW with all authorization tokens removed. A committed receipt remains committed and cannot debit again. A storage failure never silently initializes a fresh balance or completes a pending transfer.

A loan-repayment request is preserved as a structured signal independent of the purpose dropdown. Active request-level evidence is not discarded by selecting a different payee. An official route creates a new loan intent; it does not bless the original personal account. The form submitted for review is captured in a single repository transaction, so a delayed draft autosave cannot replace it.

Risk events are eligible for 15 minutes. Expiration removes their influence on a **new** transaction; it never silently releases a previously held transaction. Official route responses expire after five minutes. User authentication expires after two minutes; attestations expire after one minute or sooner when the authorization expires.

## Privacy and permissions

No INTERNET, SMS, contact, call-log, phone-state or location permission. Text sharing uses ACTION_SEND `text/plain`; the incoming extra is stripped after ingestion. The user must confirm the preview before extraction. Raw input lives only in an in-memory screen ViewModel, is not saved in Room/SavedStateHandle/logs/attestations, and is cleared after a successful check or cancellation. The originating app or Android clipboard may retain its own copy; this app does not claim to erase other applications' data.

The extractor outputs only a fixed risk type, static explanation, source, timestamps and optional transaction scope. It does not fetch links. There are no telemetry or advertising SDKs. Backups/device transfer are excluded. The ledger checksum detects accidental corruption; it is not claimed as protection against a rooted attacker who can rewrite both data and checksum.

RECORD_AUDIO is requested only when the user chooses voice input. Android 12+ on-device recognition is used only if available; there is no network speech fallback, continuous recording or call listening. The microphone stops when the activity leaves the foreground. Unsupported devices retain manual text entry.

USE_BIOMETRIC enables a real BiometricPrompt when strong enrolled biometrics exist. Cancellation never becomes successful authorization. Explicit **시연 확인** remains available, clearly labelled as a simulation rather than real identity verification.

## Visual source and accessibility

Source of truth: the deployed TRACE website and its `index.html`, inspected before implementation. Exact TRACE mark paths and canonical paper/surface/ink/divider/coral colors were transferred to native vectors and Compose tokens. There is no HTML UI, custom web rendering, neon indicator, emoji icon, security score, dark theme or endlessly running animation.

Canonical coral is retained for the mark. Buttons use the canonical deep coral `#D93B25` against white (calculated contrast approximately 4.56:1). Small text uses `#676960` against paper (approximately 5.07:1), slightly darker than the site's original muted token. Small coral text uses `#CB3421` (approximately 4.71:1 against paper); input outlines use `#8B8D82` (approximately 3.06:1). These functional contrast variants do not replace the canonical logo or button colors. Platform sans-serif/Korean fallback is used; no font files or runtime font download are included.

Interactive controls use at least 48dp targets. Headings, roles, state descriptions and monetary descriptions are exposed to accessibility services. Scrollable content and IME insets keep controls reachable. Easy mode increases body copy and simplifies HOLD independently of system font scaling.

## Verification

```sh
./gradlew :core:test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
# With a running API-35 emulator and built test APK:
bash tools/run-device-suite.sh
```

The device script runs the repository/UI/golden suite twice, with a separate force-stop/relaunch HOLD check in each pass. It then captures layouts at 360×800dp, 393×873dp and 412×915dp at font scales 1.0/1.15/1.3/1.5/2.0, plus landscape and a system-dark-mode case. PNGs come from Android UiAutomation screenshots, not generated artwork. Visible click bounds and text layouts are audited. Text is remeasured at its actual drawn bounds because the Compose simple-text semantics bridge can reconstruct a wider parent paragraph; negative-control tests still reject real height/width clipping and ellipses. Raw and normalized measurements are retained in the evidence. A missing screenshot or failed test is not counted as a pass. After the debug regression, a separate script installs the optimized release APK, checks that it is not debuggable, cold-launches the real home screen, captures its native view hierarchy and PNG, and compares the installed APK hash with the exact build output. This release smoke check is explicitly distinct from full debug instrumentation.

Only the generated `verification/VERIFICATION.md`, `verification/verification.json`, instrumentation logs, JVM XML results and PNG captures establish which checks actually ran and passed. Test source alone is not evidence of execution.

## Explicit scope boundaries

The manual signal extractor is an explainable keyword/rule recognizer, not a trained language model or a guarantee against all scams. The same-process gateway demonstrates policy, signature, binding and replay invariants, not remote bank trust, hardware attestation or resistance to a modified/rooted client. All financial data is fictional.

Favorite-list membership is a UI preference, not proof that a recipient is safe. The card statement is a fixed monthly fixture; the recurring-payment screen manages upcoming-item visibility, not background payment execution. Product opening, real authentication/login, real account lookup and actual customer-center calls are not exposed as functioning services.

Automated semantics tests do not constitute manual TalkBack listening. CI emulator timing does not establish physical-device performance. Real biometric enrollment/success and availability of an offline Korean speech model require suitable physical hardware or a configured device. These boundaries must remain visible in any presentation of the evidence.
