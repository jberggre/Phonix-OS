# Privacy Hub: what it can and cannot do

The Privacy Hub exists for the two things a recycled phone can honestly do for privacy:
filter its own DNS lookups, and help its owner give a spare machine a second life as the
household filter. It is also where a Phonix-managed unit is erased at end of life.

**Status (2026-10-29, 16:29 build):** the Private DNS write path was verified on the unit
before 1.1; the 1.1 session did not retest it. **Decommission has not been executed** — it
would wipe the only unit — so treat it as written-but-unexercised. The current build adds a local
model and a read-only screen mirror; their data boundaries are below. The 1.1 changes did not
touch this screen; see [VERIFICATION.md](VERIFICATION.md) for the current work queue.

## On the phone: Private DNS

Android 9+ supports DNS-over-TLS through **Settings → Network & internet → Private DNS**.
Phonix does not ship its own resolver; it offers two public, filtering ones and accepts any
hostname the owner runs themselves:

| Preset | Hostname | Blocks |
| --- | --- | --- |
| AdGuard DNS | `dns.adguard-dns.com` | ads, trackers |
| AdGuard Family | `family.adguard-dns.com` | ads, trackers, adult content |

References: <https://adguard-dns.io/en/public-dns.html> and the AdGuard DNS knowledge base.

This is a real system setting, not a mock-up. When `provision.sh` has granted
`WRITE_SECURE_SETTINGS` (it does so during bench provisioning), the Hub writes
`private_dns_mode=hostname` and `private_dns_specifier=<host>` directly. Without that
permission there is no public API, so the Hub copies the hostname and opens the Settings
screen for the owner to paste it.

Limits worth repeating to customers:

- DNS filtering does not remove in-page ads served from the same domain as the content.
- The chosen resolver sees every lookup. It is a trade of one intermediary for another.
- Private DNS applies to this phone only.

## Local AI, Optional Distro Downloads, and Phone Screen

These are the features that touch sensitive data or make explicit opt-in network requests, so the boundaries are explicit:

- **Local AI:** the question, system prompt and streamed answer stay on the phone. `llama-server`
  binds `127.0.0.1:8081` (or `:8080`); there is no cloud AI key and no external inference call. The one network
  request is the **model download** from Hugging Face when the customer taps Set up. The model is
  stored app-privately and can be deleted by clearing Phonix’s data (or the in-app reset). The
  curated notes are sent to the local model only as context; they do not leave the device.
- **Linux Distributions:** **Alpine Linux 3.24** ships bundled inside the APK (`assets/alpine-rootfs.tgz`) and unpacks completely offline with zero network calls. If the user explicitly chooses **Debian 12**, **Ubuntu 24.04 LTS**, **Arch Linux ARM**, or **Kali Linux** in the Setup Wizard or Linux System tab, Phonix downloads the official ARM64 rootfs tarball over HTTPS (`images.linuxcontainers.org`, `cdimage.ubuntu.com`, or `os.archlinuxarm.org`) into `filesDir/linux/<distroId>`. No telemetry or user identifiers are sent during rootfs or package downloads (`apk`, `apt-get`, `pacman`).
- **Phone Screen:** `MediaProjection` captures the default display and writes it only to the
  in-process frame state used by the Phone Screen window. It is read-only, shows Android’s
  mandatory capture-consent dialog, and is stopped when the window closes. There is no recording,
  no upload and no input injection. A foreground notification is always shown while capture is
  active; a hidden capture would be a privacy defect.
- **Network ADB** is a provisioning tool, not a product feature. It can start the app and tap the
  consent dialog, but it does not bypass Android’s consent and should be disabled before the unit
  is shipped.

## In the house: Pi-hole on a spare machine

A phone cannot be the LAN's DNS server, and the app says so rather than offering a switch
that fails:

- DNS listens on **port 53**, below the unprivileged port floor of 1024. Android does not
  grant `CAP_NET_BIND_SERVICE` to apps, and proot does not change the UID inside the
  container. Root would be required, and Phonix does not ask for root.
- A phone that sleeps, throttles or updates is a poor always-on resolver.

The supported path, which the Hub walks through step by step:

1. A Raspberry Pi or an old x86 PC on Ethernet, with a DHCP reservation.
2. Official installer:
   `curl -sSL https://install.pi-hole.net | bash`
   (see <https://docs.pi-hole.net/main/basic-install/>; `sudo bash` if the image requires it).
3. Router DHCP hands out the Pi as the only DNS server. Leaving a public resolver as the
   secondary is a bypass and must not be recommended.
4. Verify from the Phonix terminal: `nslookup doubleclick.net <pi-ip>`; a Pi-hole answer of
   `0.0.0.0` or NXDOMAIN means filtering is live.

## At end of life: decommission

Once Phonix is device owner, Android blocks the normal Settings reset. The admin receiver
therefore requests `<wipe-data />` and the Hub exposes **Erase and decommission**, which
requires typing `ERASE` and calls `DevicePolicyManager.wipeData(0)`.

On a unit that is not device owner, the Hub does not pretend: it opens the platform's
factory reset screen and the system asks for confirmation. An ordinary app must never be
able to silently wipe a phone.

## Claims to avoid on the site

- "Built-in Pi-hole" — the phone cannot serve DNS on port 53.
- "Network-wide ad blocking out of the box" — only after a second machine is configured.
- "No DNS provider sees your traffic" — untrue with public Private DNS resolvers; true only
  for a self-hosted resolver with DoT.
- "Fully automatic decommission" — a person must type `ERASE`; that is intentional.
- "The AI never uses the network" — the initial model download is a network request; inference is
  local. Say “inference is local”, not “the feature never touches the network”.
- "The mirror can control the phone" — it is read-only; no taps are injected.
