# Holo Voice — Hololive voice pack player for Android

An Android app for listening to the Hololive voice packs you own (for example, ones you bought from the official shop).
The app ships no audio of its own. You import your packs from your phone.

## Features
- **Home**: a big hero card at the top shows what's playing over its blurred topic thumbnail (with play/pause and progress; tap for Now Playing), or *Talent of the day* (a different talent each day) when nothing is loaded. Quick chips below it: *Shuffle all*, *Favorites*, *New for you*, *Most played* and *Alarms*. Then shelves: your talents, recently played, new topics, *Not played yet*, *Most played* and favorites; clip cards show the topic thumbnail with a small talent badge. A short intro explains the basics on first launch (⋮ → *How it works* shows it again).
- **Talents tab**: every hololive member with their Japanese and English names, under a big *Talents* headline with counts and a search bar. A *Now speaking* card shows who is talking in the current clip. Talents you have clips for are full photo cards with the names and clip count on a dark gradient (in the sort you choose; the speaking talent gets a ring); the rest of the official roster follows as small dimmed round pictures. Status tags in names (like "[Alum]") are shown as a small badge instead of in the name. Search works in English or 日本語. Tap one to open their page: a banner with their picture, both names, any status tags, clip / topic / play counts, *Play all* and *Shuffle*, and topic filters above all their clips.
- **Sorting everywhere**:
  - clips by track order, name, talent, duration, recently added or most played
  - topics by name, talent, recently added or most clips
  - playlists by name, recently created or size
  - talents by name, most clips or most played
  - Your choices are remembered.
- **Grid or list**: switch clip collections between big picture tiles and a compact list.
- **Search and filters**: a filter box on every clip list, plus *Favorites*, *Talent* and *Topic* filter chips in Search. Before you type, Search shows your recent searches, your talents and topic cards to browse; while searching, results are grouped into matching talents (English or 日本語), topics and clips. The keyboard search key saves a query to recent searches.
- **Undo**: removing from favorites, the queue or a playlist, or deleting a playlist, shows an *Undo* button.
- **Clip thumbnails**: clips show their topic's thumbnail, with a small round talent picture on grid tiles. Inside a topic, clips show the talent picture instead. Up next and playlists show the talent's round picture with a small topic thumbnail on its corner, and list view adds a small talent badge to topic thumbnails.
- **Topics**: each imported folder or ZIP is one topic, and its sub-folders become categories. The Topics tab shows every topic as a cover card with its name, talent pictures and clip count, a search bar, and a *Playing from* card for the topic you're listening to; topics are sorted by name by default, and sorting by talent groups the cards under each talent. Opening a topic shows a banner with its cover, its talents (tap one to open their page), clip / category / length / play counts, *Play all* and *Shuffle*, and category chips to filter the clips. Long-press a topic to rename it, change its talent, or delete it.
- **Auto-sync**: imported folders are rescanned when you open the app, so files you add or delete show up without importing again. Tap ⟳ in Topics, or pull down on Home, Topics or Talents, to re-scan right away. Picking a folder that is already in the library updates it instead of creating a duplicate.
- **Soundboard**: each pack is a grid of clips. Tap a clip to play the pack from that clip onward, and tap the playing clip again to pause. Long-press a clip for *Play next*, *Add to queue*, *Add to playlist* and *Favorite*.
- **Now Playing**: the mini player and Now Playing show who is speaking (talent picture and name) and the topic › category. Tap the topic chip to open that topic. Now Playing has large cover art, a seek bar, previous/next, shuffle, repeat (all/one), favorite, and add to playlist.
- **Queue (Up next)**: *Shuffle*, *Repeat* and *Clear* chips on top, then the clips with how many are left; clips already played are faded. Inside Now Playing the rows sit right on the blurred backdrop; the separate Queue page also shows a now-playing card (over its blurred topic thumbnail, with play/pause). Jump to any clip, drag to reorder, swipe left to remove (with Undo), or clear the queue.
- **Playlists**: create your own playlists that mix clips from any pack. The Playlists tab shows each one as a card with a mosaic of its topics' thumbnails, its clip count and length, and a play button (long-press for shuffle, rename or delete); a pinned Favorites card sits first. A playlist's page has a blurred banner, its mosaic, name (tap to rename), clips / length / talents, *Play all* and *Shuffle*; drag to reorder and swipe to remove clips.
- **Playback speed**: 0.5× to 2×.
- **Sleep timer**: pause after 5 to 90 minutes or at the end of the current clip. It keeps working when the app is closed.
- **Resume**: the queue, position, speed, shuffle and repeat are restored after the app is closed. On Android 13+ the system media controls can resume playback.
- **Background play**: playback continues with the screen off, and you can control it from the notification, lock screen, or headset.
- **Talent pictures**: the app shows the profile picture of every talent whose name appears in a clip's file name. Names come from the [official talent list](https://hololive.hololivepro.com/talents). Every talent's picture is downloaded once, on first start, and stored in the app (a few MB), so pictures also show offline. Changed or new pictures are picked up when the talent list is refreshed.
  - Full names count anywhere in the file name, in English or 日本語: `兎田ぺこら_おはよう.mp3`, `UsadaPekora 01.mp3`.
  - Given or family names count too: `Pekora`, `ぺこら`, `宝鐘`. Short names such as `Ao` or `そら` only count as separate words.
  - **Several talents per clip**: `ぺこら×マリン.mp3` or `Pekora & Marine.mp3` gets both talents and appears under each talent in the Talents tab. Its talent picture is a split collage: two talents are split diagonally, three show one large and two small pictures, and four or more show a 2×2 grid with "+N". The notification and lock screen show the same collage.
  - If a match is wrong, long-press the clip and choose *Talents…* to pick one or more talents. You can also long-press a topic and choose *Set talents for all clips…*.
  - Talents ⋮ → *Match talents automatically* re-runs matching.
  - The talent list is cached and refreshed weekly.
- **Favorites and search**: star any clip, and search titles, packs and talents. The Favorites page has a star banner with clip / talent / topic / length counts, *Play all* and *Shuffle*, and talent chips to show only one talent's favorites.

## Settings
Open Settings from Home → ⋮ → *Settings*, or from the side rail on a tablet. A summary at the top shows the app version and how many topics, clips and talents you have; the options below are grouped in rounded cards, each with a coloured icon:
- **Appearance**: theme (system / light / dark), *Match the playing talent* (on by default: the app's main colour fades to a colour taken from the speaking talent's picture), Material You colours, accent colour and tile size.
- **Playback**:
  - how many seconds the rewind and skip-ahead buttons jump
  - the default speed for new queues
  - pause when headphones disconnect
  - restore the last queue when the app starts
- **Gestures**: turn clip swipes, double-tap to favorite, tab swipes, player swipes, and vibration feedback on or off.
- **Library & talents**:
  - sync folders on start, and match talents on import
  - *Sync now*, *Refresh talent list*, *Match all talents* and *Redownload talent pictures*
- **Synced folders**: every imported folder, with a button to remove it.
- **Data**: clear the play history.

## Widget and shortcuts

- **Home-screen widget**: long-press your home screen → Widgets → Holo Voice. It shows the playing clip's talent picture, title and who is speaking over the topic's thumbnail, with previous, play/pause and next. The buttons work even when the app is closed (the last queue continues), and tapping the widget opens Now Playing. It's resizable (4×2 by default).
- **App shortcuts**: long-press the app icon for *Continue*, *Shuffle all* and *Favorites*, plus *Shuffle* for your two most played talents. Drag one to the home screen to pin it.

## Voice alarms

Wake up to a talent's voice. Open **Alarms** from the ⏰ icon on Home (or Settings › Playback › Voice alarms), or long-press a clip and choose *Set as alarm…*. The page opens with a big *Next alarm* card (time, how long until it rings, and what it plays, over that sound's blurred picture); each alarm is a card with its time, label, highlighted weekdays, a sound pill and an on/off switch.

- Set the time, the days it repeats (or once), a label and the snooze length (5, 10 or 15 minutes).
- Choose what it plays:
  - **Clip**: always the same clip.
  - **Talent**, **Topic** or **Favorites**: a different random clip each time.
- **Preview** plays the sound in the normal player.
- The alarm rings at alarm volume, so it also rings with the phone on silent. The clip repeats, starting quiet and getting louder over 20 seconds.
- A full-screen alarm shows the talent's picture over the lock screen, with **Snooze** and **Stop**. Both are also in the notification.
- An alarm that isn't stopped gives up after 10 minutes and counts as snoozed.
- If the clip can't be played (for example, the file was moved), the system alarm sound plays instead.
- Alarms survive restarts and time-zone changes.
- If Android blocks notifications, exact alarms or full-screen alarms, the Alarms screen shows a red row that opens the right setting.

## Player
- Rewind and skip-ahead buttons sit beside previous and next. They jump 10 seconds by default; change this in Settings, or long-press either button.
- **A–B loop**: tap *A–B* to mark the start, tap again to mark the end, and that part repeats (also with the screen off). Tap *Loop* to stop; it also ends when the next clip starts.
- The player opens over the page you're on. Drag it down (you see that page underneath) or press back to close it. Tapping the media notification opens it.
- Swipe up on the player, or tap the *Up next* bar, to see the queue; it rises with your finger, and you pull it down (by its header, or from the top of the list) to close it. Drag down to close the player.
- On tablets and unfolded foldables, a side rail replaces the bottom bar, and the player shows the queue beside the controls.
- The bottom bar stays on every page; tap the highlighted tab again to jump back to its top page. On Android 14+, the back swipe shows the page you're going back to as you swipe.
- The notification and lock-screen player show the topic thumbnail as the background with the talent picture on top (an accent-coloured gradient when the topic has no thumbnail), and the status bar shows the app's own icon.

## Gestures
| Where | Gesture | Action |
|---|---|---|
| Mini player | Swipe left / right | Next / previous clip |
| Mini player | Swipe up | Open Now Playing (it rises with your finger) |
| Now Playing | Drag down | Minimize back to the mini player |
| Now Playing | Swipe up | Show the queue (it rises with your finger; pull it down to close) |
| Now Playing | Swipe the artwork left / right | Next / previous clip |
| Clip tile or row | Swipe right across half the item | Play next |
| Clip tile or row | Swipe left across half the item | Favorite / unfavorite (with Undo) |
| Clip tile or row | Double-tap | Favorite |
| Clip tile or row | Long-press | More actions |
| Clip, topic and talent grids | Pinch | Bigger or smaller tiles (remembered) |
| Inside a topic | Swipe left / right | Change category |
| Main tabs | Swipe left / right | Next / previous tab (the next tab slides in with your finger) |

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

## Tests
JVM unit tests live in `app/src/test` and run on every push in CI. Run them with:

```
./gradlew testDebugUnitTest
```

They cover:
- file-name parsing and talent matching (English, 日本語, typos)
- status tags in names
- sorting
- alarm times and labels
- clip pictures and "who is speaking"
- Home's smart lists
- recent searches
- per-topic talent pictures
- duration formatting

## Tech
Kotlin, Jetpack Compose (Material 3), Room, Media3 ExoPlayer + MediaSessionService, Storage Access Framework, and Coil. The minimum Android version is 8.0 (API 26).
