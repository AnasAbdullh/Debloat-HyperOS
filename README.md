# Debloat HyperOS

A rootless HyperOS/MIUI bloatware remover, similar in spirit to Canta, built on
Kotlin + Jetpack Compose (Material 3) with an MVVM + Repository architecture.
All privileged operations run through **Shizuku** — no root required.

## How the "no root" part works

Shizuku hosts a background service started once via ADB (`adb shell sh
/sdcard/Android/data/moe.shizuku.privileged.api/start.sh`, or wirelessly via
Shizuku's own Android 11+ wireless-debugging flow, or via Shizuku's root
mode if the device *is* rooted — the app itself never needs root). Once
running, Shizuku exposes a binder that lets registered apps execute shell
commands under the **`shell` (uid 2000)** identity — the same privilege
level `adb shell` has. That's enough to run:

- `pm list packages --user 0 <pkg>` — check install state
- `pm uninstall --user 0 <pkg>` — remove a package for the current user (reversible, no data wipe of the APK itself on most OEM builds)
- `cmd package install-existing <pkg>` — reinstall/restore it for the current user

The end user must install the separate **Shizuku** app and start its
service once per boot (or use Shizuku's "start on boot" support on rooted
devices, or pair once over Wi-Fi debugging on Android 11+). This app cannot
start Shizuku itself — it only binds to it.

## Project layout

```
app/src/main/java/com/debloat/hyperos/
├── DebloatApp.kt                # Application: Shizuku init + repository wiring
├── MainActivity.kt              # Compose host
├── shizuku/ShizukuManager.kt    # Binder lifecycle, permission flow, shell exec
├── data/
│   ├── entity/DebloatAppEntity.kt
│   ├── dao/DebloatDao.kt
│   ├── db/AppDatabase.kt
│   └── PresetModels.kt          # kotlinx.serialization models for the JSON preset
├── repository/DebloatRepository.kt  # Seeding, live status refresh, batch ops
├── viewmodel/
│   ├── DebloatViewModel.kt
│   └── DebloatViewModelFactory.kt
└── ui/
    ├── theme/                   # AMOLED dark theme (#121212 / #1E1E24 / #FF7A00)
    ├── components/              # HeaderCard, CategorySectionHeader, AppCard, BottomActionBar
    └── screens/MainScreen.kt
app/src/main/assets/debloat_presets.json   # Bundled default package list
```

## Data flow

1. On first launch, `DebloatRepository.seedIfNeeded()` reads
   `assets/debloat_presets.json` and inserts every package into Room
   (`debloat_apps` table), optimistically marked `isInstalled = true`.
2. `refreshInstallStates()` then walks every row and calls
   `pm list packages --user 0 <pkg>` through Shizuku to get the real state,
   updating Room — which the UI observes reactively via `Flow`.
3. Selecting apps toggles `isSelected` in Room per-row or per-category.
4. "Uninstall Selected" / "Restore Selected" (label swaps based on the active
   filter tab) walks the selected set sequentially on `Dispatchers.IO`,
   calling `pm uninstall --user 0` or `cmd package install-existing`, syncing
   Room after each call so the list updates live and a failed item is
   automatically deselected with its error surfaced in the final snackbar.

## Known follow-ups if you build this out further

- The `MSA (Ad Service)` and a few other packages are true system apps on
  some HyperOS builds and may return a non-zero exit code from `pm
  uninstall --user 0` if the OEM has flagged them non-removable for the
  current user — that's surfaced via the failure list in the batch result,
  not silently swallowed.
- Consider adding a DataStore-backed "last successful backup of removed
  package list" export/import so a user can restore their exact state on a
  fresh Shizuku session (e.g. after a factory reset).
- Icons are pulled from Compose's Material icon set only; if you want real
  per-app launcher icons, you'd resolve them via `PackageManager` for apps
  that are still installed (Shizuku doesn't change `PackageManager` read
  access — visibility is already governed by `QUERY_ALL_PACKAGES` in the
  manifest).
