<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/6fd3f50a-dcd1-4147-b2b5-f450f7a3a299

## APK downloaden op Android

[<img alt="Download de Android APK" src="https://img.shields.io/badge/Download-Android%20APK-3DDC84?style=for-the-badge&logo=android&logoColor=white">](https://github.com/Ice1984m/Kalli-app/releases/latest/download/app-debug.apk)

Klik op de knop op je Android-apparaat om de nieuwste APK meteen te downloaden. Open daarna de downloadmelding en bevestig de installatie. Android kan eenmalig toestemming vragen om apps uit deze browser of bestandsbeheerder te installeren.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
