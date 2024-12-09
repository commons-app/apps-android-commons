package fr.free.nrw.commons.utils

fun interface TimeProvider {
    fun currentTimeMillis(): Long
}