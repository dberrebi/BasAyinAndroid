# Bas Ayin · בת עין

An offline Android study app for the Hebrew *Bas Ayin* of Rabbi Avraham Dov of Ovruch. The app uses the centered navigation and reading layout of [Ohr HaChaim for Android](https://github.com/dberrebi/OhrHachaimAndroid-) and the blue-and-gold identity and book data of [Bas Ayin for iOS](https://github.com/dberrebi/BasAyiniOS).

- Browse five books in edition order, a separate list of festivals and special Shabbatot, and the Likkutim and responsa.
- Open the coming Shabbat's reading, including joined portions and relevant festivals, for Israel or the Diaspora.
- Search the complete Hebrew text with or without nikud and quotation marks. Open a result at its exact paragraph.
- Jump to numbered paragraphs, save favourites, continue reading, or copy and share a passage with its source.
- Choose Hebrew or English menus, system/light/dark appearance, and reader text size. The book is in Hebrew.

## Build and run

Use JDK 17 or 21 and Android SDK Platform 36 with Build Tools 36.0.0. Open the repository in Android Studio or run these commands from its root with `ANDROID_HOME` pointing to the SDK.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android 8.0 (API 26) or newer device. With an emulator or device connected, run the tests for the bundled edition, SQLite search, and regional calendars.

```sh
./gradlew :app:connectedDebugAndroidTest
```

The application ID is `app.basayin.android`. The app stores reading progress, favourites, and preferences on-device. It has no internet permission, analytics, or account.

The [release guide](play/RELEASING.md) covers signing, Play Console, and the optional GitHub testing workflow. [Hebrew and English listing copy](play/store-listing.md) and the [privacy policy](docs/privacy/index.html) are included.

## Shared data and attribution

`data/bas_ayin.sqlite` and `data/manifest.json` are copied from the companion iOS checkout at `3074d50c8da640d99034ba9424d4a1f8c969c9dd`. The database includes 74 sections, 2,805 paragraphs, 188 verse citations, and a 2025–2045 reading calendar. To refresh it, use the iOS repository's documented importer and bring over the database and manifest together. The build and app both verify the database checksum. Stable section and paragraph IDs let saved places survive compatible data updates.

- Text adapted for navigation and formatting from *Bat Ayin*, `Jerusalem, 2001`, via Sefaria and the National Library of Israel. This edition is **noncommercial only**. Keep distributions using it free of ads and purchases. [Sefaria](https://www.sefaria.org/Bat_Ayin) · [National Library of Israel](https://www.nli.org.il/he/books/NNL_ALEPH002047217/NLI) · [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)
- Torah reading and holiday schedule from Hebcal.com, adapted to the sections of this edition. [Hebcal](https://www.hebcal.com) · [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- Noto Serif Hebrew, © 2022 The Noto Project Authors. [SIL Open Font License 1.1](app/src/main/assets/licenses/noto-serif-hebrew.txt)
- The app mark is shared with the Bas Ayin iOS project.
