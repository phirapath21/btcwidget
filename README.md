# Satoshi Dashboard (Blockclock Widget) 🪙🕒

[![Android Build](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

A premium, lightweight Android app and Home Screen widget inspired by the iconic hardware **E-Ink Blockclocks**. Keep track of Bitcoin metrics, Satoshi purchasing power, block height, and MicroStrategy (MSTR) stock statistics directly on your home screen with real-time API synchronization and sleek hardware preset themes.

---

## ✨ Features

### 1. Interactive 2x1 Home Screen Widget
- **Tap to Cycle Modes**: Simply tap the widget's central layout to cycle through **8 different information displays**.
- **Responsive Layout**: Specially crafted XML grid structure tailored for perfect layout alignment, utilizing scaled monospace text and e-ink layout guidelines.
- **Dynamic Previews**: Instant visual preview inside the settings dashboard replicates how the widget looks on your home screen.

### 2. The 8 Info Modes
1. **BTC/USD Price** — Spot price of Bitcoin (from Coinbase API).
2. **Sats per Dollar** — SATS per 1 USD (Moscow Time style).
3. **Block Height** — Current Bitcoin block height (from Mempool.space).
4. **Moscow Time (Clean)** — Sats per dollar, full screen view.
5. **MSTR Holdings** — MicroStrategy's total Bitcoin holdings (843,738 BTC).
6. **MSTR / BTC Ratio** — Share price to BTC ratio (value per share).
7. **MSTR Stock Price** — MicroStrategy share price (from Yahoo Finance API).
8. **BTC Circulation** — Total circulating Bitcoin supply in millions (e.g. `19.850 MIL`).

### 3. Sleek Hardware Themes
Seamlessly switch styles. The selected theme applies instantly to both your **in-app dashboard preview** and your **homescreen widgets**:
- 🌑 **E-Ink Dark (Default)** — High contrast dark grey slate with gold borders.
- ☀️ **E-Ink Light** — Clean zinc paper-white background with dark grey text.
- 🍊 **Bitcoin Orange** — Energetic, warm orange accents.
- 🧪 **Matrix Green** — Retro hacker terminal style with glowing emerald greens.
- 👑 **Coinkite Gold** — Premium industrial gold accents on a charcoal canvas.

---

## 🛠️ Tech Stack & Architecture

- **Core UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material Design 3.
- **Widget Engine**: Standard Android `AppWidgetProvider` paired with custom remote views tinting and state management.
- **Concurreny & Network**: Kotlin Coroutines for asynchronous networking.
- **Storage**: `SharedPreferences` for ultra-fast, local caching of the widget modes, selected themes, and API data.
- **API Integrations**:
  - **Coinbase Spot API** (BTC Price)
  - **Yahoo Finance v8 API** (MSTR Price)
  - **Mempool.space API** (Block Height)
  - **Blockchain.info API** (Circulating Supply Fallback)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala or newer recommended)
- **JDK 17** (or standard Java SDK configuration)
- An Android device or emulator running **Android 8.0 (Oreo) / API Level 26** or higher.

### Installation & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/phirapath21/btcwidget.git
   cd btcwidget
   ```

2. **Build the debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on device**:
   Connect your device via USB (with Developer Mode and USB debugging active) and run:
   ```bash
   ./gradlew installDebug
   ```

4. **Add the Widget**:
   - Go to your home screen.
   - Long press on an empty space, tap **Widgets**.
   - Scroll down to find **Satoshi Dashboard** (or **Blockclock**).
   - Drag the 2x1 widget onto your home screen!

---

## 📂 Project Structure

```
btcwidget/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/btcwidget/
│   │   │   │   ├── BtcWidgetProvider.kt      # Main AppWidgetProvider & intent routing
│   │   │   │   ├── MainActivity.kt          # App entry point
│   │   │   │   ├── PriceData.kt             # Data classes & Network repository
│   │   │   │   ├── ui/main/
│   │   │   │   │   ├── MainScreen.kt        # Jetpack Compose dashboard UI
│   │   │   │   │   └── MainScreenViewModel.kt# Live data updates and caching
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   └── btc_widget_blockclock.xml # Widget layout (XML layout)
│   │   │       └── drawable/                # Theme background resources
└── settings.gradle.kts
```

---

## 📜 License

This project is licensed under the MIT License. Feel free to use, modify, and build upon it!
