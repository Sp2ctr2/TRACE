# Stage build verification

The first Kotlin compile identified a missing `BankIcons.Play` used by the hidden presentation destination. The app-only vector is now defined in `android-native/app/src/main/kotlin/app/saeon/trace/ui/StageNavigation.kt`. It is not part of the reusable TRACE UI library.

Re-run the full native build and both Android viewport test jobs. Do not treat a successful source transformation as a successful APK or a test source file as executed verification.

Presentation timing follows the earlier 180-second TRACE keynote draft. The 55-second rehearsal covers the original 1:15 to 2:10 demonstration segment. It is a rehearsal aid, not a claim about current competition regulations. Each scene is explicitly launched; no clock tick approves, sends, or releases a payment.
