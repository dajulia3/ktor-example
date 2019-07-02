import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.40"
    kotlin("kapt") version "1.3.40"
    id("com.hpe.kraal") version "0.0.15" //add kraal to get ktor working in graalnative
}

group = "com.claimsy"
version = "0.0.1-SNAPSHOT"

application {
    mainClassName = "com.claimsy.app.ApplicationKt"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("io.ktor:ktor-server-cio:$ktor_version")
    implementation("org.slf4j:slf4j-simple:1.7.26")
    compile("io.ktor:ktor-server-core:$ktor_version")
    //consider Rocker for templates
    compile("io.ktor:ktor-server-host-common:$ktor_version")
    testCompile("io.ktor:ktor-server-tests:$ktor_version")

    //JSON - moshi support
    compile("com.squareup.moshi:moshi:1.8.0")
    implementation("com.squareup.moshi:moshi-adapters:1.8.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
    kaptTest("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
    compile("com.squareup.okio:okio:2.2.2")

    //Test libraries
    testImplementation("io.strikt:strikt-core:0.21.1")

    //Integration Test Libs
    testImplementation("io.rest-assured:rest-assured:3.0.0")
    testImplementation("uk.co.datumedge:hamcrest-json:0.2")

    //Consider requery as orm

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.0")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")


tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += listOf("-Xuse-experimental=kotlin.Experimental", "-progressive")
}
//tasks.withType<KotlinCompile>().configureEach {
//    kotlinOptions {
//        jvmTarget = "1.8"
//        // need use-experimental for Ktor CIO
//        freeCompilerArgs += listOf("-Xuse-experimental=kotlin.Experimental", "-progressive")
//        // disable -Werror with: ./gradlew -PwarningsAsErrors=false
//        allWarningsAsErrors = project.findProperty("warningsAsErrors") != "false"
//    }
//}

val fatjar by tasks.creating(Jar::class) {

    from(kraal.outputZipTrees) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    manifest {
        attributes("Main-Class" to "com.claimsy.app.ApplicationKt")
    }

    destinationDirectory.set(project.buildDir.resolve("fatjar"))
    archiveFileName.set("example.jar")
}

tasks.named("assemble").configure {
    dependsOn(fatjar)
}