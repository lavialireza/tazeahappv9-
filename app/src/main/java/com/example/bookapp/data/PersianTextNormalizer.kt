package com.example.bookapp.data

/**
 * یکسان‌ساز مرکزی متن فارسی/عربی برای جستجو، مقایسه و پیوند اصطلاحات.
 * متن ورودی هرگز تغییر داده نمی‌شود؛ خروجی فقط برای مقایسه ساخته می‌شود.
 */
object PersianTextNormalizer {
    data class MappedText(
        val text: String,
        /** اندیس شروع هر نویسه نرمال‌شده در متن اصلی (UTF-16). */
        val sourceStarts: List<Int>,
        /** اندیس پایان انحصاری هر نویسه نرمال‌شده در متن اصلی (UTF-16). */
        val sourceEnds: List<Int>
    )

    fun normalize(value: String): String = normalizeWithOffsets(value).text

    fun normalizeWithOffsets(source: String): MappedText {
        val out = StringBuilder(source.length)
        val starts = ArrayList<Int>(source.length)
        val ends = ArrayList<Int>(source.length)

        source.forEachIndexed { index, raw ->
            if (isDiacritic(raw) || raw == TATWEEL) return@forEachIndexed

            val mapped = mapCharacter(raw)
            if (mapped == ' ') {
                // فاصله، نیم‌فاصله و جداکننده‌های whitespace به یک فاصله تبدیل می‌شوند.
                if (out.isNotEmpty() && out.last() != ' ') {
                    out.append(' ')
                    starts += index
                    ends += index + 1
                }
            } else {
                out.append(mapped.lowercaseChar())
                starts += index
                ends += index + 1
            }
        }

        // حذف فاصله ابتدا و انتها بدون از دست دادن نگاشت اندیس‌ها.
        while (out.isNotEmpty() && out.first() == ' ') {
            out.deleteCharAt(0)
            starts.removeAt(0)
            ends.removeAt(0)
        }
        while (out.isNotEmpty() && out.last() == ' ') {
            out.setLength(out.length - 1)
            starts.removeAt(starts.lastIndex)
            ends.removeAt(ends.lastIndex)
        }

        return MappedText(out.toString(), starts, ends)
    }

    private fun mapCharacter(value: Char): Char = when (value) {
        'ي', 'ى' -> 'ی'
        'ك' -> 'ک'
        'ۀ', 'ة' -> 'ه'
        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
        '\u200c', '\u200d' -> ' '
        else -> if (value.isWhitespace()) ' ' else value
    }

    private fun isDiacritic(value: Char): Boolean =
        value in '\u0610'..'\u061A' ||
        value in '\u064B'..'\u065F' ||
        value == '\u0670' ||
        value in '\u06D6'..'\u06ED'

    private const val TATWEEL = '\u0640'
}
