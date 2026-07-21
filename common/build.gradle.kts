import com.texelsaurus.Versions
import com.texelsaurus.Properties

plugins {
    id("java-conv")
    // Was org.spongepowered.gradle.vanilla 0.2.1-SNAPSHOT. VanillaGradle deobfuscates
    // using the client_mappings artifact from Mojang's version manifest — which no
    // longer exists, because 26.1+ ships unobfuscated. It fails hard on 26.2 with
    // "No CLIENT_MAPPINGS download information was within the manifest!".
    // Loom provides the Minecraft classpath here instead.
    id("net.fabricmc.fabric-loom") version "1.18.0-alpha.9"
}

loom {
    accessWidenerPath = file("src/main/resources/${Properties.modid}.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
}

configurations {
    register("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    register("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}