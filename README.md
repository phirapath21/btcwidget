# Satoshi Dashboard (Blockclock Widget) 🪙🕒

[![Android Build](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

A premium, feature-rich Android app and Home Screen widget suite inspired by the iconic hardware **E-Ink Blockclocks**. Keep track of Bitcoin spot price, mempool transaction fees, halving counts, Satoshi purchasing power, block height, and MicroStrategy (MSTR) stock statistics directly from your home screen. Designed for high visual fidelity, seamless data synchronization, and elegant custom styling.

---

## ✨ Premium Features

### 1. Interactive Widgets (2x1 and 4x2 Dual-Panel Layouts)
- **Tap to Cycle**: Tap the widget panels to cycle through active info displays.
- **Dual-Panel (4x2)**: Monitor two different metrics simultaneously side-by-side. Tap the left and right panels to cycle each side independently.
- **Haptic Feedback**: Soft vibration feedback on taps confirms mode selection changes directly from your home screen.
- **Stale Data Warning Badge**: Displays a subtle warning icon `(!)` in the corner of the widget if a background update fails, notifying you when you are viewing cached offline data.
- **Dynamic Dashboard Previews**: Instant visual preview inside the settings app reflects your active theme, mimicking exactly how it will look on your home screen.

### 2. The 10 Information Modes
1. **BTC/USD Price** — Spot price of Bitcoin (Coinbase API).
2. **Sats per Dollar** — SATS per 1 USD (Moscow Time style, formatted).
3. **Block Height** — Current Bitcoin block height (Mempool.space API).
4. **Moscow Time (Clean)** — Full screen clean layout of SATS per dollar.
5. **MSTR Holdings** — MicroStrategy's total Bitcoin holdings (843,738 BTC).
6. **MSTR / BTC Ratio** — Share price to BTC ratio (value per share).
7. **MSTR Stock Price** — MicroStrategy share price (Yahoo Finance API).
8. **BTC Circulation** — Total circulating Bitcoin supply in millions (e.g. `19.850 MIL`).
9. **Halving Countdown** — The number of blocks remaining until the next Bitcoin halving event.
10. **Mempool Priority Fees** — Real-time recommended transaction fees (Fastest/Mid/Low) directly from Mempool.space.

### 3. Active Mode Customization
- Choose exactly which of the 10 modes are enabled for widget cycling. Disable modes you do not need, making cycling faster and more personalized.

### 4. Sleek Hardware Themes
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
- **Concurrency & Network**: Kotlin Coroutines for asynchronous networking.
- **Storage**: `SharedPreferences` for ultra-fast local caching of widget modes, selected themes, and API data.
- **API Integrations**:
  - **Coinbase Spot API** (BTC Price)
  - **Yahoo Finance v8 API** (MSTR Price)
  - **Mempool.space API** (Block Height & recommended fees)
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
   - Drag either the 2x1 widget or the 4x2 Dual-Panel widget onto your home screen!

---

## 📂 Project Structure

```
btcwidget/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/btcwidget/
│   │   │   │   ├── BtcWidgetProvider.kt      # Main AppWidgetProvider (2x1) & intent routing
│   │   │   │   ├── BtcDualWidgetProvider.kt  # Dual-Panel AppWidgetProvider (4x2)
│   │   │   │   ├── MainActivity.kt          # App entry point
│   │   │   │   ├── PriceData.kt             # Data classes & Network repository
│   │   │   │   ├── ui/main/
│   │   │   │   │   ├── MainScreen.kt        # Jetpack Compose dashboard UI
│   │   │   │   │   └── MainScreenViewModel.kt# Live data updates and caching
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   ├── btc_widget_blockclock.xml # 2x1 Widget layout (XML layout)
│   │   │       │   └── btc_dual_widget_blockclock.xml # 4x2 Widget layout (XML layout)
│   │   │       └── drawable/                # Theme background resources
│   │   └── build.gradle.kts
└── settings.gradle.kts
```

---

## 📜 License

This project is licensed under the MIT License. Feel free to use, modify, and build upon it!
