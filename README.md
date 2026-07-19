# Mtapk

Android app that takes an APK file, fully decompiles it (manifest, resources,
smali code, assets, native libs — apktool-style), lets you browse and edit
every file it produced, and downloads the result back to you as a ZIP.

## How it works

1. **Pick an APK** from device storage (Storage Access Framework).
2. The app copies it locally and runs it through
   [apktool-lib](https://github.com/iBotPeaches/Apktool) (`org.apktool:apktool-lib:3.0.2`,
   the same decoding engine behind the `apktool` CLI) with full decoding enabled:
   - `AndroidManifest.xml` → readable XML (binary AXML decoded)
   - `resources.arsc` → `res/` resource files
   - every `classesN.dex` → smali source under `smali*/`
   - `assets/`, `lib/`, and any other raw files copied as-is
3. **Browse** the decoded output as a file tree; tap any text-like file
   (smali, XML, yml, ...) to open it in a full-screen editor and save changes
   directly to disk.
4. **Download**: tap the download icon, pick a save location, and the app
   zips the entire decoded (and edited) directory tree and writes it there.

This intentionally stops at "zip of the decoded files" rather than trying to
recompile smali back to dex / resources back to a signed, installable APK —
that's a separate, much riskier feature (recompilation + signing) that wasn't
part of the request.

## Project layout

- `core/` — plain Kotlin/JVM module with no Android dependency: wraps
  apktool-lib for decoding, zips a directory back up, and classifies files as
  text/binary. Has a real JUnit test suite (`./gradlew :core:test`), including
  a hand-built minimal binary AndroidManifest.xml fixture so the tests
  exercise the actual apktool-lib manifest decoder end-to-end, not a mock.
- `app/` — the Android app (Jetpack Compose, Material 3): pick APK → decode →
  browse → edit → export.

## Building

```
./gradlew :app:assembleDebug
```

Requires an Android SDK with `platform-35` and `build-tools;35.0.0` (point
`local.properties` / `ANDROID_HOME` at it, or just open the project in a
recent Android Studio and let it sync).

## What was verified in this dev environment vs. what needs a real device

This project was built in a sandboxed environment with no Android
emulator/device attached, so UI behavior could not be visually exercised.
What *was* verified here, for real:

- `core` module: compiles and its full JUnit suite passes (`./gradlew
  :core:test`), including a test that runs a hand-crafted binary
  AndroidManifest.xml through the real apktool-lib decoder and asserts on
  the readable output — this proves the Gradle dependency wiring (Maven
  Central + JitPack, needed for apktool-lib's smali/baksmali dependencies)
  and the decode pipeline genuinely work, not just that they compile.
- `app` module: a minimal Android SDK (platform 35, build-tools 35) was
  installed in this environment specifically to verify the app; `./gradlew
  :app:assembleDebug` succeeds end-to-end, producing a real installable
  debug APK (dexing/desugaring of apktool-lib's dependency tree — guava,
  commons-io/text, smali, baksmali, dexlib2 — all succeeded).

What's **not** verified: actually running the app on a device/emulator
(picking a real-world APK, browsing/editing files, exporting a ZIP through
the system save dialog). Please try that flow on a device — this is
sizeable, newly-written code and a first real run may turn up rough edges,
especially with the SAF file pickers and edge cases in the decompiled output
(unusual APKs, very large dex files, non-UTF8 resource strings, etc.).

One known non-issue: `./gradlew :app:lintDebug` currently crashes with an
`IncompatibleClassChangeError` inside AGP's *own* bundled Compose/lifecycle
lint checks (a Kotlin Analysis API version mismatch in the lint tooling
itself) — unrelated to this project's code, and doesn't affect building or
running the app.

## Notes

- minSdk 24, target/compileSdk 35, Kotlin 2.1.20, AGP 8.7.3.
- Core library desugaring is enabled since apktool-lib uses some
  `java.nio.file` APIs.
- No network permissions, no analytics — everything happens on-device.
