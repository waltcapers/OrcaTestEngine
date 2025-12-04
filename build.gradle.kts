plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.gm.research.orcatestengine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")
    implementation("com.squareup.okio:okio:3.7.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.networknt:json-schema-validator:1.0.83")

    implementation("org.slf4j:slf4j-nop:2.0.7")

    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.shadowJar {
    archiveBaseName.set("orca")
    archiveClassifier.set("all")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "orca.cli.OrcaCLI"
    }
}

tasks.register<Exec>("jpackage") {
    dependsOn(tasks.shadowJar)

    val outputDir = "${project.buildDir}/jpackage"
    val inputJar = "${project.projectDir}/build/libs/orca-all.jar"
    val mainClass = "orca.cli.OrcaCLI"

    val appName = "Orca"
    val vendorName = "Wally Works"
    val version = "1.0"

    val jpackageTool = "${System.getenv("JAVA_HOME")}/bin/jpackage"

    doFirst {
        file(outputDir).mkdirs()
    }

    commandLine(
        jpackageTool,
        "--type", "app-image",
        "--input", "${project.projectDir}/build/libs",
        "--main-jar", "orca-all.jar",
        "--main-class", mainClass,
        "--name", appName,
        "--vendor", vendorName,
        "--app-version", version,
        "--dest", outputDir,
        "--icon", "icon.icns" // OPTIONAL, macOS icon
    )
}

