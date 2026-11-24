package fr.free.nrw.commons.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unit tests for [ExifValueEncoder].
 */
class ExifValueEncoderTest {
    @Test
    fun testEncodeByte() {
        val result =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_BYTE,
                "42",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals(byteArrayOf(42), result)
    }

    @Test
    fun testEncodeAscii() {
        val result =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_ASCII,
                "Commons",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals("Commons\u0000".toByteArray(Charsets.US_ASCII), result)
    }

    @Test
    fun testEncodeShort() {
        val resultLittle =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_SHORT,
                "258",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals(byteArrayOf(0x02, 0x01), resultLittle)

        val resultBig =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_SHORT,
                "258",
                ByteOrder.BIG_ENDIAN,
            )
        assertArrayEquals(byteArrayOf(0x01, 0x02), resultBig)
    }

    @Test
    fun testEncodeLongAndSlong() {
        val expected =
            ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(65538)
                .array()
        val resultLong =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_LONG,
                "65538",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals(expected, resultLong)

        val resultSlong =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_SLONG,
                "65538",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals(expected, resultSlong)
    }

    @Test
    fun testEncodeRational() {
        val result =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_RATIONAL,
                "1/100, 2/200",
                ByteOrder.BIG_ENDIAN,
            )
        val expected =
            ByteBuffer
                .allocate(16)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(1)
                .putInt(100)
                .putInt(2)
                .putInt(200)
                .array()
        assertArrayEquals(expected, result)
    }

    @Test
    fun testEncodeRationalInvalidFormat() {
        val invalidParts =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_RATIONAL,
                "1/2/3",
                ByteOrder.BIG_ENDIAN,
            )
        assertNull(invalidParts)

        val nonNumber =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_RATIONAL,
                "abc/def",
                ByteOrder.BIG_ENDIAN,
            )
        assertNull(nonNumber)
    }

    @Test
    fun testEncodeUndefined() {
        val result =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_UNDEFINED,
                "RawData",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertArrayEquals("RawData".toByteArray(Charsets.US_ASCII), result)
    }

    @Test
    fun testEncodeInvalidNumberFormat() {
        val result =
            ExifValueEncoder.encodeValue(
                ExifValueEncoder.TYPE_SHORT,
                "not_a_number",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertNull(result)
    }

    @Test
    fun testEncodeUnknownType() {
        val result =
            ExifValueEncoder.encodeValue(
                999,
                "value",
                ByteOrder.LITTLE_ENDIAN,
            )
        assertNull(result)
    }

    @Test
    fun testTypeUnitSize() {
        assertEquals(1, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_BYTE))
        assertEquals(1, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_ASCII))
        assertEquals(1, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_UNDEFINED))
        assertEquals(2, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_SHORT))
        assertEquals(4, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_LONG))
        assertEquals(4, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_SLONG))
        assertEquals(8, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_RATIONAL))
        assertEquals(8, ExifValueEncoder.typeUnitSize(ExifValueEncoder.TYPE_SRATIONAL))
        assertEquals(1, ExifValueEncoder.typeUnitSize(999))
    }

    @Test
    fun testReadU16AndU32() {
        val tempFile = File.createTempFile("u_test_", ".bin")
        try {
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.write(byteArrayOf(0x12, 0x34, 0x56, 0x78))
                raf.seek(0)
                assertEquals(0x3412, ExifValueEncoder.readU16(raf, ByteOrder.LITTLE_ENDIAN))
                raf.seek(0)
                assertEquals(0x1234, ExifValueEncoder.readU16(raf, ByteOrder.BIG_ENDIAN))

                raf.seek(0)
                assertEquals(0x78563412.toInt(), ExifValueEncoder.readU32(raf, ByteOrder.LITTLE_ENDIAN))
                raf.seek(0)
                assertEquals(0x12345678, ExifValueEncoder.readU32(raf, ByteOrder.BIG_ENDIAN))
            }
        } finally {
            tempFile.delete()
        }
    }
}
