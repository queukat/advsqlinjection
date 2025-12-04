plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
    id("ivy-publish")
}

group = "com.queukat"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {

}

intellij {
    pluginName.set("Advanced SQL Injection")
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
        untilBuild.set("")
        changeNotes.set(
            """
            <h2>What’s new</h2>
            <ul>
                <li>Initial version of Advanced SQL Injection with Kotlin UI DSL based settings.</li>
            </ul>
            """.trimIndent()
        )
    }

    buildSearchableOptions {
        enabled = true
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf("default"))
    }
}
