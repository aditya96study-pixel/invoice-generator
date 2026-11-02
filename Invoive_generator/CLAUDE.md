# Amitabh Tours & Travels — Invoice App

Android app for a single owner-driver (cab/taxi service in Pune) to create formal PDF invoices for customers, track trips by GPS, and see history, customers and earnings.

## Status

- **2026-10-27:** Requirements gathered, spec written.
- **2026-10-28:** v1.0.0 built and tested on an emulator (invoice flow, all 10 PDF styles incl. long-text overflow, GPS tracking, mark paid, earnings). Installable APK: `release/AmitabhTours-1.0.0.apk`.
- Owner signature (`user-assets/signature.png`, background removed) is bundled; UPI ID `amitraul100@okhdfcbank` / "Amit Raul" is the default. No stamp supplied yet.
- Also tested on emulator: gallery receipt + annexure page, customer signature pad, print dialog, PDF ZIP export. Not yet tested on a real phone (camera capture only testable there).

## Build
- `JAVA_HOME="C:/Program Files/Microsoft/jdk-21.0.13.21-hotspot"` then `./gradlew :app:assembleRelease` (Gradle 9.6, AGP 9.4.1, Kotlin 2.4, compileSdk 37).
- Build output goes to `%LOCALAPPDATA%/AmitabhToursBuild` (kept out of OneDrive, see root `build.gradle.kts`).
- Release signing key: `keystore/` (keep it safe; updates must use the same key).
- Keyboard: screens with text fields use `padding(pad).consumeWindowInsets(pad).imePadding()`. The New Invoice form must NOT add `imePadding()` to its content: its `TotalBar` already rises above the keyboard (adding both blanked the form, fixed 2026-10-28).
- Chart x labels are placed by `xLabelPositions()` in `Charts.kt` (unit-tested, never overlap).
- Debug builds include `DebugSeedReceiver` to fill a year of sample invoices for checking charts: `adb shell am broadcast -n com.amitabhtours.invoice/.DebugSeedReceiver` (test devices only).
- Debug builds include `DebugRenderReceiver` to render every style to PDF: `adb shell am broadcast -n com.amitabhtours.invoice/.DebugRenderReceiver --ez online true`.

## Files to read first

1. `docs/ORIGINAL_PROMPT.md` — the user's request word for word, plus every Q&A answer. This is the source of truth.
2. `docs/BUILD_SPEC.md` — full functional and technical spec (screens, data, fare math, invoice fields, PDF, GPS, earnings, help section, build order).
3. `docs/THEMES.md` — the 10 invoice themes with colours and layouts.
4. `branding/` — proposed logo (SVG). Turn into launcher icon + in-app logo during the build.
5. Sample images in the project root:
   - `Use for data not theme.jpeg` — his current paper bill book. **Use for fields only, not styling.**
   - `Type1.jpeg`, `Type2.jpeg`, `Type3.jpeg` — base designs for themes 1–3 (use slightly different shades).
   - `Uber_style.jpeg` — base design for theme 4 (needs a tracked trip + map).

## Hard rules from the user

- Business name on everything: **Amitabh Tours & Travels**. Correct English, formal, neat. Fix the samples' spelling (e.g. "Costumer" → "Customer", "Kondhawa" → "Kondhwa").
- **No GST number.** Never print "Tax Invoice" or any GST/tax line. Print "GST not applicable" note (see spec).
- Previous values appear as **greyed text** in the form; if untouched, they are used as-is.
- Invoice date and time are taken automatically.
- Uber-style (and other map) themes are **greyed out and not clickable** unless the trip was GPS-tracked.
- Fare total is calculated automatically. A charge line prints **only if it has a value**.
- UPI QR, customer on-screen signature, and owner signature/stamp are **optional features** (toggle in Settings).
- Earnings screen must say clearly that it only covers work recorded in the app.
- App must include a detailed **How to use** section.

## Build environment notes (checked 2026-10-27)

- Android SDK present at `%LOCALAPPDATA%\Android\Sdk` (platforms 35, 36, 37.0; build-tools 35.0.1, 36.0.0). `adb` is not on PATH (use `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`).
- **Only Java 8 JRE is installed. Install JDK 17 or 21 before building** (e.g. `winget install Microsoft.OpenJDK.21`) and set `JAVA_HOME`. Android Studio is not installed; build with the Gradle wrapper from the command line.
- Flutter is installed but **not** used for this project.
