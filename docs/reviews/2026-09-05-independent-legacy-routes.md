# Independent Legacy routes in mixed authentication

Fixes the P2 found in the final parallel-voice review. Previously, any background capture delayed every otherwise-ready Legacy route until all captures finished. This follow-up supersedes the batch-wait limitation in 2026-09-05-parallel-voice-auth.md.

Changed module: biometric. Direct consumers include biometric-custom-voice and app; neither changed in this follow-up.

Production files:
- impl/BiometricPromptApi28Impl.kt: starts ready routes immediately; each background onReady starts only its own type with a separate Bundle. Does not write capture data into shared Builder extras. Queued starts require both the authentication generation and current Legacy owner. The Builder remains authoritative for the enrollment flag.
- impl/ParallelSoftwareCapture.kt: independently consumes each provider completion once, without an all-providers barrier.
- engine/LegacyAuthSession.kt: owns the set of started module tags until cancellation, independent of individual module success.
- engine/LegacyBiometric.kt: internal session-aware entry point adds new modules without cleaning Core or restarting existing modules; public authenticate signature remains unchanged. Callbacks from canceled owners are ignored.
- engine/core/Core.kt: runs only selected new module tags using the existing cryptographic preparation and failure policy; checks session ownership before starting each module.

Tests changed/added: ParallelSoftwareCaptureTest.kt, LegacyAuthSessionTest.kt, LegacyBiometricCancellationTest.kt.

Commands executed:
```powershell
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.LegacyAuthSessionTest" --tests "dev.skomlach.biometric.compat.impl.ParallelSoftwareCaptureTest" --tests "dev.skomlach.biometric.compat.impl.Api28EnrollmentCompletionTest" --console=plain
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.LegacyBiometricCancellationTest" --console=plain
git -c safe.directory=C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat -c core.safecrlf=false diff --check -- biometric/src/main/java/dev/skomlach/biometric/compat/engine/LegacyBiometric.kt biometric/src/main/java/dev/skomlach/biometric/compat/engine/core/Core.kt biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt biometric/src/test/java/dev/skomlach/biometric/compat/engine/LegacyBiometricCancellationTest.kt
```

Results: first run PASS (9 tests, 7s); second PASS (4 tests, 3s), after adding the concrete Legacy/Core regression test. Diff check PASS. No production changes between these passing runs.

The concrete engine test starts fingerprint, adds face without canceling/restarting fingerprint, completes fingerprint, adds iris, repeats face without restarting it, then verifies cancellation of every signal. These are fake modules through real LegacyBiometric/Core, not physical sensors. Other tests cover independent provider readiness, duplicates, failure/silence of another provider, session ownership, and the existing enrollment completion policy.

Static re-review: the original all-providers barrier is removed. No Voice-specific dispatch condition introduced. Background capture remains an explicit factory capability; UI-dependent providers retain staged preparation. No new button or QA control.

Not run: app/APK assembly, device installation/QA, full suite, release/R8, or encrypted multi-module runtime. The previously installed Pixel APK does not include this follow-up. Physical sensor resource conflicts and UI-dependent provider combinations remain runtime-unverified; this does not close universal OEM coverage.
