import org.jetbrains.changelog.Changelog

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "com.itsjeel01"
version = "1.0.0-beta.3"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("Git4Idea")
    }

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253.1"
        }

        vendor {
            name = "Jeel Patel"
            email = "anchor+itsjeel01@gmail.com"
            url = "https://github.com/alph-a07/anchor"
        }

        changeNotes.set(providers.provider {
            val currentVersion = project.version.toString()

            val extension = changelog
            val versionItem = if (extension.has(currentVersion)) {
                extension.get(currentVersion)
            } else {
                extension.getLatest()
            }

            extension.renderItem(versionItem, Changelog.OutputType.HTML)
        })
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))

        val currentVersion = project.version.toString()
        val targetChannels = mutableListOf("default")

        if (currentVersion.contains("beta", ignoreCase = true)) {
            targetChannels.add("beta")
        } else if (currentVersion.contains("alpha", ignoreCase = true)) {
            targetChannels.add("alpha")
        }
        channels.set(targetChannels)
    }
}

changelog {
    path.set(file("CHANGELOG.md").path)
    version.set(project.version.toString())
    combinePreReleases.set(true)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}