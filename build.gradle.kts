import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.31")
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.31"
}

group = "com.github.liranai.Adjutant"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    jcenter()
    maven ( "https://jitpack.io")
}


apply(plugin = "kotlinx-serialization")

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.11.0")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.discord4j:discord4j-core:3.0.6")
    testCompile("junit", "junit", "4.12")
    implementation("com.sedmelluq:lavaplayer:1.3.17")
    ///implementation ("com.google.cloud:google-cloud-speech:1.6.0")
    implementation ("com.github.goxr3plus:java-google-speech-api:8.0.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = archiveVersion.get()
        attributes["Main-Class"] = "me.liranai.adjutant.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}