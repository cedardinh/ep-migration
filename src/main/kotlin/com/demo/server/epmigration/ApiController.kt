package com.demo.server.epmigration

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiController {

    @GetMapping("/ping")
    fun ping(): Map<String, String> {
        return mapOf(
            "status" to "success",
            "message" to "ep-migration api is running"
        )
    }
}
