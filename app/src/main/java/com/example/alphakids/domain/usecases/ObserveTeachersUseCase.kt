package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.TeacherRepository
import javax.inject.Inject

class ObserveTeachersUseCase @Inject constructor(
    private val repository: TeacherRepository
) {
    operator fun invoke() = repository.observeTeachers()
}
