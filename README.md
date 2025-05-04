# Meme ji

<p align="center">

  <img src="app/src/main/res/mipmap-xhdpi/ic_launcher_round.png?raw=true" width="128" alt="Meme-ji App Icon">
</p>

<p align="center">
  
  <a href="https://f-droid.org/packages/com.elejar.memeji/">
    <img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80">
  </a>

  <a href="https://github.com/ele-jar/Meme-ji/releases/latest">
    <img alt="Get it on GitHub" src="https://img.shields.io/github/v/release/ele-jar/Meme-ji?display_name=tag&logo=github&label=GitHub%20Release" height="80">
  </a>
</p>

**Meme-ji** is a simple, native Android app for browsing and sharing memes sourced from an online JSON database. Explore various categories or search for your favorites!

---

## ✨ Features

*   **Browse Memes:** View memes sourced from an online JSON database (`ele-jar/meme-database`).
*   **Categories:** Explore memes grouped by automatically generated tags.
*   **Search:** Find memes by name or tag content.
*   **Share:** Easily share your favorite memes via other apps.
*   **Download:** Save memes directly to your device's Downloads folder.
*   **Cutie Mode:** An optional setting (in Settings) to filter out potentially sensitive content tagged "18+".
*   **Dark Theme:** Follows system theme by default (currently dark mode).
*   **Material You:** Dynamic color theming on supported devices (Android 12+).

---

## Screenshots

<p align="center">
  
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png?raw=true" width="20%" alt="Screenshot Home"/>  
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png?raw=true" width="20%" alt="Screenshot Categories"/>  
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png?raw=true" width="20%" alt="Screenshot More"/>  
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png?raw=true" width="20%" alt="Screenshot Settings"/>
</p>

---

## 🚀 Download

The recommended way to install is via F-Droid (once available). You can also download the APK directly from GitHub Releases.

*   **[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.elejar.memeji/)** (*Coming soon after F-Droid review*)
*   **[GitHub Releases](https://github.com/ele-jar/Meme-ji/releases/latest)** (Download the `Meme-ji-vX.X.X-release.apk` file)

---

## 🛠️ Building from Source

You can build the app yourself:

1.  **Prerequisites:**
    *   Java Development Kit (JDK) - Version 17 recommended.
    *   Android SDK (latest stable recommended).
2.  **Clone the repository:**
    ```bash
    git clone https://github.com/ele-jar/Meme-ji.git
    cd Meme-ji
    ```
3.  **Build the unsigned release APK:**
    ```bash
    ./gradlew assembleRelease
    ```
    The APK will be located in `app/build/outputs/apk/release/`.
    *(Note: F-Droid builds and signs the app using their own keys).*

---

## 🤝 Contributing

Contributions are welcome! If you find a bug or have a feature request:

1.  Check the [GitHub Issues](https://github.com/ele-jar/Meme-ji/issues) to see if it has already been reported.
2.  If not, please [open a new issue](https://github.com/ele-jar/Meme-ji/issues/new/choose).

---

## 📜 License

This project is licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for details.
