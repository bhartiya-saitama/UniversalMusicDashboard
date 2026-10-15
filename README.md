# Universal Music Dashboard v0.1

Landscape-first Android media controller for Xiaomi Pad 6 (Android 14 / HyperOS 2).

## What works in v0.1
- Discover active local Android MediaSession players after notification-listener access is granted.
- Display metadata, album artwork, progress, play/pause, previous/next, and seek where the player supports them.
- Switch between multiple active local media sessions.
- Immersive landscape UI, screen kept awake while the app is open.

## Not yet included
Remote playback on other devices; Spotify API integration; volume controls; album-based ambient colors; polished animations. These are planned, not implemented in this initial build.

## Build without a computer
1. Create a **private** GitHub repository named `UniversalMusicDashboard` (do not initialize with a README).
2. Upload the **contents** of this project, preserving directories including `.github/workflows/build-apk.yml`. GitHub's web uploader may be awkward with folders; a mobile Git client that supports committing a ZIP's extracted contents is easier.
3. Open the repository's **Actions** tab, choose **Build Android APK**, and select **Run workflow** (or push to `main`).
4. When the build completes, open the workflow run and download `universal-music-dashboard-debug-apk` from **Artifacts**. GitHub delivers an artifact ZIP; extract it to obtain `app-debug.apk`.
5. On the Xiaomi tablet, open the APK and allow installs from that download/file-manager source if prompted. Enable **Notification access** for Universal Music Dashboard when requested in the app.
6. Play music locally in Spotify, Apple Music, or YouTube Music on the Xiaomi tablet and test controls.

## Notes
- The app does **not** read music from another phone or PC in v0.1.
- Android media controls depend on each player's active MediaSession and supported actions.
- This project has not been compiled in the current environment; GitHub Actions is the build validation step.
- Debug builds are for personal testing, not Play Store distribution.
- For later release builds, use a persistent signing key and never commit signing credentials.
