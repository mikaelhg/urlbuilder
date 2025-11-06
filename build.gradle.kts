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
}
