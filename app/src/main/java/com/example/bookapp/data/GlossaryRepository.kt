package com.example.bookapp.data

/**
 * درگاه واحد دسترسی به فرهنگ اصطلاحات.
 * تمام نوشتن‌ها در این لایه محافظت می‌شوند تا Viewer حتی با فراخوانی مستقیم
 * Repository نیز نتواند محتوای اصلی فرهنگ را تغییر دهد.
 */
class GlossaryRepository(private val db: AppDatabase) {
    private val dao get() = db.glossaryDao()

    suspend fun visibleTerms(includeUnreviewed: Boolean): List<GlossaryTermEntity> =
        if (includeUnreviewed) dao.allTerms()
        else dao.termsByStatus(GlossaryTermEntity.STATUS_VERIFIED)

    suspend fun verifiedTerm(uid: String): GlossaryTermEntity? =
        dao.termByUid(uid)?.takeIf { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED }

    suspend fun meanings(termId: Long, verifiedOnly: Boolean = false): List<GlossaryMeaningEntity> =
        dao.meanings(termId).let { rows ->
            if (verifiedOnly) rows.filter { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED } else rows
        }

    suspend fun spellings(termId: Long): List<GlossarySpellingEntity> = dao.spellings(termId)

    suspend fun addTerm(
        headword: String,
        partOfSpeech: String = "",
        pronunciation: String = "",
        etymology: String = "",
        notes: String = ""
    ): Long {
        ViewerContentWriteGuard.check()
        val cleanHeadword = headword.trim()
        require(cleanHeadword.isNotEmpty()) { "سرواژه نمی‌تواند خالی باشد." }
        val normalized = PersianTextNormalizer.normalize(cleanHeadword)
        require(normalized.isNotBlank()) { "سرواژه معتبر نیست." }
        require(dao.byNormalizedHeadword(normalized) == null) { "این سرواژه یا شکل نرمال‌شده آن قبلاً ثبت شده است." }
        val now = System.currentTimeMillis()
        return dao.insertTerm(
            GlossaryTermEntity(
                uid = ContentUid.new(),
                headword = cleanHeadword,
                normalizedHeadword = normalized,
                partOfSpeech = partOfSpeech.trim(),
                pronunciation = pronunciation.trim(),
                etymology = etymology.trim(),
                notes = notes.trim(),
                reviewStatus = GlossaryTermEntity.STATUS_PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateTerm(term: GlossaryTermEntity) {
        ViewerContentWriteGuard.check()
        require(term.id > 0 && term.headword.isNotBlank()) { "اطلاعات اصطلاح معتبر نیست." }
        val normalized = PersianTextNormalizer.normalize(term.headword)
        val duplicate = dao.byNormalizedHeadword(normalized)
        require(duplicate == null || duplicate.id == term.id) { "سرواژه تکراری است." }
        val updated = term.copy(
            headword = term.headword.trim(),
            normalizedHeadword = normalized,
            updatedAt = System.currentTimeMillis()
        )
        check(dao.updateTerm(updated) == 1) { "اصطلاح برای ویرایش پیدا نشد." }
    }

    suspend fun setReviewStatus(termId: Long, status: String) {
        ViewerContentWriteGuard.check()
        require(status in setOf(
            GlossaryTermEntity.STATUS_PENDING,
            GlossaryTermEntity.STATUS_VERIFIED,
            GlossaryTermEntity.STATUS_REJECTED
        ))
        val current = dao.term(termId) ?: error("اصطلاح پیدا نشد.")
        dao.updateTerm(current.copy(reviewStatus = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTerm(termId: Long) {
        ViewerContentWriteGuard.check()
        check(dao.deleteTermById(termId) == 1) { "اصطلاح برای حذف پیدا نشد." }
        // حذف اصطلاح با کلید خارجی CASCADE، شکل‌های نوشتاری، معنی‌ها و تاریخچه آن را حذف می‌کند.
    }

    suspend fun addMeaning(
        termId: Long,
        meaning: String,
        sourceTitle: String = "",
        sourceAuthor: String = "",
        sourcePage: String = "",
        sourceUrl: String = "",
        usageTaziehUid: String? = null,
        usageVersionUid: String? = null,
        usageRoleUid: String? = null,
        usageSectionUid: String? = null,
        usageQuote: String = ""
    ): Long {
        ViewerContentWriteGuard.check()
        require(dao.term(termId) != null) { "اصطلاح پیدا نشد." }
        val cleanMeaning = meaning.trim()
        require(cleanMeaning.isNotEmpty()) { "معنی نمی‌تواند خالی باشد." }
        require(dao.duplicateMeaning(termId, cleanMeaning, sourceTitle.trim(), sourceAuthor.trim(), sourcePage.trim()) == null) {
            "این معنی با همین مشخصات منبع قبلاً ثبت شده است."
        }
        val now = System.currentTimeMillis()
        return dao.insertMeaning(
            GlossaryMeaningEntity(
                uid = ContentUid.new(), termId = termId, meaning = cleanMeaning,
                sourceTitle = sourceTitle.trim(), sourceAuthor = sourceAuthor.trim(),
                sourcePage = sourcePage.trim(), sourceUrl = sourceUrl.trim(),
                usageTaziehUid = usageTaziehUid, usageVersionUid = usageVersionUid,
                usageRoleUid = usageRoleUid, usageSectionUid = usageSectionUid,
                usageQuote = usageQuote.trim(), reviewStatus = GlossaryTermEntity.STATUS_PENDING,
                createdAt = now, updatedAt = now
            )
        )
    }

    suspend fun updateMeaning(meaning: GlossaryMeaningEntity) {
        ViewerContentWriteGuard.check()
        require(meaning.id > 0 && meaning.meaning.isNotBlank()) { "اطلاعات معنی معتبر نیست." }
        check(dao.updateMeaning(meaning.copy(meaning = meaning.meaning.trim(), updatedAt = System.currentTimeMillis())) == 1) {
            "معنی برای ویرایش پیدا نشد."
        }
    }

    suspend fun deleteMeaning(meaningId: Long) {
        ViewerContentWriteGuard.check()
        check(dao.deleteMeaningById(meaningId) == 1) { "معنی برای حذف پیدا نشد." }
    }

    suspend fun addSpelling(termId: Long, spelling: String, spellingType: String = "VARIANT"): Long {
        ViewerContentWriteGuard.check()
        require(dao.term(termId) != null) { "اصطلاح پیدا نشد." }
        val clean = spelling.trim()
        val normalized = PersianTextNormalizer.normalize(clean)
        require(clean.isNotEmpty() && normalized.isNotBlank()) { "شکل نوشتاری معتبر نیست." }
        return dao.insertSpelling(
            GlossarySpellingEntity(termId = termId, spelling = clean, normalizedSpelling = normalized, spellingType = spellingType)
        )
    }

    suspend fun deleteSpelling(spellingId: Long) {
        ViewerContentWriteGuard.check()
        check(dao.deleteSpellingById(spellingId) == 1) { "شکل نوشتاری برای حذف پیدا نشد." }
    }
}
