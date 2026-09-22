# AJIYA - Rebuild & Release Notes V1

## Build & Release Verification
- **Compilation**: Clean Gradle Kotlin DSL build passing with zero errors.
- **Unit Tests**: All 33 automated tests verified via Robolectric (`:app:testDebugUnitTest`).
- **Target SDK**: Android 14+ (API 34/35 compatible).
- **Kotlin Version**: 2.0+ with Jetpack Compose Material 3.

## Included Modules & Capabilities
1. **Google Play Compliance Suite**:
   - `ProminentLocationDisclosureDialog`: Clear, standalone disclosure for `ACCESS_BACKGROUND_LOCATION` before tracking starts.
   - `ProminentSmsDisclosureDialog`: Transparent justification for offline emergency SMS dispatch.
   - Play Console 5-step video review walkthrough script inside `SecurityLegalScreen.kt`.
2. **Production Gateway Switcher**:
   - Integrated `AppConfigManager` and in-app `ProductionConfigCard` to switch between Sandbox testing and Live African telecom/billing endpoints without recompilation.
3. **CI/CD Pipeline**:
   - `.github/workflows/android-build.yml` for automated test execution and APK artifact generation.
