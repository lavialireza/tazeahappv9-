بسته اصلاحی خطاهای Build — تعزیه و شبیه‌خوانی

این بسته برای اصلاح خطاهای کامپایل گزارش‌شده در اجرای assembleAdminQa تهیه شده است.

فایل‌ها:
1) app/src/main/java/com/example/bookapp/ui/AppNavigation.kt
   - افزودن onExit به اتصال منوی اصلی.
   - حذف آرگومان‌های onRehearse و canRehearse از MyRoleScreen، مطابق امضای فعلی آن صفحه در پروژه‌ای که خطاهای Build را گزارش کرده است.
   - تکمیل اتصال RehearsalScreen به مجوز پخش صوت و افزودن یادداشت تمرین.
   - یادداشت تمرین در جدول notes ذخیره می‌شود و به uid بخش مربوط متصل است.
   - حفظ اتصال callbackهای رابطه پاسخ و نشانه‌های صحنه.

2) app/src/main/java/com/example/bookapp/ui/screens/DialogueReaderScreen.kt
   - نسخه‌ای با امضای سازگار با callbackهای رابطه پاسخ و نشانه‌های صحنه که AppNavigation به آن‌ها متصل است.

3) app/src/main/java/com/example/bookapp/ui/screens/TextScreen.kt
   - حذف ارجاع به GLOSSARY_TERMS تعریف‌نشده در بررسی تکراری بودن اصطلاح.

4) app/src/main/java/com/example/bookapp/ui/screens/GlossaryScreen.kt
   - صفحه فرهنگ اصطلاحات متصل به Room و نمایش محل‌های استفاده.

روش اعمال:
فایل‌ها را با حفظ همین مسیرها در پروژه جایگزین کنید.
سپس دستور زیر را در GitHub Actions اجرا کنید:
gradle :app:assembleAdminQa -PreleaseNumber=$GITHUB_RUN_NUMBER --stacktrace --warning-mode all

محدودیت بررسی:
این بسته بر اساس خطاهای کامپایل ارسال‌شده اصلاح شده است؛ اجرای Build نهایی در محیط GitHub Actions کاربر انجام می‌شود. موفقیت Build از پیش ادعا نمی‌شود.
