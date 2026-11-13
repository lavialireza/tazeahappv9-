package com.example.bookapp.data

/** نوع پیوندی که در فهرست محل‌های استفاده نمایش داده می‌شود. */
enum class GlossaryLocationKind {
    /** واژه با تطبیق نرمال‌شده در متن واقعی پیدا شده است. */
    AUTOMATIC_MATCH,
    /** این بخش به‌عنوان شاهد/نمونه کاربرد در مدخل اصطلاح ثبت شده است. */
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
 * فهرست محل‌های استفاده را در زمان درخواست از متن اصلی می‌سازد.
 * هیچ داده‌ای در متن یا دیتابیس برای تطبیق خودکار نوشته نمی‌شود.
 */
object GlossaryOccurrenceService {
    suspend fun findForTerm(db: AppDatabase, termUid: String): List<GlossaryTextLocation> {
        if (termUid.isBlank()) return emptyList()

        val term = db.glossaryDao().termByUid(termUid)
            ?.takeIf { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED }
            ?: return emptyList()

        val spellings = db.glossaryDao().spellings(term.id)
        val verifiedMeanings = db.glossaryDao().meanings(term.id)
            .filter { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED }
        val fieldsById = db.fieldDao().getAll().associateBy { it.id }
        val result = mutableListOf<GlossaryTextLocation>()

        db.taziehDao().getAll().forEach { tazieh ->
            val fieldTitle = fieldsById[tazieh.fieldId]?.title.orEmpty()
            db.roleDao().getByTazieh(tazieh.id).forEach { role ->
                db.sectionDao().getByRole(role.id).forEach { section ->
                    val matches = GlossaryTextLinker.link(
                        text = section.content,
                        terms = listOf(term),
                        spellings = spellings,
                        meanings = verifiedMeanings
                    )
                    matches.forEach { occurrence ->
                        val start = occurrence.start.coerceIn(0, section.content.length)
                        val end = occurrence.endExclusive.coerceIn(start, section.content.length)
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
                            start = start,
                            endExclusive = end,
                            quote = section.content.substring(start, end)
                        )
                    }

                    // ارجاع ثبت‌شده فقط زمانی به این بخش وصل می‌شود که شناسه‌های موجود
                    // با سلسله‌مراتب واقعی بخش، نقش و مجلس تعارض نداشته باشند.
                    verifiedMeanings.forEach { meaning ->
                        if (meaning.usageSectionUid != section.uid) return@forEach
                        if (!meaning.usageRoleUid.isNullOrBlank() && meaning.usageRoleUid != role.uid) return@forEach
                        if (!meaning.usageTaziehUid.isNullOrBlank() && meaning.usageTaziehUid != tazieh.uid) return@forEach
                        if (!meaning.usageVersionUid.isNullOrBlank() && meaning.usageVersionUid != role.versionUid) return@forEach

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
            listOf(
                it.sectionUid, it.kind.name, it.start?.toString().orEmpty(),
                it.endExclusive?.toString().orEmpty(), it.quote, it.sourceTitle,
                it.sourceAuthor, it.sourcePage
            ).joinToString("|")
        }.sortedWith(
            compareBy<GlossaryTextLocation> { it.fieldTitle }
                .thenBy { it.taziehTitle }
                .thenBy { it.roleTitle }
                .thenBy { it.sectionTitle }
                .thenBy { it.start ?: Int.MAX_VALUE }
                .thenBy { it.kind.ordinal }
        )
    }
}
