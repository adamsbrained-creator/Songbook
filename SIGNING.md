# Signing Songbook

Android only installs a new APK over an old one when both are signed with **the same key**.
The workflows sign every build with your release key, so each build updates the app and keeps your songs.

## One-time setup (5 minutes, on github.com)

1. Open your repo ▸ **Settings ▸ Secrets and variables ▸ Actions ▸ New repository secret**.
2. Add these four secrets. The values are in `GITHUB-SECRETS.txt`, which is **not** part of the project:

   | Secret name | Value |
   |-------------|-------|
   | `SIGNING_KEYSTORE_BASE64` | the whole text of `songbook-release.jks.base64.txt` |
   | `SIGNING_STORE_PASSWORD`  | the password from `GITHUB-SECRETS.txt` |
   | `SIGNING_KEY_ALIAS`       | `songbook` |
   | `SIGNING_KEY_PASSWORD`    | the same password |

3. Run a workflow (see below). The run's summary page shows the certificate fingerprint of the APK.
   It must be `6D:40:D9:83:...:49:48` for the release key.

## The two workflows

* **Signed build** (`.github/workflows/signed-build.yml`): every push to `main` builds the project files in the repo.
* **Signed build from zip** (`.github/workflows/signed-build-from-zip.yml`): upload a project zip to the repo,
  in the main folder or in `zips` (Add file ▸ Upload files), and it is unpacked, built and signed. Nothing
  else to commit. The two workflow files themselves must be real files in `.github/workflows/` (GitHub
  never unpacks zips to find workflows): add them once with Add file ▸ Create new file.

Both publish the APK under **Releases** and as a run artifact. Version codes count up with the commits
(1000 + number of commits), so both workflows can be mixed freely and Android never sees a downgrade.

## Switching from the old debug key (important, once)

Builds up to 2.5 were signed with the debug key in `app/debug.keystore`. Android will **not** install a
release-key APK over them. Do this once:

1. In the old app: Settings ▸ Backup ▸ save the backup zip somewhere safe.
2. Uninstall the old app, install the first release-key APK.
3. Settings ▸ Restore ▸ pick the backup zip. Songs, albums, settings and demos come back.

From then on every build updates in place. If you would rather not switch, leave the four secrets out:
the workflows then fall back to the old debug key (a warning shows in the run) and updates keep working.

## Keep the key safe

* Back up `songbook-release.jks` and its password (a password manager is ideal). **If you lose them, no
  future build can update the installed app**, it would have to be uninstalled first.
* Never commit the `.jks` file or the passwords to the repo (`.gitignore` already blocks `*.jks`).
* Do not make the secrets part of a public fork or pull request.
