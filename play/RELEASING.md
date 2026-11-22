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
- Host [the privacy policy](../docs/privacy/index.html) publicly over HTTPS. One option is GitHub Pages from `main` → `/docs` after the owner puts the source in `dberrebi/BasAyinAndroid`. Verify `https://dberrebi.github.io/BasAyinAndroid/privacy/` from a signed-out session before using it. The page can also be hosted on the owner's existing public website.
- Use the owner's approved support email. For the current build, the app works without accounts or special access, has no internet permission, ads, SDK for analytics, or in-app purchases, and stores favourites, history, and settings locally. External links and sharing run when the user requests them. Verify the final artifact against these facts before answering Play's Data safety and Ads forms.
- Complete Content rating, Target audience, availability countries, any regional trader declaration, and other account-specific forms with the owner as needed. The Jerusalem 2001 Sefaria edition is [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/). Keep the distribution free of ads and purchases and retain its attribution.

## Optional GitHub Actions uploads after initial setup

Use the [manual GitHub workflow](../.github/workflows/play-test-upload.yml) once the app exists in Play Console and its upload key is registered. It targets **internal testing** or the default **closed testing** track (`alpha`), uses a draft release by default, and never targets production. If Play Console uses a different closed track, use the browser unless the workflow is updated with the actual track ID.

Create or select a service account with the Google Play Android Developer API enabled for its Google Cloud project. Invite it in Play Console with **app-level testing release access for Bas Ayin only**. Add these values through GitHub **Settings → Secrets and variables → Actions** for the Bas Ayin repository. Retain access restrictions on the upload key and account.

| Setting | Value |
| --- | --- |
| `BASAYIN_PLAY_SERVICE_ACCOUNT_JSON` secret | The scoped Play service-account JSON key |
| `BASAYIN_UPLOAD_KEYSTORE_BASE64` secret | Base64 of the registered upload keystore |
| `BASAYIN_UPLOAD_PASSWORD` secret | The keystore password |
| `BASAYIN_UPLOAD_KEY_PASSWORD` optional secret | Separate key password if used |
| `BASAYIN_UPLOAD_ALIAS` optional variable | Key alias if it differs from `upload` |

The workflow uses `100 + GITHUB_RUN_NUMBER` as the code. Start it only when that yields a new code above the version already in Play. After an upload, promote the tested version from Play Console when ready and eligible. A service account with only testing access cannot promote it to production.
