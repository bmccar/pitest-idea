import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    //gradlePluginPortal()
    maven("https://repo.gradle.org/gradle/libs-releases")

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

val pitVersion = "1.18.2"
val pitJunit5PluginVersion = "1.2.2"
val junitPlatformVersion = "1.12.2"
val junitVersion = "5.13.1"
val lowestBundledJunitVersion = "5.10.0"
val pluginName: String by project
// $pluginName does not always get resolved without this trick, is there a better way?
val pluginNameAsString = "$pluginName"

// Expose useful constants from this file in Java code by generating a Java source file.
val generateConstants by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/constants")
    outputs.dir(outputDir)

    doLast {
        val constantsFile = outputDir.get().file("org/pitestidea/constants/PluginVersions.java").asFile
        constantsFile.parentFile.mkdirs()
        constantsFile.writeText("""
            package org.pitestidea.constants;
            
            public final class PluginVersions {
                public static final String PLUGIN_NAME = "$pluginName";
                public static final String PITEST_VERSION = "$pitVersion";
                public static final String PIT_JUNIT5_PLUGIN_VERSION = "$pitJunit5PluginVersion";
                public static final String JUNIT_PLATFORM_VERSION = "$junitPlatformVersion";
                public static final String JUNIT_BUNDLED_VERSION = "$junitVersion";
                public static final String LOWEST_BUNDLED_JUNIT_VERSION = "$lowestBundledJunitVersion";
                
                private PluginVersions() {}
            }
        """.trimIndent())
    }
}

sourceSets.main {
    java.srcDir(generateConstants)
}

tasks.compileJava {
    dependsOn(generateConstants)
}

// Create different Gradle configurations so that different versions of the same dependency can be resolved.
// Otherwise, Gradle will only ever choose the latest version of a given dependency.
// Note that creating configurations this way is deprecated and set to break in Gradle 9, but the suggested fix breaks now.
val pitest182 by configurations.creating {
    description = "PiTest dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

val junitPlatform113 by configurations.creating {
    description = "JUnit-platform dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

val junitPlatform112 by configurations.creating {
    description = "JUnit-platform dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

val junitPlatform111 by configurations.creating {
    description = "JUnit-platform dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

val junitPlatform110 by configurations.creating {
    description = "JUnit-platform dependencies"
    isCanBeConsumed = false
    isCanBeResolved = true
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.21")
    implementation("org.gradle:gradle-tooling-api:8.7")

    // Pitest sometimes has problems with commons-txt 1.12.0 even though that is what it resolves to.
    // Problem doesn't show up in 1.10.0
    pitest182("org.pitest:pitest-command-line:$pitVersion") {
        exclude(group = "org.apache.commons", module = "commons-text")
    }
    pitest182("org.pitest:pitest-junit5-plugin:1.2.2")
    pitest182("org.apache.commons:commons-text:1.10.0")

    // platform-launcher is not typically included by junit users, but this plugin needs it to invoke pitest
    junitPlatform113("org.junit.platform:junit-platform-launcher:1.13.1") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
    }
    junitPlatform112("org.junit.platform:junit-platform-launcher:1.12.2") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
    }
    junitPlatform111("org.junit.platform:junit-platform-launcher:1.11.4") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
    }
    junitPlatform110("org.junit.platform:junit-platform-launcher:1.10.5") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
    }

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    testImplementation("org.mockito:mockito-core:5.16.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
}

// Ensure the custom configurations above end up in the sandbox (doesn't happen by default).
tasks.prepareSandbox {
    from(pitest182) {
        into("$pluginNameAsString/lib/ifn-pitest")
    }
    from(junitPlatform113) {
        into("$pluginNameAsString/lib/ifc-junit-jupiter-api/5.13.0")
    }
    from(junitPlatform112) {
        into("$pluginNameAsString/lib/ifc-junit-jupiter-api/5.12.0")
    }
    from(junitPlatform111) {
        into("$pluginNameAsString/lib/ifc-junit-jupiter-api/5.11.0")
    }
    from(junitPlatform110) {
        into("$pluginNameAsString/lib/ifc-junit-jupiter-api/5.10.0")
    }
}

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    test {
        useJUnitPlatform()
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
