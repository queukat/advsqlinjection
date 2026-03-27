plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
    id("ivy-publish")
}

group = "com.queukat"
version = "1.1.0"

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
        changeNotes.set(
            """
            <h2>What’s new</h2>
            <ul>
                <li>Introduced typed injection rules with safe migration from legacy raw-string settings.</li>
                <li>Added path-aware rule matching, ordered rule execution, and non-overlapping multi-segment injection.</li>
                <li>Reworked the settings UI with add, edit, delete, reorder, validation, example rules, and current-file preview.</li>
                <li>Updated product wording and setup guidance to reflect the plugin's real generic language-injection use case.</li>
                <li>Added automated tests for migration, matching semantics, ordering, path filters, and persistence round-trips.</li>
            </ul>
            """.trimIndent()
        )
    }

    buildSearchableOptions {
        enabled = true
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
