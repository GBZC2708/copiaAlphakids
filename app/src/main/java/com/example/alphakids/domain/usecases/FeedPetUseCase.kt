package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.StoreRepository
import javax.inject.Inject

class FeedPetUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke(
        estudianteId: String,
        itemId: String,
        hungerDelta: Int,
        happinessDelta: Int
    ): Result<Unit> {
        return repository.feedPet(estudianteId, itemId, hungerDelta, happinessDelta)
    }
}
