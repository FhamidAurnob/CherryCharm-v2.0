package com.example.cherrycharm.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

private val taka: NumberFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

/** ৳1,250 — Bangladeshi Taka, grouped the way customers read it. */
fun Double.toTaka(): String = "৳" + taka.format(this.roundToLong())

fun Long.toDate(pattern: String = "dd MMM yyyy"): String =
    if (this <= 0L) "" else SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

fun Long.toDateTime(): String = toDate("dd MMM yyyy, hh:mm a")

fun Long.timeAgo(now: Long = System.currentTimeMillis()): String {
    val diff = now - this
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> toDate("dd MMM")
    }
}

fun String.shortId(): String = take(8).uppercase()

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(trim()).matches()

/** Bangladeshi mobile: 01XXXXXXXXX (11 digits), optional +88 prefix. */
fun String.isValidBdPhone(): Boolean =
    Regex("^(\\+?88)?01[3-9]\\d{8}$").matches(trim().replace(" ", "").replace("-", ""))
