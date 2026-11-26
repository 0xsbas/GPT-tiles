# Classical Tiles

A small, offline Android rhythm game inspired by four-lane falling-tile games. Tap each tile as it reaches the line. The game scores timing, tracks streaks, and ends after three missed notes.

## Music and rights

The library contains simplified gameplay arrangements of 15 public-domain classical compositions. Every sound in the app is synthesized locally from generated piano-like wave samples; there are **no bundled commercial recordings, streaming tracks, or third-party audio libraries**. The note charts are short, simplified adaptations for gameplay, not complete critical editions or exact recordings.

The compositions included are:

1. Prelude in C Major, BWV 846 — J. S. Bach
2. Minuet in G Major, BWV Anh. 114 — Christian Petzold (traditionally attributed to Bach)
3. Jesu, Joy of Man's Desiring — J. S. Bach
4. Air (Orchestral Suite No. 3) — J. S. Bach
5. Rondo Alla Turca (Turkish March) — W. A. Mozart
6. Eine kleine Nachtmusik — W. A. Mozart
7. Piano Sonata No. 16 in C, K. 545 — W. A. Mozart
8. Lacrimosa (Requiem in D minor) — W. A. Mozart
9. Für Elise, WoO 59 — Ludwig van Beethoven
10. Ode to Joy (Symphony No. 9) — Ludwig van Beethoven
11. Moonlight Sonata, I. Adagio sostenuto — Ludwig van Beethoven
12. Symphony No. 5, opening motif — Ludwig van Beethoven
13. Prelude in E Minor, Op. 28 No. 4 — Frédéric Chopin
14. Canon in D — Johann Pachelbel
15. Spring, The Four Seasons, I. Allegro — Antonio Vivaldi

Public-domain status applies to the underlying compositions. This project deliberately avoids using modern recordings or third-party sheet-music editions. The note patterns and synthesized samples included here were created for this game.

## Build the APK with GitHub Actions

You do **not** need Android Studio. The repository includes a GitHub Actions workflow that builds a debug APK in the cloud and uploads it as an artifact.

1. Download and extract the `classical-tiles-project.zip` archive.
2. Create a new, empty repository on GitHub.
3. In a terminal opened inside the extracted `classical-tiles` folder, run the following commands (replace the URL with your repository URL):

   ```bash
   git init
   git add .
   git commit -m "Add Classical Tiles Android game"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
   git push -u origin main
   ```

4. On GitHub, open the repository's **Actions** tab. Select **Build Classical Tiles APK**. It will start automatically after the first push; you can also choose **Run workflow** to build it manually later.
5. Open the completed workflow run. At the bottom, under **Artifacts**, download **classical-tiles-debug-apk**.
6. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Transfer that APK to your Android phone and install it. Android may ask you to allow installation from the Files app or browser you used.

The APK is debug-signed by Gradle and is suitable for testing on your own phone. A fresh GitHub-hosted runner creates a fresh debug signing key for each build, so uninstall the previous test build before installing a newly built APK if Android reports a signature conflict.

## Game controls

- Tap one of the four lanes when a falling tile reaches the horizontal target line.
- Accurate taps earn more points; consecutive hits increase your streak bonus.
- Three missed tiles end the round.
- Tap the pause icon to pause or return to the song library.
- The result screen offers replay, next song, and song library.

## Technical details

- Native Android Activity + custom Canvas rendering; no web view or app framework dependency.
- Minimum Android version: Android 6.0 (API 23).
- Target SDK: Android 14 (API 34); compile SDK: Android 15 (API 35).
- Java 17, Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Gradle 8.9.
- No network permission is requested. Gameplay and sound work offline.

## Project layout

```text
.github/workflows/build-apk.yml   GitHub Actions build workflow
app/src/main/java/...             Game, menu, audio, and song charts
app/src/main/res/raw/             Locally synthesized piano note samples
app/src/main/res/values/          App theme
```
