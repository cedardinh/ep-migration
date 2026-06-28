package com.demo.server.epmigration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.demo.server.epmigration",
        "io.cryptoblk.migration.listener"
    ]
)
class EpMigrationApplication

fun main(args: Array<String>) {
    runApplication<EpMigrationApplication>(*args)
}
