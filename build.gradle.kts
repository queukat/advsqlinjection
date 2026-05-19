import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
    alias(libs.plugins.sonarqube)
    jacoco
}

group = "com.queukat"
version = "1.1.2"

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
val defaultPluginVerifierMatrix = "baseline"
val pluginVerifierMatrices = mapOf(
    "baseline" to listOf("IC-2022.3.3"),
    "release" to listOf("IC-2022.3.3", "IC-2024.3.6", "IU-2025.3", "IU-2026.1.2")
)

fun parseIdeVersions(value: String): List<String> =
    value.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)

fun resolvePluginVerifierMatrix(name: String): List<String> {
    val normalizedName = name.trim().lowercase()
    return pluginVerifierMatrices[normalizedName] ?: throw GradleException(
        "Unknown plugin verifier matrix '$name'. Known matrices: ${pluginVerifierMatrices.keys.sorted().joinToString(", ")}."
    )
}

val selectedPluginVerifierMatrix = providers.gradleProperty("pluginVerifierMatrix")
    .orElse(providers.environmentVariable("PLUGIN_VERIFIER_MATRIX"))
    .map(::resolvePluginVerifierMatrix)

val pluginVerifierIdeVersions = providers.gradleProperty("pluginVerifierIdeVersions")
    .map(::parseIdeVersions)
    .orElse(providers.environmentVariable("PLUGIN_VERIFIER_IDE_VERSIONS").map(::parseIdeVersions))
    .orElse(selectedPluginVerifierMatrix)
    .orElse(resolvePluginVerifierMatrix(defaultPluginVerifierMatrix))

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

intellij {
    pluginName.set("Advanced Language Injection")
    version.set("2022.3")
    plugins.set(listOf("java", "yaml", "properties"))
    updateSinceUntilBuild.set(false)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sonar {
    properties {
        property("sonar.projectKey", "advsqlinjection")
        property("sonar.projectName", "Advanced Language Injection")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile.absolutePath
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "src/main/kotlin/com/queukat/advsqlinjection/actions/**",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionHelp.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionRuleDialog.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionRulePreview.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionRulePreviewDialog.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionRulesCellRenderer.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsConfigurable.kt",
                "src/main/kotlin/com/queukat/advsqlinjection/settings/AdvancedSQLInjectionSettingsPanel.kt"
            ).joinToString(",")
        )
    }
}


tasks {
    withType<Test> {
        extensions.configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    test {
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        classDirectories.setFrom(instrumentCode)
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    named("sonar") {
        dependsOn(jacocoTestReport)
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("223.7571.182")
        changeNotes.set(currentReleaseNotesHtml)
    }

    buildSearchableOptions {
        enabled = true
    }

    runPluginVerifier {
        ideVersions.set(pluginVerifierIdeVersions)
        doFirst {
            logger.lifecycle("Plugin verifier IDE versions: ${pluginVerifierIdeVersions.get().joinToString(", ")}")
        }
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
        token.set(providers.environmentVariable("PUBLISH_TOKEN_PLUGIN"))
        channels.set(listOf("default"))
    }
}
