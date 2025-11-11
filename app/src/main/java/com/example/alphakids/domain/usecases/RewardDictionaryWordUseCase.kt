package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.DictionaryRepository
import javax.inject.Inject

class RewardDictionaryWordUseCase @Inject constructor(
    private val repository: DictionaryRepository
) {
    suspend operator fun invoke(
        studentId: String,
        teacherId: String,
        wordId: String,
        rewardCoins: Int
    ): Result<Unit> {
        return repository.rewardStudentForWord(studentId, teacherId, wordId, rewardCoins)
    }
}
