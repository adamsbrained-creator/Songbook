# Songbook 1.5 (Android · Kotlin · Jetpack Compose)

Native Android build of the Songbook mockup: Library, Song page, Song editor,
Albums, Album and Group pages, Workflow, Favorites, New song sheet, Song menu, Settings and a first-run intro.

## New in 1.5
| # | Feature | Where |
|---|---|---|
| 1 | Song page has a light theme: follows the app, or force Dark / Light (Aa dialog or Settings) | `SongScreen.kt`, `AppSettings.songTheme` |
| 2 | Image upload for songs, albums and groups (system photo picker, stored in `filesDir/covers`, included in Backup) | `data/ImageStore.kt`, `ui/components/Images.kt` |
| 3 | Song page has two pages: **Chords** and plain **Lyrics** (no chords/tabs, long lines wrap) | `SongScreen.kt` (`PlainLyrics`) |
| 4 | Section names: `VERSE` / `verse` / as typed (Settings ▸ Reading ▸ Section Names) | `SectionStyle`, `SectionHeading` |
| 5 | Auto Scroll switch on the song page; when on, the label opens the speed card (1-10) | `SongContent`, `ScrollSpeedDialog` |
| 6 | Grid: 2 or 3 columns for Albums / Groups, List or Grid for songs, dividers between rows | `Tiles.kt`, `LibraryScreen.kt`, Settings ▸ Layout |
| 7 | Badges on songs, albums and groups: Finished / In progress / Unfinished or your own text and color | `ui/components/Badges.kt` |
| 8 | Groups: Albums ▸ Select ▸ pick albums ▸ Group (2 = Duology, 3 = Trilogy, 4+ = Collection) | `GroupScreen.kt`, `GroupEntity` |
| 9 | Intro with a name field; the name shows on the main page. Replay it from Settings ▸ About | `OnboardingScreen.kt` |
| 10 | Greeting variants (Hello / Hi / Hey / Welcome back / time of day / custom) and Chord Distance (-6 to +16 dp) | Settings, `greetingLine()`, `ReadingDialog` |
| 11 | Separation between songs (thin dividers; cards in grid mode) | `SongRow(divider = ...)` |
| 12 | Workflow page: stages Idea ▸ Lyrics ▸ Chords ▸ Arrangement ▸ Finished, next-step note, one-tap advance | `WorkflowScreen.kt`, `Stage` |
| 13 | Hide the Edit button on the song page (Settings ▸ Song Display ▸ Edit Button) | `SongContent` |
| + | App icon (adaptive + Android 13 themed) and in-app copy of it | `res/drawable/ic_launcher_*.xml`, `AppIconArt` |

Database version is now 2. `MIGRATION_1_2` keeps all existing songs and albums; existing songs count as "Finished".
New songs start in the **Lyrics** stage (change it in the New Song sheet, or pick Finished to skip the workflow).

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
| 2 Song page | `ui/screens/SongScreen.kt` |
| 3 Song editor | `ui/screens/EditorScreen.kt` |
| 4 Albums / 5 Album page | `ui/screens/AlbumsScreen.kt`, `AlbumScreen.kt` |
| 6 Favorites | `ui/screens/FavoritesScreen.kt` |
| 7 New song | `ui/screens/NewSongSheet.kt` |
| 8 Song menu | `ui/components/Dialogs.kt` (`SongMenuSheet`) |
| 9 Settings | `ui/screens/SettingsScreen.kt` |
| Workflow / Group / Intro | `WorkflowScreen.kt`, `GroupScreen.kt`, `OnboardingScreen.kt` |

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
* First launch adds the mockup's sample data (four albums and "I Don't Exist", which starts in the Workflow). To skip it, delete the
  `seedIfNeeded()` call in `SongbookApp.kt`.
* The name on the main page comes from the intro / Settings ▸ Profile (`AppSettings.userName`).
* Poppins is bundled in `res/font` (Regular, Medium, Bold). The mockup's SemiBold uses the Bold file; add
  `poppins_semibold.ttf` and point `FontWeight.SemiBold` at it in `Theme.kt` for an exact match.
  The chord font is the system monospace instead of DM Mono.
* Backup / Export / Import in Settings use the Android file picker (JSON backup, plain-text export).
