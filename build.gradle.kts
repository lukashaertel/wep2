import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gdxVersion = "1.9.10"

plugins {
    kotlin("jvm") version "1.3.50"
}

group = "eu.metatools"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    compile("org.jgroups:jgroups:4.1.8.Final")
    compile("com.google.guava:guava:28.1-jre")

    compile("com.badlogicgames.gdx:gdx:$gdxVersion")
    compile("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    compile("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.esotericsoftware:kryo:5.0.0-RC4")
    testImplementation("junit:junit:4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}