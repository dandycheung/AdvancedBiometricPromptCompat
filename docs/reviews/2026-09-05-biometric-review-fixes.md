# Виправлення рев’ю біометричних модулів — 2026-09-05

## Поточна резолюція після виправлень і Pixel QA

**Актуальна корекція:** фокусний fingerprint UI алгоритм відкликано; BIO-REG-007 знову OPEN. Multi-window metrics/host ownership/lifecycle оновлено, legacy збережено; 18 tests і debug assemble PASS. [Android 17 multi-window звіт](2026-09-05-multiwindow-android17.md) має пріоритет над нижчими історичними висновками.

**Оновлення UI policy:** модельний виняток Pixel/API37 замінено session-scoped арбітражем фокуса для framework fingerprint-only routes. Permission settings dialog надає пріоритет системному перекладу. Нові зміни, 15 focused tests і межі доказів — у [свіжому звіті](2026-09-05-universal-fingerprint-ui.md). Наведений нижче артефакт і повний device run належать попередній реалізації.

**Виправлення внесено; повного release sign-off ще немає.** Актуальний стан усіх 10 знахідок — у [беклозі](biometric-regression-backlog.md), точні зміни, команди, результати та обмеження — у [verification report](2026-09-05-pixel-regression-verification.md). Нижчі висновки попередніх проходів є історією і не замінюють цей стан.

Checkout main, HEAD `91163809` + робочі зміни. Змінено `biometric` і `common`; код `app` не редагувався. Фінальний debug APK встановлено на Pixel 9 Pro Fold / API 37, lastUpdateTime `2026-09-05 17:09:42`; SHA256 `25C7E7DD327D9F05076806168B722BF00A8DA62D38037277D22832D1380BB664`. Staged/unstaged сторонні зміни збережені, commit/push не виконано.

| Область | Результат | Залишок |
| --- | --- | --- |
| Preflight першого hardware enrollment | Код відділяє етап до Settings від effective software checks | Пристрій без hardware templates не тестувався |
| Legacy/new crypto | Versioned actual IV, authenticated legacy fallback лише для historical format; 9/9 JVM та Android runtime tests | Реальний APK upgrade/Keystore restart і споживачі з фіксованим 12-byte IV ще в QA |
| System → software / Setup ANY | Hardware confirmation не завершує voice setup; completion scope незмінний | Негативний system-fatal replay ще потрібний |
| FACE/FINGERPRINT контракт | AUTO Finger — strict legacy; generic BIOMETRIC_API — явний unsupported, жодного вигаданого sensor success | FACE-only не підтримується через generic API; STRONG/WEAK цього не доводять |
| Подвійний fingerprint UI | На Pixel прибрано compat dialog та зайві background icons для system-owned route | Folded/інші прошивки не перевірялися |
| Voice permissions | USER_FIXED → Settings → grant → voice capture; lifecycle повернення виправлено | Перша відмова та той самий Builder ще в QA |
| Toast retention | Library Toast прибрано | Початковий leak trace не локалізований; BIO-REG-004 лишається Investigate |

Спільний device run підтвердив AUTO Finger fingerprint-only без нижнього fallback; Setup AUTO ANY перейшов до voice enrollment і завершився `BIOMETRIC_VOICE`, наступна voice auth теж успішна. Ці повні цикли виконано на batch 2. На final APK повторено AUTO/FACE (unsupported) і LEGACY/FINGERPRINT; точне підтвердження callback наведене у verification report.

Focused Gradle tests і фінальний targeted debug assemble пройшли. Перші помилки test compilation та Android Cipher initialization виправлені й повторно перевірені; вони не приховані зі звіту. Full suite, release/R8, інші пристрої, heap dump, first hardware enrollment, APK downgrade/upgrade не запускалися. До закриття перелічених залишків не називати рев’ю повністю чистим.
## Історія: попередній повторний regression-review


П’ять зауважень останнього review опрацьовано в робочому дереві:

1. API28, Generic і Silent тепер мають generation/token для кожної auth-сесії. Пізні system/legacy callback-и попередньої сесії ігноруються; API28 створює окремий системний `BiometricPrompt` із session-bound callback для кожної авторизації.
2. Decrypt без нового protected secret використовує старий сумісний секрет із перевернутої назви ключа. Нове encrypt як і раніше створює і синхронно зберігає випадковий 256-бітний секрет.
3. Canceled-результати належать `AuthSessionState`, очищуються на `begin()` та `invalidate()` і не переносяться в наступну API28/Generic/Silent сесію.
4. Відкладений lifecycle-dismiss скасовується на `ON_START`/`ON_RESUME`, тому короткий background/foreground перехід не може прибрати вже відновлений biometric UI або залишити blur cleanup у неправильному стані.
5. Усі production helper-файли, від яких залежать зміни, були присутні та пройшли компіляцію в тому проході. Згадка про untracked стосувалася тогочасного стану, а не поточного index; перед комітом перевіряти актуальний git status.

Нові regression-тести спочатку підтвердили RED для відсутньої session isolation, foreground-cancel policy та legacy decrypt fallback; після реалізації всі сфокусовані запуски завершились `BUILD SUCCESSFUL`. Повторно перевірено cancellation/completion/permission/API28 routing; нових P0/P1 у зміненій області статично не знайдено. `diff --check` для змінених tracked-файлів — без помилок.

```powershell
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.impl.AuthSessionStateTest" --tests "dev.skomlach.biometric.compat.impl.PendingAuthStartTest" --tests "dev.skomlach.biometric.compat.impl.AuthenticationCompletionPolicyTest" --tests "dev.skomlach.biometric.compat.AuthFlowCompletionTest" --tests "dev.skomlach.biometric.compat.AuthFlowGateTest" --tests "dev.skomlach.biometric.compat.BiometricPermissionPolicyTest" --tests "dev.skomlach.biometric.compat.Api28StartAuthPlanTest" --tests "dev.skomlach.biometric.compat.engine.LegacyBiometricCancellationTest" --tests "dev.skomlach.biometric.compat.utils.appstate.AppBackgroundDetectorPolicyTest" --tests "dev.skomlach.biometric.compat.crypto.AppFlowSecretStoreTest" --console=plain
```

Android device QA, RoboForm rebuild/install, release/R8 і повний suite у цьому проході не запускались.

## Повторна перевірка: відкладені помилки Generic/Silent

Попередній висновок про завершення був передчасним: після першого набору виправлень залишався P1 — відкладена LOCKED_OUT-помилка могла завершити наступну сесію. Цей блокер тепер виправлено.

- Generic і Silent мають окремий скасовуваний таймер помилки з перевіркою generation, включно з runnable, уже взятим із черги. Таймер інвалідується при новій авторизації, перезапуску маршруту, cancel і закритті UI. Callback та результати фіксуються під час планування.
- Silent використовує такий самий захист для таймера автоматичного скасування. Лічильник помилок і набір canceled очищаються на початку нової авторизації.
- Зупинка сенсора через stopAuth сама по собі залишає таймер кінцевої помилки активним: тимчасова втрата фокуса не повинна поглинути результат. Закриття сесії скасовує його.
- Повторно прочитано authenticate/startAuth/stopAuth/cancelAuthentication/onUiClosed, доставку відкладених помилок обох реалізацій, спільний scheduler та прямий шлях cleanup. У цих змінених шляхах нових підтверджених блокерів не знайдено. Це обмежений повторний огляд, а не новий аудит усіх модулів.

Змінено лише модуль biometric: BiometricPromptGenericImpl.kt, BiometricPromptSilentImpl.kt, PendingAuthStartTest.kt, LegacyBiometricCancellationTest.kt; також оновлено цей звіт. Публічний API та залежності не змінювались.

Один запуск перевірки завершився BUILD SUCCESSFUL; PendingAuthStartTest — 4 тести, LegacyBiometricCancellationTest — 3, разом 7, без failures/errors/skipped. Debug Kotlin скомпільовано як залежність тестової задачі. Доданий тест перевіряє scheduler разом із реальним Core cancellation signal: застаріла помилка не доставляється і не скасовує новий сигнал; актуальна помилка доставляється і скасовує його. Це JVM-перевірка, без Android UI або реального сенсора.

```powershell
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.impl.PendingAuthStartTest" --tests "dev.skomlach.biometric.compat.engine.LegacyBiometricCancellationTest" --console=plain
```

Scoped diff check — exit 0:

```powershell
git -c safe.directory=C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat -c core.safecrlf=false diff --check -- biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptGenericImpl.kt biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptSilentImpl.kt biometric/src/test/java/dev/skomlach/biometric/compat/engine/LegacyBiometricCancellationTest.kt biometric/src/test/java/dev/skomlach/biometric/compat/impl/PendingAuthStartTest.kt docs/reviews/2026-09-05-biometric-review-fixes.md
```

Повний suite, app/release/R8, downstream-збірки та device QA не запускались. Android UI/OEM lifecycle залишаються runtime-неперевіреними; зазначені нижче зміни сумісності crypto/ALL залишаються актуальними. Коміт і push не виконувались.

## Попередній набір виправлень і перевірок

Сім зауважень виправлено у поточному робочому дереві main. Зміни не закомічені. Код app не змінювався; сторонні локальні зміни збережено.

## Результат

1. Generic, Silent і API28 використовують спільну політику завершення: ALL вимагає успіху кожного обов’язкового типу; фатальна помилка завершує ALL відмовою. API28 записує legacy-результат для його власного типу.
2. Автоматичний crypto fallback використовує випадковий 256-бітний секрет із захищених preferences замість перевернутої назви ключа. Збереження секрету перевіряється до повернення cipher. При помилці fallback ключ більше не видаляється автоматично для повторної спроби.
3. Legacy cleanup скасовує всі Core cancellation signals незалежно від стану start gate.
4. Відкладені старти Generic/Silent/API28 та очікування legacy init мають власника й generation; cancel видаляє та інвалідує runnable.
5. Завершення до відкриття UI також звільняє flow; помилки crypto verification проходять той самий cleanup.
6. SoftwareBiometricModule приймає швидкий success через session token, без фільтра config_shortAnimTime.
7. Cleanup завершується до release і зовнішніх callback-ів. Lifecycle-таймери скасовуються під час detach, старий observer видаляється за захопленим посиланням. Callback може запустити наступний flow.

Додатково: FaceAuthenticationAttemptPolicy тепер викликається production-кодом; формальний Voice тест константи замінено тестами фактичного шляху доставки success.

## Сумісність та межі доказів

- Старі ciphertext автоматичного fallback, створені з передбачуваним секретом, розшифровуються сумісним legacy-шляхом лише коли нового protected secret для ключа ще немає. Нове шифрування використовує випадковий protected secret; при невдалому legacy decrypt дані або ключі автоматично не видаляються.
- ALL для кількох primary-модальностей одного системного prompt відхиляється з INTERNAL_ERROR: один системний результат не доводить окремий успіх кожної модальності. ANY та ALL із однією системною модальністю дозволені.
- Рівень fallback залишається APP_FLOW_NOT_BIOMETRIC_BOUND. Захищене зберігання спирається на існуючий SharedPreferenceProvider та його стандартну політику fail-closed для Keystore.
- Повна збірка app, release/R8, повний набір тестів, instrumented/device QA не запускались. Не підтверджено runtime-поведінку OEM, USB/JNI, Android Keystore, camera/microphone lifecycle та стійкість до підробок.

## Перевірка

Фінальний запуск: BUILD SUCCESSFUL; 55 тестів, 0 failures, 0 errors, 0 skipped. biometric: 50, Face-TF: 3, Voice: 2. Компіляція debug Kotlin змінених модулів виконана як залежність unit-test tasks.

Перший запуск також був успішний; після додаткових lifecycle-виправлень виконано фінальний запуск із перевірками enrollment та API28.

Перший запуск:

```powershell
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.AuthFlowCompletionTest" --tests "dev.skomlach.biometric.compat.AuthFlowGateTest" --tests "dev.skomlach.biometric.compat.impl.AuthenticationCompletionPolicyTest" --tests "dev.skomlach.biometric.compat.impl.PendingAuthStartTest" --tests "dev.skomlach.biometric.compat.crypto.AppFlowSecretStoreTest" --tests "dev.skomlach.biometric.compat.engine.LegacyBiometricCancellationTest" --tests "dev.skomlach.biometric.compat.custom.SoftwareBiometricSessionGuardTest" :biometric-custom-face-tf:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.internal.face.tensorflow.FaceAuthenticationAttemptPolicyTest" :biometric-custom-voice:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.internal.voice.VoiceBiometricManagerFlowTest" --console=plain
```

Фінальний запуск:

```powershell
.\gradlew.bat :biometric:testDebugUnitTest --tests "dev.skomlach.biometric.compat.AuthFlowCompletionTest" --tests "dev.skomlach.biometric.compat.AuthFlowGateTest" --tests "dev.skomlach.biometric.compat.impl.AuthenticationCompletionPolicyTest" --tests "dev.skomlach.biometric.compat.impl.PendingAuthStartTest" --tests "dev.skomlach.biometric.compat.crypto.AppFlowSecretStoreTest" --tests "dev.skomlach.biometric.compat.engine.LegacyBiometricCancellationTest" --tests "dev.skomlach.biometric.compat.custom.SoftwareBiometricSessionGuardTest" --tests "dev.skomlach.biometric.compat.Api28StartAuthPlanTest" --tests "dev.skomlach.biometric.compat.EnrollOutcomeResolverTest" --tests "dev.skomlach.biometric.compat.EffectiveBiometricCancellationTest" :biometric-custom-face-tf:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.internal.face.tensorflow.FaceAuthenticationAttemptPolicyTest" :biometric-custom-voice:testDebugUnitTest --tests "dev.skomlach.biometric.compat.engine.internal.voice.VoiceBiometricManagerFlowTest" --console=plain
```

Scoped diff check (exit 0, виконано до й після фінального запуску):

```powershell
git -c safe.directory=C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat -c core.safecrlf=false diff --check -- biometric biometric-custom-face-tf biometric-custom-voice
```

## Змінені файли цієї роботи

Список порівняно зі знімком робочого дерева перед виправленнями, а не з HEAD; він не включає сторонні попередні зміни.

- [biometric/src/main/java/dev/skomlach/biometric/compat/AuthFlowCompletion.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/AuthFlowCompletion.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/BiometricPromptCompat.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/crypto/AppFlowCryptoStorage.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/crypto/AppFlowCryptoStorage.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/crypto/AppFlowSecretStore.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/crypto/AppFlowSecretStore.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/crypto/BiometricCryptoObjectHelper.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/crypto/BiometricCryptoObjectHelper.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/engine/core/Core.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/engine/core/Core.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/engine/internal/SoftwareBiometricModule.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/engine/internal/SoftwareBiometricModule.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/engine/LegacyBiometric.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/engine/LegacyBiometric.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/impl/AuthenticationCompletionPolicy.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/AuthenticationCompletionPolicy.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptApi28Impl.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptGenericImpl.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptGenericImpl.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptSilentImpl.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/BiometricPromptSilentImpl.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/impl/PendingAuthStart.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/impl/PendingAuthStart.kt)
- [biometric/src/main/java/dev/skomlach/biometric/compat/utils/appstate/AppBackgroundDetector.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/main/java/dev/skomlach/biometric/compat/utils/appstate/AppBackgroundDetector.kt)
- [biometric/src/test/java/dev/skomlach/biometric/compat/AuthFlowCompletionTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/test/java/dev/skomlach/biometric/compat/AuthFlowCompletionTest.kt)
- [biometric/src/test/java/dev/skomlach/biometric/compat/crypto/AppFlowSecretStoreTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/test/java/dev/skomlach/biometric/compat/crypto/AppFlowSecretStoreTest.kt)
- [biometric/src/test/java/dev/skomlach/biometric/compat/engine/LegacyBiometricCancellationTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/test/java/dev/skomlach/biometric/compat/engine/LegacyBiometricCancellationTest.kt)
- [biometric/src/test/java/dev/skomlach/biometric/compat/impl/AuthenticationCompletionPolicyTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/test/java/dev/skomlach/biometric/compat/impl/AuthenticationCompletionPolicyTest.kt)
- [biometric/src/test/java/dev/skomlach/biometric/compat/impl/PendingAuthStartTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric/src/test/java/dev/skomlach/biometric/compat/impl/PendingAuthStartTest.kt)
- [biometric-custom-face-tf/src/main/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/FaceAuthenticationAttemptPolicy.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-face-tf/src/main/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/FaceAuthenticationAttemptPolicy.kt)
- [biometric-custom-face-tf/src/main/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/TensorFlowFaceUnlockManager.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-face-tf/src/main/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/TensorFlowFaceUnlockManager.kt)
- [biometric-custom-face-tf/src/test/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/FaceAuthenticationAttemptPolicyTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-face-tf/src/test/java/dev/skomlach/biometric/compat/engine/internal/face/tensorflow/FaceAuthenticationAttemptPolicyTest.kt)
- [biometric-custom-voice/src/main/java/dev/skomlach/biometric/compat/engine/internal/voice/VoiceBiometricManager.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-voice/src/main/java/dev/skomlach/biometric/compat/engine/internal/voice/VoiceBiometricManager.kt)
- [biometric-custom-voice/src/test/java/dev/skomlach/biometric/compat/engine/internal/voice/VoiceBiometricManagerFlowTest.kt](/C:/Users/skoml/StudioProjects_5/AdvancedBiometricPromptCompat/biometric-custom-voice/src/test/java/dev/skomlach/biometric/compat/engine/internal/voice/VoiceBiometricManagerFlowTest.kt)
