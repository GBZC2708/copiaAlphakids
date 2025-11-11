package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.TeacherRepository
import javax.inject.Inject

class ObserveTeacherByIdUseCase @Inject constructor(
    private val repository: TeacherRepository
) {
    operator fun invoke(id: String) = repository.observeTeacherById(id)
}
