# AJIYA — GitHub Actions Cloud Build

This package is prepared to build AJIYA in GitHub Actions without requiring the Gradle wrapper JAR to be present locally.

## Quick start

1. Create a new GitHub repository, for example `ajiya-android`.
2. Upload **all contents of this folder** to the repository root (do not upload the parent folder itself).
3. Push to GitHub.
4. Open **Actions → AJIYA Android Build**.
5. Open the latest workflow run.
6. Wait for **Build debug APK** and **Run unit tests**.
7. Under **Artifacts**, download `ajiya-debug-apk`.
8. Extract the downloaded artifact and install the APK on an Android test phone.

## What the workflow does

- Uses Ubuntu GitHub-hosted runner.
- Uses Temurin JDK 21.
- Installs Gradle 9.3.1 directly, so the missing local Gradle wrapper JAR is not a blocker.
- Installs Android SDK platform 36 and build tools 36.0.0.
- Copies `.env.example` to `.env` if no `.env` exists.
- Builds `assembleDebug`.
- Runs `testDebugUnitTest`.
- Uploads the debug APK as a workflow artifact.
- Uploads test reports when available.

## Important safety note

This is a debug build for controlled testing. It is **not** a production emergency-dispatch release. Do not connect real emergency contacts or production credentials until the backend, identity, permissions, dispatch, and device behavior have been validated.

## Production secrets

Do not commit Paystack, Supabase, Firebase, Termii, signing keys, or other production secrets to GitHub. Add them later as GitHub Actions Secrets/Variables and use a separate release workflow.
