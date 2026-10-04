# Songbook (Android · Kotlin · Jetpack Compose)

Native Android build of the Songbook mockup: Library, Song page (dark reader), Song editor,
Albums, Album page, Favorites, New song sheet, Song menu and Settings.

## Open it
1. Install **Android Studio** (current stable version).
2. `File ▸ Open…` and pick this `Songbook` folder. Accept the Gradle sync / "Trust project" prompts.
   If Android Studio offers to update the Android Gradle Plugin or Kotlin, you can say yes.
3. Pick a phone emulator (or plug in a phone with USB debugging) and press **Run ▶**.

Requires Android 8.0 (API 26) or newer.

## What is in it
| Mockup screen | Code |
|---|---|
| 1 / 1b Library (raised or simple nav) | `ui/screens/LibraryScreen.kt`, `ui/components/Common.kt` (`SbBottomBar`, `SbFab`) |
| 2 Song page (dark) | `ui/screens/SongScreen.kt` |
| 3 Song editor | `ui/screens/EditorScreen.kt` |
| 4 Albums / 5 Album page | `ui/screens/AlbumsScreen.kt`, `AlbumScreen.kt` |
| 6 Favorites | `ui/screens/FavoritesScreen.kt` |
| 7 New song | `ui/screens/NewSongSheet.kt` |
| 8 Song menu | `ui/components/Dialogs.kt` (`SongMenuSheet`) |
| 9 Settings | `ui/screens/SettingsScreen.kt` |

* Colors, radii, spacing and gradients are copied from the mockup's HTML (`ui/theme/Theme.kt`).
* Data: Room database (`data/`), settings in DataStore (`data/SettingsStore.kt`).
* Navigation and the sheets/dialogs live in `ui/SongbookRoot.kt`.

## How songs are written
Plain text with three simple conventions:

```
# Verse 1                      <- section heading
[Em]I woke up under[D]neath    <- chords in [brackets], placed right before the letter they sit above
e|--0--2--3--|                 <- a line like this is treated as tab
```
The reader draws chords above the exact character they precede.

## Notes
* First launch adds the mockup's sample data (four albums and "I Don't Exist"). To skip it, delete the
  `seedIfNeeded()` call in `SongbookApp.kt`.
* "Hello John" comes from `AppSettings.userName` in `data/SettingsStore.kt` (default `John`).
* Poppins is bundled in `res/font` (Regular, Medium, Bold). The mockup's SemiBold uses the Bold file; add
  `poppins_semibold.ttf` and point `FontWeight.SemiBold` at it in `Theme.kt` for an exact match.
  The chord font is the system monospace instead of DM Mono.
* Backup / Export / Import in Settings use the Android file picker (JSON backup, plain-text export).
