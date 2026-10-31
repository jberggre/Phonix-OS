# Phonix OS — instructions for the agent

## Read other sessions before you write anything

This project has been worked on by several Freebuff sessions, and sessions fail mid-round. The
code that a session *committed* is often not its useful work: uncommitted and untracked files in a
sibling worktree are usually where the real progress is. A later session that starts from `main`
without looking has repeatedly re-implemented work that already existed, and once shipped a
fabricated feature because it trusted the committed tree.

Do this first, every session:

```bash
ls -1 ~/.freebuff/worktrees/                       # one directory per session
for d in ~/.freebuff/worktrees/*/*/; do
  echo "== $d"; (cd "$d" && git log --oneline -1 && git status --short && \
    git ls-files --others --exclude-standard | head -20)
done
```

Then, for any worktree whose branch or status looks related:

- `git diff` its touched files and **read untracked files in full** — an untracked
  `DistroInstaller.kt` is a whole feature.
- Read its `docs/HANDOFF.md` and `docs/VERIFICATION.md` diffs: failed sessions often record
  findings they never committed.
- Grep the session transcripts if the worktree has nothing:
  `~/.config/freebuff-desktop/projects/*/desktop-v2.db` (SQLite) and
  `~/.config/freebuff-desktop/local-projects/*/threads/`.

Only then read this repo's [docs/HANDOFF.md](docs/HANDOFF.md) and
[docs/VERIFICATION.md](docs/VERIFICATION.md), which are the canonical queue and evidence.

## Hard rules learned the hard way

- **Never label a feature as working when the code does not do the work.** A previous round
  "installed" Debian by copying Alpine's `bin/` and writing a Debian `os-release`. Every claim in
  `docs/` must have a measurement behind it, or be labelled `[static]` / "not run".
- **Never delete a working rootfs before its replacement exists.** Install into a staging tree,
  promote only after verification, prune afterwards, and never prune the bundled Alpine rescue
  image.
- **Check symlinks before writing into a guest tree.** Debian ships `etc/resolv.conf` as a symlink
  into `/run`, so `File(...).writeText()` fails with ENOENT after promotion. Unlink, then write.
- **Measure the active distro, not `linux/alpine`.** A hardcoded path made the hub show
  "On disk: -" and stale prompts after a real switch.

## Device and build

- Bench unit: **SM-G973U**, Android 12 / API 31, device owner
  `com.example.receiver.PhonixDeviceAdminReceiver`, package `com.aistudio.phonix.vpxmkr`.
  It has a real **HDMI monitor (1920×1080)** attached, so DeX is testable now.
- Reach it over wireless adb (`adb pair`, then `adb connect` with the *separate* debug port), or
  USB. `adb mdns services` may stay empty on Windows even when `adb devices` shows the phone.
- **Do not uninstall or `force-stop` the package** and never clear device policy: it is device
  owner and those are not recovery options.
- The device-owner signing key is `debug.keystore` (gitignored, **not** in the repo). Its
  **certificate** SHA-256 is `7D:2B:6C:3D:…:B5:DA`; every copy of the file hashes to `7670a511…`.
  The hash quoted in `docs/` is the certificate fingerprint, not the file hash.
- Toolchain: `~/android-toolchain` (JDK 21, Gradle 9.3.1, SDK 36). Build with
  `gradle --no-daemon assembleDebug testDebugUnitTest`; `local.properties` needs
  `sdk.dir` with forward slashes.
- Acceptance: `tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk`
  (non-destructive), `tools/deck-check.sh` for display metrics.

## Style

Match the existing prose: measured claims, `[measured]`/`[static]` labels, and no marketing. Commit
messages explain *why* the change was needed, in the imperative.

- **Never add attribution.** Commit messages end at their own last line: no
  `Co-Authored-By`, no "Generated with" trailer.
