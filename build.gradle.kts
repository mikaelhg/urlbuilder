plugins {
    java
    `java-library`
    `maven-publish`
    jacoco
    id("com.github.spotbugs") version "5.0.14"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.cucumber:cucumber-java:7.12.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.12.0") {
        exclude("org.junit.vintage:vintage-engine")
    }
    testImplementation("org.junit.platform:junit-platform-suite-engine:1.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

group = "io.mikael"
version = "2.0.10-SNAPSHOT"
description = "urlbuilder"
java.sourceCompatibility = JavaVersion.VERSION_1_8

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.jacocoTestReport {
    reports {
        csv.required.set(true)
    }
}

tasks.spotbugsMain {
    excludeFilter.set(file("spotbugs-exclude.xml"))
    reports.create("html") {
        required.set(true)
        outputLocation.set(file("$buildDir/reports/spotbugs.html"))
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
}
