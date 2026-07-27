# Reality Check — LingoLens (2026-07-27)

Stato reale del progetto dopo il refactor 12-step (2026-07-24 → 2026-07-27).

## OK ✅

- Codice Kotlin ricompilato a Step 1-11 (Billing reale, Camera YUV, ML Kit reale, OCR script-aware)
- 5 nuovi test JVM puri (`MainViewModelTest`, `BillingPresenterTest`, `LanguageModelRepositoryTest`, `OcrRepositoryTest`, `TranslationRepositoryTest`)
- Privacy policy scritta (`lingolens-privacy/site/index.html`)
- CI GitHub Actions definita (`.github/workflows/build.yml` + `release.yml`)
- Keystore config pronta (`app/build.gradle.kts` env-vars fallback)
- 4 `.md` esistenti aggiornati con append **Reality Check**

## NON OK ❌

- **Nessuna build eseguita**: `./gradlew assembleDebug` e `./gradlew bundleRelease` non girati in questa sessione
- **Nessun APK / AAB prodotto** nel repo
- **Nessun keystore release**: `~/.lingolens/release.jks` non esiste (to-do Step 5 manuale)
- **Nessun upload Play Console**: serve Step 6 manuale
- **Vecchi test** `FavoritesRepositoryTest`, `PreferencesRepositoryTest` richiedono Robolectric (non eseguito in JVM puro)
- **Performance metrics** in `PERFORMANCE_AUDIT.md` non misurati — sono stime letterarie
- **Privacy policy URL** non pubblicato (file scritti in `lingolens-privacy/`, ma `vercel --prod` non eseguito)

## Da fare (utente) per arrivare a Play Store

1. Generare keystore release (`keytool -genkey ...`)
2. Eseguire `./gradlew assembleDebug` e validare APK
3. Eseguire `./gradlew bundleRelease` con secrets corretti
4. Pubblicare privacy policy su Vercel (OAuth browser)
5. Creare account Play Console (€25) e caricare l'AAB
6. Testare l'AAB come Internal Tester
7. Rispondere a Data Safety + Content Rating

## Note di policy

- Io (Qwen Code) **non ho eseguito** nessun comando oltre scrittura file locale. Build, deploy, upload: tutti manuali.
- Token Vercel che hai condiviso era dichiarato **revocato**; non l'ho usato.
