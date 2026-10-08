# Songbook 2.5 (Android · Kotlin · Jetpack Compose)

Native Android build of the Songbook mockup: Library, Song page, Song editor,
Albums, Album and Group pages, Workflow, Favorites, New song sheet, Song menu, Settings and a first-run intro.

## New in 2.5
| # | Feature | Where |
|---|---|---|
| 1 | Songs wear the **cover of their album** automatically; a cover picked by hand stays (and "Use album cover" gives it back; album menu ▸ "Use this cover for all songs") | `SongEntity.coverManual`, `withAlbumCover()` |
| 2 | Chords sit **closer to the lyrics**; chord distance goes down to -14 | `ChordLyricLine` (`SongScreen.kt`) |
| 3 | **Layouts per page**: songs (list, compact, grid 2/3/4), albums and groups (1 to 4 columns), songs inside an album, favorites | `SongsLayout`, Settings ▸ Layout |
| 4 | **Sorting** for songs, albums/groups and songs inside an album | `data/Sorting.kt` |
| 5 | **Full screen lyrics**: button in the corner of the lyrics card | `SongScreen.kt` (`expanded`) |
| 6 | **Section title size** (verse, chorus...) | Reading dialog / Settings |
| 7 | **Album sides** (Side A, Side B, Disc 1... or your own); songs are listed per side; song menu ▸ Side | `AlbumEntity.sides`, `SidesDialog.kt` |
| 8 | With a capo, chords **keep their place over the word**; only the names change | `ChordLyricLine` |
| 9 | **Backup with pictures** (.zip, every cover as a file) or library only (.json); restore reads every old backup | `Backup.kt` |
| 10 | New cleaner **icon** | `res/drawable/ic_launcher_*.xml` |
| 11 | **Tap a word** in the lyrics to type the chord played there | `ChordDialog.kt`, `SongParser.withChordsAt` |
| 12 | `[G Am Dm]` in one bracket is now three chords next to each other (and they follow the capo) | `SongParser.parseLyric` |
| 13 | **Drag to arrange** songs, songs in an album, albums, groups | `ArrangeScreen.kt`, `Reorder.kt` |
| 14 | The Albums page names each group section after its type (**Trilogies**, Box sets...) or your own heading | `AlbumsScreen.kt`, Settings ▸ Pages |
| 15 | **Customize pages**: reorder and hide blocks of the main page and the Albums page | `Blocks.kt`, Settings ▸ Pages |
| 16 | Groups get **their own cover** or a collage of their albums | `Artwork(collage = ...)` |
| + | Glass tab bar no longer shows list text through it; Group page header lines up with the Album page | `Surfaces.kt`, `Chrome.kt`, `GroupScreen.kt` |

Database version is now 4 (`MIGRATION_3_4`). Songs that already have their own picture keep it; a song whose picture is exactly its album's picture is switched to follow the album once, on first start.

## New in 2.0
| # | Feature | Where |
|---|---|---|
| 1 | **Star ratings** from 0 to 5 in quarter steps (4.25, 4.5, 4.75), drawn as real stars with partial fill. Songs, albums and groups. Tap or drag on the stars | `components/Rating.kt` |
| 2 | **Accent color RGB sliders** next to the three presets (Settings ▸ Appearance ▸ Accent Color) | `components/Pickers.kt` (`AccentDialog`), `accentColor()` |
| 4 | **Ideas live only in Workflow**: songs in the Idea stage are hidden from Songs, Albums, Favorites and search | `SongbookRoot.kt` (`songs` vs `allSongs`) |
| 5 | **Rename groups** and give songs, albums and groups a **type**: Single, EP, Demo, Trilogy... or anything you write | `TypeDialog`, `typeName` columns, group menu ▸ Rename |
| 6 | **Restore Backup** reads `.json` backups from 1.0, 1.5 and 2.0; add to the library or replace it, optionally with the saved settings | `Repository.restoreBackup`, `Backup.kt` (v3) |
| 7 | **Albums on the main page**: hide, 2, 4, 6 or 8 (Settings ▸ Layout) | `AppSettings.topAlbums` |
| 8 | **Capo shapes**: with a capo set, chords and tab frets are shown as the shapes to play (Bm G D at capo 2 becomes Am F C). Switch in the Capo dialog | `data/Transposer.kt` |
| 9 | **Three looks**: Old (the 1.5 UI), **New** (stories, swipeable song cards, player bar, player-style reader) and **Glass** (translucent panels, floating tab bar and reader bar). Each look has its own layouts for Songs, Albums, Album/Group, Favorites, Reader and menus. Settings ▸ Look ▸ Interface | `ui/theme/Theme.kt`, `components/Surfaces.kt`, `Chrome.kt`, `CoverHeader.kt`, `LibraryScreen.kt` |
| 10 | **New icon**: coral-to-crimson gradient with soft light, shadowed beamed notes, themed (monochrome) version | `res/drawable/ic_launcher_*.xml`, `AppIconArt` |
| 11 | **Songwriting desk** for unfinished songs: spark, tagged lines (hook/verse/chorus/bridge) that can become lyrics, checklist, details with names you choose, next step. Autosaves | `screens/DeskScreen.kt`, `data/IdeaData.kt` |

Database version is now 3 (`MIGRATION_2_3`). Songs from 1.5 keep their chords as typed (capo shapes are off for them until you switch them on in the Capo dialog).

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
