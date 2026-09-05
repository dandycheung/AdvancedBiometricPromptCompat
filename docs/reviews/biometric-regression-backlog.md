# Беклог регресій біометричних модулів

Оновлено 2026-09-05 після виправлень і спільного Pixel QA. **Виправлення внесено; повного release sign-off ще немає.** Актуальні докази, команди та межі перевірки: [Pixel verification report](2026-09-05-pixel-regression-verification.md). Початкові причини й acceptance checklist нижче збережено як історію; поточний статус визначає реєстр.

Код: AdvancedBiometricPromptCompat, main, HEAD `91163809` + незакомічені зміни цієї задачі. Змінено `biometric` та `common`; код `app` не редагувався. Demo debug APK використано для device QA; його hash зафіксовано у verification report.

Пристрій: Pixel 9 Pro Fold, Android 17 / API 37, `google/comet/comet:17/CP2A.260805.005/15828068:user/release-keys`. Перевірки виконано unfolded у двох орієнтаціях. Початковий RECORD_AUDIO: denied, USER_SET + USER_FIXED. Через Settings UI надано дозвіл; користувач завершив voice enrollment, потім voice authentication. Hardware templates не видалялися.

Статуси доказів: **STATIC** — логіка простежена в поточному коді; **USER-DEVICE** — спостереження користувача; **TRACE** — видимий фрагмент LeakCanary; **INVESTIGATE** — причина/межі ще не доведені. P1 — блокер відповідного сценарію; P2 — дефект або кандидат, що потребує діагностики. Пріоритет не означає підтвердження root cause.

## Реєстр

| ID | Пріоритет | Доказ | Сценарій | Статус |
| --- | --- | --- | --- | --- |
| BIO-REG-001 | P1 | STATIC + compile | Перше hardware enrollment | Код виправлено; first-enrollment device QA відкрите |
| BIO-REG-002 | P1 | JVM + PIXEL | Старі та нові ciphertext | 9 crypto tests пройшли на JVM/Android; реальний upgrade/Keystore restart ще в QA |
| BIO-REG-003 | P1 | STATIC + JVM + PIXEL | System terminal → software stage | Код виправлено; success → voice пройдено, fatal-system replay ще в QA |
| BIO-REG-004 | P2 | TRACE | Detached Toast View | Library Toast прибрано; власник початкового retention ще Investigate |
| BIO-REG-005 | P1 | PIXEL + USER-DEVICE | Strict FINGERPRINT | AUTO — fingerprint-only; generic BIOMETRIC_API — явна unsupported-помилка |
| BIO-REG-006 | P1 | STATIC + PIXEL | Strict FACE | Хибний success заборонено; FACE-only через generic API явно unsupported |
| BIO-REG-007 | P2 | OPEN / DESIGN BLOCKER | Подвійний fingerprint UI | Фокусну евристику відкликано через Samsung/Bubble/Split-Screen ризики. Pixel hardcode не повернуто; дубль на framework route знову можливий. [Актуальний звіт](2026-09-05-multiwindow-android17.md) |
| BIO-REG-008 | P1 | PIXEL + USER-DEVICE | Setup AUTO ANY → Voice | Enrollment завершено BIOMETRIC_VOICE; наступна voice auth успішна |
| BIO-REG-009 | P2 | STATIC | Hardware confirmation / новий enrollment | Контракт уточнено; явне створення нового hardware template ще в QA |
| BIO-REG-010 | P1 | PIXEL + JVM | Microphone denial recovery | USER_FIXED → Settings → grant → voice пройдено; перша відмова/reuse Builder ще в QA |

Нумерація користувача збережена в деталях: пункт 3 містив два різні дефекти, тому його розділено на BIO-REG-006 і BIO-REG-007. BIO-REG-001..003 — три попередні знахідки рев’ю; BIO-REG-004..010 — сім окремих спостережень користувача.

## BIO-REG-001 — preflight до першого hardware enrollment

Модуль: biometric. Сценарій: лише hardware fingerprint, жодного enrolled fingerprint або доступного software-маршруту; виклик setupBiometric з hardware enrollment.

Причина STATIC: preflight перевіряє getPendingEnrollTypes до systemSetup. Hardware fingerprint вилучено з effective software enrollment, а confirmed/enrolled ще немає — flow завершується до InitiateSystemBiometricEnrollFragment. Це окремий сценарій від Pixel-конфігурації, де hardware вже enrolled.

Код: [setup preflight](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:455), [перевірка effective types](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:1077), [pending enrollment types](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:2282).

- [ ] Розділити preflight до системного enrollment та після повернення з нього.
- [ ] Регресія: fingerprint-only без шаблонів відкриває системні налаштування; success/cancel/error доставляються один раз, gate звільняється.
- [ ] Перевірити повернення без нового шаблону та з новим шаблоном; software preparation виконується на відповідному етапі.

## BIO-REG-002 — змішані старі й нові ciphertext

Модуль: biometric/crypto. Сценарій: прочитати старий ciphertext → створити новий під тим самим alias → повторно прочитати старий.

Причина STATIC: decrypt без protected secret бере reversed(keyName); encrypt зберігає випадковий secret, який надалі вибирається і для старих записів. Через API старий ciphertext більше не дешифрується; це не доказ фізичного видалення даних. Core використовує спільний alias BiometricModule<tag>, тому alias не є версією кожного запису.

Код: [AppFlowSecretStore.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/crypto/AppFlowSecretStore.kt:11), [Core key alias](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/engine/core/Core.kt:142).

- [ ] Запроєктувати versioned ciphertext/key aliases або явну міграцію зі співіснуванням записів; не видаляти дані/ключі при помилці.
- [ ] Тест: кілька старих записів → нове шифрування → усі старі та нові записи читаються, також після перезапуску процесу.
- [ ] Невдала міграція не псує старі записи. Нове шифрування не повертається до передбачуваного секрету; legacy-доступ має явно обмежений контракт сумісності.

## BIO-REG-003 — відкладений software fallback після system failure

Модулі: biometric; прямі споживачі — biometric-custom-voice, biometric-custom-behavior.

Сценарій: API28/AUTO + ANY, system fingerprint та Voice/Behavior; системний prompt закінчується фатальною помилкою. Software потребує ready extras і ще не запущено.

Причина STATIC: completion стає PENDING, але systemPromptStarted залишається true; shouldShowPostSystemCompatDialog не відкриває UI, потрібний для запуску software. Не покладатися на випадковий lifecycle-cancel як коректний кінцевий результат.

Код: [completion та fallback](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt:845), [stage plan](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricAuthState.kt:191), [VoicePromptFactory](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-voice/src/main/java/dev/skomlach/biometric/compat/engine/internal/voice/VoicePromptFactory.kt:10).

- [ ] Моделювати активність/завершення system prompt окремо від факту його колишнього запуску.
- [ ] Тест повного переходу: system fatal → software UI → success/failure/cancel; рівно один terminal callback.
- [ ] Перевірити також скасування та вилучення останнього legacy-маршруту через permission failure: не залишати PENDING без активного маршруту.

## BIO-REG-004 — LeakCanary / Toast detached View

Джерело: пункт 1 користувача, [оригінальний скріншот](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/docs/reviews/evidence/2026-09-05-pixel9profold-toast-leak.png). Пріоритет P2 до визначення власника; модуль поки не встановлений.

TRACE: FileObserver thread → PathClassLoader → leakcanary.ToastEventListener.toastCurrentlyShown → android.widget.Toast.mNextView → detached FrameLayout. Утримується приблизно 13.0 kB/161 objects у Toast і 10.2 kB/141 objects у View; watchDurationMillis=56041, retainedDurationMillis=51040. Context — dev.skomlach.biometric.app.MainActivity, mDestroyed=false. Це кандидат на утримання View, а не доведений leak знищеної Activity. Власником static-посилання на скріншоті є LeakCanary; не оголошувати ні production leak, ні false positive без перевірки.

Точка перевірки в бібліотеці: [Toast у legacy-error callback](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt:987). Зв’язок саме цього Toast зі скріншотом поки не доведено. Код app лишається поза scope виправлень модулів; test harness дозволяє локалізувати походження.

- [ ] Зібрати повний leak trace/heap, версію LeakCanary та stack створення Toast.
- [ ] Повторити auth/error/cancel, дочекатися завершення Toast; порівняти retained objects після GC та між циклами.
- [ ] Визначити власника (library / test harness / LeakCanary / platform), потім виправляти відповідний lifecycle. Не вимикати leak detection як доказ виправлення.
- [ ] Закриття: View звільняється після dismiss або доказово встановлено instrumentation-only утримання й відсутність накопичення.

## BIO-REG-005 — FINGERPRINT успішно завершується через face

Джерело: пункт 2 користувача. AuthFlow AUTO або BIOMETRIC_API + FINGERPRINT, на Pixel enrolled face і fingerprint. Фактично спрацьовує face; очікування — підтвердження пальцем.

STATIC: allowedAuthenticators залежить від crypto, а не від запитаного сенсора; checkAuthResult розносить системний результат на primary types. Таке маркування не доводить фактично використаний сенсор.
Код: [allowedAuthenticators](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt:185), [completedTypes](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt:784).

- [ ] Визначити строгий контракт FINGERPRINT: або маршрут із доказом конкретного сенсора, або явна непідтримуваність цього контракту на generic system API.
- [ ] Не повертати специфічний fingerprint-success лише на підставі назви request.
- [ ] Device acceptance: face не завершує strict FINGERPRINT успіхом; finger завершує лише за підтверджуваного маршруту; загальний biometric-success має чесну семантику.
- [ ] STRONG не вважати fingerprint-фільтром; спільне рішення з BIO-REG-006.

## BIO-REG-006 — FACE успішно завершується через fingerprint

Джерело: перша частина пункту 3 користувача. AuthFlow AUTO або BIOMETRIC_API + FACE; після перемикання на fingerprint спрацьовує палець. Користувач описав це як «енролиться палець»; наявний опис AuthFlow не доводить створення нового hardware-шаблону — розділити authentication result та реальну зміну enrollment.

Код/причина маркування — ті самі, що BIO-REG-005; симптом залишено окремо для acceptance.

- [ ] FACE request не видає fingerprint-success за перевірений face-success.
- [ ] Не застосовувати WEAK як face-only фільтр: він також допускає STRONG.
- [ ] Device acceptance: face/finger перемикання, crypto on/off, AUTO/BIOMETRIC_API; зіставити result із фактичним сенсором. Окремо перевірити, чи змінювався hardware enrollment.
- [ ] Якщо strict FACE неможливий через доступний public API — явно відобразити обмеження, не підміняти гарантію.

## BIO-REG-007 — два biometric UI одночасно

Локальне виправлення standard AUTO/FINGERPRINT: [перевірений runtime-профіль](2026-09-05-verified-framework-ui-profile.md). На Pixel після явного збереження SYSTEM звичайна кнопка без override показує лише системний діалог; success підтверджено логом і користувачем. AUTO/ANY збережено на API28 з face recognition. Це локальна явна конфігурація, не автоматична OEM-детекція; для неперевірених пристроїв UNKNOWN і загальний OPEN залишаються.

Runtime follow-up: Pixel explicit SYSTEM fixture — mixed LEGACY ANY/ALL пройшов fingerprint → voice → success із двома результатами, без одночасних діалогів за підтвердженням користувача. ANY завершується пальцем без voice; cancel першого етапу завершує весь запит. [Часові мітки й APK](2026-09-05-biometric-ui-ownership.md). Загальний OPEN лишається для UNKNOWN/OEM та неперевірених сценаріїв; попередня прогалина mixed-success на цьому Pixel закрита.

Повторне рев’ю: змішаний legacy-запит більше не стартує SYSTEM fingerprint разом із compat-модулями; додано послідовні етапи, stage-generation guard і завершення при втраті доступності наступного етапу. 16 targeted tests PASS. Статичну знахідку виправлено; нові mixed/enrollment переходи ще потребують device QA. Загальний статус OPEN зберігається.

Оновлення: [backend UI ownership](2026-09-05-biometric-ui-ownership.md) реалізовано: SYSTEM/COMPAT/UNKNOWN, framework-only override, session snapshot та заборона неявної заміни backend для SYSTEM. 11 targeted tests PASS. На Pixel 9 Pro Fold explicit SYSTEM fixture пройшов AUTO/FINGERPRINT success і cancel; користувач підтвердив лише системний діалог. AUTO/VOICE зберіг COMPAT UI та коректно скасувався. Автоматичний default для бокового сенсора лишається UNKNOWN; дефект загалом відкритий, інші OEM і mixed/enrollment transitions не перевірені.

Джерело: друга частина пункту 3 користувача. AuthFlow, «будь-який FINGERPRINT»: fallback dialog з fingerprint icon, поверх нього системний biometric UI. Уточнення матриці API робити під час відтворення, не звужувати репорт лише до AUTO.

Очікування: один активний інтерактивний prompt. Допустимий фон/blur не має виглядати як другий biometric dialog або дублювати кнопки/стани.

Кандидати, не доведена причина: [initial compat dialog policy](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt:479), [ActivityViewWatcher](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/utils/activityView/ActivityViewWatcher.kt:27), WindowForegroundBlurring, legacy route selection. Треба відрізнити справжній compat dialog від overlay із піктограмами.

- [ ] Відтворити AUTO, BIOMETRIC_API, LEGACY_API + FINGERPRINT, folded/unfolded; записати вікна, обраний route, OEM UI flags.
- [ ] Для system route — один system prompt; для справжнього legacy-only route — потрібний compat UI збережено.
- [ ] Після cancel/success не лишаються overlay, blur, активний sensor або другий callback.

## BIO-REG-008 — Setup AUTO ANY не доходить до voice enrollment

Джерело: пункт 4 користувача. Hardware face+fingerprint уже enrolled, software voice — єдиний шаблон, який треба створити. Фактично показано лише face icon і face auth.

Очікування користувача: hardware слугує підтвердженням, voice — enrollment target. Не ототожнювати загальний ANY-success з виконаним setup voice і не вимагати непідтверджуваного окремого успіху обох system-сенсорів.

STATIC-кандидати: [pending / pre-satisfied / confirmed enrollment](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:2261), shouldKeepSystemEnrollType (зберігає hardware face), resolveEnrollSessionOutcome. Спостереження підтверджено користувачем; конкретна причина завершення на face ще потребує трасування.

- [ ] Розділити confirmation types та enrollment targets; уже enrolled hardware не замінює voice enrollment.
- [ ] Hardware confirmation → permission/preparation voice → створення voice template → лише потім успішне завершення setup.
- [ ] Cancel або voice failure не повертає хибний setup success; існуючі hardware enrollment не видаляються.
- [ ] Перевірити ANY/ALL відповідно до визначеного контракту, повторний setup і rollback лише новостворених software-даних.
- [ ] Пов’язано з BIO-REG-001/003, але не дубль: тут system auth успішний і hardware вже enrolled.

## BIO-REG-009 — Setup AUTO Finger/Face виглядає як AuthFlow

Джерело: пункт 5 користувача. Hardware уже enrolled. Поточна поведінка схожа на звичайну авторизацію.

Очікування: явний результат «підтверджено наявну біометрику» проти «додано нову»; системний enrollment UI потрібен при відсутньому enrollment або явному запиті enrollNewHardwareBiometric. Сам по собі authentication UI для підтвердження вже enrolled hardware може бути коректним — не оголошувати його дефектом без уточнення результату.

Код: [setupBiometric](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:338), [systemSetup](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:437), enrollment outcome / confirmed-types.

- [ ] Матриця Finger і Face: enrolled / not enrolled × explicit hardware enroll true / false.
- [ ] Визначити очікувані callback/result/UI для кожного випадку; жодного нового шаблону не заявляти без доказу.
- [ ] Перевірити cancel із системних налаштувань та повернення без нового enrollment.
- [ ] Узгодити з BIO-REG-005/006/008 без перетворення кожного setup на безумовний повторний hardware enrollment.

## BIO-REG-010 — microphone permission після відмови

Джерело: пункт 6 користувача. Setup LEGACY_API + VOICE: відмовити RECORD_AUDIO, завершити спробу, повторити setup — запит більше не з’являється.

Кандидати: [preflight permissions](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt:1176), [PermissionsFragment policy](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/common/src/main/java/dev/skomlach/common/permissionui/PermissionsFragment.kt:264), VoiceBiometricManager.getPermissions, доступність/кеш маршруту. beginAuthFlow уже очищає disabled module/type sets — збережений blacklist не є встановленою причиною.

Android може припинити показ системного permission dialog після повторних відмов. Відрізнити першу відмову, повторну/USER_FIXED та library suppression. Не вимагати обходу рішення ОС.

- [ ] Відтворити після першої відмови та після повторних; зафіксувати RECORD_AUDIO flags, rationale, факт виклику request і вихід із preflight.
- [ ] Повторити на тому самому Builder і з новим Builder/процесом; окремо після ручного grant у Settings.
- [ ] Якщо OS дозволяє повторний prompt — наступний явний setup повертає користувача в permission flow.
- [ ] Якщо OS більше не показує prompt — зрозумілий denied-result і явна дія переходу в Settings; немає нескінченного очікування або запуску capture без permission.
- [ ] Після grant voice enrollment працює; canceled-попередня сесія не впливає на нову.

## Межі Android API, перевірені 2026-09-05

BIOMETRIC_STRONG означає Class 3; BIOMETRIC_WEAK — Class 2 або вище і включає STRONG. Це вимоги до сили автентифікатора, а не вибір face/fingerprint. [AndroidX Authenticators](https://developer.android.com/reference/androidx/biometric/BiometricManager.Authenticators).

AuthenticationResult.getAuthenticationType відрізняє biometric/device credential/unknown, але не повертає окремо face чи fingerprint і не надає фактичний strength використаного сенсора. Для поточного generic AndroidX шляху не обіцяти modality-specific proof без додаткового підтверджуваного API. [AuthenticationResult](https://developer.android.com/reference/androidx/biometric/BiometricPrompt.AuthenticationResult).

Google вказує, що Pixel 8 і новіші, включно з Fold, можуть використовувати Face Unlock для підтвердження особи в застосунках. Це ще одна причина не ототожнювати сильну біометрику лише з пальцем; конкретний strength сенсора на тестовій AOS17-збірці тут не вимірювався. [Pixel Face Unlock](https://support.google.com/pixelphone/answer/9517039?hl=en).

Повторні відмови можуть вимкнути подальший системний permission prompt; під час діагностики врахувати USER_SET/USER_FIXED. [Runtime permissions](https://developer.android.com/training/permissions/requesting).

## Порядок роботи та критерій закриття рев’ю

Спершу BIO-REG-001/002/003 і контракт BIO-REG-005/006; далі переходи setup/permissions BIO-REG-008/010, потім UI і leak-діагностика. Це пріоритизація, не дозвіл ігнорувати відкриті P2.

Для закриття потрібні focused regression tests на рівні orchestration/crypto, а не лише helper predicates; після змін — targeted verification за AGENTS.md і device replay на тому самому Pixel з ідентифікованим APK. Зберегти доказ по кожному ID та відмінність USER-DEVICE / агентське відтворення. Не закривати задачі лише через попередній зелений набір тестів.

Перший прохід змінював лише документацію. Поточний прохід включає код biometric/common, focused tests, debug APK та Pixel QA; точні команди наведено у verification report. Release/R8, full suite, інші пристрої, commit/push не виконувалися.

## AUTO/ANY AuthFlow: програмна біометрика поряд із системним prompt (2026-09-05)

Відновлено паралельне voice-захоплення через існуючий delegate/onReady(extras), з Toast-підказками без другого діалогу. Варіант із кнопкою «Інший спосіб» відкочено. На Pixel підтверджено voice-success і Back зі зупинкою мікрофона. Enrollment залишається послідовним; інші UI-залежні software providers не оголошено придатними до фонового захоплення. Команди, SHA та межі: [звіт](2026-09-05-parallel-voice-auth.md). BIO-REG-007 не закрито для всіх OEM.

### Follow-up: незалежний старт Legacy-маршрутів

P2 з фінального рев’ю parallel voice виправлено в коді: готові модулі стартують одразу, кожен підготовлений provider долучається окремо до тієї самої Legacy-сесії. Немає прив’язки до VOICE і спільного бар’єра очікування. 13 цільових JVM-тестів пройшли, включно з реальними LegacyBiometric/Core та тестовими fingerprint/face/iris модулями. Новий APK і device replay не виконувалися; попередній Pixel APK цих змін не містить. [Деталі та команди](2026-09-05-independent-legacy-routes.md).
