package com.texelsaurus;

object Versions {
    const val mod = "19.1.4"
    const val java = "25"
    const val minecraft = "26.2"
    const val minecraftRange = "[26.2,26.3)"
    const val minecraftLower = "26.2"
    const val minecraftUpper = "26.3"

    // :forge is still commented out of settings.gradle.kts — ForgeGradle 6 cannot run on
    // Gradle 9. These coordinates are recorded for that eventual re-enable and are NOT
    // build-verified; note the interpolation is "${minecraft}-${forge}", so this holds the
    // build number only. The 26.2 line is 65.x.
    const val forge = "65.1.0"
    const val forgeVersionRange = "[65,)"
    const val forgeLoaderRange = "[65,)"

    // NeoForge: the whole 26.2 line is still -beta (48 builds, zero stable as of 2026-08-04), so
    // this jar necessarily ships against a beta loader and the range has to admit prereleases.
    const val neoForge = "26.2.0.48-beta"
    const val neoForgeVersionRange = "[26.2.0-beta,)"
    const val neoForgeLoaderRange = "[4,)"

    const val fabric = "0.155.2+26.2"
    const val fabricLoaderMin = "0.19.3"
    const val fabricLoader = "0.19.3"
}
