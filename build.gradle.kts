import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
    id("org.springframework.boot") version "2.3.12.RELEASE"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    jacoco
    kotlin("plugin.jpa") version "1.3.72"
    kotlin("plugin.allopen") version "1.3.72"
}

group = "com.demo.server"
version = "0.0.1-SNAPSHOT"
description = "ep-migration"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

extra["spring-framework.version"] = "5.2.19.RELEASE"
extra["web3j.version"] = "4.9.8"
extra["okhttp3.version"] = "4.9.0"

repositories {
    mavenCentral()
}

val web3jCodegen by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:2.2.2")
    implementation("org.web3j:core:${property("web3j.version")}")
    runtimeOnly("org.postgresql:postgresql")
    compileOnly("javax.persistence:javax.persistence-api")
    compileOnly("org.hibernate:hibernate-core")
    compileOnly("org.springframework.data:spring-data-jpa")
    compileOnly("org.springframework:spring-tx")
    web3jCodegen("org.web3j:codegen:${property("web3j.version")}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.register("generateTopazLifecycleWrapper") {
    val artifactFile = file("contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json")
    val abiFile = file("$buildDir/generated/web3j/TopazLifecycle.abi")
    val wrapperFile = file("src/main/java/com/demo/server/epmigration/chain/generated/TopazLifecycle.java")

    inputs.file(artifactFile)
    outputs.file(wrapperFile)

    doLast {
        val artifact = JsonSlurper().parse(artifactFile) as Map<*, *>
        abiFile.parentFile.mkdirs()
        abiFile.writeText(JsonOutput.toJson(artifact["abi"]))
        javaexec {
            classpath = web3jCodegen
            main = "org.web3j.codegen.SolidityFunctionWrapperGenerator"
            args = listOf(
                "-a",
                abiFile.absolutePath,
                "-o",
                file("src/main/java").absolutePath,
                "-p",
                "com.demo.server.epmigration.chain.generated",
                "-c",
                "TopazLifecycle"
            )
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests tagged with @Tag(\"integration\"). Requires local Docker Besu."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform {
        includeTags("integration")
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/demo/server/epmigration/chain/generated/**")
            }
        })
    )
    reports {
        xml.isEnabled = true
        html.isEnabled = true
        csv.isEnabled = false
    }
}
allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}
