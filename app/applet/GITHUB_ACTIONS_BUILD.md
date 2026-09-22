# AJIYA - GitHub Actions CI/CD Pipeline

This repository includes an automated GitHub Actions workflow configured in `.github/workflows/android-build.yml` to automatically test, compile, and produce downloadable APK artifacts on every commit or pull request.

## Workflow Overview

- **Triggers**: Pushes or PRs targeting `main` or `master`, as well as manual trigger (`workflow_dispatch`).
- **Build Environment**: `ubuntu-latest` with JDK 17 (Eclipse Temurin) and Gradle caching.
- **Verification**: Automatically runs `./gradlew testDebugUnitTest` across all crisis logic, anti-coercion PIN triggers, geofencing, and telecom fallback tests.
- **Output Artifact**: Compiles and uploads `app/build/outputs/apk/debug/app-debug.apk` as a downloadable artifact named `ajiya-debug-apk`.

## How to Download Your Compiled APK from GitHub

1. Navigate to your repository on GitHub.
2. Click on the **Actions** tab at the top.
3. Click on the latest workflow run (e.g., "Build APK and Run Unit Tests").
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click on **`ajiya-debug-apk`** to download the ready-to-install Android package (.zip containing `app-debug.apk`).

## Configuring Production Secrets in GitHub

To securely supply your production credentials without committing them to git:
1. Go to your GitHub repository **Settings** > **Secrets and variables** > **Actions**.
2. Add the following repository secrets if desired:
   - `TERMII_API_KEY`
   - `TERMII_SENDER_ID`
   - `PAYSTACK_PUBLIC_KEY`
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
