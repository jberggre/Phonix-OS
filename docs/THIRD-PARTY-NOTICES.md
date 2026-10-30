# Third-party notices and asset provenance

Phonix would not exist without a lot of other people's work. This page records what the build
actually contains, under which licence, and where the artwork in the repository came from. It is
written for a shipped box, not just for developers.

Phonix is **not affiliated with, endorsed by, or sponsored by** Samsung, Google, Microsoft, or any
Linux distribution. Product names (Samsung DeX, Android, Pixel, Windows) appear only to describe
what a feature is compatible with; the interface does not reproduce their artwork or trademarks.

## Code and runtime libraries

| Component | Version in this tree | Licence | Where it lives |
|---|---|---|---|
| Kotlin, Kotlin Coroutines | 2.2.10 / 1.10.2 | Apache-2.0 | app code |
| Jetpack Compose (UI, Material 3, Material Icons) | BOM 2024.09.00 | Apache-2.0 | app UI |
| AndroidX (Core, Lifecycle, Activity, WindowManager, Room) | see `gradle/libs.versions.toml` | Apache-2.0 | app code |
| OkHttp / Retrofit / Moshi | 4.10.0 / 2.12.0 / 1.15.2 | Apache-2.0 | update check only |
| Robolectric (tests only) | 4.16.1 | MIT | `app/src/test` |
| PRoot | bundled `libproot.so` | **GPL-2.0** | `app/src/main/jniLibs` |
| Alpine Linux minirootfs | 3.24.3, aarch64 | packages under their own licences (predominantly MIT, GPL-2.0, BSD); the full list is inside the guest at `/lib/apk/db/installed` | `app/src/main/assets/alpine-rootfs.tgz` |
| musl libc | shipped by Alpine | MIT | inside the guest |
| BusyBox | shipped by Alpine | GPL-2.0 | inside the guest |
| X.Org / Xvfb / x11vnc | installed from Alpine on demand | MIT-style / GPL-2.0 | guest, not in the APK |
| llama.cpp and `llama-server` | built for Alpine, installed on demand | MIT | guest, not in the APK |
| Qwen2.5-1.5B-Instruct (Q4_K_M GGUF) | downloaded by the user, not bundled | Apache-2.0 | guest, user's storage |

PRoot is GPL-2.0. It is redistributed as an unmodified shared library. If Phonix is sold as a
binary, the GPL requires that the corresponding source be made available; `docs/LINUX.md` records
where the library came from and how to rebuild it.

**Deliberately absent:** Firebase AI, Firebase App Check, Play Services location, and the
Credential Manager sign-in stack are not dependencies. They were listed in the starter project and
never used; removing them cut the debug APK from 33,549,802 to 31,303,117 bytes and keeps the
"your question never leaves the phone" claim true.

## Artwork in this repository

- **`res/drawable/img_phoenix_logo.png`, `img_phoenix_wallpaper.png`** — generated for this project
  by the owner with an image model, then cropped. They are not traced or derived from any existing
  logo.
- **`res/drawable-nodpi/ic_phonix_*.png`** — generated the same way from a single sheet the owner
  provided, then sliced by connected component. The artwork is original to the project; it does not
  reproduce Samsung, Google, Microsoft, or distribution mascots.
- **Wallpapers** — drawn in code from gradients, type and the project's own marks. See
  [WALLPAPER-BRIEF.md](WALLPAPER-BRIEF.md).
- **No third-party icons, screenshots, fonts, or wallpapers are bundled.** The Material icons used
  in a few chrome elements come from `androidx.compose.material:material-icons-*` (Apache-2.0).

Because the brand art was produced with a generative model, its copyright status varies by
jurisdiction. It is used here as the project's own identity; a commercial release should have that
question answered by counsel rather than assumed.

## Written content

The offline knowledge library (`model/KnowledgeLibrary.kt`) and the assistant's built-in notes were
written for this project from general technical and first-aid knowledge. They are not copied from
the WHO, CDC, FEMA, or any book. They are a **starter corpus**, not a substitute for professional
medical, electrical, or structural advice, and the UI says so on the assistant screen.

If a real book pack is ever imported, its licence and attribution belong on this page before the
pack ships.
