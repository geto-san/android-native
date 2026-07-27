<div align="center">
  <img src="https://via.placeholder.com/150" width="120" height="120" alt="WildWatch Logo" />
  <h1>WildWatch</h1>
  <p><b>Beautiful and Professional Wildlife Conservation Platform for Uganda</b></p>

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
  [![Compose](https://img.shields.io/badge/Compose-Material3-green.svg?style=flat-square&logo=jetpack-compose)](https://developer.android.com/jetpack/compose)
  [![Firebase](https://img.shields.io/badge/Firebase-Auth%20|%20Firestore-orange.svg?style=flat-square&logo=firebase)](https://firebase.google.com/)
  [![Platform](https://img.shields.io/badge/Platform-Android-blue.svg?style=flat-square&logo=android)](https://www.android.com/)
</div>

---

## 🌟 What is WildWatch?

WildWatch is a modern, high-fidelity mobile and backend platform designed to empower both **Rangers** and the **Public Community** in protecting Uganda's wildlife. From the dense forests of Bwindi to the savannas of Queen Elizabeth Park, WildWatch provides the tools needed for real-time monitoring, incident reporting, and professional patrol management.

Built with an **Instagram-inspired aesthetic**, it bridges the gap between complex professional monitoring software and intuitive, community-driven conservation apps.

---

## 🚀 Key Features

### 📡 Monitoring & Reporting
*   **Wildlife Sightings:** Easily report animal sightings with high-quality photo evidence and precise GPS tagging.
*   **Conflict Reporting:** Document human-wildlife impact (crop damage, property loss) to facilitate community support and compensation.
*   **Offline-First:** Submit reports anywhere, even with zero signal. WildWatch queues your data and syncs automatically when you return to base.

### 🛡️ Professional Ranger Tools
*   **Ranger Dashboard:** A dedicated command center for field officers to track active incidents and alerts.
*   **Patrol Tracking:** (Soon) Background GPS breadcrumbs to map patrol effectiveness and coverage.
*   **Role-Based Security:** Granular permissions ensure sensitive data remains protected and only accessible to authorized personnel.

### 🎨 Look & Feel
*   **Dynamic Theming:** High-fidelity Support for **Light** and **Dark Mode (True Black)** for nighttime field operations.
*   **Modern UI:** Built entirely with Jetpack Compose using Material 3 and a clean, card-based design language.
*   **Instagram-Style Permissions:** Beautiful, non-intrusive dialogs for requesting system resources like Camera and Location.

### 👤 User Experience
*   **Guest Mode:** Start contributing immediately without an account. Your privacy is respected while you stay informed.
*   **Unified Profile:** A one-stop shop for your activity history, achievements, and app-wide settings.
*   **Real-time Alerts:** Stay updated with community alerts directly from park authorities.

---

## 🛠️ Tech Stack

WildWatch is built using the latest industry-standard technologies for a robust, scalable, and maintainable codebase.

*   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Architecture:** [MVVM (Model-View-ViewModel)](https://developer.android.com/topic/architecture)
*   **Dependency Injection:** [Hilt](https://dagger.dev/hilt/)
*   **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
*   **Backend:** [Firebase (Auth, Firestore, Functions)](https://firebase.google.com/)
*   **Maps:** [Mapbox SDK](https://www.mapbox.com/)
*   **Networking:** [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html)
*   **Logging:** [Timber](https://github.com/JakeWharton/timber)

---

## 📸 Previews

| Home Dashboard | Professional Tools | Profile & Settings |
|:---:|:---:|:---:|
| <img src="https://via.placeholder.com/300x600" width="200" /> | <img src="https://via.placeholder.com/300x600" width="200" /> | <img src="https://via.placeholder.com/300x600" width="200" /> |

---

## 🏃 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Java 17+
- Node.js & Firebase CLI (for backend features)

### Running the App
1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Add your `google-services.json` to the `app/` directory.
4.  Sync Gradle and run on a physical device or emulator.

### Local Backend (Emulators)
To test professional features and role-based access locally:
```bash
cd park-backend
npx firebase-tools emulators:start
```
Use the provided `seed.ts` script to populate test data.

---

## 🗺️ Roadmap

Check out our detailed [AGENTS.md](AGENTS.md) for the full development lifecycle and upcoming phases.

---

## 🤝 Support & Community

WildWatch is a community-driven effort. If you find a bug or have a feature request, please open an issue or reach out to the conservation team.

**Uganda Wildlife Authority (UWA)** | *Protecting the Pearl*
