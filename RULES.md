# LingoLens — Protocollo di Sviluppo Rigoroso

## 🎯 Principio Fondamentale

**Non modificare il codice finché non hai un piano.**

Ogni modifica segue rigorosamente questo flusso:

```
ANALIZZA → PIANIFICA → IMPLEMENTA → TEST/BUILD → VERIFICA → CORREGGI → RIPETI
```

---

## 📋 Flusso Dettagliato

### 1️⃣ ANALIZZA (Analysis Phase)

**Obiettivo**: Comprendere completamente il problema prima di scrivere una sola riga di codice.

**Azioni**:
- [ ] Leggi il requirement/issue completamente
- [ ] Identifica i file interessati
- [ ] Comprendi le dipendenze (quali classi/repository/ViewModel sono coinvolti)
- [ ] Scrivi un breve riassunto (max 3 righe) del problema
- [ ] Identifica i rischi potenziali (crash, memory leak, lifecycle issues)

**Documenta in**: Commento nel file o in una nota temporanea

**Esempio**:
```
PROBLEMA: CameraScreen non mostra l'overlay di traduzione
FILE INTERESSATI: CameraScreen.kt, TranslationOverlay.kt, CameraManager.kt
DIPENDENZE: OcrRepository, TranslationRepository
RISCHIO: Lifecycle della camera potrebbe non essere sincronizzato con Compose recomposition
```

---

### 2️⃣ PIANIFICA (Planning Phase)

**Obiettivo**: Definire esattamente cosa fare prima di farlo.

**Azioni**:
- [ ] Scrivi uno pseudocodice o un diagramma mentale
- [ ] Elenca i cambiamenti specifici (file, funzioni, logica)
- [ ] Definisci il criterio di successo (cosa significa "fatto"?)
- [ ] Stima il rischio (basso/medio/alto)
- [ ] Identifica i test necessari

**Documenta in**: Commento nel codice o in PROGRESS.md

**Esempio**:
```kotlin
// PIANO:
// 1. Aggiungere un LaunchedEffect in CameraScreen per sincronizzare il lifecycle
// 2. Passare il TranslationResult dal ViewModel all'Overlay
// 3. Verificare che l'overlay si posizioni correttamente sui blocchi di testo
// 4. Test: Verificare che la traduzione appaia entro 500ms dal riconoscimento
```

---

### 3️⃣ IMPLEMENTA (Implementation Phase)

**Obiettivo**: Scrivere il codice seguendo il piano.

**Azioni**:
- [ ] Scrivi il codice seguendo il piano (non improvvisare)
- [ ] Aggiungi commenti per ogni sezione logica
- [ ] Usa logging (Timber.d) per tracciare il flusso
- [ ] Gestisci gli errori (try-catch, Result types)
- [ ] Segui le convenzioni del progetto (naming, formatting)

**Regole**:
- ✅ Usa Kotlin idiomatico (data classes, extension functions, scope functions)
- ✅ Gestisci il lifecycle correttamente (LaunchedEffect, DisposableEffect)
- ✅ Usa coroutines per operazioni async
- ✅ Loggare con Timber per debugging
- ❌ Non usare var globali
- ❌ Non fare blocking operations sul main thread
- ❌ Non ignorare i warning del compilatore

**Esempio**:
```kotlin
// ✅ CORRETTO
LaunchedEffect(translationResult) {
    if (translationResult != null) {
        Timber.d("Traduzione ricevuta: ${translationResult.text}")
        // Aggiorna l'overlay
    }
}

// ❌ SBAGLIATO
var lastResult = translationResult // var globale
if (lastResult != null) {
    updateOverlay(lastResult) // Potrebbe essere chiamato in background thread
}
```

---

### 4️⃣ TEST/BUILD (Verification Phase)

**Obiettivo**: Verificare che il codice compili e funzioni.

**Azioni**:
- [ ] Esegui il build: `./gradlew assembleDebug`
- [ ] Verifica assenza di errori di compilazione
- [ ] Verifica assenza di warning critici
- [ ] Controlla i log (Timber) per errori runtime
- [ ] Testa il flusso manualmente (se possibile su emulator/device)

**Comandi**:
```bash
# Build APK
./gradlew assembleDebug

# Controlla warning
./gradlew build --warning-mode all

# Pulisci cache
./gradlew clean

# Esegui test (se presenti)
./gradlew test
```

**Criteri di Successo**:
- ✅ Build completa senza errori
- ✅ Nessun warning critico
- ✅ Nessun crash a runtime
- ✅ Il flusso funziona come atteso

---

### 5️⃣ VERIFICA (Verification Phase)

**Obiettivo**: Controllare che la soluzione risolva il problema.

**Azioni**:
- [ ] Verifica il criterio di successo definito in PIANIFICA
- [ ] Controlla i log per errori
- [ ] Testa i casi limite (edge cases)
- [ ] Verifica che non ci siano regressioni

**Checklist**:
- ✅ Il problema è risolto?
- ✅ Il codice è leggibile e manutenibile?
- ✅ Ci sono memory leak?
- ✅ Il lifecycle è gestito correttamente?
- ✅ I log sono chiari?

---

### 6️⃣ CORREGGI (Fix Phase)

**Obiettivo**: Se la verifica fallisce, correggere gli errori.

**Azioni**:
- [ ] Leggi l'errore completamente
- [ ] Traccia la causa (non il sintomo)
- [ ] Torna a PIANIFICA e rivedi il piano
- [ ] Implementa la correzione
- [ ] Torna a TEST/BUILD

**Esempio di debug**:
```
ERRORE: "NullPointerException in TranslationOverlay.kt:42"
CAUSA: translationResult è null quando l'overlay tenta di accedervi
SOLUZIONE: Aggiungere un null-check prima di usare translationResult
```

---

### 7️⃣ RIPETI (Repeat)

**Obiettivo**: Se ci sono ancora problemi, ripeti il ciclo.

**Quando ripetere**:
- ❌ Se il build fallisce
- ❌ Se ci sono crash a runtime
- ❌ Se il criterio di successo non è soddisfatto
- ❌ Se i test falliscono

**Quando fermarsi**:
- ✅ Se il build è pulito
- ✅ Se il criterio di successo è soddisfatto
- ✅ Se i test passano
- ✅ Se non ci sono regressioni

---

## 📊 Checklist per Ogni Modifica

Usa questa checklist per ogni modifica al codice:

```markdown
## Modifica: [Nome della modifica]

### ANALIZZA
- [ ] Ho letto il requirement completamente
- [ ] Ho identificato i file interessati
- [ ] Ho compreso le dipendenze
- [ ] Ho identificato i rischi

### PIANIFICA
- [ ] Ho scritto uno pseudocodice
- [ ] Ho definito il criterio di successo
- [ ] Ho identificato i test necessari

### IMPLEMENTA
- [ ] Ho scritto il codice seguendo il piano
- [ ] Ho aggiunto commenti
- [ ] Ho aggiunto logging
- [ ] Ho gestito gli errori

### TEST/BUILD
- [ ] Il build è pulito (./gradlew assembleDebug)
- [ ] Nessun warning critico
- [ ] Nessun crash a runtime

### VERIFICA
- [ ] Il criterio di successo è soddisfatto
- [ ] Nessun memory leak
- [ ] Nessuna regressione

### CORREGGI (se necessario)
- [ ] Ho identificato la causa dell'errore
- [ ] Ho implementato la correzione
- [ ] Ho ripetuto TEST/BUILD

### STATUS
- [ ] ✅ COMPLETATO
- [ ] ⏳ IN CORSO
- [ ] ❌ BLOCCATO (motivo: ...)
```

---

## 🔧 Convenzioni del Progetto

### Naming
- **Classi**: PascalCase (es. `TranslationOverlay`)
- **Funzioni**: camelCase (es. `updateOverlay()`)
- **Variabili**: camelCase (es. `translationResult`)
- **Costanti**: UPPER_SNAKE_CASE (es. `MAX_LANGUAGES`)
- **Private**: Prefisso `_` per StateFlow privati (es. `_state`)

### Organizzazione dei File
```
app/src/main/java/com/lingolens/
├── domain/
│   ├── model/          # Data classes (Language, TranslationResult)
│   └── usecase/        # Business logic
├── data/
│   ├── repository/     # Repository implementations
│   └── camera/         # Camera-specific logic
├── presentation/
│   ├── viewmodel/      # ViewModels
│   ├── screen/         # Screen composables
│   └── overlay/        # Overlay components
└── MainActivity.kt     # Entry point
```

### Logging
```kotlin
// ✅ CORRETTO
Timber.d("Traduzione completata: ${result.text}")
Timber.e(exception, "Errore durante OCR")

// ❌ SBAGLIATO
println("Debug: $result")
Log.d("TAG", "message")
```

### Error Handling
```kotlin
// ✅ CORRETTO
suspend fun translateText(text: String): Result<String> {
    return try {
        val translation = mlKit.translate(text)
        Result.success(translation)
    } catch (e: Exception) {
        Timber.e(e, "Errore nella traduzione")
        Result.failure(e)
    }
}

// ❌ SBAGLIATO
fun translateText(text: String): String {
    return mlKit.translate(text) // Nessun error handling
}
```

---

## 📈 Metriche di Qualità

Ogni modifica deve soddisfare questi criteri:

| Criterio | Target | Come Verificare |
|----------|--------|-----------------|
| **Build Time** | < 30s | `./gradlew assembleDebug --profile` |
| **Crash Rate** | 0% | Testare su device/emulator |
| **Memory Leak** | 0 | Android Studio Profiler |
| **Lifecycle Issues** | 0 | Logcat per errori di lifecycle |
| **Code Coverage** | > 70% | Jacoco report (se configurato) |
| **Performance** | 60 FPS | Verificare su device reale |

---

## 🚨 Errori Comuni da Evitare

### 1. Modificare senza pianificare
```kotlin
// ❌ SBAGLIATO: Modifico direttamente senza pensare
_state.value = newState // Potrebbe causare race condition
```

### 2. Ignorare il lifecycle
```kotlin
// ❌ SBAGLIATO: Camera non è sincronizzata con Compose
cameraManager.startCamera() // Potrebbe crashare se Activity è distrutta
```

### 3. Blocking operations sul main thread
```kotlin
// ❌ SBAGLIATO: Blocca il UI thread
val result = mlKit.translate(text) // Operazione sincrona
```

### 4. Memory leak con coroutines
```kotlin
// ❌ SBAGLIATO: Coroutine non è cancellata
GlobalScope.launch { /* ... */ }

// ✅ CORRETTO: Usa viewModelScope
viewModelScope.launch { /* ... */ }
```

### 5. Nessun error handling
```kotlin
// ❌ SBAGLIATO: Se OCR fallisce, l'app crasha
val text = ocrRepository.recognizeText(frame)

// ✅ CORRETTO: Gestisci l'errore
val result = ocrRepository.recognizeText(frame)
if (result.isSuccess) {
    // Usa il testo
} else {
    Timber.e("OCR fallito")
}
```

---

## 📝 Template per Commit Message

```
[TIPO] Descrizione breve (max 50 caratteri)

Descrizione più lunga (max 72 caratteri per riga):
- Cosa è stato fatto
- Perché è stato fatto
- Come è stato testato

TIPO:
- feat: Nuova feature
- fix: Correzione di bug
- refactor: Refactoring del codice
- test: Aggiunta di test
- docs: Documentazione
- chore: Manutenzione

Esempio:
feat: Aggiungi OnboardingScreen con 4 step interattivi

- Implementato OnboardingViewModel per gestire lo stato
- Creato OnboardingScreen con animazioni fluide
- Aggiunto PreferencesRepository per tracciare primo avvio
- Testato su emulator API 30

Criterio di successo:
- ✅ Build pulito
- ✅ Onboarding mostra 4 step
- ✅ Nessun crash
```

---

## 🎓 Riassunto

**Ricorda sempre**:

1. **Analizza** prima di scrivere codice
2. **Pianifica** prima di implementare
3. **Implementa** seguendo il piano
4. **Testa** prima di considerare "fatto"
5. **Verifica** che il criterio di successo sia soddisfatto
6. **Correggi** se necessario
7. **Ripeti** finché non è perfetto

**Il codice veloce è spesso il codice che causa problemi.**
**Il codice lento ma rigoroso è il codice che funziona.**

---

## 📞 Supporto

Se sei bloccato:
1. Leggi il PROGRESS.md per lo stato attuale
2. Controlla i log (Timber) per errori
3. Torna a ANALIZZA e ricomincia il ciclo
4. Se l'errore persiste, chiedi aiuto con il debug
