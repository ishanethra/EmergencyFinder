# Emergency Finder 🏥🚔🚒

**Emergency Finder** is a life-saving Android application designed to help users quickly locate and contact the nearest emergency services (Hospitals, Police Stations, and Fire Stations) during critical situations. 

The app prioritizes speed and reliability, featuring offline caching and a dual-source map data system to ensure help is always reachable, even with poor connectivity.

---

## 🌟 Key Features

### 1. **Smart Emergency Discovery**
*   **Dual Data Sources**: Uses **Google Places API** as the primary source and automatically falls back to **OpenStreetMap (Overpass API)** if Google services are unavailable or credits are exhausted.
*   **Categorized Search**: Instantly find Hospitals, Police, and Fire Stations within a 25km radius.
*   **Real-time Distance**: Calculates and displays the exact distance (in km/m) from your current location using the Haversine formula.

### 2. **Life-Saving SOS Tools**
*   **One-Tap SOS**: A prominent floating button to immediately dial national emergency numbers (e.g., 112).
*   **Automated SMS SOS**: If a user is too hurt to speak, the app sends an automated SMS to pre-saved emergency contacts containing their **exact GPS coordinates** via a Google Maps link.
*   **Shake-to-SOS (Accident Detection)**: Uses the phone's accelerometer to detect hard shakes (common in accidents/falls), automatically triggering the SOS SMS sequence.

### 3. **Offline Reliability**
*   **Local Caching**: All fetched services are stored in a local SQLite database. The app shows last-known nearby services instantly upon launch while fetching fresh data in the background.
*   **Connectivity Awareness**: Dynamically switches between "Live" and "Cached" modes based on internet availability.

---

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Modern, declarative UI)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Database**: SQLite (via `SQLiteOpenHelper`)
*   **Location**: Google Play Services (Fused Location Provider)
*   **Networking**: Google Places API & Overpass API (OSM)
*   **Dependency Injection**: ViewModel & AndroidViewModel

---

## 🚀 Getting Started

### Prerequisites
*   Android Studio Ladybug or newer.
*   Android device/emulator with API level 24 (Nougat) or higher.
*   A **Google Cloud API Key** with the "Places API" enabled.

### Setup
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/EmergencyFinder.git
    ```
2.  **Add your API Key**:
    Open `PlacesRepository.kt` and replace the `API_KEY` constant with your own key:
    ```kotlin
    private const val API_KEY = "YOUR_GOOGLE_PLACES_API_KEY"
    ```
3.  **Permissions**:
    The app requires the following permissions (requested at runtime):
    *   `ACCESS_FINE_LOCATION`
    *   `SEND_SMS`
    *   `CALL_PHONE`

---

## 📱 Screenshots

| Home Screen | SOS Trigger | Emergency List |
| :---: | :---: | :---: |
| ![Home](https://via.placeholder.com/200x400?text=Home+Screen) | ![SOS](https://via.placeholder.com/200x400?text=SOS+Trigger) | ![List](https://via.placeholder.com/200x400?text=Services+List) |

---

## 🛡 Security & Privacy
*   **No Cloud Tracking**: All emergency contacts and cached locations are stored locally on the device.
*   **Permission Transparency**: The app only uses location and SMS permissions when explicitly triggered by the user or an emergency shake event.

---

## 🤝 Contributing
Contributions are welcome! If you have ideas for improving the accident detection algorithm or adding more map providers, please open an issue or submit a pull request.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
