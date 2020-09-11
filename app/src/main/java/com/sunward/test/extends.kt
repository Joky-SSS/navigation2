package com.sunward.test

import android.view.View
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow

fun DoubleArray.pointWisePower(n: Int): DoubleArray {
    forEach { it.pow(n) }
    return this
}

fun Double.dms2deg(): Double {
    val negative = this < 0
    val absDms = if (negative) -this else this
    val degree = floor(absDms)
    val minute = floor((absDms - degree) * 100)
    val second = (absDms - degree - minute / 100) * 10000
    val value = degree + minute / 60 + second / 3600
    return if (negative) -value else value
}

fun Double.deg2dms(): Double {
    val negative = this < 0
    val absDegree = if (negative) -this else this
    val d = floor(absDegree)
    val m1 = (absDegree - d) * 60
    val m = floor(m1)
    val s = (m1 - m) * 60
    val dms = d + m / 100 + s / 10000
    return if (negative) -dms else dms
}

fun Int.deg2arc() = this * PI / 180.0

fun Double.deg2arc() = this * PI / 180.0

fun Double.rad2deg() = this * 180 / PI

fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun Double.fullString(): String = FORMATTER.format(this)

fun Double.roundFraction(scale: Int) =
    if (isInfinite() || isNaN()) 0.0 else BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP)
        .toDouble()

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

val FORMATTER = DecimalFormat().apply {
    maximumFractionDigits = 340
    maximumIntegerDigits = 309
    isGroupingUsed = false
    roundingMode = RoundingMode.UNNECESSARY
}
