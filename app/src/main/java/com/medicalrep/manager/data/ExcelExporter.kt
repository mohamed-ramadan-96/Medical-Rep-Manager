package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Creates a three-sheet XLSX workbook locally; no data leaves the device. */
object ExcelExporter {
    fun export(context: Context, destination: Uri, doctors: List<DoctorEntity>,
               centers: List<MedicalCenterEntity>, visits: List<DoctorVisitEntity>, isArabic: Boolean) {
        val centerNames = centers.associate { it.id to it.name }
        val doctorNames = doctors.associate { it.id to it.name }
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        fun date(value: Long?): String = value?.let { formatter.format(Date(it)) }.orEmpty()

        val doctorRows = mutableListOf<List<Any?>>()
        doctorRows.add(if (isArabic) listOf("رقم الطبيب","الاسم","التخصص","أرقام الهاتف","المركز","الملاحظات","تاريخ الإضافة")
            else listOf("Doctor ID","Name","Specialty","Phone numbers","Medical center","Notes","Created at"))
        doctors.forEach { d -> doctorRows.add(listOf(d.id,d.name,d.specialty,d.phoneNumbers,
            d.centerId?.let { centerNames[it] }.orEmpty().ifBlank { d.medicalCenter },d.notes,date(d.createdAt))) }

        val visitRows = mutableListOf<List<Any?>>()
        visitRows.add(if (isArabic) listOf("رقم الزيارة","الطبيب","تاريخ ووقت الزيارة","النتيجة","الملاحظات","موعد المتابعة")
            else listOf("Visit ID","Doctor","Visited at","Outcome","Notes","Next follow-up"))
        visits.forEach { v -> visitRows.add(listOf(v.id,doctorNames[v.doctorId].orEmpty(),date(v.visitedAt),
            v.outcome,v.notes,date(v.nextFollowUpAt))) }

        val centerRows = mutableListOf<List<Any?>>()
        centerRows.add(if (isArabic) listOf("رقم المركز","اسم المركز","العنوان","الهاتف","ملاحظات","تاريخ الإضافة")
            else listOf("Center ID","Center name","Address","Phone","Notes","Created at"))
        centers.forEach { c -> centerRows.add(listOf(c.id,c.name,c.address,c.phone,c.notes,date(c.createdAt))) }

        val sheets: List<Pair<String, List<List<Any?>>>> = listOf(
            (if (isArabic) "الأطباء" else "Doctors") to doctorRows,
            (if (isArabic) "الزيارات" else "Visits") to visitRows,
            (if (isArabic) "المراكز" else "Centers") to centerRows
        )
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Unable to open selected destination")
        output.use { raw ->
            ZipOutputStream(raw).use { zip ->
                fun put(path: String, text: String) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                val overrides = sheets.indices.joinToString("") { i ->
                    "<Override PartName=\"/xl/worksheets/sheet${i+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                }
                put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$overrides</Types>""")
                put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
                val workbookSheets = sheets.mapIndexed { i, pair ->
                    "<sheet name=\"${xml(pair.first)}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"
                }.joinToString("")
                put("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$workbookSheets</sheets></workbook>""")
                val relations = sheets.indices.joinToString("") { i ->
                    "<Relationship Id=\"rId${i+1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i+1}.xml\"/>"
                }
                put("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$relations</Relationships>""")
                sheets.forEachIndexed { sheetIndex, pair ->
                    val rowsXml = pair.second.mapIndexed { rowIndex, row ->
                        val cells = row.mapIndexed { colIndex, value ->
                            val ref = "${columnName(colIndex+1)}${rowIndex+1}"
                            if (value is Number) "<c r=\"$ref\"><v>$value</v></c>"
                            else "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value?.toString().orEmpty())}</t></is></c>"
                        }.joinToString("")
                        "<row r=\"${rowIndex+1}\">$cells</row>"
                    }.joinToString("")
                    put("xl/worksheets/sheet${sheetIndex+1}.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$rowsXml</sheetData></worksheet>""")
                }
            }
        }
    }

    private fun xml(value: String): String = value.replace("&","&amp;").replace("<","&lt;")
        .replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
        .filter { it == '\t' || it == '\n' || it == '\r' || it.code >= 0x20 }

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
