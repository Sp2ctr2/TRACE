# 새온은행 1.1 — TRACE quiet banking rebuild

## Sources inspected again on 2026-09-20

- The user's supplied warm-light TRACE phone image: preserve one headline, one outlined transaction summary, three short reasons and one primary action. The decorative phone shell, cables and perspective are not app UI.
- https://sp2ctr2.github.io/TRACE/ — direct web retrieval failed in the assistant environment. Actual Chromium captures, page HTML, CSS tokens and the TRACE symbol were instead read from the repository's reference-audit Actions artifact (run 35485650727), and the same live capture job runs on this revision again.
- https://github.com/Sp2ctr2/TRACE/blob/main/index.html — source and font declarations inspected. The supplied source archive was loaded at commit 6a3a9d953371e15ca7f5da80587ca495819c1212.
- https://tossmini-docs.toss.im/tds-mobile/components/BottomCTA/Single/ — one reachable primary action, safe-area treatment and keyboard handling. These are interaction references, not an imported WebView or React UI.
- https://docs.tosspayments.com/sdk/v2/widget-android — explicit amount/order data and callback validation; this is a merchant payment SDK, not a bank-app design template. It is NOT installed or called by this offline demonstration.
- https://developers.kftc.or.kr/dev/openapi/open-banking/deposit — bank/recipient/amount identifiers, masked output, transaction IDs and explicit response/error handling. No live Open Banking credentials, consent flow or financial-network connection is supplied or claimed.

## Implemented changes

One warm-light surface is used across the page, toolbar and bottom action region. Redundant micro-headings, repetitive avatar circles, white floating panels and secondary explanatory menus were removed from the main flows. TRACE uses the source symbol paths and the canonical coral/deep-coral values. Small text and input outlines retain the measured contrast variants. Platform Korean sans is used; no bundled or remote font dependency is introduced.

Home, assets, recipient selection, amount editor, review, HOLD, VERIFY, UNKNOWN, official route, safety center and the main menu have simplified native Compose destinations. The immutable banking state machine, signature validation, idempotent ledger commit and fail-closed policy are retained.

The **시연 센터** is reachable from Home → 시연 or 전체 → 시연 센터. Scenario preview is read-only; only the separate start action resets fictional banking state. Reading-mode preferences survive scenario changes. The legacy hidden route is retained as an alias for prior test automation, not as the only way into the center.

Screen modes are **기본**, **큰 글씨** and **어린이**. They persist in DataStore. Large and child modes simplify the home screen and use one-step-at-a-time safety guidance. Child mode says to consult a known trusted adult and explicitly does not claim the app contacted an adult or received parental authorization. Changing modes cannot authorize, release or commit a transfer.

## Verification

The workflow builds debug, instrumented test and optimized release-demo APKs, reruns JVM tests, runs the full device suite twice, tests force-stop/relaunch HOLD persistence, then executes phone-size/font-scale/landscape/system-dark matrices. Four new canonical captures cover the demo preview, reading modes, child HOLD and child guidance. New tests cover read-only preview, mode persistence, guardian-authorization non-claims and unchanged ledger invariants.

A successful build is not a successful visual audit. Final claims must use the actual Actions logs and screenshots produced for this revision. Manual TalkBack listening and physical-device biometric/voice testing remain separate from the automated checks.

## Installation

The installed app is a fictional bank and never transfers real funds. Development signing is used; CI runner keys may differ between builds. A signature mismatch can require uninstalling the previous demo, which deletes its local demonstration state. The release-demo binary is optimized, but is not a production bank release or a store signing identity.
