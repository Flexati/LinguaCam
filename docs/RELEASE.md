# Release build instructions — LingoLens

## One-time setup

### 1. Genera il keystore (esegui UNA volta sola, conserva per sempre)

```bash
mkdir -p ~/.lingolens
keytool -genkey -v \
  -keystore ~/.lingolens/release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias lingolens \
  -storepass <SCEGLI_PASSWORD_SICURA> \
  -keypass   <SCEGLI_PASSWORD_SICURA> \
  -dname "CN=LingoLens, OU=Mobile, O=LingoLens, L=IT, C=IT"
```

**ATTENZIONE**:
- `<SCEGLI_PASSWORD_SICURA>` deve essere almeno 12 caratteri, non una parola nota
- Salva le password in un password manager (Bitwarden / 1Password / KeePass)
- **NON committare** `release.jks`. NON condividerlo in chat. NON scriverlo in `local.properties`
- **Perderai la possibilità di aggiornare su Play Store se perdi questo file.** Conservalo su almeno 2 posti (es. cloud cifrato + USB cifrato)

### 2. Crea il file `~/.lingolens/keystore.properties`

```properties
storeFile=/home/<TUO_USER>/.lingolens/release.jks
storePassword=<la store password che hai scelto>
keyAlias=lingolens
keyPassword=<la key password che hai scelto>
```

Modo rapido:

```bash
cat > ~/.lingolens/keystore.properties <<EOF
storeFile=\${HOME}/.lingolens/release.jks
storePassword=__SOSTITUISCI__
keyAlias=lingolens
keyPassword=__SOSTITUISCI__
EOF
chmod 600 ~/.lingolens/keystore.properties
```

Poi con `nano ~/.lingolens/keystore.properties` sostituisci `__SOSTITUISCI__` con le tue password vere.

**NON** mettere password in `local.properties`.
**NON** mettere `keystore.properties` dentro al repo.
**MAI** condividere password in chat.

### 3. Build release AAB

```bash
cd /path/to/lingolens_working
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
| `Keystore was tampered with, or password was incorrect` | password sbagliata | reinserisci `~/.lingolens/keystore.properties` |
| `Keystore file not set for signing config release` | file mancante | crea `~/.lingolens/keystore.properties` come sopra |
| `Execution failed for task ':app:packageReleaseBundle'` | path storeFile non assoluto | usa `storeFile=/home/NOME/.lingolens/release.jks` (path assoluto) |
| `keytool: command not found` | JDK non installato | `apt install openjdk-17-jdk-headless` |
