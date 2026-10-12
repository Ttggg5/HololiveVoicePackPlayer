# Holo Voice — Hololive voice pack player for Android

An Android app for listening to the Hololive voice packs you own (for example, ones you bought from the official shop).
The app ships no audio of its own. You import your packs from your phone.

## Features
- **Home**: Continue listening, recently played, your talents, recently added topics and favorites, plus a *Shuffle all* button. A short intro explains the basics on first launch (⋮ → *How it works* shows it again).
- **Talents tab**: every talent in your library with their picture and clip count. Tap one to see all their clips across topics, filter by topic, then use *Play all* or *Shuffle*.
- **Sorting everywhere**:
  - clips by track order, name, talent, duration, recently added or most played
  - topics by name, talent, recently added or most clips
  - playlists by name, recently created or size
  - talents by name, most clips or most played
  - Your choices are remembered.
- **Grid or list**: switch clip collections between big picture tiles and a compact list.
- **Search and filters**: a filter box on every clip list, plus *Favorites*, *Talent* and *Topic* filter chips in Search.
- **Undo**: removing from favorites, the queue or a playlist, or deleting a playlist, shows an *Undo* button.
- **Topics**: each imported folder or ZIP is one topic, and its sub-folders become categories, shown as tabs on the topic screen. Long-press a topic to rename it, change its talent, or delete it.
- **Auto-sync**: imported folders are rescanned when you open the app, so files you add or delete show up without importing again. Picking a folder that is already in the library updates it instead of creating a duplicate.
- **Soundboard**: each pack is a grid of clips. Tap a clip to play the pack from that clip onward, and tap the playing clip again to pause. Long-press a clip for *Play next*, *Add to queue*, *Add to playlist* and *Favorite*.
- **Now Playing**: the mini player and Now Playing show who is speaking (talent picture and name) and the topic › category. Tap the topic chip to open that topic. Now Playing has large cover art, a seek bar, previous/next, shuffle, repeat (all/one), favorite, and add to playlist.
- **Queue (Up next)**: jump to any clip, drag to reorder, swipe left to remove, or clear the queue.
- **Playlists**: create your own playlists that mix clips from any pack. You can reorder and remove clips, and rename or delete playlists.
- **Playback speed**: 0.5× to 2×.
- **Sleep timer**: pause after 5 to 90 minutes or at the end of the current clip. It keeps working when the app is closed.
- **Resume**: the queue, position, speed, shuffle and repeat are restored after the app is closed. On Android 13+ the system media controls can resume playback.
- **Background play**: playback continues with the screen off, and you can control it from the notification, lock screen, or headset.
- **Talent pictures**: the app matches each clip to a talent from the [official talent list](https://hololive.hololivepro.com/talents) and shows that talent's profile picture. The picture appears on the clip tile, in the queue and playlists, on Now Playing, and in the notification.
  - Name each file after the talent, in Japanese or English (`兎田ぺこら.mp3`, `Usada Pekora.mp3`, `Pekora (2).mp3`). The closest name wins, and matching tolerates full-width characters, katakana/hiragana differences, first names only, and small typos.
  - If a match is wrong, long-press the clip and choose *Change talent…*, or long-press a pack and choose *Set talent picture for all clips…*.
  - Library ⋮ menu → *Match talents automatically* re-runs matching. *Refresh talent list* downloads the list again.
  - The list is cached and refreshed weekly, so an internet connection is only needed the first time.
- **Favorites and search**: star any clip, and search titles, packs and talents.

## Importing topics
Tap **Import** on the Topics tab:
- **Import folder**: pick a folder on your device. It becomes one topic, and each sub-folder becomes a category. The files stay where they are, and the app keeps read access so it can sync the folder later.
- **Import ZIP**: pick a `.zip` archive. The audio and images are extracted into app storage, so you can delete the ZIP afterwards. A single wrapping folder inside the ZIP is ignored. Shift_JIS (Japanese Windows) file names are supported.

Supported audio: mp3, m4a, aac, ogg, opus, wav, flac. An image in the folder becomes the pack cover; files named like `cover`/`jacket` are preferred.
Leading track numbers (`01_おはよう.mp3`) are used for ordering and removed from titles. The talent is guessed from names like `Talent - Pack` or `【Talent】Pack`, and you can edit it.

## Building
Requirements: JDK 17+ and the Android SDK (API 35).

```bash
./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit tests
```

Every push also builds a debug APK on GitHub Actions (download it from the run's **Artifacts**).
To install it, copy it to your phone and open it with "install unknown apps" allowed, or run `adb install app-debug.apk`.

## Tech
Kotlin, Jetpack Compose (Material 3), Room, Media3 ExoPlayer + MediaSessionService, Storage Access Framework, and Coil. The minimum Android version is 8.0 (API 26).
