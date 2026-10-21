# AJIYA — GitHub Actions Cloud Build

This repository is configured to build the AJIYA Android application on GitHub Actions.

## Quick Start

1. Commit or push the updated files to your GitHub repository (specifically `.github/workflows/android-build.yml` and `gradle/wrapper/gradle-wrapper.jar`).
2. Go to **Actions** → **AJIYA Android Build** in your repository.
3. Trigger the workflow manually via **Run workflow** (or push to `main`).
4. Once completed, download `AJIYA-debug-apk` under **Artifacts** at the bottom of the summary page.

## Why the Previous Error Occurred
The error:
```
Error: Unable to access jarfile .../gradle/wrapper/gradle-wrapper.jar
Error: Process completed with exit code 1
```
occurred because `gradle/wrapper/gradle-wrapper.jar` was either:
- Not pushed to the GitHub repository (often filtered out by global `.gitignore` rules or omitted during manual drag-and-drop file uploads).
- Missing before the workflow attempted to invoke `./gradlew`.

## How It Is Now Resolved
1. **Automated Gradle Setup:** The workflow now uses `gradle/actions/setup-gradle@v4` to provide Gradle 9.3.1 directly on the runner.
2. **Self-Healing Wrapper:** The workflow checks if `gradle/wrapper/gradle-wrapper.jar` exists; if it is missing from the checkout, it regenerates or downloads it automatically before calling `./gradlew`.
3. **Git Tracking:** `.gitignore` has been updated with explicit rules (`!gradle/wrapper/gradle-wrapper.jar`) so that the JAR file is never ignored when committing code.
4. **Environment Readiness:** Prepares `.env` from `.env.example` to prevent build issues with the Secrets Gradle Plugin.
