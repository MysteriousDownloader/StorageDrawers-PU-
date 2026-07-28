package com.texelsaurus;

object Properties {
    const val group = "com.texelsaurus.minecraft.storagedrawers"
    const val name = "Storage Drawers"
    const val filename = "StorageDrawers"
    const val author = "Texelsaur"

    // Port maintainer. The GitHub handle goes in `authors`, so it appears alongside the
    // original author in the mod list byline; the in-game name is listed separately under
    // Contributors and carries the profile link.
    const val maintainer = "chaevsfe"
    const val contributor = "BumblePie"
    const val contributorUrl = "https://github.com/chaevsfe"

    // This fork. `homepage` stays pointed at the original CurseForge project.
    const val sourcesUrl = "https://github.com/chaevsfe/StorageDrawers"
    const val issuesUrl = "https://github.com/chaevsfe/StorageDrawers/issues"
    const val modid = "storagedrawers"

    // Both of these were the ORIGINAL project's ids. Publishing with them would attempt to
    // upload this fork to Texelsaur's pages, so they are placeholders until this fork has its
    // own. Replace modrinthProjectId with the id from the fork's Modrinth project settings.
    // Must stay numeric: CurseForgeGradle parses it at configuration time, so a word here
    // breaks every build, not just publishing. 0 is a deliberate no-such-project.
    const val curseProjectId = "0"
    const val modrinthProjectId = "REPLACE_WITH_FORK_MODRINTH_PROJECT_ID"
    const val description = "Interactive compartment storage for your workshops"
    const val license = "MIT"
    const val distRelease = "release"
    const val distGameVersions = "26.2"
}