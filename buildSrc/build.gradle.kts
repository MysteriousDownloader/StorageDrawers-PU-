import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    gradleApi()
    implementation(group = "net.darkhax.curseforgegradle", name = "CurseForgeGradle", version = "1.1.26")
    implementation(group = "com.modrinth.minotaur", name = "Minotaur", version = "2.8.+")
}

// buildSrc's runtime classpath is EXPORTED as a parent classloader to every project's
// buildscript, and classloading is parent-first. So whatever ASM lands here wins over the
// ASM that Fabric Loom declares for itself — Loom asks for 9.10.1 and gets whatever is
// below.
//
// Minotaur 2.8.10 drags in org.ow2.asm:asm:9.5, which refuses any class file newer than
// major version 65 (Java 21). Minecraft 26.2 is compiled to major version 69 (Java 25), so
// every `new ClassReader(...)` inside Loom's MinecraftJarMerger threw
// "Unsupported class file major version 69". That merger runs each class through
// CompletableFuture without ever joining the futures, so the exception was swallowed with
// no log line and the merge produced minecraft-merged.jar containing all 20k asset/data
// entries and ZERO classes — i.e. an empty Minecraft classpath and ~100s of bogus
// "package net.minecraft.* does not exist" compile errors.
//
// Keep this at or above the ASM version Loom declares. 9.10.1 accepts up to class file
// major version 71.
val asmVersion = "9.10.1"

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion(asmVersion)
            because("MC 26.2 is Java 25 bytecode; an older ASM here silently breaks Loom's jar merge")
        }
    }
}