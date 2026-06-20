package com.demo.server.epmigration

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "ep.chain.lifecycle-contract-address=0x0000000000000000000000000000000000000001",
        "ep.chain.signer-private-key=0x0000000000000000000000000000000000000000000000000000000000000001"
    ]
)
class EpMigrationApplicationTests {

    @Test
    fun contextLoads() {
    }

}
