# REWIFI — Brutalist WiFi Vault

> Save WiFi networks **once**, survive every phone reset, crash or OS flash.
> Built for the *"walked into the cafe, lost all my passwords again"* problem.

REWIFI is a privacy-first Android app that stores your WiFi credentials in an
encrypted local vault, gives each network a **scan-to-connect QR code**, can
**share networks over NFC**, and keeps **encrypted backups** on Google Drive (or
any file you export) — so a factory reset never costs you your saved passwords
again.

> ⚠️ **Name is a placeholder.** Rename freely (`applicationId`, `strings.xml`).

---

## ✨ Features

- 🔐 **Encrypted vault** — passwords sealed with AES-256-GCM via the hardware
  Android Keystore. Only ciphertext ever touches the database.
- 👆 **Biometric / device-PIN lock** — optional, off by default, with a
  configurable auto-lock timeout when the app is backgrounded.
- ➕ **Manual entry** of SSID + password, with an **optional note**
  (*"cafe near the park, ask the waiter"*).
- 📷 **WiFi QR code** per network, with the REWIFI logo in the center — scan with
  any phone camera to join instantly. Share or save the QR as a PNG.
- 📲 **Scan to connect** — point the in-app scanner at any WiFi QR to **connect
  the phone and save the network to your vault** in one flow.
- 📡 **NFC sharing** — write a network to an NFC tag; a tap joins the WiFi, no
  typing.
- 👁 **Reveal & copy** the password in one tap.
- ☁️ **Google Drive sync** — connect your account and REWIFI keeps an encrypted
  backup in a `REWIFI` folder in your Drive. Auto-syncs after every change and
  once a day in the background; restores on a new phone when you sign back in.
- 💾 **File backup & restore** — export a **passphrase-encrypted** backup file to
  anywhere, and import it on any device.
- 🧭 **First-run walkthrough** — a five-screen intro explains the app, followed by
  a skippable backup setup.
- 🧩 **Home-screen widget + Quick Settings tile** — jump straight into the scanner.
- 🎨 **Brutalist UI** — yellow `#FFE500` / black `#0A0A0A` / paper `#F4F4F2`, thick
  borders, hard offset shadows, snappy mechanical animations, and an animated
  splash (QR assemble → logo stamp → WiFi arcs → mechanical wordmark → loader).

---

## 📱 Why it's a *manual* vault

Android does **not** allow an app to read the system's saved WiFi passwords. So
REWIFI is a vault you fill once — by typing a network or scanning its QR — and
then reconnect from anywhere via QR, NFC, or reveal-and-copy.

---

## 🔑 How backups stay safe across a reset

Your vault passwords are encrypted with a **hardware key that is destroyed on a
factory reset** — the very event you're protecting against. So backups are
**re-encrypted before they leave the device**:

- **File export** is encrypted with a **passphrase you choose**
  (PBKDF2-HMAC-SHA256, 120k iterations → AES-256-GCM). Decrypt it on any device by
  entering the passphrase.
- **Google Drive backups** are encrypted with a key **derived from your signed-in
  Google account** — no passphrase to set or remember. The same account
  re-derives the same key on a new phone, so the backup restores after a reset.

> **File backups:** if you forget the passphrase, the backup is unrecoverable —
> that's the price of it being genuinely secure and portable.
>
> **Drive backups** never overwrite an existing backup with an empty vault (e.g.
> right after a reinstall), so a fresh install can't wipe your cloud copy.

---

## 🛠 Build & run

1. Open the `REWIFI/` folder in **Android Studio** (Koala / Ladybug+).
2. Let it sync Gradle and install the missing SDK / JDK 17 when prompted.
3. Run on a device or emulator (min **Android 9 / API 28**).

CLI (needs Android SDK + JDK 17):
```bash
cd REWIFI
gradle wrapper            # first time only — the wrapper isn't checked in
./gradlew assembleDebug   # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug    # build + install on a connected device/emulator
```

> **Google Drive sync** needs an OAuth client (package `com.rewifi.app` + your
> signing SHA-1) registered in Google Cloud, using the non-sensitive
> `drive.file` scope. The vault, QR, NFC, and file backup all work without it.

---

## 🧱 Tech stack

Kotlin · Jetpack Compose · Material 3 · Room · Android Keystore · AndroidX
Biometric · ZXing (QR) · NFC · WorkManager (daily Drive sync) · Google Sign-In +
Drive REST via OkHttp · App Widget + Quick Settings Tile.

```
data/           Room DB, VaultRepository, Crypto (Keystore), BackupCrypto (PBKDF2),
                QrGenerator (QR + center logo), WifiQr/WifiConnector (scan→connect),
                NfcWriter, DriveAuth/DriveBackup/DriveBackupWorker, SettingsStore
vault/          VaultViewModel, BiometricLock
ui/theme/       Brutalist Color / Type / Theme
ui/components/  BrutalCard, BrutalButton, BrutalField
ui/screens/     Splash · Intro (5-screen) · Setup · Lock · Vault · Edit ·
                Detail (QR) · Scanner · NfcWrite · Backup · Settings
MainActivity    Splash → intro → setup → lock gates, back-stack navigation
ScanWidgetProvider / ScanTileService   Home-screen widget + Quick Settings tile
```

---

## 🗺 Roadmap

- [x] Google Drive sync (auto + daily background worker).
- [x] Scan-to-connect and NFC sharing.
- [ ] Search / filter as the list grows.
- [ ] App-specific PIN in addition to device credential.

---

## 🔒 Privacy

Offline-first: no analytics, no tracking, no accounts. Your vault lives only on
your device. The **only** network calls are to **Google Drive — and only if you
connect it** — to upload/download your own encrypted backup. Backup files are
always encrypted before they leave the device.

## 📄 License

Add a license of your choice (MIT recommended for open source) before publishing.
