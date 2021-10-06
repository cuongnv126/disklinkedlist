plugins {
    kotlin("jvm") version "1.5.31"
    java

    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version ("1.1.0")
}

apply(from = "${rootDir}/lib-publish.gradle")
apply(from = "${rootDir}/scripts/publish-root.gradle")
apply(from = "${rootDir}/scripts/publish-module.gradle")

group = "org.cuongnv.disklinkedlist"
version = "0.5.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.cuongnv.consoleformatter:consoleformatter:0.0.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}