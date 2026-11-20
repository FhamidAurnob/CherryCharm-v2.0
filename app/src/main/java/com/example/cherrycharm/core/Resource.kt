package com.example.cherrycharm.core

/**
 * State wrapper used by every repository call and screen.
 * [Idle] is for one-shot actions that have not started yet.
 */
sealed interface Resource<out T> {
    data object Idle : Resource<Nothing>
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String) : Resource<Nothing>
}

val <T> Resource<T>.dataOrNull: T?
    get() = (this as? Resource.Success)?.data

val Resource<*>.isLoading: Boolean get() = this is Resource.Loading

/** Runs [block] and wraps the result, turning exceptions into a readable [Resource.Error]. */
suspend inline fun <T> resourceOf(fallbackMessage: String, block: () -> T): Resource<T> =
    try {
        Resource.Success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(e.friendlyMessage(fallbackMessage))
    }

fun Throwable.friendlyMessage(fallback: String): String {
    val raw = localizedMessage ?: return fallback
    return when {
        "network" in raw.lowercase() || "unavailable" in raw.lowercase() ->
            "You seem to be offline. Check your connection and try again."
        "PERMISSION_DENIED" in raw -> "You don't have permission to do that."
        "password is invalid" in raw || "INVALID_LOGIN_CREDENTIALS" in raw ||
            "credential is incorrect" in raw -> "Incorrect email or password."
        "no user record" in raw -> "No account found with this email."
        "email address is already in use" in raw -> "An account with this email already exists."
        "badly formatted" in raw -> "Please enter a valid email address."
        "CONFIGURATION_NOT_FOUND" in raw ->
            "Sign-in isn't configured. Enable Email/Password in Firebase Console → Authentication."
        "requires recent authentication" in raw -> "For security, please enter your password again."
        else -> raw
    }
}
