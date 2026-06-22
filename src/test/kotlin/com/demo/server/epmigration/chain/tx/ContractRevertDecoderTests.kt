package com.demo.server.epmigration.chain.tx

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger

class ContractRevertDecoderTests {
    @Test
    fun `returns null when revert data has no usable selector`() {
        assertNull(ContractRevertDecoder.decode(null))
        assertNull(ContractRevertDecoder.decode("execution reverted"))
        assertNull(ContractRevertDecoder.decode("0x123456"))
    }

    @Test
    fun `extracts first hex value from candidate messages`() {
        assertEquals("0x1234abcd", ContractRevertDecoder.extractHex("no hex here", "data=0x1234abcd;"))
    }

    @Test
    fun `decoded revert exposes value semantics`() {
        val revert = DecodedContractRevert("Error", "0x08c379a0", "boom", "0xdead")
        val same = DecodedContractRevert("Error", "0x08c379a0", "boom", "0xdead")
        val different = revert.copy(data = "0xbeef")

        assertEquals(same, revert)
        assertEquals(same.hashCode(), revert.hashCode())
        assertEquals("Error", revert.component1())
        assertEquals("0xdead", revert.component4())
        assertEquals("0xbeef", different.data)
        assertTrue(revert.toString().contains("0x08c379a0"))
    }

    @Test
    fun `renders decoded revert without business argument names`() {
        assertEquals(
            "Error(boom)",
            DecodedContractRevert("Error", "0x08c379a0", "boom", "0xdead").toLogMessage()
        )
        assertEquals(
            "CustomError(selector=0x12345678)",
            ContractRevertDecoder.decode("0x12345678")!!.toLogMessage()
        )
        assertEquals(
            "CustomError(selector=0x12345678)",
            DecodedContractRevert("CustomError", "0x12345678", "", "0x12345678").toLogMessage()
        )
    }

    @Test
    fun `decodes standard Error string and malformed Error string`() {
        val decoded = ContractRevertDecoder.decode(calldata("Error(string)", Utf8String("bad input")))
        val malformed = ContractRevertDecoder.decode("0x08c379a0${"20".padStart(64, '0')}")
        val truncated = ContractRevertDecoder.decode(
            "0x08c379a0" +
                "20".padStart(64, '0') +
                "04".padStart(64, '0') +
                "ff"
        )

        assertNotNull(decoded)
        assertEquals("Error", decoded!!.kind)
        assertEquals("0x08c379a0", decoded.selector)
        assertEquals("bad input", decoded.value)
        assertEquals("<malformed string>", malformed!!.value)
        assertEquals("<malformed string>", truncated!!.value)
    }

    @Test
    fun `decodes standard Panic code`() {
        val decoded = ContractRevertDecoder.decode(calldata("Panic(uint256)", Uint256(BigInteger.valueOf(17L))))
        val missingPayload = ContractRevertDecoder.decode("0x4e487b71")

        assertNotNull(decoded)
        assertEquals("Panic", decoded!!.kind)
        assertEquals("0x4e487b71", decoded.selector)
        assertEquals("17", decoded.value)
        assertEquals("0", missingPayload!!.value)
    }

    @Test
    fun `custom error keeps only selector and raw data`() {
        val data = calldata("AnyDomainError(string)", Utf8String("domain-specific-value"))
        val decoded = ContractRevertDecoder.decode(data)

        assertNotNull(decoded)
        assertEquals("CustomError", decoded!!.kind)
        assertEquals(selector("AnyDomainError(string)"), decoded.selector)
        assertNull(decoded.value)
        assertEquals(data.toLowerCase(), decoded.data)
    }

    private fun selector(signature: String): String {
        return Hash.sha3String(signature).substring(0, 10)
    }

    private fun calldata(signature: String, vararg args: Type<*>): String {
        return selector(signature) + Numeric.cleanHexPrefix(FunctionEncoder.encodeConstructor(args.toList()))
    }
}
