package com.example.alphakids.domain

import com.example.alphakids.domain.common.StringNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringNormalizerAdditionalTest {

    @Test
    fun `normalize trims extra spaces`() {
        val normalized = StringNormalizer.normalize("  hola   mundo  ")
        assertEquals("hola mundo", normalized)
    }

    @Test
    fun `normalize handles empty string`() {
        val normalized = StringNormalizer.normalize("")
        assertEquals("", normalized)
    }

    @Test
    fun `matches returns false when different`() {
        assertFalse(StringNormalizer.matches("perro", "gato"))
    }

    @Test
    fun `matches ignores diacritics`() {
        assertTrue(StringNormalizer.matches("niño", "nino"))
    }
}
