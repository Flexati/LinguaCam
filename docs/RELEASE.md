# Release build instructions — LinguaCam

## One-time setup

### 1. Genera il keystore (esegui UNA volta sola, conserva per sempre)

```bash
mkdir -p ~/.linguacam
keytool -genkey -v \
  -keystore ~/.linguacam/release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias linguacam \
  -storepass <SCEGLI_PASSWORD_SICURA> \
  -keypass   <SCEGLI_PASSWORD_SICURA> \
  -dname "CN=LinguaCam, OU=Mobile, O=LinguaCam, L=IT, C=IT"
```

**ATTENZIONE**:
- `<SCEGLI_PASSWORD_SICURA>` deve essere almeno 12 caratteri, non una parola nota
- Salva le password in un password manager (Bitwarden / 1Password / KeePass)
- **NON committare** `release.jks`. NON condividerlo in chat. NON scriverlo in `local.properties`
- **Perderai la possibilità di aggiornare su Play Store se perdi questo file.** Conservalo su almeno 2 posti (es. cloud cifrato + USB cifrato)

### 2. Crea il file `~/.linguacam/keystore.properties`

```properties
storeFile=/home/<TUO_USER>/.linguacam/release.jks
storePassword=<la store password che hai scelto>
keyAlias=linguacam
keyPassword=<la key password che hai scelto>
```

Modo rapido:

```bash
cat > ~/.linguacam/keystore.properties <<EOF
storeFile=\${HOME}/.linguacam/release.jks
storePassword=__SOSTITUISCI__
keyAlias=linguacam
keyPassword=__SOSTITUISCI__
EOF
chmod 600 ~/.linguacam/keystore.properties
```

Poi con `nano ~/.linguacam/keystore.properties` sostituisci `__SOSTITUISCI__` con le tue password vere.

**NON** mettere password in `local.properties`.
**NON** mettere `keystore.properties` dentro al repo.
**MAI** condividere password in chat.

### 3. Build release AAB

```bash
cd /path/to/linguacam_working
./gradlew bundleRelease
```

Output atteso:

```
app/build/outputs/bundle/release/app-release.aab
```

### 4. Verifica firma

```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose \
  app/build/outputs/bundle/release/app-release.aab
```

Output atteso: `Verified using v2 scheme (APK Signature Scheme v2): true`.

## ABI Enforcement (Step 5)

Il `app/build.gradle.kts` configura:

```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "x86_64")
}
```

→ Solo architetture 64-bit (richiesto da Google Play dal 2019).
→ Niente `armeabi-v7a` o `x86`.

Per verificare che il bundle includa solo queste ABI:

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging \
  app/build/outputs/bundle/release/app-release.aab | grep native-code
```

Output atteso: `native-code: 'arm64-v8a' 'x86_64'`

## Troubleshooting

| Errore | Causa | Fix |
|---|---|---|
| `Keystore was tampered with, or password was incorrect` | password sbagliata | reinserisci `~/.linguacam/keystore.properties` |
| `Keystore file not set for signing config release` | file mancante | crea `~/.linguacam/keystore.properties` come sopra |
| `Execution failed for task ':app:packageReleaseBundle'` | path storeFile non assoluto | usa `storeFile=/home/NOME/.linguacam/release.jks` (path assoluto) |
| `keytool: command not found` | JDK non installato | `apt install openjdk-17-jdk-headless` |
