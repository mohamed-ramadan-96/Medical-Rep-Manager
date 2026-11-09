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

        val header = rows.first().mapValues { it.value.trim().lowercase() }
        val nameColumn = header.entries.firstOrNull {
            it.value in setOf("name", "doctor name", "الاسم", "اسم الطبيب", "اسم الدكتور")
        }?.key ?: 0
        val phoneColumn = header.entries.firstOrNull {
            it.value in setOf("phone", "phone number", "phone numbers", "الهاتف", "رقم الهاتف", "أرقام الهاتف", "رقم التليفون")
        }?.key ?: 1

        return rows.drop(1).mapNotNull { row ->
            val name = row[nameColumn]?.trim().orEmpty()
            val phone = row[phoneColumn]?.trim().orEmpty()
            if (name.isBlank() || phone.isBlank()) null else ImportedDoctorRow(name, phone)
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
