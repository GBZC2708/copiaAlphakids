package com.example.alphakids.data.common

import com.example.alphakids.domain.common.DomainError
import com.example.alphakids.domain.common.FirebaseDomainError
import com.example.alphakids.domain.common.UnexpectedError
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

object FirebaseExceptionMapper {
    fun toDomainError(throwable: Throwable): DomainError {
        if (throwable !is FirebaseException) {
            return UnexpectedError(throwable.message ?: "Error desconocido", throwable)
        }

        return when (throwable) {
            is FirebaseNetworkException -> FirebaseDomainError.Network
            is FirebaseAuthInvalidCredentialsException -> FirebaseDomainError.Authentication
            is FirebaseAuthInvalidUserException -> FirebaseDomainError.Authentication
            is FirebaseAuthException -> FirebaseDomainError.Authentication
            is FirebaseFirestoreException -> when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirebaseDomainError.PermissionDenied
                FirebaseFirestoreException.Code.UNAVAILABLE -> FirebaseDomainError.Unavailable
                FirebaseFirestoreException.Code.ABORTED -> FirebaseDomainError.Unavailable
                else -> FirebaseDomainError.Unknown(throwable, throwable.message)
            }
            is StorageException -> when (throwable.errorCode) {
                StorageException.ERROR_NOT_AUTHORIZED -> FirebaseDomainError.PermissionDenied
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> FirebaseDomainError.Unavailable
                StorageException.ERROR_NETWORK_UNAVAILABLE -> FirebaseDomainError.Network
                else -> FirebaseDomainError.Unknown(throwable, throwable.message)
            }
            else -> FirebaseDomainError.Unknown(throwable, throwable.message)
        }
    }
}
