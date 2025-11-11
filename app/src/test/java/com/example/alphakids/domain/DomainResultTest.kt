package com.example.alphakids.domain

import com.example.alphakids.data.common.FirebaseExceptionMapper
import com.example.alphakids.domain.common.DomainResult
import com.example.alphakids.domain.common.FirebaseDomainError
import com.example.alphakids.domain.common.domainResultOf
import com.example.alphakids.domain.common.StringNormalizer
import com.google.firebase.FirebaseNetworkException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DomainResultTest {

    @Test
    fun `domainResultOf wraps success`() {
        val result = domainResultOf(FirebaseExceptionMapper::toDomainError) { 42 }
        assertIs<DomainResult.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `domainResultOf maps firebase exception`() {
        val result = domainResultOf(FirebaseExceptionMapper::toDomainError) {
            throw FirebaseNetworkException("network")
        }
        val error = (result as DomainResult.Error).error
        assertTrue(error is FirebaseDomainError.Network)
    }

    @Test
    fun `string normalizer removes accents`() {
        val normalized = StringNormalizer.normalize("Árbol")
        assertEquals("arbol", normalized)
    }

    @Test
    fun `string normalizer matches ignoring case`() {
        assertTrue(StringNormalizer.matches("Café", "cafe"))
    }

    @Test
    fun `domainResultOf propagates unknown error`() {
        val result = domainResultOf(FirebaseExceptionMapper::toDomainError) {
            throw IllegalStateException("boom")
        }
        assertIs<DomainResult.Error>(result)
    }
}
