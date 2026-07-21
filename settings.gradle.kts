pluginManagement {
    repositories {
        maven("https://maven.neoforged.net/releases")
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Snapshots"
        }
        maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
            name = "Fuzs Mod Resources"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "StorageDrawers"
include("common")
// Scoped out for the 26.2 Fabric port — both platforms do have 26.2 builds
// (NeoForge 26.2.0.28-beta, Forge 26.2-65.0.7); re-enable once Fabric is green.
//include("forge")
//include("neoforge")
include("fabric")