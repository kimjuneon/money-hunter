Money Hunter One Store Android WebView

Purpose
- One Store review build for obtaining a store link used in game rating certification.
- This is not the Apps in Toss production build.
- The WebView loads the existing web game URL with target=onestore.
- Toss SDK, Toss ads, Toss point reward copy, and Toss-only reward flows are not used by this Android shell.

Build commands from repository root
- APK:
  ./gradlew -p android-onestore :app:assembleRelease
- AAB:
  ./gradlew -p android-onestore :app:bundleRelease
- Override game URL:
  ./gradlew -p android-onestore :app:assembleRelease -PonestoreGameUrl=https://YOUR_ONESTORE_REVIEW_URL/

Default package
- com.juneonsoft.moneyhunter

Default app name
- 머니헌터

Notes
- For local emulator testing against a local Spring server:
  ./gradlew -p android-onestore :app:assembleDebug -PonestoreGameUrl=http://10.0.2.2:8080/
- The Android app appends target=onestore automatically if it is missing.
