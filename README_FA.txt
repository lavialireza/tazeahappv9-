بسته پیاده‌سازی مسیرها و تمرین چندنقشی تعزیه

مبنای بررسی: ZIP بارگذاری‌شده در ۹ نوامبر ۲۰۲۶.

فایل‌های این بسته را در مخزن فعال lavialireza/tazeahappv9- با همان مسیر قرار دهید:

۱) app/src/main/java/com/example/bookapp/ui/AppNavigation.kt
   جایگزینی کامل فایل؛ هر دو ورودی «نقش من» و «فهرست بخش‌های نقش» به صفحه تنظیم تمرین هدایت می‌شوند. مسیر مطالعه، جستجو و مقایسه دست‌نخورده باقی مانده است.

۲) app/src/main/java/com/example/bookapp/ui/screens/RehearsalSetupScreen.kt
   فایل جدید؛ انتخاب چند بخش از نقش خود و نقش‌های دیگر، انتخاب نوع تمرین و انتخاب خط شروع برای هر بخش. نقطه شروع هر بخش در SharedPreferences با کلید start_line_<sectionId> ذخیره می‌شود.

۳) app/src/main/java/com/example/bookapp/ui/screens/MultiRoleRehearsalScreen.kt
   فایل جدید؛ متن نقش‌های دیگر کامل دیده می‌شود و متن نقش خود کاربر خط‌به‌خط آشکار می‌شود. آخرین نوبت و وضعیت تمرین‌شده بودن بخش‌ها حفظ می‌شود.

۴) RehearsalScreen_MERGE_FA.txt
   راهنمای ادغام کوچک و ضروری در RehearsalScreen.kt جدیدتری که کاربر قبلاً فرستاده است. فایل قدیمی داخل ZIP را جایگزین نسخه جدید نکنید.

نیازی به تغییر AppDatabase، Entityها، DAOها یا شماره نسخه دیتابیس نیست؛ از ساختار موجود نسخه ۱۶ استفاده شده است.

ترتیب مکالمه چندنقشی:
- فقط وقتی تمام نوبت‌های انتخاب‌شده دارای sourceSequenceIndex و وضعیت DOCUMENTED یا DIRECTOR_CONFIRMED باشند، ترتیب مستند منبع استفاده می‌شود.
- در غیر این صورت، اگر metadata برای همه نوبت‌ها موجود باشد، rehearsalOrderIndex برای ترتیب تمرین استفاده می‌شود و UI اعلام می‌کند که این الزاماً ترتیب اصلی اجرا نیست.
- اگر اطلاعات کافی نباشد، ترتیب انتخاب کاربر حفظ می‌شود و ترتیب تاریخی/اجرایی قطعی ادعا نمی‌شود.

این محیط Gradle نصب‌شده یا Gradle wrapper در ZIP نداشت؛ بنابراین بیلد اندروید اجرا نشده است. پس از قرار دادن فایل‌ها و ادغام کوچک RehearsalScreen، هر دو QA را با GitHub Actions اجرا کنید:
:app:assembleAdminQa
:app:assembleViewerQa
