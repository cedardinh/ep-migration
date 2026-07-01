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

springBoot {
    mainClassName = "com.demo.server.epmigration.EpMigrationApplicationKt"
}

extra["spring-framework.version"] = "5.2.19.RELEASE"
extra["web3j.version"] = "4.9.8"
extra["okhttp3.version"] = "4.9.0"

repositories {
    mavenCentral()
}

val web3jCodegen by configurations.creating

data class Web3jWrapperSpec(
    val contractName: String,
    val artifactPath: String
)

val web3jWrapperSpecs = listOf(
    Web3jWrapperSpec("TopazLifecycle", "contracts/artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json"),
    Web3jWrapperSpec("TopazPayment", "contracts/artifacts/contracts/TopazPayment.sol/TopazPayment.json"),
    Web3jWrapperSpec("TopazContacts", "contracts/artifacts/contracts/TopazContacts.sol/TopazContacts.json"),
    Web3jWrapperSpec("TopazAccessControl", "contracts/artifacts/contracts/TopazAccessControl.sol/TopazAccessControl.json")
)

val generatedWeb3jInputRoot = file("$buildDir/generated/web3j")

fun executableFromPath(name: String): String {
    val override = System.getenv("${name.toUpperCase()}_EXECUTABLE")
    if (!override.isNullOrBlank()) {
        return override
    }
    val pathCandidates = System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.map { File(it, name) }
        ?: emptyList()
    val fallbackCandidates = listOf(
        File("/opt/homebrew/bin", name),
        File("/usr/local/bin", name),
        File("/usr/bin", name)
    )
    return (pathCandidates + fallbackCandidates)
        .firstOrNull { it.canExecute() }
        ?.absolutePath
        ?: name
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
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

val compileTopazContracts = tasks.register<Exec>("compileTopazContracts") {
    val contractSources = fileTree("contracts/contracts") {
        include("**/*.sol")
    }

    description = "Compiles Solidity contracts with Hardhat so web3j can generate wrappers from ABI/bin."
    group = "build"
    workingDir = file("contracts")
    commandLine(executableFromPath("npm"), "run", "compile")
    inputs.files(contractSources)
    inputs.file("contracts/hardhat.config.js")
    inputs.file("contracts/package.json")
    inputs.file("contracts/package-lock.json")
    outputs.files(web3jWrapperSpecs.map { file(it.artifactPath) })
}

val prepareTopazContractAbiBin = tasks.register("prepareTopazContractAbiBin") {
    description = "Extracts ABI and BIN files from Hardhat artifacts for web3j codegen."
    group = "build"
    dependsOn(compileTopazContracts)
    inputs.files(web3jWrapperSpecs.map { file(it.artifactPath) })
    outputs.dir(generatedWeb3jInputRoot)

    doLast {
        web3jWrapperSpecs.forEach { spec ->
            val artifactFile = file(spec.artifactPath)
            val artifact = JsonSlurper().parse(artifactFile) as Map<*, *>
            val generatedInputDir = file("$generatedWeb3jInputRoot/${spec.contractName}")
            val abiFile = file("$generatedInputDir/${spec.contractName}.abi")
            val binFile = file("$generatedInputDir/${spec.contractName}.bin")
            val bytecode = (artifact["bytecode"] as? String)
                ?.removePrefix("0x")
                ?.takeIf { it.isNotBlank() }

            generatedInputDir.mkdirs()
            abiFile.writeText(JsonOutput.toJson(artifact["abi"]))
            if (bytecode != null) {
                binFile.writeText(bytecode)
            } else if (binFile.exists()) {
                binFile.delete()
            }
        }
    }
}

val generateTopazContractWrappers = tasks.register("generateTopazContractWrappers") {
    description = "Generates web3j Java wrappers for Topaz contracts that expose events."
    group = "build"
    dependsOn(prepareTopazContractAbiBin)
    inputs.dir(generatedWeb3jInputRoot)
    outputs.files(web3jWrapperSpecs.map {
        file("src/main/java/com/demo/server/epmigration/chain/generated/${it.contractName}.java")
    })

    doLast {
        web3jWrapperSpecs.forEach { spec ->
            val generatedInputDir = file("$generatedWeb3jInputRoot/${spec.contractName}")
            val abiFile = file("$generatedInputDir/${spec.contractName}.abi")
            val binFile = file("$generatedInputDir/${spec.contractName}.bin")
            val bytecode = binFile.takeIf { it.exists() && it.readText().isNotBlank() }

            val generatorArgs = mutableListOf<String>()
            if (bytecode != null) {
                generatorArgs.add("-b")
                generatorArgs.add(binFile.absolutePath)
            }
            generatorArgs.addAll(
                listOf(
                    "-a",
                    abiFile.absolutePath,
                    "-o",
                    file("src/main/java").absolutePath,
                    "-p",
                    "com.demo.server.epmigration.chain.generated",
                    "-c",
                    spec.contractName
                )
            )

            javaexec {
                classpath = web3jCodegen
                main = "org.web3j.codegen.SolidityFunctionWrapperGenerator"
                args = generatorArgs
            }
        }
    }
}

val generateTopazLifecycleWrapper = tasks.register("generateTopazLifecycleWrapper") {
    description = "Compatibility alias for generating all Topaz web3j wrappers."
    group = "build"
    dependsOn(generateTopazContractWrappers)
}

tasks.named("compileKotlin") {
    dependsOn(generateTopazContractWrappers)
}

tasks.named("compileJava") {
    dependsOn(generateTopazContractWrappers)
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
