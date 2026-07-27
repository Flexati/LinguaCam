# LingoLens — Offline Camera Translator

**LingoLens** è un'applicazione Android di traduzione visuale offline-first. L'app utilizza la fotocamera del dispositivo per riconoscere il testo e tradurlo in tempo reale, senza richiedere una connessione internet.

## 🎯 Visione del Prodotto

> "Traduci il mondo intorno a te. Anche senza internet."

LingoLens è pensata per i viaggiatori, gli espatriati e chiunque si trovi in un luogo straniero con una connessione internet limitata o assente. L'app funziona completamente offline dopo il download iniziale dei modelli linguistici.

## ✨ Caratteristiche Principali

### Core MVP
- **Traduzione in tempo reale**: Punta la fotocamera verso un cartello, menu o testo straniero e visualizza la traduzione istantaneamente
- **Offline-first**: Funziona completamente senza internet dopo il download dei modelli
- **Privacy-first**: Elaborazione on-device, nessun invio di dati a server esterni
- **Selezione lingue**: Supporta 20 lingue al lancio
- **Overlay visivo**: La traduzione viene mostrata direttamente sulla preview della fotocamera

### Piano Gratuito
- Massimo 2 lingue installate
- Esperienza completa di traduzione
- Nessun dark pattern o limitazioni artificiali

### Piano Pro (Acquisto una tantum)
- Lingue illimitate
- Cronologia traduzioni
- Modalità conversazione bidirezionale
- Accesso prioritario a nuove lingue

## 🛠 Stack Tecnologico

| Componente | Tecnologia | Versione |
|-----------|-----------|---------|
| Linguaggio | Kotlin | 1.9.10 |
| UI Framework | Jetpack Compose | 2023.10.00 |
| Camera | CameraX | 1.3.0 |
| OCR | ML Kit Text Recognition | 16.0.0 |
| Traduzione | ML Kit Translation | 17.0.1 |
| Riconoscimento lingua | ML Kit Language ID | 17.0.4 |
| State Management | ViewModel + Coroutines | - |
| Design System | Material 3 | 1.1.1 |
| Logging | Timber | 5.0.1 |

## 📋 Lingue Supportate

LingoLens supporta le seguenti 20 lingue al lancio:

| Codice | Lingua | Nativo |
|--------|--------|--------|
| it | Italiano | Italiano |
| en | Inglese | English |
| es | Spagnolo | Español |
| fr | Francese | Français |
| de | Tedesco | Deutsch |
| pt | Portoghese | Português |
| ru | Russo | Русский |
| ja | Giapponese | 日本語 |
| zh | Cinese | 中文 |
| ko | Coreano | 한국어 |
| ar | Arabo | العربية |
| hi | Hindi | हिन्दी |
| tr | Turco | Türkçe |
| nl | Olandese | Nederlands |
| pl | Polacco | Polski |
| sv | Svedese | Svenska |
| da | Danese | Dansk |
| fi | Finlandese | Suomi |
| el | Greco | Ελληνικά |
| cs | Ceco | Čeština |

## 🏗 Architettura

LingoLens segue il pattern **MVVM** con **Clean Architecture**:

```
app/
├── data/
│   └── repository/
│       ├── LanguageModelRepository.kt     (Gestione modelli)
│       ├── TranslationRepository.kt       (Traduzione on-device)
│       └── OcrRepository.kt               (Riconoscimento testo)
├── domain/
│   └── model/
│       ├── Language.kt                    (Modello lingua)
│       └── TranslationResult.kt           (Risultato traduzione)
├── presentation/
│   ├── screen/
│   │   ├── MainScreen.kt                  (Schermata principale)
│   │   └── CameraScreen.kt                (Schermata fotocamera)
│   ├── viewmodel/
│   │   └── MainViewModel.kt               (State management)
│   └── ui/
│       └── theme/
│           ├── Theme.kt                   (Tema Material 3)
│           └── Type.kt                    (Tipografia)
└── MainActivity.kt                        (Entry point)
```

## 🔐 Principi Non Negoziabili

1. **OFFLINE-FIRST**: La traduzione funziona senza connessione dopo il download iniziale dei modelli
2. **PRIVACY-FIRST**: Testo e immagini elaborati on-device, nessun invio a server esterni
3. **ZERO API A CONSUMO**: Nessuna API cloud a pagamento per la funzione principale
4. **PRODOTTO REALE**: Un'app installabile, usabile e acquistabile, non una demo
5. **ZERO FAKE FEATURES**: Nessuna funzione dichiarata "fatta" se non verificata funzionante
6. **ROBUSTEZZA**: Ogni feature verificata con test reali e gestione errori
7. **ACCESSIBILITÀ**: Supporto TalkBack, contrasto colore adeguato, target touch 48dp
8. **LOCALIZZAZIONE**: Interfaccia localizzata in italiano e inglese al lancio

## 📱 Requisiti di Sistema

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34
- **Permessi**: CAMERA, INTERNET, ACCESS_NETWORK_STATE, WRITE_EXTERNAL_STORAGE

## 🚀 Come Compilare

```bash
# Clone il repository
git clone https://github.com/yourusername/lingolens.git
cd lingolens

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build App Bundle per Google Play
./gradlew bundleRelease
```

## 🔄 Loop Engineering Protocol

Questo progetto segue il **Loop Engineering Protocol** per garantire qualità e verificabilità:

- **Loop 0**: Discovery (Analisi tecnica e rischi)
- **Loop 1**: Product Decomposition (Scomposizione in sistemi)
- **Loop 2**: Planning (Piano dettagliato per ogni feature)
- **Loop 3**: Implementation (Implementazione feature)
- **Loop 4**: Verify (Verifica tecnica)
- **Loop 5**: Adversarial Review (Ricerca attiva di problemi)
- **Loop 6**: Product Polish (Affinamento UX)
- **Loop 7**: Release Audit (Verifica pre-release)

Consulta `PROGRESS.md` per lo stato attuale del progetto.

## 📊 Stato del Progetto

**Sessione 1**: Core Foundation completato
- ✅ Struttura progetto Android
- ✅ Tema Material 3
- ✅ Modelli dati (Domain)
- ✅ Repository Layer (Language, Translation, OCR)
- ✅ ViewModel
- ✅ UI Compose (Main + Camera screens)

**In sviluppo**: Camera Integration e Overlay System

Consulta `PROGRESS.md` per dettagli completi.

## 🎨 Design System

LingoLens utilizza **Material 3** con una palette colore moderna:

| Colore | Hex | Utilizzo |
|--------|-----|----------|
| Primary | #00897B | Teal - Azioni principali |
| Secondary | #FF6F00 | Orange - Accenti |
| Tertiary | #7B1FA2 | Purple - Elementi secondari |
| Error | #D32F2F | Red - Messaggi di errore |
| Background | #FAFAFA | Light gray - Sfondo |

## 📝 Licenza

LingoLens è distribuito sotto licenza [MIT](LICENSE).

## 👥 Autore

Sviluppato da **Manus AI** per Loop Engineering.

---

**Prossimo passo**: Consultare `PROGRESS.md` per continuare lo sviluppo.

---

## Reality Check — 2026-07-27

**Stato reale del repository** (evidence-based, post Steps 1-12 del refactor 2026-07-24):

| Cosa | Stato | Prova |
|---|---|---|
| Codice sorgente | presente, ricompilato dopo Step 1-11 | 32+ file `.kt` in `app/src/main/` |
| Build (`./gradlew assembleDebug`) | **NON eseguito** da questa sessione | `app/build/outputs/apk/debug/` non esiste al 2026-07-27 |
| Build (`./gradlew bundleRelease`) | **NON eseguito** | nessun AAB firmato |
| Keystore release | **NON generato** | glob `*.jks` → 0 risultati |
| Play Console account | **NON creato da me** | è Step 6 manuale (tu) |
| Test unitari | ricompilati con firme reali (Step 11) | 5 nuovi test JVM in `app/src/test/` |
| Vecchi test `FavoritesRepositoryTest`/`PreferencesRepositoryTest` | richiedono Robolectric (non in scope v1.0) | NON eseguiti in JVM puro |

**Claim ritirati / da considerare inaccurati**:
- ❌ "Codice sorgente scritto e corretto" → oggi ricompilato ma non buildato end-to-end
- ❌ "100% test pass rate" → test ricompilati in JVM puro; gli strumentati richiedono Robolectric
- ❌ "PRODUCTION READY" → non verificato in assenza di build, device test, e Play Console upload

**Cosa serve ancora per Play Store**:
1. Eseguire `./gradlew assembleDebug` con JDK 17 + Android SDK
2. Generare keystore release (`keytool -genkey ...`) e conservarlo in `~/.lingolens/release.jks`
3. Eseguire `./gradlew bundleRelease`
4. Creare app su Play Console, prodotto in-app `pro_plan` (€4.99), Data Safety, Privacy policy URL
5. Testare l'AAB come Internal Tester prima della produzione

