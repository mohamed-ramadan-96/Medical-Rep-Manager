package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Exports only doctor names and phone numbers to a one-sheet XLSX workbook. */
object ExcelExporter {
    fun export(context: Context, destination: Uri, doctors: List<DoctorEntity>, isArabic: Boolean) {
        val rows = buildList {
            add(if (isArabic) listOf("اسم الطبيب", "رقم الهاتف") else listOf("Doctor name", "Phone number"))
            doctors.forEach { doctor -> add(listOf(doctor.name, doctor.phoneNumbers)) }
        }
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Unable to open selected destination")
        output.use { raw ->
            ZipOutputStream(raw).use { zip ->
                fun put(path: String, text: String) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""")
                put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
                put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Doctors" sheetId="1" r:id="rId1"/></sheets></workbook>""")
                put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""")
                val rowsXml = rows.mapIndexed { rowIndex, row ->
                    val cells = row.mapIndexed { colIndex, value ->
                        val ref = "${columnName(colIndex + 1)}${rowIndex + 1}"
                        "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>"
                    }.joinToString("")
                    "<row r=\"${rowIndex + 1}\">$cells</row>"
                }.joinToString("")
                put("xl/worksheets/sheet1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$rowsXml</sheetData></worksheet>""")
            }
        }
    }

    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
        .filter { it == '\\t' || it == '\\n' || it == '\\r' || it.code >= 0x20 }

    private fun columnName(number: Int): String {
        var n = number
        val result = StringBuilder()
        while (n > 0) {
            val remainder = (n - 1) % 26
            result.append(('A'.code + remainder).toChar())
            n = (n - 1) / 26
        }
        return result.reverse().toString()
    }
}
