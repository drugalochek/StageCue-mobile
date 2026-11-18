# StageCue Mobile

Android audio player for stage sound cues.

## Features
- Import audio files through Android's document picker (no broad storage permission).
- Local track library with saved metadata.
- Rename tracks and assign cue numbers 1–40.
- Play, pause, previous and next controls.
- Graphite Blue interface.

## Build APK on your phone
1. Open the **Actions** tab in this repository.
2. Select **Build StageCue APK**.
3. Tap **Run workflow** on the `main` branch.
4. Wait for the workflow to finish successfully.
5. Open the completed run and download the `StageCue-debug-apk` artifact.
6. Extract the ZIP and install `app-debug.apk` on your Android device. Android may ask you to allow installation from the browser or file manager you used.

The first build can take several minutes while Gradle downloads dependencies.
