# ملف المشروع / Project Profile

## العربية

### الغرض والنطاق
وفقًا لـ`README.md`، يهدف التطبيق إلى تقديم مشغّل أندرويد للقرآن والأذكار يعتمد البطاقات، مع الاستماع الصوتي وإمكانية التشغيل عند مطابقة نص إشعار، وربط الأجهزة ومزامنتها عبر MQTT، وتنزيل الصوت. ويتسق ذلك مع التنفيذ الظاهر في `app/src/main/java/com/example/MainActivity.kt` و`QuranNotificationListenerService.kt` و`data/` ومع مكونات التشغيل والتخزين في `app/src/main/AndroidManifest.xml`. يحتوي المستودع أيضًا على كود مساعد للذكاء الاصطناعي والإعلانات والإعلانات الإدارية وSupabase؛ انظر `data/AiSupportManager.kt` و`data/AdManager.kt` و`data/FirebaseAnnouncementManager.kt` و`supabase/`.

### المكونات والتدفقات الأساسية
- التطبيق وحدة Android واحدة (`settings.gradle.kts` يضم `:app`). تعرض `MainActivity.kt` لوحة التطبيق وتدفقات البطاقات وربط الأجهزة.
- بيانات البطاقات محلية عبر نموذج/DAO/قاعدة Room ومستودع وViewModel: `data/QuranCard.kt`, `QuranCardDao.kt`, `QuranDatabase.kt`, `QuranCardRepository.kt`, `QuranCardViewModel.kt`.
- تشغيل الصوت وخدمات الخلفية والتنزيل: `data/QuranAudioPlayer.kt`, `PlaybackService.kt`, `OfflineDownloader.kt`, و`NotificationUtils.kt`.
- يراقب `QuranNotificationListenerService.kt` الإشعارات، بينما يدير `data/MqttManager.kt` و`MqttService.kt` و`SyncManager.kt` الاتصال/المزامنة؛ توجد شيفرة الربط وQR في `DeviceLinkRepository.kt` و`ui/QuickLinkUI.kt` و`utils/QrCodeAnalyzer.kt` و`QrCodeUtils.kt`.
- موارد Android في `app/src/main/res/` وأصول إطارات الخلفية في `app/src/main/assets/bg_frames/`.
- وظيفة Supabase `supabase/functions/send-push-notification/index.ts` تستقبل أحداث `remote_messages` وتصرح بأنها جاهزة لربط مزود إشعارات مستقبلي؛ النتيجة الحالية في الكود توضح `notified: false`. مخطط الجدول في `supabase/migrations/20260813000000_remote_messages.sql`.

### شجرة مختصرة
```text
.
├── README.md                         # وصف المشروع وتعليماته المنشورة
├── settings.gradle.kts               # إعداد Gradle وإدراج :app
├── build.gradle.kts                  # إضافات Gradle على مستوى الجذر
├── gradle/libs.versions.toml         # إصدارات الإضافات والمكتبات
├── gradle/wrapper/                   # إعداد Gradle Wrapper
├── app/build.gradle.kts              # إعداد Android والتبعيات والاختبارات
├── app/src/main/AndroidManifest.xml  # تعريف التطبيق والمكونات والصلاحيات
├── app/src/main/java/com/example/    # Kotlin: الواجهة والبيانات والخدمات والمرافق
├── app/src/main/res/                 # موارد Android
├── app/src/main/assets/              # أصول التطبيق، ومنها bg_frames/
├── app/src/test/                     # اختبارات محلية/Robolectric ولقطة Compose
├── app/src/androidTest/              # اختبار Android instrumented
└── supabase/                         # README ودالة Edge ومخطط قاعدة بيانات
```

### الملفات المحورية
`app/src/main/java/com/example/MainActivity.kt` هو نقطة الواجهة؛ `data/QuranCard*` و`QuranDatabase.kt` تمثل حفظ البطاقات؛ `QuranAudioPlayer.kt` و`PlaybackService.kt` و`OfflineDownloader.kt` للتشغيل والتنزيل؛ `QuranNotificationListenerService.kt` للتشغيل المرتبط بالإشعارات؛ `MqttManager.kt` و`SyncManager.kt` و`DeviceLinkRepository.kt` للتواصل بين الأجهزة؛ `app/src/main/AndroidManifest.xml` يعلن مكونات النظام؛ و`app/build.gradle.kts` و`gradle/libs.versions.toml` يحددان البناء والتبعيات.

### التقنية والإصدارات والتبعيات
الإعداد مبني على Kotlin DSL وGradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`)، وAndroid Gradle Plugin `9.1.1`، وKotlin `2.2.10`، وCompose BOM `2024.09.00`، وKSP `2.3.5` (`gradle/libs.versions.toml`). التطبيق يحدد `compileSdk 36` و`minSdk 24` و`targetSdk 36`، Java source/target 11، ومعرّف التطبيق `com.aistudio.quranplayer.shmrzp` وإصدار التطبيق `2.8.3` (code 21) في `app/build.gradle.kts`. المكتبات المعلنة تشمل AndroidX Compose وRoom `2.7.0` وMedia3 `1.3.1` وHiveMQ MQTT `1.3.3` وRetrofit `2.12.0` وOkHttp `4.10.0` وZXing `3.5.3` وFirebase BOM `34.12.0`، إلى جانب CameraX وMoshi/Gson وCoroutines؛ المرجع الكامل في `gradle/libs.versions.toml` و`app/build.gradle.kts`.

### الإعداد والتشغيل والاختبار والبناء
- README يقترح Android Studio ويذكر Android 7.0/API 24 أو أحدث (`README.md`)، ويتفق حد API الأدنى مع `app/build.gradle.kts`. ابدأ بفتح المشروع في Android Studio مع Gradle Wrapper؛ لا يوثق المستودع أمرًا محددًا لتشغيل التطبيق من سطر الأوامر.
- يهيئ `app/build.gradle.kts` Secrets Gradle Plugin لاستخدام `.env` افتراضيًا و`.env.example` كقيم افتراضية. أسماء المفاتيح الموجودة في ملف المثال: `GEMINI_API_KEY` و`YOUTUBE_API_KEY`؛ لا تضع القيم في التوثيق أو المستودع. أسماء متغيرات البيئة التي يقرأها إعداد التوقيع: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`.
- الاختبارات الموجودة: `app/src/test/java/com/example/ExampleUnitTest.kt` و`ExampleRobolectricTest.kt` و`GreetingScreenshotTest.kt`، واختبار instrumented واحد في `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`. لا يحدد README أمر اختبار؛ يمكن استخدام مهام Gradle المعرفة في المشروع، لكن لم يُوثق هنا اسم أمر غير مذكور.
- لم يظهر مجلد `.github` أو ملفات CI في المستودع المفحوص. إعدادات الإصدار والتوقيع موجودة في `app/build.gradle.kts`؛ تجنب مشاركة بيانات التوقيع.

### إرشادات المطور والنقاط غير المحسومة
اقرأ إعداد التطبيق ونسخ المكتبات قبل تغيير التبعيات، وتتبع ميزات البطاقات من الواجهة عبر ViewModel والمستودع إلى DAO/Room. اختبارات المشروع الظاهرة في معظمها اختبارات نموذجية (جمع/فرز، سياق التطبيق، ولقطة واجهة)، وليست تغطية شاملة لمنطق التطبيق. يذكر README ملفات ومسارات APK/مخرجات ومكونات مثل ملفات `.env.example` وإرشادات الربط؛ الموجود فعليًا هو `.env.example`، لكن لم توجد مجلدات APK/مخرجات المشار إليها ولا جميع أسماء الملفات في شجرة README. لا يوجد `google-services.json` ضمن الملفات المفحوصة؛ لذلك لا يمكن من الشجرة وحدها تأكيد إعداد Firebase التشغيلي. كما أن وظيفة Supabase الحالية لا ترسل إشعارًا فعليًا وفق تنفيذها. لا تفترض صلاحية قيم أو تكاملات خارجية اعتمادًا على README فقط.

## English

### Purpose and scope
According to `README.md`, the application aims to provide an Android Quran and adhkar audio player organized around cards, with audio playback, notification-text-triggered playback, device linking/synchronization over MQTT, and audio downloads. This is consistent with the visible implementation in `app/src/main/java/com/example/MainActivity.kt`, `QuranNotificationListenerService.kt`, `data/`, and playback/storage components declared in `app/src/main/AndroidManifest.xml`. The repository also contains supporting code for AI assistance, ads, announcements, and Supabase; see `data/AiSupportManager.kt`, `data/AdManager.kt`, `data/FirebaseAnnouncementManager.kt`, and `supabase/`.

### Main components and flows
- The app is a single Android module (`settings.gradle.kts` includes `:app`). `MainActivity.kt` hosts the application dashboard and card/device-linking flows.
- Card data is local, using a Room model/DAO/database plus repository and ViewModel: `data/QuranCard.kt`, `QuranCardDao.kt`, `QuranDatabase.kt`, `QuranCardRepository.kt`, and `QuranCardViewModel.kt`.
- Audio playback, background services, and downloads are represented by `data/QuranAudioPlayer.kt`, `PlaybackService.kt`, `OfflineDownloader.kt`, and `NotificationUtils.kt`.
- `QuranNotificationListenerService.kt` monitors notifications; `data/MqttManager.kt`, `MqttService.kt`, and `SyncManager.kt` manage MQTT/device synchronization. Linking and QR code code is in `DeviceLinkRepository.kt`, `ui/QuickLinkUI.kt`, `utils/QrCodeAnalyzer.kt`, and `QrCodeUtils.kt`.
- Android resources are under `app/src/main/res/`; background-frame assets are under `app/src/main/assets/bg_frames/`.
- The Supabase function `supabase/functions/send-push-notification/index.ts` receives `remote_messages` events and describes itself as ready for a future push-provider integration; its current response explicitly reports `notified: false`. The table schema is in `supabase/migrations/20260813000000_remote_messages.sql`.

### Short tree
```text
.
├── README.md                         # Published project description and instructions
├── settings.gradle.kts               # Gradle setup and :app inclusion
├── build.gradle.kts                  # Root-level Gradle plugins
├── gradle/libs.versions.toml         # Plugin and library versions
├── gradle/wrapper/                   # Gradle Wrapper configuration
├── app/build.gradle.kts              # Android, dependencies, and test configuration
├── app/src/main/AndroidManifest.xml  # App components and permissions
├── app/src/main/java/com/example/    # Kotlin UI, data, services, and utilities
├── app/src/main/res/                 # Android resources
├── app/src/main/assets/              # App assets, including bg_frames/
├── app/src/test/                     # Local/Robolectric tests and Compose screenshot
├── app/src/androidTest/              # Instrumented Android test
└── supabase/                         # README, Edge Function, and database migration
```

### Key files
`app/src/main/java/com/example/MainActivity.kt` is the UI entry point; `data/QuranCard*` and `QuranDatabase.kt` implement card persistence; `QuranAudioPlayer.kt`, `PlaybackService.kt`, and `OfflineDownloader.kt` cover playback/downloads; `QuranNotificationListenerService.kt` handles notification-triggered playback; `MqttManager.kt`, `SyncManager.kt`, and `DeviceLinkRepository.kt` support device communication; `app/src/main/AndroidManifest.xml` declares system components; and `app/build.gradle.kts` plus `gradle/libs.versions.toml` define build configuration and dependencies.

### Technology, versions, and dependencies
The project uses Kotlin DSL and the Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`), Android Gradle Plugin `9.1.1`, Kotlin `2.2.10`, Compose BOM `2024.09.00`, and KSP `2.3.5` (`gradle/libs.versions.toml`). The app config sets `compileSdk 36`, `minSdk 24`, `targetSdk 36`, Java source/target 11, application ID `com.aistudio.quranplayer.shmrzp`, and app version `2.8.3` (code 21) in `app/build.gradle.kts`. Declared libraries include AndroidX Compose, Room `2.7.0`, Media3 `1.3.1`, HiveMQ MQTT `1.3.3`, Retrofit `2.12.0`, OkHttp `4.10.0`, ZXing `3.5.3`, Firebase BOM `34.12.0`, CameraX, Moshi/Gson, and Coroutines; see `gradle/libs.versions.toml` and `app/build.gradle.kts` for the complete declarations.

### Setup, run, test, and build
- The README recommends Android Studio and states Android 7.0/API 24 or later (`README.md`); the minimum API matches `app/build.gradle.kts`. Open the project in Android Studio with the Gradle Wrapper. The repository does not document a specific command for launching the app from the command line.
- `app/build.gradle.kts` configures the Secrets Gradle Plugin to use `.env` by default and `.env.example` as defaults. The example file contains the setting names `GEMINI_API_KEY` and `YOUTUBE_API_KEY`; do not put values in this documentation or repository. The signing configuration reads these environment-variable names: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, and `KEY_ALIAS`.
- Existing tests are `app/src/test/java/com/example/ExampleUnitTest.kt`, `ExampleRobolectricTest.kt`, and `GreetingScreenshotTest.kt`, plus one instrumented test at `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`. The README does not specify a test command; Gradle tasks are configured, but no undocumented command is asserted here.
- No `.github` directory or CI files were found in the inspected repository. Release/signing configuration is in `app/build.gradle.kts`; avoid sharing signing data.

### Developer notes and open questions
Review the app configuration and version catalog before changing dependencies, and trace card behavior from UI through ViewModel/repository to DAO/Room. The visible tests are mostly example-level checks (addition/sorting, app context, and a UI screenshot), not comprehensive application-logic coverage. The README refers to APK/output paths and components such as `.env.example` and linking instructions; `.env.example` exists, but the referenced APK/output directories and some README-listed filenames are absent from the current tree. No `google-services.json` was found among inspected files, so operational Firebase configuration cannot be confirmed from the tree alone. The current Supabase function does not actually send a notification. Do not assume external settings or integrations are operational based on README claims alone.
