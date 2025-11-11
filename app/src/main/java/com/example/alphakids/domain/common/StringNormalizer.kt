package com.example.alphakids.domain.common

import java.text.Normalizer

object StringNormalizer {
    fun normalize(input: String): String {
        return Normalizer.normalize(input.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
    }

    fun matches(expected: String, candidate: String): Boolean {
        return normalize(expected) == normalize(candidate)
    }
}
