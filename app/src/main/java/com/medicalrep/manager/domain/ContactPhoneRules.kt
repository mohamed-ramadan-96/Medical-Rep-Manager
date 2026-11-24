package com.medicalrep.manager.domain

/**
 * Normalization rules used when matching imported doctor phone numbers.
 * Separators are split before normalization so multiple numbers remain distinct.
 */
object ContactPhoneRules {
    fun split(value: String): List<String> =
        value.split(Regex("[,;\\n\\r•|/]"))
            .map(String::trim)
            .filter(String::isNotBlank)

    fun normalize(value: String): String? {
        val digits = value.map { char ->
            when (char) {
                in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                else -> char
            }
        }.filter(Char::isDigit)
        return digits.joinToString("").takeIf(String::isNotBlank)
    }
}
