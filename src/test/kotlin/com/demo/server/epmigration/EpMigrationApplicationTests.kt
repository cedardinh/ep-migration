package com.demo.server.epmigration

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "ep.chain.lifecycle-contract-address=0x0000000000000000000000000000000000000001",
        "ep.chain.payment-contract-address=0x0000000000000000000000000000000000000002",
        "ep.chain.contacts-contract-address=0x0000000000000000000000000000000000000003",
        "ep.chain.signer-private-key=0x0000000000000000000000000000000000000000000000000000000000000001",
        "ep.chain.listener-enabled=false"
    ]
)
class EpMigrationApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `main boots the application`() {
        main(
            arrayOf(
                "--spring.main.web-application-type=none",
                "--ep.chain.lifecycle-contract-address=0x0000000000000000000000000000000000000001",
                "--ep.chain.payment-contract-address=0x0000000000000000000000000000000000000002",
                "--ep.chain.contacts-contract-address=0x0000000000000000000000000000000000000003",
                "--ep.chain.signer-private-key=0x0000000000000000000000000000000000000000000000000000000000000001",
                "--ep.chain.listener-enabled=false"
            )
        )
    }

}
