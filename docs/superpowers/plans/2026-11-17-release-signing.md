# Permanent Release Signing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every APK the release workflow publishes is a minified release build signed with one permanent key, so new versions install over old ones without wiping data.

**Architecture:** `app/build.gradle.kts` reads the keystore location and passwords from environment variables and the version code/name from Gradle properties. The workflow decodes the keystore from a GitHub secret, builds `assembleRelease`, and refuses to publish an APK whose certificate doesn't match a pinned fingerprint.

**Tech Stack:** Gradle Kotlin DSL (AGP 9.5.1), GitHub Actions, `keytool`, `apksigner`.

**Spec:** `docs/superpowers/specs/2026-11-17-staff-shifts-design.md`, section "Before this work: one permanent signing key".

## Global Constraints

- Branch `ci/release-signing`, created from `main` (it already holds this plan). Open a PR; **never push to `main`**. Merging the PR triggers the first signed release.
- Only the user creates the keystore, chooses its passwords and sets the GitHub secrets. The agent never sees or stores the passwords. User steps are marked **(user)**: the agent stops and asks the user to run them, suggesting the `! <command>` prefix.
- Local builds use `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- With no signing env vars set, local release builds must still assemble (unsigned). Debug builds don't change.
- No dependency upgrades.

## Review Focus

1. **A secret is missing or empty in CI.** The workflow must fail before building, with a message naming the missing secret, not publish an unsigned APK.
2. **The wrong keystore is uploaded** (e.g. a test key). The fingerprint check fails the run before the release step.
3. **R8 strips something used by reflection** (Room, Hilt, kotlinx.serialization Nav keys). The release APK must start, navigate every tab, place and pay an order, and survive a process restart (saved nav state is deserialized).
4. **The versionCode doesn't increase.** Android refuses downgrades, so each CI release must have a higher versionCode than the one before.
5. **A tag push and a branch push** both still produce a correctly named release.

---

### Task 1: Gradle signing config, versioning, and a verified release build

**Files:**
- Modify: `app/build.gradle.kts` (`defaultConfig`, add `signingConfigs`, `buildTypes.release`)
- Modify: `app/proguard-rules.pro` (only if Step 4 finds a stripping bug)

**Interfaces:**
- Produces, for Task 2: env vars `QAHWA_KEYSTORE_FILE`, `QAHWA_KEYSTORE_PASSWORD`, `QAHWA_KEY_ALIAS`, `QAHWA_KEY_PASSWORD`; Gradle properties `-PversionCode=<int>` and `-PversionName=<string>`; output `app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 1: Switch to the branch**

```bash
git checkout ci/release-signing && git status --short   # expect a clean tree
```

- [ ] **Step 2: Add the signing config and version properties**

In `app/build.gradle.kts`:
- `defaultConfig.versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1`
- `defaultConfig.versionName = (findProperty("versionName") as String?) ?: "1.0"`
- A `signingConfigs.create("release")` block, created **only when** `System.getenv("QAHWA_KEYSTORE_FILE")` is non-null. It reads the file plus the three other env vars.
- `buildTypes.release.signingConfig = signingConfigs.findByName("release")`. When that's null, the build stays unsigned.

- [ ] **Step 3: Make a throwaway local keystore (not the real key) and build a signed release**

```bash
S=/tmp/claude-1000/-home-alolozi-SideProjects-qahwa-manager/d61953ee-3da0-4fec-96ee-c672390832f8/scratchpad
keytool -genkeypair -keystore $S/throwaway.jks -alias test -keyalg RSA -keysize 2048 -validity 1 \
  -storepass throwaway -keypass throwaway -dname "CN=Throwaway"
QAHWA_KEYSTORE_FILE=$S/throwaway.jks QAHWA_KEYSTORE_PASSWORD=throwaway QAHWA_KEY_ALIAS=test QAHWA_KEY_PASSWORD=throwaway \
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew assembleRelease -PversionCode=42 -PversionName=v-test
~/Android/Sdk/build-tools/36.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
~/Android/Sdk/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk | head -1
```

Expected: BUILD SUCCESSFUL. The signer DN is `CN=Throwaway`. Badging shows `versionCode='42' versionName='v-test'`.

Also run it without the env vars. Expected: BUILD SUCCESSFUL, producing `app-release-unsigned.apk`.

- [ ] **Step 4: Smoke-test the minified APK on the emulator**

Boot `qahwa-api34` (see memory: `-no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect`). Uninstall any existing `com.qahwa.manager` first, because it was signed with a different key. Then:

```bash
adb install app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.qahwa.manager/com.example.qahwamanager.MainActivity
```

Then on the emulator:
- Open every tab.
- Add a menu item.
- Place an order for table 3 and mark it paid. Check that Sales shows it.
- Switch the language to Arabic.
- Run `adb shell am kill com.qahwa.manager` while on an order detail screen, then relaunch.

Run `adb logcat -d | grep -E "FATAL|ClassNotFound|NoSuchMethod|SerializationException"` after each step. Expected: no matches. If R8 broke something, add the narrowest `-keep` rule to `app/proguard-rules.pro`, with a comment naming the crash, and repeat this step.

- [ ] **Step 5: Run unit tests**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro
git commit -m "build: Sign release builds from env vars and take version from Gradle properties"
```

---

### Task 2: Release workflow builds, verifies and publishes the signed APK

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `README.md` (Download & Install section)

**Interfaces:**
- Consumes: Task 1's env vars, properties and APK path.
- Requires these GitHub secrets: `QAHWA_KEYSTORE_BASE64`, `QAHWA_KEYSTORE_PASSWORD`, `QAHWA_KEY_ALIAS`, `QAHWA_KEY_PASSWORD`. Also requires the repository **variable** `QAHWA_RELEASE_CERT_SHA256` (lowercase hex, no colons).

- [ ] **Step 1: Rewrite the build steps in `release.yml`**

Order of steps:
1. Checkout, JDK 17, Gradle setup (unchanged).
2. **Check secrets.** Fail with `::error::Missing secret <NAME>` if any of the four secrets or the variable is empty.
3. **Compute the tag** (the existing logic, moved before the build). Also output `version_code=${GITHUB_RUN_NUMBER}`.
4. **Decode the keystore** to `$RUNNER_TEMP/release.jks` with `base64 -d`.
5. `./gradlew test assembleRelease -PversionCode=<version_code> -PversionName=<tag>`, with the four `QAHWA_*` env vars (`QAHWA_KEYSTORE_FILE=$RUNNER_TEMP/release.jks`).
6. **Verify the certificate.** Run `apksigner verify --print-certs` (from the newest folder in `$ANDROID_HOME/build-tools`) on `app/build/outputs/apk/release/app-release.apk` and take the `Signer #1 certificate SHA-256 digest`. Fail the run unless it equals `vars.QAHWA_RELEASE_CERT_SHA256`, comparing lowercased with colons stripped.
7. Copy the APK to `QahwaManager-<tag>.apk`, then upload the artifact and create the release (unchanged).
8. `rm -f $RUNNER_TEMP/release.jks` with `if: always()`.

- [ ] **Step 2: Lint the workflow locally**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/release.yml'))" && echo OK`
Expected: `OK`. If `actionlint` is installed, also run `actionlint .github/workflows/release.yml` and expect no output.

- [ ] **Step 3: Update the README install note**

Under "Download & Install", point to the latest release (not v1.0.0). Add one line: devices that have a build from before this change need to uninstall it once, because earlier builds were signed with temporary keys.

- [ ] **Step 4: Commit and push the branch**

```bash
git add .github/workflows/release.yml README.md
git commit -m "ci: Publish minified release APKs signed with the permanent key"
git push -u origin ci/release-signing
```

- [ ] **Step 5 (user): Create the permanent key and back it up**

Ask the user to run these themselves:

```bash
! keytool -genkeypair -v -keystore ~/qahwa-release.jks -alias qahwa -keyalg RSA -keysize 4096 -validity 36500
! keytool -list -v -keystore ~/qahwa-release.jks -alias qahwa | grep SHA256
```

Tell them: copy `~/qahwa-release.jks` and its passwords somewhere safe off this laptop, such as a password manager. If either is lost, no future version can update an installed app.

- [ ] **Step 6 (user): Set the secrets and the variable**

```bash
! base64 -w0 ~/qahwa-release.jks | gh secret set QAHWA_KEYSTORE_BASE64
! gh secret set QAHWA_KEYSTORE_PASSWORD
! gh secret set QAHWA_KEY_ALIAS --body qahwa
! gh secret set QAHWA_KEY_PASSWORD
! gh variable set QAHWA_RELEASE_CERT_SHA256 --body <the SHA256 from Step 5, colons removed, lowercase>
```

Verify: `gh secret list` shows the four names, and `gh variable list` shows the fingerprint.

- [ ] **Step 7: Prove the workflow on the branch before merging**

Run: `gh workflow run release.yml --ref ci/release-signing`, then `gh run watch`.
Expected: green. The run publishes a release tagged `v1.0.<run>-<sha>` from the branch. Download the APK and check that `apksigner verify --print-certs` shows the permanent fingerprint. Delete that test release and its tag with `gh release delete <tag> --cleanup-tag --yes`, because it came from a branch.

- [ ] **Step 8: Open the PR**

`gh pr create --base main` with a summary, a note that merging publishes the first signed release, and the attribution line. **The user reviews and merges.**
