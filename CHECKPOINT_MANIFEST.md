## CHECKPOINT MANIFEST

**Data/Ora del Checkpoint**: 23 Luglio 2026, 10:41 UTC+2
**Nome del Progetto**: LinguaCam

### Stato Generale

- **Stato della Build**: BLOCKED
- **Stato del Resource Linking**: VERIFIED (risolto per `colorTertiary` e `colorSurface`)
- **Stato della Compilazione Kotlin**: BLOCKED (errori di `Unresolved reference`, `Type mismatch`, `Experimental API`)

### File Modificati (VERIFIED)

- `/home/ubuntu/lingolens_project_working/settings.gradle.kts`
- `/home/ubuntu/lingolens_project_working/build.gradle.kts`
- `/home/ubuntu/lingolens_project_working/app/build.gradle.kts`
- `/home/ubuntu/lingolens_project_working/gradle.properties`
- `/home/ubuntu/lingolens_project_working/gradle/wrapper/gradle-wrapper.properties`
- `/home/ubuntu/lingolens_project_working/app/src/main/AndroidManifest.xml`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/data/repository/TranslationRepository.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/ui/theme/Theme.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/presentation/screen/MainScreen.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/presentation/screen/OnboardingScreen.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/presentation/screen/FavoritesScreen.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/presentation/screen/ProPlanScreen.kt`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/MainActivity.kt`
- `/home/ubuntu/lingolens_project_working/PROGRESS.md`

### File Creati (VERIFIED)

- `/home/ubuntu/lingolens_project_working/local.properties`
- `/home/ubuntu/lingolens_project_working/app/src/main/res/values/strings.xml`
- `/home/ubuntu/lingolens_project_working/app/src/main/java/com/linguacam/ui/theme/Typography.kt`

### File Eliminati (VERIFIED)

- `/home/ubuntu/lingolens_project_working/app/src/main/res/values/themes.xml`

### File Mancanti / Non Recuperabili (BLOCKED)

- Nessun file originale è stato perso in questa sessione, ma il file `Typography.kt` non era presente nell'archivio iniziale e la sua ricreazione è stata necessaria.

### Correzioni Gradle Applicate (VERIFIED)

- Aggiornamento delle versioni di AGP (8.2.0), Kotlin (1.9.20), Compose Compiler Extension (1.5.5), Compose BOM (2023.10.01).
- Aggiornamento di Gradle Wrapper alla versione 8.2.
- Aggiunta di `android.useAndroidX=true` in `gradle.properties`.
- Aggiunta di `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8` in `gradle.properties`.
- Abilitazione della generazione di `BuildConfig`.

### Correzioni AndroidManifest Applicate (VERIFIED)

- Rimozione dei riferimenti a risorse e stili mancanti.
- Correzione del posizionamento dell'attributo `android:allowBackup`.

### Errori di Build Ancora Presenti (BLOCKED)

- `Unresolved reference: rowPadding` in `CameraManager.kt` (nonostante il ripristino del file originale, indica un problema di compatibilità delle dipendenze CameraX).
- `Unresolved reference: contentPadding` in `FavoritesScreen.kt`.
- `Unresolved reference: viewModel` in `MainActivity.kt` (nonostante l'import e l'uso di `viewModel()`).
- `Type mismatch` in `TranslationRepository.kt` (relativo a `Translator?` vs `Translator`).
- `This material API is experimental` warnings in `MainScreen.kt`, `ProPlanScreen.kt` (non bloccanti, ma indicano l'uso di API instabili).

### Problemi Ancora Non Risolti (BLOCKED)

- La build non è ancora completata con successo a causa degli errori di compilazione Kotlin sopra elencati.
- La compatibilità delle dipendenze CameraX con le versioni aggiornate di Gradle e Kotlin necessita di ulteriore investigazione.
- La risoluzione degli errori `Unresolved reference` per `viewModel` e `contentPadding` richiede un'analisi più approfondita delle dipendenze Compose e ViewModel.
