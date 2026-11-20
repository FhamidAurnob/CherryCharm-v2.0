package com.example.cherrycharm.data.model

/*
 * Firestore stores whole numbers as Long and decimals as Double, and the admin
 * dashboard (TypeScript) writes either depending on the value. These helpers read
 * any numeric type safely, so the app never crashes or silently drops a field.
 * We map by hand instead of toObject() so field names like "isActive" match exactly.
 */

internal fun Any?.asDouble(default: Double = 0.0): Double = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull() ?: default
    else -> default
}

internal fun Any?.asDoubleOrNull(): Double? = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull()
    else -> null
}

internal fun Any?.asLong(default: Long = 0L): Long = when (this) {
    is Number -> toLong()
    is com.google.firebase.Timestamp -> toDate().time
    is String -> toLongOrNull() ?: default
    else -> default
}

internal fun Any?.asInt(default: Int = 0): Int = when (this) {
    is Number -> toInt()
    is String -> toIntOrNull() ?: default
    else -> default
}

internal fun Any?.asString(default: String = ""): String = (this as? String) ?: default

internal fun Any?.asBool(default: Boolean = false): Boolean = (this as? Boolean) ?: default

internal fun Any?.asStringList(): List<String> =
    (this as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

@Suppress("UNCHECKED_CAST")
internal fun Any?.asMap(): Map<String, Any?> = (this as? Map<String, Any?>) ?: emptyMap()

internal fun Any?.asMapList(): List<Map<String, Any?>> =
    (this as? List<*>)?.map { it.asMap() } ?: emptyList()
