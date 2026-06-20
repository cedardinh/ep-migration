package com.demo.server.epmigration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EpMigrationApplication

fun main(args: Array<String>) {
    runApplication<EpMigrationApplication>(*args)
}
