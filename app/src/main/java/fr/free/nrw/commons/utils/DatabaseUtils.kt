package fr.free.nrw.commons.utils

import android.annotation.SuppressLint
import android.database.Cursor

fun Cursor.getStringArray(name: String): List<String> =
    stringToArray(getString(name))

@SuppressLint("Range")
fun Cursor.getString(name: String): String =
    getString(getColumnIndex(name))

/**
 * Converts string to List
 * @param listString comma separated single string from of list items
 * @return List of string
 */
fun stringToArray(listString: String?): List<String> {
    if (listString.isNullOrEmpty()) return emptyList();
    val elements = listString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return listOf(*elements)
}

/**
 * Converts string to List
 * @param list list of items
 * @return string comma separated single string of items
 */
fun arrayToString(list: List<String?>?): String? {
    return list?.joinToString(",")
}

