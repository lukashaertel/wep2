import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gdxVersion = "1.9.10"

plugins {
    kotlin("jvm") version "1.4.31"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.esotericsoftware:kryo:5.0.4")
    implementation ("com.maltaisn:msdf-gdx:0.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.6")

    implementation(project(":up"))
    implementation(project(":fio"))
    implementation(project(":fig"))

    implementation(files("libs/reaktor-1.0-SNAPSHOT.jar"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}