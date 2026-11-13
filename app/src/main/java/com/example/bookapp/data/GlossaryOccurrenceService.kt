package com.example.bookapp.data

/** نوع پیوندی که در فهرست محل‌های استفاده نمایش داده می‌شود. */
enum class GlossaryLocationKind {
    /** واژه به‌صورت خودکار در متن اصلی پیدا شده است. */
    AUTOMATIC_MATCH,
    /** این بخش به‌عنوان نمونه کاربرد/شاهد در مدخل واژه ثبت شده است. */
    EXPLICIT_USAGE
}

data class GlossaryTextLocation(
    val termUid: String,
    val headword: String,
    val sectionId: Long,
    val sectionUid: String,
    val sectionTitle: String,
    val roleTitle: String,
    val taziehTitle: String,
    val fieldTitle: String,
    val kind: GlossaryLocationKind,
    val start: Int? = null,
    val endExclusive: Int? = null,
    val quote: String = "",
    val sourceTitle: String = "",
    val sourceAuthor: String = "",
    val sourcePage: String = ""
)

/**
 * بازیابی محل‌های استفاده یک اصطلاح بدون ذخیره‌سازی تکراری و بدون تغییر متن بخش‌ها.
 * نتایج خودکار از متن واقعی استخراج می‌شوند؛ نمونه‌های ثبت‌شده در مدخل جداگانه برچسب می‌خورند.
 */
object GlossaryOccurrenceService {
    suspend fun findForTerm(db: AppDatabase, termUid: String): List<GlossaryTextLocation> {
        if (termUid.isBlank()) return emptyList()

        val allTerms = db.glossaryDao().allTerms()
        val term = allTerms.firstOrNull { it.uid == termUid } ?: return emptyList()
        if (term.reviewStatus != GlossaryTermEntity.STATUS_VERIFIED) return emptyList()

        val spellings = db.glossaryDao().spellings(term.id)
        val meanings = db.glossaryDao().meanings(term.id)
        val fieldsById = db.fieldDao().getAll().associateBy { it.id }
        val explicitBySectionUid = meanings
            .filter { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED && !it.usageSectionUid.isNullOrBlank() }
            .groupBy { it.usageSectionUid!! }

        val result = mutableListOf<GlossaryTextLocation>()
        db.taziehDao().getAll().forEach { tazieh ->
            val fieldTitle = fieldsById[tazieh.fieldId]?.title.orEmpty()
            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                db.sectionDao().getByRole(role.id).forEach { section ->
                    val matches = GlossaryTextLinker.link(
                        text = section.content,
                        terms = listOf(term),
                        spellings = spellings,
                        meanings = meanings
                    )
                    matches.forEach { occurrence ->
                        result += GlossaryTextLocation(
                            termUid = occurrence.termUid,
                            headword = occurrence.headword,
                            sectionId = section.id,
                            sectionUid = section.uid,
                            sectionTitle = section.title,
                            roleTitle = role.title,
                            taziehTitle = tazieh.title,
                            fieldTitle = fieldTitle,
                            kind = GlossaryLocationKind.AUTOMATIC_MATCH,
                            start = occurrence.start,
                            endExclusive = occurrence.endExclusive,
                            quote = section.content.substring(occurrence.start, occurrence.endExclusive)
                        )
                    }

                    explicitBySectionUid[section.uid].orEmpty().forEach { meaning ->
                        result += GlossaryTextLocation(
                            termUid = term.uid,
                            headword = term.headword,
                            sectionId = section.id,
                            sectionUid = section.uid,
                            sectionTitle = section.title,
                            roleTitle = role.title,
                            taziehTitle = tazieh.title,
                            fieldTitle = fieldTitle,
                            kind = GlossaryLocationKind.EXPLICIT_USAGE,
                            quote = meaning.usageQuote,
                            sourceTitle = meaning.sourceTitle,
                            sourceAuthor = meaning.sourceAuthor,
                            sourcePage = meaning.sourcePage
                        )
                    }
                }
            }
        }

        return result.distinctBy {
            listOf(it.sectionUid, it.kind.name, it.start?.toString().orEmpty(), it.endExclusive?.toString().orEmpty(), it.quote, it.sourceTitle, it.sourcePage).joinToString("|")
        }.sortedWith(
            compareBy<GlossaryTextLocation> { it.fieldTitle }
                .thenBy { it.taziehTitle }
                .thenBy { it.roleTitle }
                .thenBy { it.sectionTitle }
                .thenBy { it.start ?: Int.MAX_VALUE }
        )
    }
}
