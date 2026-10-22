# WeatherLite Android App (OpenWeather)

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-OpenWeather-orange.svg)](https://openweathermap.org/api)

## 📖 Table of Contents

* [✨ Features](#-features)
* [🛠️ Technologies & Libraries Used](#️-technologies--libraries-used)
* [🏗️ Architecture](#️-architecture)
* [⚙️ Setup & Installation](#️-setup--installation)
* [🔑 API Key Setup](#-api-key-setup)
* [🤔 Project Motivation & Learning](#-project-motivation--learning)
* [🚀 Enhancements & Roadmap](#-enhancements--roadmap)
* [🤝 Contributing](#-contributing)

## ✨ Features

* **Searchable US State & City Selection:** Browse popular major cities by state or search across all cities within a selected US state using a custom searchable dropdown.
* **Current Location Weather (GPS):** Resolves device GPS coordinates via reverse geocoding to the nearest supported US city, complete with runtime permission rationale and system settings dialogs.
* **Location Bookmarks:** Save, load, and delete favorite state/city pairs via a Material 3 modal bottom sheet backed by Room.
* **Configurable Temperature Units:** Toggle between Celsius and Fahrenheit in the Settings screen, persisted across launches with Jetpack DataStore.
* **Detailed Weather Display:** Shows temperature, weather condition description, remote weather condition icon, and localized observation time.
* **Polished UI & Error Handling:** Custom animated loading indicator (`WeatherLoadingOrb`), smooth Navigation 3 transitions, and clear error messaging for network, geocoding, or permission failures.

## 📱 Screenshots

| Weather Details | City Search | Bookmarks Sheet | GPS & Settings |
| :---: | :---: | :---: | :---: |
| <img width="300" height="600" alt="image" src="https://github.com/user-attachments/assets/a549cc40-3af1-4081-86ec-f4492b8e0dba" /> | <img width="300" height="600" alt="image" src="https://github.com/user-attachments/assets/004e0bee-6435-4a2d-beec-2348edf9fd50" /> | <img width="300" height="600" alt="image" src="https://github.com/user-attachments/assets/0aceaed1-3203-4c6c-ad8d-ccb35514df33" /> | <img width="300" height="600" alt="image" src="https://github.com/user-attachments/assets/f76f1f57-a1ac-4f9d-b55d-f0ee6da07529" /> |

## 🛠️ Technologies & Libraries Used

* **Programming Language:** Kotlin 2.2+ (including explicit backing fields)
* **UI & Navigation:**
    * Jetpack Compose (Material 3, Dynamic Color, Edge-to-Edge)
    * AndroidX Navigation 3 (`navigation3-runtime`, `navigation3-ui`)
    * Coil (`AsyncImage` for weather icons)
* **Core Android Jetpack & Persistence:**
    * ViewModel & Lifecycle (`collectAsStateWithLifecycle`)
    * Room Database (local coordinate caching, bookmarks, and schema migrations)
    * Jetpack DataStore Preferences (temperature unit & permission prompt persistence)
    * WorkManager (`CoroutineWorker` + `@HiltWorker` for background weather and geocoding work)
* **Location:**
    * Google Play Services Location (`FusedLocationProviderClient` + `kotlinx-coroutines-play-services`)
* **Networking & Serialization:**
    * Retrofit 2 & OkHttp 4 (with debug `HttpLoggingInterceptor`)
    * Kotlinx Serialization (`kotlinx-serialization-json`)
* **Asynchronous Programming:**
    * Kotlin Coroutines & Flow (`StateFlow`, `Channel`)
* **Dependency Injection:**
    * Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@HiltWorker`)
* **Testing & Code Quality:**
    * JUnit 4, Mockito (`mockito-core`, `mockito-kotlin`), Turbine, `kotlinx-coroutines-test`
    * Compose UI Testing & `HiltAndroidTest`
    * Pinterest ktlint (`ktlintCheck` / `ktlintFormat`)

## 🏗️ Architecture

This project follows **Clean Architecture** principles with the **MVVM (Model-View-ViewModel)** pattern:

* **UI Layer (`ui/`):** Stateless Jetpack Compose screens (`WeatherScreen`, `SettingsScreen`), reusable components (`SearchableDropDown`, `WeatherLoadingOrb`, `WeatherScaffold`), and immutable state holders (`WeatherUiState`, `LocationSelectionState`, `WeatherDataState`).
* **ViewModel Layer (`viewmodel/`):** `WeatherViewModel` and `SettingsViewModel` coordinate UI actions, combine reactive repository streams into a single immutable `StateFlow<WeatherUiState>`, and emit one-off UI events via `Channel`.
* **Data Layer (`data/`, `network/`, `worker/`):**
    * `WeatherRepository`, `LocationRepository`, and `SettingsRepository` encapsulate data operations.
    * Local persistence via Room (`AppDatabase`, `LocationDao`, `BookmarkDao`) and Preferences DataStore.
    * Background API execution via `WeatherWorker` and `SearchWorker` through `ApiService`.
    * `ApiServiceHost` provides a configurable host/key abstraction so switching weather API providers requires minimal changes.

## ⚙️ Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/andrev91/WeatherDemo.git
    cd WeatherDemo
    ```
2.  **Open in Android Studio:**
    * Open Android Studio.
    * Click on "Open" and select the cloned `WeatherDemo` directory.
3.  **API Key:** Follow the instructions in the [API Key Setup](#-api-key-setup) section below.
4.  **Build and Run:**
    * Let Android Studio sync the Gradle files.
    * Build the project (`Build > Make Project`) or run `./gradlew assembleDebug`.
    * Run the app on an emulator or a physical Android device (API 28+).

## 🔑 API Key Setup

This project requires an API key from OpenWeather to fetch weather and geocoding data.

1.  **Get an API Key:**
    * Go to the [OpenWeather Developer Portal](https://openweathermap.org/api) and register for a free API key.
2.  **Store the API Key:**
    * Create or open `local.properties` in the root directory of the project (the same level as `gradle.properties` and `settings.gradle.kts`).
    * Make sure `local.properties` remains listed in `.gitignore` so your key is never committed to Git.
    * Add your API key to `local.properties`:
        ```properties
        OPEN_WEATHER_API_KEY=YOUR_ACTUAL_OPENWEATHER_API_KEY
        ```
    * *(Optional)* If switching `ApiServiceHost` to another configured provider (such as `ACCUWEATHER`), you can also define `ACCUWEATHER_API_KEY` in `local.properties`.

## 🤔 Project Motivation & Learning

* **Why I built this:** This project was developed as an initiative to practice and demonstrate modern Android development skills, including API integration, Clean/MVVM architecture, reactive state management with Kotlin Coroutines/Flow, and 100% Jetpack Compose UI.
* **What I learned:** Through this project, I gained hands-on experience combining Retrofit and WorkManager with Hilt dependency injection, persisting relational and preference data with Room and DataStore, handling Android runtime location permissions and reverse geocoding, and testing reactive ViewModels with Turbine and Coroutines Test.

## 🚀 Enhancements & Roadmap

* [x] Implement location search functionality for users to filter states and cities.
* [x] Use device GPS to fetch weather for the current location (with runtime permission and rationale handling).
* [x] Improve UI/UX with polished visuals, Material 3 bottom sheet bookmarks, and custom animations (`WeatherLoadingOrb`).
* [x] Implement a local database (Room) to cache geocoded locations and bookmarks, plus DataStore for user preferences.
* [x] Write unit and Compose UI tests with automated GitHub Actions CI workflows (`ktlint` & unit tests).
* [x] Add support for dark mode and Material 3 dynamic themes.
* [ ] Add more detailed weather information (e.g., hourly/multi-day forecasts, UV index, wind & humidity metrics).
* [ ] Expand location support beyond US states and cities.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! If you have suggestions for improvements or find any bugs, please feel free to open an issue or submit a pull request.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---
