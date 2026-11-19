# BudgetWise

**Take control of your money.**

BudgetWise is a native Android personal finance application written in Kotlin with Jetpack Compose and Material Design 3. Designed with an offline-first architecture, it enables users to record daily income and expenses, enforce monthly category budgets, track savings goals, review spending insights, and query their financial ledger through an integrated conversational assistant—without requiring external bank credentials.

> **Project Status:** Active development. Core personal finance tracking, local database persistence, security controls, and conversational assistant workflows are implemented and verified with local JVM and Robolectric tests, while additional capabilities remain on the roadmap.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Problem Statement](#problem-statement)
3. [Project Objectives](#project-objectives)
4. [Key Features](#key-features)
5. [Technology Stack](#technology-stack)
6. [Application Architecture](#application-architecture)
7. [Project Directory Structure](#project-directory-structure)
8. [Installation and Setup](#installation-and-setup)
9. [Build and Execution](#build-and-execution)
10. [Database and Data Persistence](#database-and-data-persistence)
11. [Application Screenshots](#application-screenshots)
12. [Testing](#testing)
13. [Current Limitations](#current-limitations)
14. [Future Development Plans](#future-development-plans)
15. [License](#license)

---

## Project Overview

BudgetWise consolidates everyday cash-flow tracking, budgeting, goal monitoring, and spending analysis into a single mobile workspace. All financial records are stored locally on the device using Room (SQLite) with integer centavo precision to avoid floating-point rounding drift, while user preferences and security metadata are persisted through Jetpack DataStore.

The interface adapts across compact phones and expanded tablet or foldable screens using Material 3 canonical navigation patterns (`NavigationBar` on compact layouts, `NavigationRail` on wide layouts) and supports dynamic system, light, and dark themes.

---

## Problem Statement

Many individuals struggle to maintain visibility into daily spending habits, recognize when category spending is approaching monthly limits, and measure progress toward personal savings targets. Existing personal finance applications often rely on mandatory cloud accounts, third-party bank-linking credentials, or subscription paywalls—creating privacy concerns and onboarding friction for users who prefer direct, device-local control over their records.

---

## Project Objectives

- **Accurate Financial Computation:** Perform deterministic monetary arithmetic using integer minor units (centavos) and `BigDecimal` (`RoundingMode.HALF_UP`) across cash-flow summaries, budget utilization, savings rates, and period comparisons.
- **Offline-First Privacy:** Keep all transactions, budgets, goals, categories, and chat sessions stored locally on the device with zero bank login requirements and optional balance masking.
- **Actionable Visibility:** Surface monthly and all-time trends, category distributions, budget health thresholds, and week-over-week deltas in clear visual summaries.
- **Local App Security:** Protect personal records using a salted PBKDF2 4-digit PIN lock, progressive brute-force cooldowns, biometric/device-credential unlock, and secure recovery paths.
- **Maintainable Engineering:** Enforce clean separation of concerns across UI, reactive state management, pure domain logic, and persistence layers backed by automated unit tests.

---

## Key Features

### Implemented Functionality

- **Home Financial Dashboard**
  - Switch between **This Month** and **All-Time** scopes for income, expenses, net cash flow, and savings rate.
  - Month selector for navigating historical periods, quick balance-masking toggle, spending breakdown chart, budget health snapshot, and recent transactions list.
- **Transaction Ledger & Activity Filtering**
  - Create, edit, and delete income and expense entries with title, amount, category, date, payment method (`Cash`, `Card`, `Bank Transfer`, `E-Wallet`, `Other`), and notes.
  - Real-time search, type filtering (All / Income / Expense), category filtering, month-only vs. all-time scope toggle, and sorting by date or amount.
- **Monthly Category Budgets**
  - Configure monthly spending limits per category with automatic slot de-duplication per `(category, year, month)`.
  - Visual progress indicators and four-tier health statuses: **Normal** (`< 70%`), **Approaching** (`70%–89.9%`), **Near Limit** (`90%–99.9%`), and **Exceeded** (`>= 100%`).
- **Savings Goals & Contribution Ledger**
  - Create savings goals with target amounts and target dates, record deposits or withdrawals atomically, inspect contribution history, and automatically update completion status.
- **Spending Insights & Visual Analytics**
  - Multi-month cash-flow comparison charts, daily/monthly spending trend charts, category expense breakdowns, and deterministic rule-based insights (top category, budget alerts, savings rate evaluation, and month-over-month comparisons).
  - Rolling 7-day week-over-week spending analyzer with category delta tracking.
- **Financial Assistant & Multi-Session Chat Management**
  - Conversational assistant grounded in the user's live BudgetWise ledger context (respecting Privacy Mode when balance masking is enabled), backed by Firebase AI Logic with streaming responses and automatic model fallback.
  - Dedicated **Chat History** panel supporting multiple independent conversations, automatic topic-based chat naming (preserving manually edited titles), manual chat renaming, single-chat and individual message deletion with confirmation, message editing with regeneration, response retry, raw Markdown copy-to-clipboard, and rich Markdown rendering.
- **Local Security & Privacy**
  - Optional 4-digit App Lock hashed with PBKDF2 (`PBKDF2WithHmacSHA256`, 16-byte random salt, 10,000 iterations) and verified in constant time.
  - Progressive lockout protection after 5 consecutive failed attempts (30s → 60s → 120s → 300s cap) with a live countdown timer.
  - Biometric and device-credential unlock via AndroidX `BiometricPrompt`, secure PIN recovery via device lock verification, or confirmed local data reset (`RESET`).
  - One-tap **Hide Balances** privacy mode masking monetary values across cards, charts, and insights.
- **Personalization & Settings**
  - Multi-currency formatting (`PHP`, `USD`, `EUR`, `GBP`, `JPY`, `SGD`, `AUD`, `CAD`), custom category creation and management, System/Light/Dark appearance modes, and demonstration dataset seeding or full local data reset.

---

## Technology Stack

| Layer | Technologies |
| --- | --- |
| **Language** | Kotlin `2.2.10` (JVM target 11) |
| **UI Framework** | Jetpack Compose (`Compose BOM 2024.09.00`), Material Design 3, Material Icons Extended |
| **Navigation** | Navigation Compose `2.8.9` with `@Serializable` type-safe routes (`kotlinx-serialization-json 1.8.0`) |
| **Architecture & State** | AndroidX Lifecycle ViewModel `2.8.7`, Kotlin Coroutines & `StateFlow` (`1.10.2`) |
| **Local Database** | Room `2.7.0` (`room-runtime`, `room-ktx`, `room-compiler` via KSP `2.3.5`) |
| **Preferences Storage** | Jetpack DataStore Preferences `1.1.7` |
| **Security & Biometrics** | AndroidX Biometric `1.1.0`, `javax.crypto` PBKDF2 hashing |
| **Assistant Integration** | Firebase AI Logic (`firebase-ai`), Firebase App Check (`recaptcha` & `debug`) via Firebase BOM `34.17.0` |
| **Testing** | JUnit `4.13.2`, Robolectric `4.16.1`, `kotlinx-coroutines-test 1.10.2`, AndroidX Test Core `1.6.1` |
| **Build System** | Gradle Kotlin DSL, Android Gradle Plugin `9.1.1`, Gradle Version Catalog (`libs.versions.toml`) |
| **SDK Targets** | `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36` |

---

## Application Architecture

BudgetWise follows an **MVVM (Model-View-ViewModel)** architecture with **Unidirectional Data Flow (UDF)**:

```text
+-------------------------------------------------------------------+
|                        UI Layer (Compose)                         |
|  Screens: Home, Activity, Budgets, Goals, Insights, Settings      |
|  Overlays & Modals: PinLock, LaunchSplash, FinancialChatSheet     |
+---------------------------------+---------------------------------+
                                  | observes StateFlow / dispatches events
                                  v
+-------------------------------------------------------------------+
|                     ViewModel Layer (MVVM)                        |
|  BudgetWiseViewModel: combines Room Flows, DataStore preferences, |
|  active filters, and chat session state into immutable UI state   |
+----------------+---------------------------------+----------------+
                 |                                 |
                 v                                 v
+---------------------------------+ +-------------------------------+
|       Domain Layer (Pure)       | |      Data & AI Layers         |
|  FinanceCalculator              | |  BudgetWiseRepository         |
|  WeeklySpendingAnalyzer         | |  BudgetWiseDatabase (Room v3) |
|  FinancialChatContextBuilder    | |  UserPreferencesRepository    |
|  ChatTitleGenerator             | |  GeminiFinancialChatService   |
|  PinSecurityManager             | |  AppCheckInitializer          |
+---------------------------------+ +-------------------------------+
```

- **Pure Domain Logic:** All financial formulas, weekly comparisons, PIN hashing/lockout math, chat context assembly, and auto-title generation live in stateless Kotlin objects independent of Android UI frameworks, making them directly unit-testable.
- **Single Source of Truth:** Room DAOs emit reactive `Flow` streams that `BudgetWiseViewModel` combines with `UserPreferencesRepository` and filter states into immutable `BudgetWiseUiState` and `ChatUiState` snapshots.
- **Adaptive Layout System:** `BudgetWiseApp` inspects container width via `BoxWithConstraints` to switch between bottom navigation (`< 600dp`) and a side `NavigationRail` (`>= 600dp`), while constraining content width on large screens for readability.

---

## Project Directory Structure

```text
budgetwise-android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ai/
│       │   │   │   ├── AppCheckInitializer.kt
│       │   │   │   └── GeminiFinancialChatService.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── BudgetWiseDatabase.kt
│       │   │   │   │   ├── Converters.kt
│       │   │   │   │   ├── SampleDataSeeder.kt
│       │   │   │   │   ├── dao/Daos.kt
│       │   │   │   │   └── entity/Entities.kt
│       │   │   │   ├── preferences/UserPreferencesRepository.kt
│       │   │   │   └── repository/BudgetWiseRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── CategoryVisualMatcher.kt
│       │   │   │   ├── ChatTitleGenerator.kt
│       │   │   │   ├── FinanceCalculator.kt
│       │   │   │   ├── FinancialChatContextBuilder.kt
│       │   │   │   ├── Models.kt
│       │   │   │   ├── PinSecurityManager.kt
│       │   │   │   └── WeeklySpendingAnalyzer.kt
│       │   │   └── ui/
│       │   │       ├── BudgetWiseApp.kt
│       │   │       ├── components/
│       │   │       │   ├── CategoryIcons.kt
│       │   │       │   ├── Charts.kt
│       │   │       │   ├── ChatHistorySection.kt
│       │   │       │   ├── CommonComponents.kt
│       │   │       │   ├── FinancialChatSheet.kt
│       │   │       │   ├── LaunchSplashOverlay.kt
│       │   │       │   ├── MarkdownText.kt
│       │   │       │   ├── PinLockComponents.kt
│       │   │       │   ├── PinRecoveryComponents.kt
│       │   │       │   ├── StableFormDialog.kt
│       │   │       │   └── TransactionEditorSheet.kt
│       │   │       ├── screens/
│       │   │       │   ├── ActivityScreen.kt
│       │   │       │   ├── BudgetsScreen.kt
│       │   │       │   ├── GoalsScreen.kt
│       │   │       │   ├── HomeScreen.kt
│       │   │       │   ├── InsightsScreen.kt
│       │   │       │   ├── OnboardingScreen.kt
│       │   │       │   └── SettingsScreen.kt
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt
│       │   │       │   ├── Theme.kt
│       │   │       │   └── Type.kt
│       │   │       └── viewmodel/BudgetWiseViewModel.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── font/
│       │       ├── mipmap-*/
│       │       ├── values/
│       │       └── xml/
│       ├── test/java/com/example/
│       │   ├── BudgetWiseDatabaseTest.kt
│       │   ├── ChatManagementTest.kt
│       │   ├── CoreFeaturesAuditTest.kt
│       │   ├── ExampleRobolectricTest.kt
│       │   ├── ExampleUnitTest.kt
│       │   ├── FinanceCalculatorTest.kt
│       │   ├── FinancialChatContextBuilderTest.kt
│       │   ├── FinancialChatServiceTest.kt
│       │   ├── PinRecoverySecurityTest.kt
│       │   ├── PinSecurityManagerTest.kt
│       │   ├── WeeklySpendingAnalyzerTest.kt
│       │   └── ui/components/MarkdownParserTest.kt
│       └── androidTest/java/com/example/
│           └── ExampleInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── .env.example
├── .gitignore
└── README.md
```

---

## Installation and Setup

### Prerequisites

- **Android Studio** (Meerkat / Narwhal or newer with AGP 9.1+ support)
- **JDK 11** or higher
- **Android SDK** with Platform 36 installed (`minSdk = 24`)
- **Gradle** compatible with `gradle/wrapper/gradle-wrapper.properties`

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/budgetwise-android.git
cd budgetwise-android
```

### 2. Local Environment Configuration

1. Open the project in Android Studio and allow Gradle to sync dependencies.
2. Android Studio automatically configures `local.properties` with your local `sdk.dir` path (ignored by `.gitignore`).
3. **Core offline features** (transactions, budgets, savings goals, insights, categories, and PIN/biometric security) build and run immediately without any cloud credentials.

### 3. Optional: Enable the Financial Assistant (Firebase AI Logic)

The conversational assistant connects through Firebase AI Logic and Firebase App Check rather than bundling a raw API key in the APK:

1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app matching the `applicationId` in `app/build.gradle.kts`.
3. Enable **Firebase AI Logic** (Gemini Developer API backend) and **Firebase App Check**.
4. Download your `google-services.json` and place it at `app/google-services.json` (excluded from Git via `.gitignore`).
5. For debug builds with App Check enforcement enabled, register your debug token in the Firebase Console.

> `gradle.properties` sets `googleServices.missing.passthrough=true` so the app still compiles cleanly when `app/google-services.json` is omitted.

---

## Build and Execution

### Using Android Studio

1. Select the `app` run configuration.
2. Choose an Android emulator or physical device running Android 7.0 (API 24) or higher.
3. Click **Run**.

### Using Gradle CLI

```bash
# Assemble debug APK
gradle :app:assembleDebug

# Run all local JVM and Robolectric unit tests
gradle :app:testDebugUnitTest

# Install debug build on a connected device/emulator
gradle :app:installDebug
```

*(If running in a standard local environment with a generated Gradle wrapper, you can substitute `./gradlew` for `gradle`.)*

---

## Database and Data Persistence

### Room SQLite Database (`budgetwise_db`, Schema Version 3)

BudgetWise stores all structured financial and chat records locally in Room across seven normalized tables:

| Table | Purpose | Key Constraints & Indexes |
| --- | --- | --- |
| `transactions` | Income and expense ledger entries | Indexed on `dateEpochDay`, `category`, `type`; monetary values stored as `Long` centavos |
| `budgets` | Monthly category spending limits | Unique composite index on `(category, year, month)` |
| `savings_goals` | Savings targets, saved balance, target date, and status | Tracks `ACTIVE`, `COMPLETED`, and `PAUSED` states |
| `savings_contributions` | Deposit/withdrawal history per savings goal | Foreign key to `savings_goals(id)` with `ON DELETE CASCADE`; indexed on `goalId` |
| `categories` | Default and user-created categories with icon/color metadata | Unique composite index on `(name, type)` |
| `chat_sessions` | Saved assistant conversations and title metadata | Indexed on `updatedAtMillis`; tracks `isTitleManuallySet` flag |
| `chat_messages` | Ordered user and assistant turns per conversation | Foreign key to `chat_sessions(id)` with `ON DELETE CASCADE`; indexed on `sessionId` and `createdAtMillis` |

#### Schema Migrations

- **`MIGRATION_1_2`:** Introduces `chat_messages` with timestamp indexing for persistent assistant history.
- **`MIGRATION_2_3`:** Introduces `chat_sessions`, preserves any pre-existing v2 messages by grouping them into an auto-titled default session, and recreates `chat_messages` with cascading foreign-key isolation per session.

### Preferences Persistence (`budgetwise_prefs`)

Jetpack DataStore stores lightweight settings locally:
- Display name, preferred currency code, and theme mode (`SYSTEM`, `LIGHT`, `DARK`)
- Privacy balance-masking preference and initial seed completion flag
- App Lock enablement, biometric unlock toggle, salted PBKDF2 PIN hash record, failed attempt counter, and lockout expiration timestamp

---

## Application Screenshots

Screenshots will be added as UI finalization progresses. Recommended capture set for `docs/screenshots/`:

| Screen | Description | Status |
| --- | --- | --- |
| **Launch & Welcome** | Branded splash experience featuring the BudgetWise identity and tagline | *Placeholder — screenshot pending* |
| **Home Dashboard** | Monthly/All-Time cash-flow cards, spending chart, budget overview, and recent activity | *Placeholder — screenshot pending* |
| **Activity Ledger** | Searchable, filterable transaction list with add/edit modal sheet | *Placeholder — screenshot pending* |
| **Budgets** | Category spending limits, progress bars, and health status indicators | *Placeholder — screenshot pending* |
| **Savings Goals** | Goal cards, progress tracking, and deposit/withdrawal logging | *Placeholder — screenshot pending* |
| **Insights** | Multi-month cash-flow charts, category breakdowns, and automated spending insights | *Placeholder — screenshot pending* |
| **Financial Assistant & Chat History** | Multi-session chat drawer, Markdown-formatted analysis, and per-chat management | *Placeholder — screenshot pending* |
| **PIN Lock & Settings** | 4-digit PIN keypad, lockout countdown, device-lock recovery, and preferences | *Placeholder — screenshot pending* |

---

## Testing

The project includes **12 test classes** totaling **54 local JVM and Robolectric tests** under `app/src/test/java/com/example/`:

- **`FinanceCalculatorTest`** — Centavo parsing, rounding boundaries, cash-flow totals, savings rates, budget utilization thresholds, insights generation, and compact/masked currency formatting.
- **`BudgetWiseDatabaseTest`** — Room DAO operations, atomic budget upserts, atomic goal contribution balance updates, and cascading deletes.
- **`ChatManagementTest`** — Multi-session chat creation, automatic topic naming and refinement, manual rename preservation, isolated single-chat and single-message deletion, persistence across ViewModel reloads, and lossless `MIGRATION_2_3` verification.
- **`CoreFeaturesAuditTest`** — End-to-end ViewModel integration checks across transactions, budgets, goals, filters, and period scopes.
- **`PinSecurityManagerTest` & `PinRecoverySecurityTest`** — PBKDF2 hashing, constant-time verification, progressive lockout intervals, countdown formatting, device-lock PIN reset, and confirmed local reset flow.
- **`WeeklySpendingAnalyzerTest`** — Rolling 7-day comparisons, fallback to latest active window, and category delta sorting.
- **`FinancialChatContextBuilderTest` & `FinancialChatServiceTest`** — Context digest assembly, Privacy Mode redaction directives, alternating role sanitization, error surfacing, and cancellation handling.
- **`MarkdownParserTest`** — Inline bold, italic, currency highlighting, and bullet/numbered list parsing.
- **`ExampleRobolectricTest` & `ExampleUnitTest`** — Resource verification, dark-surface color contrast adaptation, window flags, and baseline Room persistence.

Run the full test suite locally:

```bash
gradle :app:testDebugUnitTest
```

---

## Current Limitations

- **Active Development:** Features, layouts, and test coverage continue to evolve as part of ongoing development.
- **Single-Device Local Storage:** Financial data is stored exclusively on-device; cross-device cloud synchronization and automated cloud backups are not implemented yet.
- **Assistant Connectivity Requirement:** While all core budgeting and analytics work offline, the conversational assistant requires network connectivity and a configured Firebase project with Firebase AI Logic enabled.
- **Manual Transaction Entry:** Bank API aggregation, SMS parsing, and receipt OCR scanning are not currently included.
- **Data Portability & Release Hardening:** Export formats (CSV/PDF), recurring transaction automation, and release ProGuard/R8 shrinking rules (`isMinifyEnabled = false`) are planned for later milestones.

---

## Future Development Plans

- **Recurring Transactions & Bill Reminders:** Scheduled income/expense templates with local notification reminders.
- **Data Export & Encrypted Local Backup:** Export filtered ledgers to CSV/PDF and support password-protected local backup and restore.
- **Optional Cloud Synchronization:** Opt-in authenticated multi-device sync while preserving the offline-first local database.
- **Home Screen Widgets:** Jetpack Glance widgets for quick daily expense entry and remaining budget checks.
- **Expanded UI & Instrumentation Testing:** Broader Compose UI test coverage and automated CI workflows for build and test verification.

---

## License

No open-source license has been selected yet. All rights are reserved while the project remains in active development.
