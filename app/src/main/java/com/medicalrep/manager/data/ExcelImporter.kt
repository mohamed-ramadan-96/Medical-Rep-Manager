package com.medicalrep.manager.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

data class ImportedDoctorRow(val name: String, val phone: String)

object ExcelImporter {
    fun readDoctors(resolver: ContentResolver, uri: Uri): List<ImportedDoctorRow> {
        val entries = HashMap<String, ByteArray>()
        resolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name == "xl/worksheets/sheet1.xml" ||
                        entry.name == "xl/sharedStrings.xml")) {
                        entries[entry.name] = zip.readBytes()
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Unable to open selected Excel file")

        val sheet = entries["xl/worksheets/sheet1.xml"] ?: error("Excel file does not contain a first worksheet")
        val shared = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val rows = parseSheet(sheet, shared)
        check(rows.isNotEmpty()) { "Excel sheet is empty" }

        check(rows.all { row -> row.keys.all { it == 0 || it == 1 } }) {
            "Excel file must not contain more than two columns"
        }
        val nameHeaders = setOf("name", "doctor name", "الاسم", "اسم الطبيب", "اسم الدكتور")
        val phoneHeaders = setOf("phone", "phone number", "phone numbers", "الهاتف", "رقم الهاتف", "أرقام الهاتف", "رقم التليفون")
        val header = rows.first().mapValues { it.value.trim().lowercase() }
        val hasHeader = header[0] in nameHeaders
        val phoneColumn = if (hasHeader && header[1].isNullOrBlank()) null else if (hasHeader) {
            check(header[1] in phoneHeaders) {
                "Second column must be Phone Number"
            }
            1
        } else null

        val dataRows = if (hasHeader) rows.drop(1) else rows
        return dataRows.mapNotNull { row ->
            val name = row[0]?.trim().orEmpty()
            val phone = if (phoneColumn != null) row[phoneColumn]?.trim().orEmpty() else ""
            if (name.isBlank()) null else ImportedDoctorRow(name, phone)
        }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        val result = mutableListOf<String>()
        var inItem = false
        var inText = false
        val current = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { inItem = true; current.setLength(0) }
                    "t" -> if (inItem) inText = true
                }
                XmlPullParser.TEXT -> if (inItem && inText) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "si" -> { result += current.toString(); inItem = false }
                }
            }
            parser.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<Map<Int, String>> {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        val rows = mutableListOf<Map<Int, String>>()
        var row: MutableMap<Int, String>? = null
        var column = 0
        var cellType = ""
        var inCell = false
        var inValue = false
        val value = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = linkedMapOf()
                    "c" -> {
                        inCell = true
                        value.setLength(0)
                        cellType = parser.getAttributeValue(null, "t").orEmpty()
                        val reference = parser.getAttributeValue(null, "r").orEmpty()
                        column = reference.takeWhile { it.isLetter() }.fold(0) { acc, char ->
                            acc * 26 + (char.uppercaseChar().code - 'A'.code + 1)
                        } - 1
                    }
                    "v", "t" -> if (inCell) inValue = true
                }
                XmlPullParser.TEXT -> if (inCell && inValue) value.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val raw = value.toString()
                        row?.set(column, if (cellType == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw)
                        inCell = false
                    }
                    "row" -> { row?.let { rows += it }; row = null }
                }
            }
            parser.next()
        }
        return rows
    }
}
