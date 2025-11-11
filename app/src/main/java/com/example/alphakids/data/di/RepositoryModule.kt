package com.example.alphakids.data.di

import com.example.alphakids.data.diagnostics.DiagnosticsRepositoryImpl
import com.example.alphakids.data.firebase.repository.AchievementRepositoryImpl
import com.example.alphakids.data.firebase.repository.AssignmentRepositoryImpl
import com.example.alphakids.data.firebase.repository.AuthRepositoryImpl
import com.example.alphakids.data.firebase.repository.DictionaryRepositoryImpl
import com.example.alphakids.data.firebase.repository.StoreRepositoryImpl
import com.example.alphakids.data.firebase.repository.StudentRepositoryImpl
import com.example.alphakids.data.firebase.repository.TeacherRepositoryImpl
import com.example.alphakids.data.firebase.repository.WordRepositoryImpl
import com.example.alphakids.data.firebase.FirestoreTransactionHelper
import com.example.alphakids.data.tts.AndroidTtsService
import com.example.alphakids.data.tts.TtsService
import com.example.alphakids.domain.repository.AchievementRepository
import com.example.alphakids.domain.repository.DiagnosticsRepository
import com.example.alphakids.domain.repository.AssignmentRepository
import com.example.alphakids.domain.repository.AuthRepository
import com.example.alphakids.domain.repository.DictionaryRepository
import com.example.alphakids.domain.repository.StoreRepository
import com.example.alphakids.domain.repository.StudentRepository
import com.example.alphakids.domain.repository.TeacherRepository
import com.example.alphakids.domain.repository.WordRepository
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth, db: FirebaseFirestore): AuthRepository {
        return AuthRepositoryImpl(auth, db)
    }

    @Provides
    @Singleton
    fun provideStudentRepository(db: FirebaseFirestore): StudentRepository {
        return StudentRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideTeacherRepository(db: FirebaseFirestore): TeacherRepository {
        return TeacherRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideWordRepository(db: FirebaseFirestore): WordRepository {
        return WordRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideAssignmentRepository(db: FirebaseFirestore): AssignmentRepository {
        return AssignmentRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(db: FirebaseFirestore): AchievementRepository {
        return AchievementRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        db: FirebaseFirestore,
        transactionHelper: FirestoreTransactionHelper
    ): DictionaryRepository {
        return DictionaryRepositoryImpl(db, transactionHelper)
    }

    @Provides
    @Singleton
    fun provideStoreRepository(
        db: FirebaseFirestore,
        transactionHelper: FirestoreTransactionHelper
    ): StoreRepository {
        return StoreRepositoryImpl(db, transactionHelper)
    }

    @Provides
    @Singleton
    fun provideDiagnosticsRepository(
        firebaseApp: FirebaseApp,
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): DiagnosticsRepository {
        return DiagnosticsRepositoryImpl(firebaseApp, auth, firestore, storage)
    }

    @Provides
    @Singleton
    fun provideTtsService(
        @ApplicationContext context: Context
    ): TtsService {
        return AndroidTtsService(context)
    }

    @Provides
    @Singleton
    fun provideFirestoreTransactionHelper(
        db: FirebaseFirestore
    ): FirestoreTransactionHelper {
        return FirestoreTransactionHelper(db)
    }
}
