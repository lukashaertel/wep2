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
    implementation("com.google.guava:guava:28.1-jre")

    implementation(project(":up"))
    implementation(project(":fio"))
    implementation(project(":fig"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}