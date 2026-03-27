plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
    id("ivy-publish")
}

group = "com.queukat"
version = "1.1.0"

val changelogFile = rootProject.file("CHANGELOG.md")

fun readChangelogSection(version: String): String {
    if (!changelogFile.exists()) {
        throw GradleException("Missing CHANGELOG.md in project root.")
    }

    val headerPattern = Regex("""^##\s+\[?${Regex.escape(version)}]?(?:\s+-\s+.+)?\s*$""")
    val nextSectionPattern = Regex("""^##\s+.+$""")

    val sectionLines = mutableListOf<String>()
    var inSection = false

    for (line in changelogFile.readLines()) {
        when {
            !inSection && headerPattern.matches(line.trim()) -> inSection = true
            inSection && nextSectionPattern.matches(line.trim()) -> break
            inSection -> sectionLines += line
        }
    }

    if (!inSection || sectionLines.joinToString("\n").trim().isEmpty()) {
        throw GradleException("Could not find changelog section for version $version in CHANGELOG.md.")
    }

    return sectionLines.joinToString("\n").trim()
}

fun escapeHtml(value: String): String = buildString {
    value.forEach { ch ->
        append(
            when (ch) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> ch
            }
        )
    }
}

fun inlineMarkdownToHtml(text: String): String {
    var html = escapeHtml(text)
    html = Regex("`([^`]+)`").replace(html) { "<code>${it.groupValues[1]}</code>" }
    html = Regex("""\[(.+?)]\((.+?)\)""").replace(html) {
        val label = it.groupValues[1]
        val url = it.groupValues[2]
        """<a href="$url">$label</a>"""
    }
    return html
}

fun markdownToHtml(markdown: String): String {
    val html = StringBuilder()
    var inList = false
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) {
            return
        }
        html.append("<p>")
        html.append(paragraphLines.joinToString(" ") { inlineMarkdownToHtml(it.trim()) })
        html.append("</p>\n")
        paragraphLines.clear()
    }

    fun closeList() {
        if (inList) {
            html.append("</ul>\n")
            inList = false
        }
    }

    markdown.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.isEmpty() -> {
                flushParagraph()
                closeList()
            }

            line.startsWith("### ") -> {
                flushParagraph()
                closeList()
                html.append("<h3>${inlineMarkdownToHtml(line.removePrefix("### ").trim())}</h3>\n")
            }

            line.startsWith("## ") -> {
                flushParagraph()
                closeList()
                html.append("<h2>${inlineMarkdownToHtml(line.removePrefix("## ").trim())}</h2>\n")
            }

            line.startsWith("- ") -> {
                flushParagraph()
                if (!inList) {
                    html.append("<ul>\n")
                    inList = true
                }
                html.append("<li>${inlineMarkdownToHtml(line.removePrefix("- ").trim())}</li>\n")
            }

            else -> paragraphLines += line
        }
    }

    flushParagraph()
    closeList()
    return html.toString().trim()
}

val currentReleaseNotesMarkdown = providers.provider { readChangelogSection(project.version.toString()) }
val currentReleaseNotesHtml = currentReleaseNotesMarkdown.map(::markdownToHtml)

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

intellij {
    pluginName.set("Advanced Language Injection")
    version.set("2022.3")
    plugins.set(listOf("java", "yaml"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


tasks {
    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("223.7571.182")
        untilBuild.set("223.*")
        changeNotes.set(currentReleaseNotesHtml)
    }

    buildSearchableOptions {
        enabled = true
    }

    register("writeReleaseNotes") {
        group = "release"
        description = "Writes the current version release notes from CHANGELOG.md to build/release-notes.md."

        val outputFile = layout.buildDirectory.file("release-notes.md")
        inputs.file(changelogFile)
        outputs.file(outputFile)

        doLast {
            val targetFile = outputFile.get().asFile
            targetFile.parentFile.mkdirs()
            targetFile.writeText(currentReleaseNotesMarkdown.get().trim() + "\n")
        }
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn(signPlugin)
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(listOf("default"))
    }
}
