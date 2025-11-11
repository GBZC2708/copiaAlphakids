package com.example.alphakids.domain.repository

import com.example.alphakids.domain.models.TeacherDictionaryWord
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun observePendingWords(studentId: String, teacherId: String): Flow<List<TeacherDictionaryWord>>
    suspend fun rewardStudentForWord(studentId: String, teacherId: String, wordId: String, rewardCoins: Int): Result<Unit>
}
