package com.example.alphakids.domain.common

sealed interface DomainError {
    val message: String
    val cause: Throwable?
}

sealed class FirebaseDomainError(
    final override val message: String,
    final override val cause: Throwable? = null
) : DomainError {
    data object Network : FirebaseDomainError("Error de red, intenta de nuevo.")
    data object Authentication : FirebaseDomainError("Error de autenticación.")
    data object PermissionDenied : FirebaseDomainError("Permiso denegado para la operación solicitada.")
    data object Unavailable : FirebaseDomainError("Servicio temporalmente no disponible.")
    data class Unknown(
        override val cause: Throwable?,
        val rawMessage: String?
    ) : FirebaseDomainError(rawMessage ?: "Error desconocido")
}

data class ValidationError(
    override val message: String,
    override val cause: Throwable? = null,
    val field: String? = null
) : DomainError

data class UnexpectedError(
    override val message: String,
    override val cause: Throwable? = null
) : DomainError

sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Error(val error: DomainError) : DomainResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): DomainResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (DomainError) -> Unit): DomainResult<T> {
        if (this is Error) action(error)
        return this
    }
}

inline fun <T> domainResultOf(
    crossinline mapper: (Throwable) -> DomainError,
    crossinline block: () -> T
): DomainResult<T> = try {
    DomainResult.Success(block())
} catch (throwable: Throwable) {
    DomainResult.Error(mapper(throwable))
}
