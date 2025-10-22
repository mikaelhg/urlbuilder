import org.gradle.kotlin.dsl.spotbugsMain

plugins {
    java
    `java-library`
    `maven-publish`
    jacoco
    id("com.github.spotbugs") version "6.4.4"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.cucumber:cucumber-java:7.29.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.29.0")
    testImplementation("org.junit.platform:junit-platform-suite-engine:6.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
}

group = "io.mikael"
version = "2.0.10-SNAPSHOT"
description = "urlbuilder"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
