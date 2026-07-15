<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/6fd3f50a-dcd1-4147-b2b5-f450f7a3a299

## Installation options

### 1. Run with Android Studio

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio.
2. Select **Open** and choose the directory containing this project.
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set one or both API keys (see `.env.example`):
   - `GEMINI_API_KEY` enables Gemini 2.5 Pro.
   - `OPENAI_API_KEY` enables GPT chat.
   The app lets you switch between configured providers in the AI Copilot tab.
5. Run the app on an emulator or physical device.

### 2. Build the APK locally

Run `./gradlew assembleDebug`. The APK is created at
`app/build/outputs/apk/debug/app-debug.apk`.

### 3. Install directly on Android from GitHub

[Download and install the latest APK](https://github.com/Ice1984m/Kalli-app/releases/download/apk-latest/app-debug.apk)

Open this link on an Android device to start the download. When it finishes, open the
download and confirm Android's installation prompt. You may need to allow your browser
or file manager to install unknown apps.
