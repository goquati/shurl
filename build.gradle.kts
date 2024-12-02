import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.1"
}

group = "io.github.goquati.shurl"
version = System.getenv("GIT_TAG_VERSION") ?: "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-forwarded-header")
    implementation("io.r2dbc:r2dbc-pool:1.0.2.RELEASE")
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

application {
    mainClass.set("io.github.goquati.shurl.MainKt")
}
ktor {
    fatJar {
        archiveFileName.set("shurl.jar")
    }
}
tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
