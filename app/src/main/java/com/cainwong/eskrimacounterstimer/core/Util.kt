package com.cainwong.eskrimacounterstimer.core

import kotlin.math.roundToInt

fun Int.bpmToMillis(): Int = (60000.toFloat() / this).roundToInt()
