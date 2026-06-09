plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

group = "com.itsjeel01"
version = "1.0.0-beta.1"

repositories {
    mavenCentral()
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
            email = "itsjeel01@gmail.com"
            url = "https://github.com/alph-a07/anchor"
        }

        changeNotes = """
        <h2>1.0.0-beta.1</h2>
    
        <p>First public beta. Core GitHub workflow inside any JetBrains IDE.</p>
    
        <h3>Added</h3>
    
        <h4>Issues</h4>
        <ul>
            <li>Issue list with Open / Closed / All filter chips</li>
            <li>State badges (green for open, purple for closed)</li>
            <li>Issue detail panel with full GitHub Flavored Markdown rendering</li>
            <li>Embedded image rendering inside issue bodies and comments</li>
            <li>Label chips rendered with GitHub colors</li>
            <li>Comment thread: view, post, edit, delete own comments</li>
            <li>Close / Reopen action from detail view</li>
            <li>Create New Issue dialog with title, body, labels</li>
        </ul>
    
        <h4>Pull Requests</h4>
        <ul>
            <li>PR list with Open / Merged / Closed / All filter chips</li>
            <li>State badges matching GitHub's color system (green open, purple merged, red closed)</li>
            <li>Source → target branch display on each PR row</li>
            <li>PR detail panel with full commit history</li>
            <li>Clickable SHA hashes that open commits in browser</li>
            <li>Open in browser action</li>
            <li>Comment input on PR detail</li>
        </ul>
    
        <h4>Branches</h4>
        <ul>
            <li>Full remote branch list with 7-character SHA display</li>
            <li>Click to open branch in browser</li>
        </ul>
    
        <h4>Core</h4>
        <ul>
            <li>Auto-detects active repository from project Git remote (HTTPS and SSH)</li>
            <li>GitHub Personal Access Token authentication via Settings</li>
            <li>Credentials stored securely using IntelliJ PasswordSafe</li>
            <li>GitHub Enterprise base URL override in Settings</li>
            <li>Auto-refresh on tool window open (configurable)</li>
            <li>Full GitHub Flavored Markdown support:
                <ul>
                    <li>Tables</li>
                    <li>Strikethrough</li>
                    <li>Task lists</li>
                    <li>Autolinks</li>
                    <li>Code blocks</li>
                    <li>Blockquotes</li>
                    <li>GitHub Alerts</li>
                </ul>
            </li>
            <li>Respects IDE theme:
                <ul>
                    <li>Dark</li>
                    <li>Light</li>
                    <li>Darcula</li>
                    <li>High Contrast</li>
                    <li>New UI</li>
                    <li>Classic UI</li>
                </ul>
            </li>
        </ul>
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