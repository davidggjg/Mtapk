package com.mtapk.core

import java.io.ByteArrayOutputStream

/**
 * Hand-rolled minimal Android Binary XML (AXML) encoder.
 *
 * This exists purely so the test suite can exercise apktool-lib's *real* binary
 * manifest decoder against real bytes, without needing to fetch a prebuilt sample
 * APK from the network (not available in this environment). It encodes exactly
 * one supported shape: `<manifest package="...">` with no children, which is
 * enough to prove the ApkDecoder -> ResDecoder -> AXML parsing pipeline actually
 * runs end to end against the real dependency.
 *
 * Format reference: frameworks/base ResourceTypes.h (ResChunk_header,
 * ResStringPool_header, ResXMLTree_node / _attrExt / _endElementExt).
 */
object MinimalAxmlFixture {

    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_TYPE = 0x0003
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val RES_XML_END_ELEMENT_TYPE = 0x0103
    private const val TYPE_STRING = 0x03
    private const val NO_ENTRY = -1 // 0xFFFFFFFF as a 32-bit pattern

    fun buildManifest(packageName: String): ByteArray {
        // String pool indices: 0 = element name, 1 = attribute name, 2 = attribute value
        val strings = listOf("manifest", "package", packageName)
        val stringPool = buildStringPool(strings)
        val startElement = buildStartElement(nameIdx = 0, attrNameIdx = 1, attrValueIdx = 2)
        val endElement = buildEndElement(nameIdx = 0)

        val body = stringPool + startElement + endElement
        return chunkHeader(RES_XML_TYPE, headerSize = 8, totalSize = 8 + body.size) + body
    }

    private fun buildStringPool(strings: List<String>): ByteArray {
        val headerSize = 28
        val stringsStart = headerSize + strings.size * 4

        val stringData = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for ((i, s) in strings.withIndex()) {
            offsets[i] = stringData.size()
            writeU16LE(stringData, s.length)
            for (c in s) writeU16LE(stringData, c.code)
            writeU16LE(stringData, 0) // NUL terminator
        }
        var dataBytes = stringData.toByteArray()
        val unpadded = stringsStart + dataBytes.size
        val pad = (4 - unpadded % 4) % 4
        dataBytes += ByteArray(pad)

        val totalSize = stringsStart + dataBytes.size
        val out = ByteArrayOutputStream()
        out.write(chunkHeader(RES_STRING_POOL_TYPE, headerSize, totalSize))
        writeU32LE(out, strings.size) // stringCount
        writeU32LE(out, 0)            // styleCount
        writeU32LE(out, 0)            // flags: UTF-16, unsorted
        writeU32LE(out, stringsStart) // stringsStart
        writeU32LE(out, 0)            // stylesStart
        for (o in offsets) writeU32LE(out, o)
        out.write(dataBytes)
        return out.toByteArray()
    }

    private fun buildStartElement(nameIdx: Int, attrNameIdx: Int, attrValueIdx: Int): ByteArray {
        // node header (16) = base(8) + lineNumber(4) + comment(4)
        // attrExt        (20) = ns(4) + name(4) + attrStart(2) + attrSize(2) + attrCount(2) + idIdx(2) + classIdx(2) + styleIdx(2)
        // one attribute  (20) = ns(4) + name(4) + rawValue(4) + Res_value{size(2)+res0(1)+type(1)+data(4)}
        val totalSize = 8 + 8 + 20 + 20
        val out = ByteArrayOutputStream()
        out.write(chunkHeader(RES_XML_START_ELEMENT_TYPE, headerSize = 16, totalSize = totalSize))
        writeU32LE(out, 1)        // lineNumber
        writeU32LE(out, NO_ENTRY) // comment
        writeU32LE(out, NO_ENTRY) // namespace
        writeU32LE(out, nameIdx)  // element name
        writeU16LE(out, 20)       // attributeStart
        writeU16LE(out, 20)       // attributeSize
        writeU16LE(out, 1)        // attributeCount
        writeU16LE(out, 0)        // idIndex
        writeU16LE(out, 0)        // classIndex
        writeU16LE(out, 0)        // styleIndex
        // attribute[0]
        writeU32LE(out, NO_ENTRY)   // ns (unnamespaced, matches real <manifest package=...>)
        writeU32LE(out, attrNameIdx)
        writeU32LE(out, attrValueIdx) // raw value (string)
        writeU16LE(out, 8)         // Res_value.size
        out.write(0)               // Res_value.res0
        out.write(TYPE_STRING)     // Res_value.dataType
        writeU32LE(out, attrValueIdx) // Res_value.data = string pool index
        return out.toByteArray()
    }

    private fun buildEndElement(nameIdx: Int): ByteArray {
        val totalSize = 8 + 8 + 8 // base + (lineNumber,comment) + (ns,name)
        val out = ByteArrayOutputStream()
        out.write(chunkHeader(RES_XML_END_ELEMENT_TYPE, headerSize = 16, totalSize = totalSize))
        writeU32LE(out, 1)        // lineNumber
        writeU32LE(out, NO_ENTRY) // comment
        writeU32LE(out, NO_ENTRY) // namespace
        writeU32LE(out, nameIdx)  // element name
        return out.toByteArray()
    }

    private fun chunkHeader(type: Int, headerSize: Int, totalSize: Int): ByteArray {
        val out = ByteArrayOutputStream()
        writeU16LE(out, type)
        writeU16LE(out, headerSize)
        writeU32LE(out, totalSize)
        return out.toByteArray()
    }

    private fun writeU16LE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
    }

    private fun writeU32LE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }
}
