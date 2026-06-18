# WealthFlow — Premium Multi-Tier Wealth Ledger (Offline-First)

WealthFlow is an enterprise-grade personal wealth ledger and budget manager featuring a **premium midnight-indigo visual aesthetic with glowing neon-coral accents**. 

Built leveraging **Clean MVVM Architecture**, it guarantees **100% resilient offline-first operations** using a reactive localized Room database engine (SQLite) coupled with an automated background-syncing framework to sync records seamlessly to a Node.js Express & SQLite remote backend when internet access is present.

---

## Architecture Design

```
+------------------------------------------------------------+
|                       ANDROID FRONTEND                     |
|                                                            |
|  [ Presentation / Jetpack Compose ]                        |
|        UI Screens (Dashboard, Ledger History, Budget Caps)  |
|                         ^                                  |
|                         v                                  |
|  [ AppViewModel / Reactive States ]                        |
|        Unidirectional State Flows / Live Push Alerts       |
|                         ^                                  |
|                         v                                  |
|  [ Repository / Coordinated Data Hub ]                    |
|        Decides Online-First with Offline Room Fallbacks    |
|             /                     \                        |
+------------/-----------------------\-----------------------+
            v                         v
   [ Room DB / SQLite ]       [ Retrofit Online Sync Service ]
   Stored 100% Locally               /
                                    / (Restful Sync Post)
                                   v
+---------------------------------+--------------------------+
|                       EXPRESS NODE.JS BACKEND              |
|                                                            |
|   [ Express HTTP Service Routes / verifyToken Auth JWT ]   |
|            /api/auth/signup & login | /api/ledger/sync     |
|                               ^                            |
|                               v                            |
|                     [ Controller Resolvers ]               |
|                               ^                            |
|                               v                            |
|                [ Backend SQLite Database File ]            |
+------------------------------------------------------------+
```

---

## 🚀 Installation & Quick Start

### 📲 Part 1: Android Application System (Jetpack Compose)

The mobile client is packaged as a standard modern Android Gradle project:

1. **Gradle Build Sync**:
   During compiling, Gradle automatically resolves dependencies defined in `gradle/libs.versions.toml`:
   - Jetpack Compose with Material 3 Design
   - Room Persistence Engine with KSP Compiler
   - Retrofit with Moshi JSON Serialization
   
2. **Launch Application**:
   Run the application on an Android Emulator or local debugging device:
   ```bash
   gradle assembleDebug
   ```
   *Note: The application automatically initializes local databases and notification warning channels on first launch.*

---

### 💻 Part 2: Node.js Express REST Backend & Authentication Server

The synchronization backend resides inside the `/backend` folder:

1. **Navigate to directory**:
   ```bash
   cd backend
   ```
2. **Install Node Packages**:
   Install production dependencies (`express`, `sqlite3`, `bcryptjs`, `jsonwebtoken`, `cors`, `dotenv`):
   ```bash
   npm install
   ```
3. **Run Server**:
   ```bash
   npm start
   ```
   Once started, the backend automatically provisions a local file database at `backend/database.sqlite` and listens on port `3000`.

---

## 🛠 Features Implemented & Code Quality Safeguards

1. **JWT Auth Platform & Device Sync**:
   - Implements full register and login workflows.
   - Saves passwords securely using SHA-256 (client-side) and bcrypt salting (server-side).
   - Generates and signs JWT tokens to permit ledger synchronizations.
   - Uses persistent local Sessions so users stay logged in across system restarts.

2. **Offline-Resilient Ledger Engine**:
   - All transactions instantly commit to the local Room database offline, maintaining fluid 60FPS UI response speeds.
   - An integrated background Coroutine worker monitors connectivity statuses dynamically and batch-uploads unsynced transactions.

3. **Active Budget Sentinel & Local Push Alerts**:
   - Allows users to designate global transaction caps and category budgets.
   - Each transaction checked in real-time. If exceeded, triggers `NotificationManager` warning panels inside the OS notifying the user instantly.

4. **Stunning Custom Visual Graphics & Data Analytics**:
   - Zero-dependency custom **Circular Progress Rings** drawn directly on the Jetpack Compose Canvas, illustrating budget utilization.
   - Beautiful **Custom Column Bar Charts** displaying weekly expense accumulations over time.
