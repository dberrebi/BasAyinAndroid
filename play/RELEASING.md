# Bas Ayin Google Play release guide

Use [WORK-PROMPT.md](WORK-PROMPT.md) for the full handoff. The included `Bas-Ayin-1.0-unsigned.aab` is a release build with application ID `app.basayin.android`, version name `1.0`, version code `1`, min SDK 26, and target SDK 36. Google Play needs the **signed release AAB**. The debug APK is for local installation only.

## First Play release

1. In [Play Console](https://play.google.com/console), verify the developer account, find the app if it already exists, and check its package name, highest version code, registered upload certificate, available tracks, and any open drafts or review.
2. For a new record, create **בת עין**, default language Hebrew, **App**, **Free**. Use `app.basayin.android` on the first upload. Let the account owner complete any legal declarations, identity verification, or Google sign-in prompts.
3. Configure [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756). Use the owner's upload key if the Console already has a certificate. For a new app with no upload key, generate and back up an owner-controlled upload key, and use Google's managed app-signing key unless the owner needs a different arrangement.
4. Build the signed release with the procedure below. Upload it to the eligible testing or production track. Use version code `1` only if it has never been uploaded. Otherwise choose a new code above all uploaded codes.
5. Enter the listing copy and graphics from the next section, finish the account's required setup and app-content forms, and submit the eligible track for review. Track review, tester enrolment, production access, and public availability separately. [App testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)

## Signing the upload

Keep the keystore, passwords, and any service-account credential in the owner's secure storage. Build with JDK 17 or 21 and Android SDK Platform 36 / Build Tools 36.0.0. Set `BASAYIN_UPLOAD_KEYSTORE` to the private absolute keystore path and inject `BASAYIN_UPLOAD_PASSWORD` securely. `BASAYIN_UPLOAD_ALIAS` defaults to `upload`. Set `BASAYIN_UPLOAD_KEY_PASSWORD` only if the key has a separate password.

```sh
./gradlew :app:bundleRelease :app:testDebugUnitTest :app:lintRelease -Pbasayin.versionCode=1
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

Verify that `jarsigner` reports a signed, verified bundle and that its signer certificate matches the registered upload key, if there is one. Also validate the AAB with [bundletool](https://github.com/google/bundletool/releases). Change `versionCode` as needed. For a new app only, the owner can generate a PKCS#12 upload key with `keytool -genkeypair -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12 -keystore /private/path/bas-ayin-upload.p12`; let `keytool` prompt for the password and certificate details.

## Listing and app content

- Use [Hebrew and English listing copy](store-listing.md) and the [localized release notes](whatsnew/).
- Icon `play/graphics/icon-512.png`, 512 × 512. Feature graphic `play/graphics/feature-graphic.png`, 1024 × 500. Real phone captures `play/graphics/phone-01-home.png` and `play/graphics/phone-02-reader.png`, 720 × 1280 each. [Google Play asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151)
- Enter the [public Android privacy policy](https://bas-ayin-android-privacy.glad-horse-8503.chatgpt.site/) in Play Console. The same URL is linked from About in both interface languages. The page source is [in the project](../docs/privacy/index.html). If the policy moves, update the link in the app and Play Console and verify the new URL while signed out.
- Use the owner's approved support email. For the current build, the app works without accounts or special access, has no internet permission, ads, SDK for analytics, or in-app purchases, and stores favourites, history, and settings locally. External links and sharing run when the user requests them. Verify the final artifact against these facts before answering Play's Data safety and Ads forms.
- Complete Content rating, Target audience, availability countries, any regional trader declaration, and other account-specific forms with the owner as needed. The Jerusalem 2001 Sefaria edition is [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/). Keep the distribution free of ads and purchases and retain its attribution.

## Automatic Google Play testing uploads from GitHub

After the first signed upload has registered the upload key and Play Console allows completed internal releases, the [GitHub workflow](../.github/workflows/play-test-upload.yml) runs for pushes to `main` that change the app, the bundled data, or the Gradle build. It runs unit tests and release lint, builds and signs the bundle, and rolls it out to **internal testers**. Listing copy, privacy policy, and other documentation changes are handled separately.

To start an upload manually, open **Actions → Upload Bas Ayin to Play testing → Run workflow**. Choose **internal testing** or the default **closed testing** track (`alpha`), and a draft or completed testing release. The manual defaults are internal and draft. Use Play Console if your closed track has a different ID. Production promotion is handled in Play Console.

Create or select a service account with the Google Play Android Developer API enabled for its Google Cloud project. Invite it in Play Console with **app-level testing release access for Bas Ayin only**. Add these values through GitHub **Settings → Secrets and variables → Actions** for the Bas Ayin repository. Retain access restrictions on the upload key and account.

| Setting | Value |
| --- | --- |
| `BASAYIN_PLAY_SERVICE_ACCOUNT_JSON` secret | The scoped Play service-account JSON key |
| `BASAYIN_UPLOAD_KEYSTORE_BASE64` secret | Base64 of the registered upload keystore |
| `BASAYIN_UPLOAD_PASSWORD` secret | The keystore password |
| `BASAYIN_UPLOAD_KEY_PASSWORD` optional secret | Separate key password if used |
| `BASAYIN_UPLOAD_ALIAS` optional variable | Key alias if it differs from `upload` |
| `BASAYIN_PLAY_VERSION_CODE_BASE` optional variable | Version-code offset, `100` by default |

Check the first run under **Actions** and verify the version in Play Console under **Testing → Internal testing**. If a required secret is missing, the workflow stops with setup instructions. If Play requires draft releases while the app is being set up, finish the internal release in Play Console before relying on automatic uploads.

Each workflow run uses the configured version-code offset plus its GitHub run number. If a manual upload has used a higher code, set `BASAYIN_PLAY_VERSION_CODE_BASE` to at least the highest code Play has seen before the next run. Start a new run after a successful upload to get a new code. Promote a tested version from Play Console when ready and eligible.
