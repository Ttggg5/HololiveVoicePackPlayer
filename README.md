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
- **Clip thumbnails**: clips show their topic's thumbnail, with a small round talent picture on grid tiles. Inside a topic, clips show the talent picture instead.
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
- **Talent pictures**: the app shows the profile picture of every talent whose name appears in a clip's file name. Names come from the [official talent list](https://hololive.hololivepro.com/talents).
  - Full names count anywhere in the file name, in English or 日本語: `兎田ぺこら_おはよう.mp3`, `UsadaPekora 01.mp3`.
  - Given or family names count too: `Pekora`, `ぺこら`, `宝鐘`. Short names such as `Ao` or `そら` only count as separate words.
  - **Several talents per clip**: `ぺこら×マリン.mp3` or `Pekora & Marine.mp3` gets both talents and appears under each talent in the Talents tab. Its talent picture is a split collage: two talents are split diagonally, three show one large and two small pictures, and four or more show a 2×2 grid with "+N". The notification and lock screen show the same collage.
  - If a match is wrong, long-press the clip and choose *Talents…* to pick one or more talents. You can also long-press a topic and choose *Set talents for all clips…*.
  - Talents ⋮ → *Match talents automatically* re-runs matching.
  - The talent list is cached and refreshed weekly.
- **Favorites and search**: star any clip, and search titles, packs and talents.

## Settings
Open Settings from Home → ⋮ → *Settings*, or from the side rail on a tablet. It has:
- **Appearance**: theme (system / light / dark), *Match the playing talent* (on by default: the app's main colour fades to a colour taken from the speaking talent's picture), Material You colours, accent colour and tile size.
- **Playback**:
  - how many seconds the rewind and skip-ahead buttons jump
  - the default speed for new queues
  - pause when headphones disconnect
  - restore the last queue when the app starts
- **Gestures**: turn clip swipes, double-tap to favorite, tab and category swipes, player swipes, and vibration feedback on or off.
- **Library & talents**:
  - sync folders on start, and match talents on import
  - *Sync now*, *Refresh talent list* and *Match all talents*
  - remove synced folders
  - clear the play history

## Player
- Rewind and skip-ahead buttons sit beside previous and next. They jump 10 seconds by default; change this in Settings, or long-press either button.
- Swipe up on the player, or tap the *Up next* bar, to see the queue. Drag down to close the player.
- On tablets and unfolded foldables, a side rail replaces the bottom bar, and the player shows the queue beside the controls.
- The notification and lock-screen player show the topic thumbnail as the background with the talent picture on top (an accent-coloured gradient when the topic has no thumbnail), and the status bar shows the app's own icon.

## Gestures
| Where | Gesture | Action |
|---|---|---|
| Mini player | Swipe left / right | Next / previous clip |
| Mini player | Swipe up | Open Now Playing |
| Now Playing | Drag down | Minimize back to the mini player |
| Now Playing | Swipe up | Show the queue |
| Now Playing | Swipe the artwork left / right | Next / previous clip |
| Clip tile or row | Swipe right across half the item | Play next |
| Clip tile or row | Swipe left across half the item | Favorite / unfavorite (with Undo) |
| Clip tile or row | Double-tap | Favorite |
| Clip tile or row | Long-press | More actions |
| Clip, topic and talent grids | Pinch | Bigger or smaller tiles (remembered) |
| Inside a topic | Swipe left / right | Change category |
| Main tabs | Fling left / right on an empty area | Next / previous tab |

## Importing topics
Tap **Import** on the Topics tab:
- **Import many topics…**: pick the folder that holds all your voice packs. Each sub-folder becomes its own topic, and folders inside those become categories. New sub-folders become topics automatically, and topics whose folder you delete disappear. If you delete a topic in the app, it stays deleted. Audio files directly in the picked folder (not in a sub-folder) are skipped.
- **Import one topic**: pick a single folder. It becomes one topic, and its sub-folders become categories. The files stay where they are, and the app keeps read access so it can sync the folder later.
- **Import ZIP**: pick a `.zip` archive. The audio and images are extracted into app storage, so you can delete the ZIP afterwards. A single wrapping folder inside the ZIP is ignored. Shift_JIS (Japanese Windows) file names are supported.

Supported audio: mp3, m4a, aac, ogg, opus, wav, flac. An image in the folder becomes the pack cover; files named like `cover`/`jacket` are preferred.
Leading track numbers (`01_おはよう.mp3`) are used for ordering and removed from titles. The talent is guessed from names like `Talent - Pack` or `【Talent】Pack`, and you can edit it.

## App icon
The launcher icon is the hololive triangle mark with small headphones in its "play" triangle, on a white background. On Android 13+ with themed icons turned on, it shows the mark's outline in your theme colour.

The mark comes from the official hololive logo, which is a trademark of COVER Corp. It is stored as `app/src/main/res/drawable-nodpi/hololive_mark.png`. Keep builds that include it for personal use, and don't publish them.

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
