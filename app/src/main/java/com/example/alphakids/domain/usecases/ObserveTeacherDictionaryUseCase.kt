package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.TeacherDictionaryWord
import com.example.alphakids.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTeacherDictionaryUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    operator fun invoke(studentId: String, teacherId: String): Flow<List<TeacherDictionaryWord>> {
        return repository.observePendingWords(studentId, teacherId)
    }
}
