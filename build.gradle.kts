plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.itsjeel01"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Use Community Edition since this plugin targets all JetBrains IDEs
        // (platform-only, depends on com.intellij.modules.platform).
        // IntelliJ IDEA Ultimate bundles Java plugins that aren't needed here.
        intellijIdeaCommunity("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Runtime dependencies bundled with the plugin (not provided by the IDE platform)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
}

intellijPlatform {
    // Disable since this plugin has no custom searchable options and
    // the headless IDE launch fails for platform-only plugins.
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
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
