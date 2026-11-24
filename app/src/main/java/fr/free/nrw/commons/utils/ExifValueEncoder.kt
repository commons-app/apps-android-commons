package fr.free.nrw.commons.utils

import timber.log.Timber
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles encoding string values into EXIF binary formats and reading
 * unsigned multi-byte integers from [RandomAccessFile].
 */
object ExifValueEncoder {
    const val TYPE_BYTE = 1
    const val TYPE_ASCII = 2
    const val TYPE_SHORT = 3
    const val TYPE_LONG = 4
    const val TYPE_RATIONAL = 5
    const val TYPE_UNDEFINED = 7
    const val TYPE_SLONG = 9
    const val TYPE_SRATIONAL = 10

    const val SHORT_SIZE_BYTES = 2
    const val LONG_SIZE_BYTES = 4
    const val RATIONAL_SIZE_BYTES = 8 // 4-byte numerator + 4-byte denominator

    /**
     * Encodes a string value into a byte array matching the given EXIF
     * [type], using [order] for multi-byte integers.
     *
     * @param type EXIF field type constant.
     * @param value string value to encode.
     * @param order byte order for multi-byte values.
     * @return encoded byte array, or null if encoding fails.
     */
    fun encodeValue(
        type: Int,
        value: String,
        order: ByteOrder,
    ): ByteArray? =
        try {
            when (type) {
                TYPE_BYTE ->
                    byteArrayOf(value.toInt().toByte())

                TYPE_ASCII ->
                    "$value\u0000".toByteArray(Charsets.US_ASCII)

                TYPE_SHORT ->
                    ByteBuffer
                        .allocate(SHORT_SIZE_BYTES)
                        .order(order)
                        .putShort(value.toInt().toShort())
                        .array()

                TYPE_LONG ->
                    ByteBuffer
                        .allocate(LONG_SIZE_BYTES)
                        .order(order)
                        .putInt(value.toLong().toInt())
                        .array()

                TYPE_RATIONAL, TYPE_SRATIONAL ->
                    encodeRationals(value, order)

                TYPE_SLONG ->
                    ByteBuffer
                        .allocate(LONG_SIZE_BYTES)
                        .order(order)
                        .putInt(value.toInt())
                        .array()

                TYPE_UNDEFINED ->
                    value.toByteArray(Charsets.US_ASCII)

                else -> null
            }
        } catch (e: NumberFormatException) {
            Timber.w("Cannot encode value '%s' for EXIF type %d", value, type)
            null
        }

    /**
     * Encodes comma-separated rational pairs (e.g. "num/den,num2/den2") into bytes.
     *
     * @param value comma-separated rational fraction string.
     * @param order byte order for integers.
     * @return encoded byte array, or null if any fraction is invalid.
     */
    private fun encodeRationals(
        value: String,
        order: ByteOrder,
    ): ByteArray? {
        val parts = value.split(",")
        val buf = ByteBuffer.allocate(parts.size * RATIONAL_SIZE_BYTES).order(order)
        for (part in parts) {
            val nd = part.trim().split("/")
            if (nd.size != 2) return null
            buf.putInt(nd[0].trim().toInt())
            buf.putInt(nd[1].trim().toInt())
        }
        return buf.array()
    }

    /**
     * Returns the size in bytes of one unit of the given EXIF [type].
     *
     * @param type EXIF field type constant.
     * @return size in bytes for a single unit of the type.
     */
    fun typeUnitSize(type: Int): Int =
        when (type) {
            TYPE_BYTE, TYPE_ASCII, TYPE_UNDEFINED -> 1
            TYPE_SHORT -> SHORT_SIZE_BYTES
            TYPE_LONG, TYPE_SLONG -> LONG_SIZE_BYTES
            TYPE_RATIONAL, TYPE_SRATIONAL -> RATIONAL_SIZE_BYTES
            else -> 1
        }

    /**
     * Reads an unsigned 16-bit integer from [raf] in the specified [order].
     *
     * @param raf random access file to read from.
     * @param order byte order.
     * @return unsigned 16-bit integer value.
     */
    @Suppress("MagicNumber") // Standard bit manipulation
    fun readU16(
        raf: RandomAccessFile,
        order: ByteOrder,
    ): Int {
        val b1 = raf.readUnsignedByte()
        val b2 = raf.readUnsignedByte()
        return if (order == ByteOrder.LITTLE_ENDIAN) {
            (b2 shl 8) or b1
        } else {
            (b1 shl 8) or b2
        }
    }

    /**
     * Reads an unsigned 32-bit integer from [raf] in the specified [order].
     *
     * @param raf random access file to read from.
     * @param order byte order.
     * @return unsigned 32-bit integer value.
     */
    @Suppress("MagicNumber") // Standard bit manipulation
    fun readU32(
        raf: RandomAccessFile,
        order: ByteOrder,
    ): Int {
        val b1 = raf.readUnsignedByte()
        val b2 = raf.readUnsignedByte()
        val b3 = raf.readUnsignedByte()
        val b4 = raf.readUnsignedByte()
        return if (order == ByteOrder.LITTLE_ENDIAN) {
            (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
        } else {
            (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }
    }
}
