import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project

val graalAnnotationsVersion: String by project

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
    maven { url = uri("https://dl.bintray.com/palantir/releases/") }
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    compile("io.ktor:ktor-server-cio:$ktor_version")
    runtime("org.slf4j:slf4j-simple:1.7.26")
    compile("io.ktor:ktor-server-core:$ktor_version")
    compile("io.ktor:ktor-server-host-common:$ktor_version")
    testCompile("io.ktor:ktor-server-tests:$ktor_version") {
        exclude(group = "ch.qos.logback")
    }
    //Jackson
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")

    //consider Rocker for templates


    //Test libraries
    testImplementation("io.strikt:strikt-core:0.21.1")

    //Integration Test Libs
    testImplementation("io.rest-assured:rest-assured:3.0.0")
    testImplementation("uk.co.datumedge:hamcrest-json:0.2")


    //Consider requery as orm

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.0")


    //Graalvm support
//    testImplementation("org.kohsuke:akuma:1.10")
//    annotationProcessor("com.palantir.graal.annotations:graal-annotations-processors:$graalAnnotationsVersion")
//    compileOnly("com.palantir.graal.annotations:graal-annotations-api:$graalAnnotationsVersion")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")


val generateGraalConfig: String? by project
val runningOnPort: String? by project

tasks {
    test {
        useJUnitPlatform()
        testLogging.showExceptions = true

        if (generateGraalConfig != null && !isOnGraalVm()) {
            throw Exception("Generating Graal config while running on a different JVM (Not Graal) is not recommended! Exiting now.")
        }

        if (generateGraalConfig != null) {
            println("Generating Graal Reflection Config Files")

            systemProperties.putAll(
                listOf(
                    "runServerInSeparateProcess" to "true",
                    "configOutputDir" to "${project.rootDir}/native-image-build/generated-config"
                )
            )

            if(runningOnPort != null) {
                systemProperties.put("runningOnPort", "8080");
            }

        }
    }
}


task("isOnGraalVm") {
    if (isOnGraalVm()) {
        println("JVM is ${getJvmName()}")
    } else {
        println("JVM is NOT Graal. JVM is ${getJvmName()}")
        throw Exception("In order to generate the config needed for a native image, ")
    }
}

fun getJvmName(): String {
    return System.getProperty("java.vm.name")
}

fun isOnGraalVm(): Boolean {
    return getJvmName().contains("Graal")
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
        attributes("Main-Class" to "com.claimsy.app.Main")
    }

    destinationDirectory.set(project.buildDir.resolve("fatjar"))
    archiveFileName.set("application.jar")
}

tasks.named("assemble").configure {
    dependsOn(fatjar)
}
