package com.example.alphakids.domain.repository

import com.example.alphakids.domain.models.Teacher
import kotlinx.coroutines.flow.Flow

interface TeacherRepository {
    fun observeTeachers(): Flow<List<Teacher>>
    fun observeTeacherById(id: String): Flow<Teacher?>
}
