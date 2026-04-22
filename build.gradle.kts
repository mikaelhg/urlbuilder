import org.gradle.kotlin.dsl.spotbugsMain

plugins {
    java
    `java-library`
    `maven-publish`
    jacoco
    id("com.github.spotbugs") version "6.5.1"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.cucumber:cucumber-java:7.34.3")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.34.3")
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "io.mikael"
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
        xml.required = true
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
        pom {
            name = "urlbuilder"
            description = "URL builder with zero runtime dependencies"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "mikaelhg"
                    name = "Mikael Gueck"
                    email = "gumi@iki.fi"
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mikaelhg/urlbuilder")
            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") as String?
                    ?: System.getenv("GPR_TOKEN")
            }
        }
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
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
    outputs.upToDateWhen { false }
}
