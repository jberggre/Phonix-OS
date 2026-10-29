package com.example.model

/**
 * PC software that can be installed into the Alpine guest with one click.
 *
 * Every package name here exists in Alpine 3.24's aarch64 repositories, and every size is the
 * real compressed size from that release's APKINDEX — not an estimate. These are native ARM64
 * Linux programs: the guest cannot run x86-64 Windows `.exe` files (see docs/LINUX.md), so this
 * list is deliberately Linux software only.
 *
 * `downloadMb` is what has to come over the network. Installed size is larger; the install
 * output in the window reports the truth for each package.
 */
data class LinuxAppEntry(
    val packageName: String,
    val title: String,
    val blurb: String,
    val downloadMb: Int,
    val category: String
)

/** `installed` is null until the guest has actually been asked; it is never guessed. */
data class LinuxAppStatus(
    val entry: LinuxAppEntry,
    val installed: Boolean? = null
)

object LinuxAppCatalog {

    val apps: List<LinuxAppEntry> = listOf(
        LinuxAppEntry(
            packageName = "firefox",
            title = "Firefox",
            blurb = "Full desktop browser inside the Linux desktop, with its own profile and " +
                "extensions. Large download; this is the heaviest everyday app here.",
            downloadMb = 93,
            category = "Internet"
        ),
        LinuxAppEntry(
            packageName = "chromium",
            title = "Chromium",
            blurb = "The open-source browser Chrome is built from. Pick one browser, not both — " +
                "together they are a quarter of a gigabyte.",
            downloadMb = 146,
            category = "Internet"
        ),
        LinuxAppEntry(
            packageName = "thunderbird",
            title = "Thunderbird",
            blurb = "Desktop email client. Useful if you want mail that is not a web tab.",
            downloadMb = 93,
            category = "Internet"
        ),
        LinuxAppEntry(
            packageName = "libreoffice-writer",
            title = "LibreOffice Writer",
            blurb = "Word-processor. Opens and saves .docx and .odt. Installs the shared " +
                "LibreOffice runtime too, so the first office app is the slow one.",
            downloadMb = 4,
            category = "Office"
        ),
        LinuxAppEntry(
            packageName = "libreoffice-calc",
            title = "LibreOffice Calc",
            blurb = "Spreadsheet. Same shared runtime as Writer; install both and the second " +
                "one only adds the calc module.",
            downloadMb = 10,
            category = "Office"
        ),
        LinuxAppEntry(
            packageName = "gimp",
            title = "GIMP",
            blurb = "Image editor. Software rendering means filters are slow on big images, but " +
                "it runs.",
            downloadMb = 20,
            category = "Graphics"
        ),
        LinuxAppEntry(
            packageName = "inkscape",
            title = "Inkscape",
            blurb = "Vector editor for SVG. Lighter than GIMP and comfortable at this resolution.",
            downloadMb = 8,
            category = "Graphics"
        ),
        LinuxAppEntry(
            packageName = "vlc",
            title = "VLC",
            blurb = "Media player for almost any file. The X11 output is software-rendered; " +
                "1080p playback is the sensible ceiling.",
            downloadMb = 6,
            category = "Media"
        ),
        LinuxAppEntry(
            packageName = "mpv",
            title = "mpv",
            blurb = "Minimal video player. Lighter than VLC if all you need is playback.",
            downloadMb = 1,
            category = "Media"
        ),
        LinuxAppEntry(
            packageName = "audacity",
            title = "Audacity",
            blurb = "Audio recorder and editor. Needs a working audio path, which the guest " +
                "does not have yet — expect to edit files, not record them.",
            downloadMb = 9,
            category = "Media"
        ),
        LinuxAppEntry(
            packageName = "geany",
            title = "Geany",
            blurb = "Lightweight code editor with syntax highlighting. The best fit for this " +
                "display and this CPU.",
            downloadMb = 5,
            category = "Development"
        ),
        LinuxAppEntry(
            packageName = "python3",
            title = "Python 3",
            blurb = "The interpreter plus its standard library. Enough for scripts; pip packages " +
                "that need to compile will need gcc and headers as well.",
            downloadMb = 8,
            category = "Development"
        ),
        LinuxAppEntry(
            packageName = "git",
            title = "Git",
            blurb = "Version control. Works normally over the phone's network.",
            downloadMb = 4,
            category = "Development"
        ),
        LinuxAppEntry(
            packageName = "htop",
            title = "htop",
            blurb = "Interactive process viewer. Install it to watch the guest's own memory and " +
                "CPU use while something heavy runs.",
            downloadMb = 1,
            category = "System"
        )
    )

    fun byPackage(packageName: String): LinuxAppEntry? =
        apps.firstOrNull { it.packageName == packageName }
}
