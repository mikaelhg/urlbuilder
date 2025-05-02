import org.gradle.kotlin.dsl.spotbugsMain

plugins {
    java
    `java-library`
    `maven-publish`
    jacoco
    id("com.github.spotbugs") version "6.1.10"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.cucumber:cucumber-java:7.22.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.1") {
        exclude("org.junit.vintage:vintage-engine")
    }
    testImplementation("org.junit.platform:junit-platform-suite-engine:1.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
}

group = "io.mikael"
version = "2.0.10-SNAPSHOT"
description = "urlbuilder"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        csv.required = true
        html.required = true
    }
}

tasks.spotbugsMain {
    excludeFilter = file("spotbugs-exclude.xml")
    reports.create("html") {
        required.set(true)
        setStylesheet("plain.xsl")
    }
}

tasks.spotbugsTest {
    enabled = false
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
