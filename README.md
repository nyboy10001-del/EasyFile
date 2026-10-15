# 📁 EasyFile - Senior-Friendly Android File Manager

**EasyFile** is a utilitarian, accessible, and senior-friendly file manager built natively for Android using **Kotlin** and **Jetpack Compose (Material 3)**.

Designed specifically with seniors and non-technical users in mind, EasyFile eliminates confusing Linux folder hierarchies (`/storage/emulated/0/...`), tiny touch targets, and complex menus in favor of a clean **Category-First Hub** with generous typography and built-in accidental deletion protection.

---

## ✨ Key Features

* **Category-First Hub:** Direct access to the 6 most essential file categories:
  * 📸 **Photos & Pictures:** Camera shots, screenshots, and downloaded pictures.
  * 💬 **WhatsApp & Chat Files:** Voice notes, received pictures, and shared documents.
  * 🎵 **Voice Notes & Music:** Audio messages, recordings, and audio files.
  * 📥 **Downloads:** Web downloads, travel tickets, invoices, and receipts.
  * 📄 **Documents:** PDFs, Word documents, text notes, and spreadsheets.
  * 🗑️ **Recycle Bin:** Protected temporary trash with 30-day recovery.
* **Senior-Ergonomic UI:**
  * Minimum 48dp–60dp button heights.
  * High-contrast text and WCAG AAA compliant color palette.
  * Every button includes a large icon AND a plain English text label.
* **Accidental Deletion Protection:**
  * When a file is deleted, it is safely moved to the **Recycle Bin**.
  * Users can restore any file with a single tap on **"Put Back"**.
* **Direct File Sharing & Opening:**
  * Built-in Android `FileProvider` integration for secure viewing and WhatsApp sharing.
* **Storage Overview:**
  * Clear visual storage meter showing remaining free space in plain numbers (e.g., *"45 GB Free"*).
* **Non-Intrusive Monetization:**
  * Clear, padded bottom banner ads designed to prevent accidental clicks.
  * Optional one-time "Remove Ads" support.

---

## 🛠️ Architecture & Tech Stack

* **Language:** Kotlin 2.0+
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel) + Kotlin Coroutines
* **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)
* **Target SDK:** Android 15 (API 35)
* **Min SDK:** Android 8.0 (API 26) - Covers 95%+ of active devices
* **CI/CD:** Automated builds and APK artifact generation via GitHub Actions

---

## 🚀 Building & Testing

### Download via GitHub Actions
Every commit pushed to the `main` branch triggers an automated build in GitHub Actions, generating a downloadable **`EasyFile-Debug-APK`** artifact that can be installed directly on any physical Android device.

### Local CLI Install (with ADB)
Connect your physical phone with USB debugging enabled, then run:
```bash
./gradlew installDebug
```
