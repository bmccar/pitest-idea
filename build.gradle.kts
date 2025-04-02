plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.17.4"
}

group = "org.pitestidea"
version = "1.0.0"

repositories {
  mavenCentral()
}

val pitVersion = "1.15.8"
val pitJunit5PluginVersion = "1.2.1"

tasks.test {
    useJUnitPlatform()
}

dependencies {
  implementation("org.pitest:pitest-command-line:$pitVersion")
  implementation("org.pitest:pitest-entry:$pitVersion")
  implementation("org.pitest:pitest:$pitVersion")
  implementation("org.pitest:pitest-junit5-plugin:$pitJunit5PluginVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
  testImplementation("org.mockito:mockito-core:5.16.1")
  testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
}

// See https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2024.1.7")
  plugins.set(listOf("com.intellij.java"))
}

tasks {
  buildSearchableOptions {
    enabled = false
  }

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set("241")
    untilBuild.set("243.*")
  }
}
