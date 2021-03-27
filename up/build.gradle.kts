import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jgroups:jgroups:4.1.8.Final")
    implementation("com.google.guava:guava:28.1-jre")

    implementation("com.esotericsoftware:kryo:5.0.0-RC4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions {
        freeCompilerArgs = listOf("-Xinline-classes")
    }
}