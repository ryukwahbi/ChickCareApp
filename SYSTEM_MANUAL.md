# CHICKCARE SYSTEM MANUAL

**A Mobile Application for Detecting Infectious Bronchitis in Chickens Using Convolutional Neural Networks (CNN)**

---

**Document Version:** 1.0  
**Date:** February 2026  
**Platform:** Android Mobile Application  
**Developed By:** BISU ChickCare Development Team

---

## TABLE OF CONTENTS

1. [Introduction](#1-introduction)
2. [System Overview](#2-system-overview)
3. [System Requirements](#3-system-requirements)
4. [System Architecture](#4-system-architecture)
5. [Installation Guide](#5-installation-guide)
6. [User Guide](#6-user-guide)
7. [System Modules and Features](#7-system-modules-and-features)
8. [Database Design](#8-database-design)
9. [Technical Specifications](#9-technical-specifications)
10. [Security Features](#10-security-features)
11. [Troubleshooting](#11-troubleshooting)
12. [Glossary](#12-glossary)

---

## 1. INTRODUCTION

### 1.1 Purpose

This System Manual provides comprehensive technical documentation for the **ChickCare** mobile application. It serves as a reference guide for system administrators, developers, and end-users, detailing the system's architecture, features, installation procedures, and operational guidelines.

### 1.2 Scope

ChickCare is an Android mobile application designed to assist poultry farmers in the early detection of **Infectious Bronchitis (IB)** in chickens using **Convolutional Neural Networks (CNN)**. Beyond disease detection, the application provides a suite of farm management tools and a community platform for poultry farmers to connect, share knowledge, and seek assistance.

### 1.3 Intended Audience

- Poultry farmers and farm owners
- Agricultural extension workers
- Veterinary professionals
- System administrators and IT support personnel
- Developers for future maintenance and enhancements

### 1.4 Definitions and Abbreviations

| Abbreviation | Definition |
|---|---|
| IB | Infectious Bronchitis |
| CNN | Convolutional Neural Network |
| TFLite | TensorFlow Lite |
| MLP | Multi-Layer Perceptron |
| API | Application Programming Interface |
| UI | User Interface |
| SDK | Software Development Kit |

---

## 2. SYSTEM OVERVIEW

### 2.1 General Description

ChickCare is an intelligent mobile application that leverages machine learning technology to detect Infectious Bronchitis in chickens. The system uses a **CNN-MLP fusion model** (`cnnmlp_fusion_v2.tflite`) that analyzes both visual (image) and auditory (audio/spectrogram) inputs to provide accurate disease detection results.

### 2.2 Key Features

The application is organized into the following major functional areas:

1. **Disease Detection** — AI-powered detection through image capture, image upload, and audio recording analysis
2. **Farm Management** — Tools for tracking egg production, feeding schedules, vaccination records, medications, expenses, and health records
3. **Community Platform** — Social features including posts, comments, messaging, friend connections, and knowledge sharing
4. **Notifications & Alerts** — Push notifications, reminders, and real-time alerts
5. **Reports & Analytics** — Data visualization and farm performance insights
6. **User Management** — Account creation, authentication, profile management, and security settings

### 2.3 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       CHICKCARE SYSTEM                          │
│                                                                 │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────────────┐    │
│  │  User    │──▶│  Android App │──▶│  Firebase Backend    │    │
│  │ (Farmer) │   │  (Kotlin +   │   │  - Authentication    │    │
│  │          │◀──│   Compose)   │◀──│  - Firestore DB      │    │
│  └──────────┘   │              │   │  - Cloud Storage     │    │
│                 │  ┌─────────┐ │   │  - Cloud Messaging   │    │
│                 │  │ TFLite  │ │   └──────────────────────┘    │
│                 │  │  Model  │ │                                │
│                 │  │ (CNN+   │ │   ┌──────────────────────┐    │
│                 │  │  MLP)   │ │   │  External Services   │    │
│                 │  └─────────┘ │──▶│  - Cloudinary (CDN)  │    │
│                 │              │   │  - Weather API        │    │
│                 │  ┌─────────┐ │   │  - Google Maps       │    │
│                 │  │ Room DB │ │   │  - ML Kit            │    │
│                 │  │ (Local) │ │   └──────────────────────┘    │
│                 │  └─────────┘ │                                │
│                 └──────────────┘                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. SYSTEM REQUIREMENTS

### 3.1 Hardware Requirements

#### 3.1.1 Mobile Device (End-User)
| Component | Minimum Requirement | Recommended |
|---|---|---|
| Operating System | Android 7.1 (API 25) | Android 13+ (API 33) |
| RAM | 3 GB | 4 GB or higher |
| Storage | 200 MB free space | 500 MB or more |
| Camera | Rear camera (required) | 12 MP or higher |
| Microphone | Built-in microphone | — |
| Internet | Wi-Fi or Mobile Data | Stable broadband |
| GPS | GPS-enabled device | — |

#### 3.1.2 Development Machine
| Component | Minimum Requirement |
|---|---|
| Operating System | Windows 10 / macOS 10.14 / Linux |
| RAM | 8 GB |
| Storage | 10 GB free for Android Studio + SDK |
| Processor | Intel i5 or equivalent |

### 3.2 Software Requirements

#### 3.2.1 Development Tools
| Software | Version |
|---|---|
| Android Studio | Latest stable (Hedgehog or newer) |
| Kotlin | 1.9+ |
| Gradle | 8.x |
| JDK | 11 |
| Android SDK | Compile SDK 36, Min SDK 25, Target SDK 36 |

#### 3.2.2 Backend Services
| Service | Purpose |
|---|---|
| Firebase Authentication | User login & registration |
| Firebase Firestore | Cloud database for app data |
| Firebase Cloud Storage | File/image storage |
| Firebase Cloud Messaging (FCM) | Push notifications |
| Cloudinary | Media upload & CDN delivery |
| Google Maps / Location Services | Location-based features |

#### 3.2.3 Libraries and Frameworks
| Library | Purpose |
|---|---|
| Jetpack Compose | Modern declarative UI framework |
| Material Design 3 | UI components and theming |
| TensorFlow Lite | On-device ML model inference |
| CameraX | Camera integration |
| Room Database | Local data persistence |
| Coil | Asynchronous image loading |
| OkHttp | HTTP client for network requests |
| Gson | JSON serialization/deserialization |
| ML Kit (Face Detection) | Face detection for selfie validation |
| UCrop | Image cropping |
| WorkManager | Background task scheduling |
| Biometric | Fingerprint/biometric authentication |
| Navigation Compose | In-app screen navigation |

---

## 4. SYSTEM ARCHITECTURE

### 4.1 Architecture Pattern

ChickCare follows the **MVVM (Model-View-ViewModel)** architecture pattern combined with a **Repository pattern** for clean separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                │
│                                                     │
│   ┌─────────────┐          ┌────────────────────┐   │
│   │   Screens   │◀────────▶│   ViewModels       │   │
│   │  (Compose)  │          │  - AuthViewModel    │   │
│   │             │          │  - DashboardVM      │   │
│   │ 56 screens  │          │  - ChatViewModel    │   │
│   │             │          │  - FriendViewModel   │   │
│   └─────────────┘          │  - + 11 more        │   │
│                            └────────┬───────────┘   │
├─────────────────────────────────────┼───────────────┤
│                    DOMAIN LAYER     │               │
│                            ┌────────▼───────────┐   │
│                            │   Repositories     │   │
│                            │  - Detection       │   │
│                            │  - Post            │   │
│                            │  - Friend          │   │
│                            │  - Message         │   │
│                            │  - + 14 more       │   │
│                            └────────┬───────────┘   │
├─────────────────────────────────────┼───────────────┤
│                    DATA LAYER       │               │
│   ┌──────────────┐   ┌─────────────▼────────────┐  │
│   │  Room (Local)│   │   Firebase (Remote)      │  │
│   │  - LocalUser │   │   - Firestore            │  │
│   │  - AppDB     │   │   - Auth                 │  │
│   └──────────────┘   │   - Storage              │  │
│                      └──────────────────────────┘  │
│   ┌──────────────┐   ┌──────────────────────────┐  │
│   │  Services    │   │   Workers                │  │
│   │  - Detection │   │   - FeedingReminder      │  │
│   │  - Audio     │   │   - AuthSync             │  │
│   │  - Messaging │   │                          │  │
│   │  - + 17 more │   └──────────────────────────┘  │
│   └──────────────┘                                  │
└─────────────────────────────────────────────────────┘
```

### 4.2 Package Structure

```
com.bisu.chickcare/
├── ChickCareApplication.kt          # Application class (initialization)
├── MainActivity.kt                   # Main entry point, navigation host
├── backend/
│   ├── data/                         # Data models & entities
│   │   ├── AppDatabase.kt           # Room database configuration
│   │   ├── LocalUser.kt             # Local user entity
│   │   ├── LocalUserDao.kt          # Data Access Object
│   │   ├── UserProfile.kt           # User profile model
│   │   ├── WeatherData.kt           # Weather data model
│   │   ├── EmergencyContact.kt      # Emergency contact model
│   │   ├── DashboardUiState.kt      # Dashboard state model
│   │   ├── Countries.kt             # Country data
│   │   └── SettingOption.kt          # Settings option model
│   ├── repository/                   # Data repositories (18 files)
│   │   ├── DetectionRepository.kt    # Disease detection data
│   │   ├── PostRepository.kt         # Community posts
│   │   ├── FriendRepository.kt       # Friend management
│   │   ├── MessageRepository.kt      # Chat messages
│   │   └── ...                       # + 14 more repositories
│   ├── service/                      # Backend services (20 files)
│   │   ├── DetectionService.kt       # Core detection logic
│   │   ├── FusionClassifier.kt       # CNN-MLP fusion model
│   │   ├── ChickenClassifier.kt      # Image classification
│   │   ├── AudioClassifier.kt        # Audio classification
│   │   ├── AudioSpectrogramConverter.kt  # Audio to spectrogram
│   │   └── ...                       # + 15 more services
│   ├── viewmodels/                   # ViewModels (15 files)
│   │   ├── AuthViewModel.kt         # Authentication logic
│   │   ├── DashboardViewModel.kt    # Dashboard data and state
│   │   └── ...                       # + 13 more ViewModels
│   ├── receiver/
│   │   └── ReminderReceiver.kt       # Boot-completed receiver
│   ├── worker/                       # Background workers
│   └── utils/                        # Utility classes
├── frontend/
│   ├── screen/                       # UI Screens (56 files)
│   │   ├── LoginScreen.kt           # User login
│   │   ├── SignupScreen.kt          # User registration
│   │   ├── DashboardScreen.kt       # Home/dashboard
│   │   ├── CameraScreen.kt          # Camera capture
│   │   ├── ResultScreen.kt          # Detection results
│   │   └── ...                       # + 51 more screens
│   ├── components/                   # Reusable UI components (18 files)
│   └── utils/                        # UI utility classes (10 files)
└── ui/
    └── theme/                        # App theme and colors
```

### 4.3 ML Model Architecture

The disease detection system uses a **CNN-MLP Fusion** approach:

```
┌────────────────────┐    ┌────────────────────┐
│   Image Input      │    │   Audio Input       │
│   (Camera/Gallery)  │    │   (Microphone)      │
└────────┬───────────┘    └────────┬───────────┘
         │                          │
         ▼                          ▼
┌────────────────────┐    ┌────────────────────┐
│  Image             │    │  Audio Spectrogram  │
│  Preprocessing     │    │  Conversion         │
│  (Resize, Crop)    │    │  (AudioSpectrogram   │
│                    │    │   Converter)         │
└────────┬───────────┘    └────────┬───────────┘
         │                          │
         ▼                          ▼
┌────────────────────┐    ┌────────────────────┐
│  CNN Feature       │    │  MLP Feature        │
│  Extraction        │    │  Extraction         │
└────────┬───────────┘    └────────┬───────────┘
         │                          │
         └──────────┬───────────────┘
                    ▼
         ┌────────────────────┐
         │  Fusion Layer      │
         │  (cnnmlp_fusion_   │
         │   v2.tflite)       │
         └────────┬───────────┘
                  ▼
         ┌────────────────────┐
         │  Classification    │
         │  Result            │
         │  (Healthy / IB     │
         │   Detected)        │
         └────────────────────┘
```

**Model File:** `cnnmlp_fusion_v2.tflite`  
**Inference:** Performed on-device using TensorFlow Lite for fast, offline-capable detection.

---

## 5. INSTALLATION GUIDE

### 5.1 For End-Users (APK Installation)

1. **Download the APK** — Obtain the ChickCare APK file (provided via CD or distribution link).
2. **Enable Unknown Sources** — Go to **Settings > Security > Install Unknown Apps**, and enable the browser or file manager you will use.
3. **Install the APK** — Locate the downloaded APK file and tap it to begin installation.
4. **Grant Permissions** — Upon first launch, the app will request the following permissions:
   - **Camera** — Required for capturing chicken images for disease detection
   - **Microphone** — Required for recording chicken audio/sounds for analysis
   - **Notifications** — For receiving alerts and reminders
   - **Location** — For weather data and emergency contact features
   - **Storage/Media** — For accessing gallery images
5. **Create an Account** — Register with your email or sign in with Google.
6. **Complete Onboarding** — Follow the onboarding walkthrough to set up your profile and preferences.

### 5.2 For Developers (Building from Source)

#### Prerequisites
- Install **Android Studio** (latest stable version)
- Install **JDK 11**
- Ensure Android SDK is up to date (Compile SDK 36)

#### Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd ChickCare
   ```

2. **Configure Local Properties**  
   Create or edit `local.properties` in the project root:
   ```properties
   sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_API_KEY=your_api_key
   CLOUDINARY_API_SECRET=your_api_secret
   ```

3. **Add Firebase Configuration**  
   - Download `google-services.json` from the Firebase Console.
   - Place it in the `app/` directory.

4. **Add the TFLite Model**  
   - Ensure `cnnmlp_fusion_v2.tflite` is located in `app/src/main/assets/`.

5. **Sync and Build**
   ```bash
   ./gradlew assembleDebug
   ```
   Or in Android Studio: **Build > Make Project**

6. **Run on Device/Emulator**
   - Connect a physical device or start an emulator.
   - Click **Run** (▶) in Android Studio.

### 5.3 CD Contents (for Hardbound Submission)

The attached CD should contain:
```
CD/
├── Manuscript/                    # Thesis manuscript (PDF)
├── Dataset/                       # Training dataset used for the CNN model
├── System Code/
│   ├── Model/                     # CNN-MLP fusion model files (.tflite, training scripts)
│   ├── Frontend/                  # Android app source code (frontend/ package)
│   └── Backend/                   # Android app source code (backend/ package)
└── Runnable System File/          # ChickCare APK file (ready to install)
```

---

## 6. USER GUIDE

### 6.1 Getting Started

#### 6.1.1 Welcome Screen
Upon launching ChickCare for the first time, the user is greeted with a **Welcome Screen** that briefly introduces the app and its purpose. Two options are presented:
- **Login** — For returning users
- **Sign Up** — For new users

#### 6.1.2 Registration (Sign Up)
1. Tap **Sign Up** on the welcome screen.
2. Fill in the required information (name, email, password, location, etc.).
3. Accept the Terms of Service and Privacy Policy.
4. Tap **Create Account**.
5. Verify your email address if prompted.

#### 6.1.3 Login
1. Tap **Login** on the welcome screen.
2. Enter your registered **email** and **password**.
3. Alternatively, use **Google Sign-In** for quick access.
4. If biometric login is enabled, use fingerprint/face recognition.
5. Tap **Login** to access the Dashboard.

#### 6.1.4 Forgot Password
1. Tap **Forgot Password?** on the login screen.
2. Enter your registered email address.
3. A password reset link will be sent to your email.
4. Follow the link to reset your password.

### 6.2 Navigation

ChickCare uses a **bottom navigation bar** with five main tabs:

| Tab | Icon | Description |
|---|---|---|
| **Home** | 🏠 | Dashboard with overview and quick actions |
| **Detection** | 🔍 | Disease detection history and records |
| **Action** | 🔧 | Action tools and farm management |
| **Help** | ❓ | Help center, FAQs, and support |
| **Profile** | 👤 | User profile and settings |

### 6.3 Dashboard (Home)

The Dashboard is the central hub of the application, providing:
- **Quick Action Buttons** — One-tap access to camera detection, audio recording, and image upload
- **Recent Detection Results** — Last detection summary with quick view
- **Weather Information** — Current weather conditions for your farm location
- **Farm Insights** — Quick overview of farm statistics
- **Recent Activity** — Latest actions and events
- **Announcements** — System-wide news and updates

### 6.4 Disease Detection

This is the core feature of ChickCare. There are three methods to perform disease detection:

#### 6.4.1 Camera Detection (Image Capture)
1. Navigate to the **Dashboard** or **Action** tab.
2. Tap the **Camera** button.
3. Point the camera at the chicken.
4. The system uses **ML Kit Face Detection** to validate the image is of a chicken and not a human selfie.
5. Capture the image.
6. Optionally crop the image using the built-in cropper (UCrop).
7. The **Processing Screen** will display while the CNN model analyzes the image.
8. View the **Result Screen** showing the detection outcome with confidence score.

#### 6.4.2 Image Upload Detection
1. Navigate to the **Dashboard** or **Action** tab.
2. Tap the **Image Upload** button.
3. Select a chicken image from the device gallery.
4. Optionally crop and adjust the image.
5. The system processes the image through the CNN model.
6. View the detection result.

#### 6.4.3 Audio Detection
1. Navigate to the **Dashboard** or **Action** tab.
2. Tap the **Audio Recording** button.
3. Record chicken sounds (coughing, sneezing, respiratory sounds).
4. The audio is converted to a **spectrogram** using `AudioSpectrogramConverter`.
5. The spectrogram image is fed through the MLP component of the fusion model.
6. View the detection result.

#### 6.4.4 Understanding Results
The **Result Screen** displays:
- **Detection Status** — Healthy or IB Detected
- **Confidence Score** — Percentage confidence of the prediction
- **Recommendations** — Suggested actions based on the result
- **Option to Save** — Save the detection to history for future reference
- **Option to Share** — Share results with community or veterinarian

### 6.5 Detection History
- View all past detection records with dates and results.
- Filter and search through detection history.
- Access detailed results for each detection.
- Archive or delete old records.
- **Trash** feature with 30-day recovery.
- Post detection results to the community.

---

## 7. SYSTEM MODULES AND FEATURES

### 7.1 Farm Management Module

#### 7.1.1 Egg Production Tracker
- Record daily egg collection data
- Track production trends over time
- View production analytics and charts

#### 7.1.2 Feeding Schedule
- Create and manage feeding schedules
- Set feeding reminders with push notifications
- Track feeding history and patterns
- **WorkManager-based background reminders** ensure notifications even when the app is closed

#### 7.1.3 Vaccination Schedule
- Create vaccination records and schedules
- Set vaccination reminders
- Track vaccination history per flock

#### 7.1.4 Medications Log
- Log all medications administered
- Track dosage, frequency, and duration
- View medication history

#### 7.1.5 Health Records
- Maintain comprehensive health records
- Track symptoms and treatments
- Historical health data for reference

#### 7.1.6 Expense Tracker
- Record farm-related expenses
- Categorize spending (feed, medicine, equipment, etc.)
- View expense summaries and reports

### 7.2 Community & Social Module

#### 7.2.1 Posts & Feed
- Create posts with text and images
- View the community feed
- Like, comment, and share posts
- Save/bookmark favorite posts
- Post detection results to share with community

#### 7.2.2 Comments
- Comment on community posts
- Reply to other users' comments
- Engage in discussions

#### 7.2.3 Friends System
- Send and receive friend requests
- View friend suggestions
- Manage active friends list
- Block/unblock users

#### 7.2.4 Messaging (Chat)
- Real-time one-on-one messaging
- Chat with friends and community members
- Push notification for new messages

### 7.3 Notifications Module

#### 7.3.1 Push Notifications
- **Firebase Cloud Messaging (FCM)** for real-time push notifications
- Friend requests, messages, post interactions, and system alerts

#### 7.3.2 Reminders
- Feeding schedule reminders
- Vaccination date reminders
- Custom reminders via `ReminderService`

#### 7.3.3 Notification Settings
- Customize which notifications to receive
- Set quiet hours
- Enable/disable specific notification categories

### 7.4 Reports & Analytics Module
- Farm performance dashboards
- Detection history analytics
- Egg production reports
- Expense summaries
- Exportable reports

### 7.5 Emergency & Support Module

#### 7.5.1 Emergency Contacts
- Store emergency contact numbers
- Quick-dial functionality

#### 7.5.2 Vet Contacts
- Find veterinary contacts
- Location-based vet search using **Google Maps/Location Services**

#### 7.5.3 Disease Database
- Comprehensive database of poultry diseases
- Symptoms, causes, and treatment information
- Reference material for farmers

#### 7.5.4 Farm Tips
- Curated tips for poultry farming best practices
- Seasonal advice and recommendations

### 7.6 User Management Module

#### 7.6.1 Profile Management
- Edit personal information
- Upload/change profile picture (via Cloudinary CDN)
- Manage multiple profiles

#### 7.6.2 Account Settings
- Email and password management
- Account deletion
- Data export

#### 7.6.3 Security & Privacy
- **Biometric Authentication** — Fingerprint/face unlock using AndroidX Biometric
- **Two-Factor Authentication (2FA)** — Additional security layer
- Privacy settings and data controls
- Recently deleted items recovery

#### 7.6.4 Language Settings
- Multi-language support
- Language preference persisted locally

#### 7.6.5 App Information
- About the app
- Terms of Service
- Privacy Policy
- Help Center with FAQs

---

## 8. DATABASE DESIGN

### 8.1 Cloud Database (Firebase Firestore)

ChickCare uses **Firebase Firestore** as its primary cloud database. Data is organized into the following collections:

| Collection | Description |
|---|---|
| `users` | User profiles and account information |
| `detections` | Disease detection records and results |
| `posts` | Community posts and content |
| `comments` | Comments on posts |
| `messages` | Chat messages between users |
| `friends` | Friend relationships and requests |
| `notifications` | User notifications |
| `feedingSchedules` | Feeding schedule records |
| `vaccinations` | Vaccination records |
| `medications` | Medication logs |
| `healthRecords` | Health record entries |
| `eggProduction` | Egg production data |
| `expenses` | Expense tracking entries |
| `emergencyContacts` | Emergency contact information |
| `alerts` | System-wide alerts and announcements |
| `reports` | Generated reports data |

### 8.2 Local Database (Room)

A local **Room Database** (`AppDatabase`) is used for offline data caching and performance optimization:

#### LocalUser Entity
| Column | Type | Description |
|---|---|---|
| uid | String (PK) | Firebase user ID |
| email | String | User email address |
| displayName | String | Display name |
| photoUrl | String | Profile photo URL |
| lastSyncTime | Long | Last data sync timestamp |

### 8.3 Cloud Storage (Firebase Storage & Cloudinary)

| Storage | Purpose |
|---|---|
| **Firebase Cloud Storage** | Detection images, audio files |
| **Cloudinary** | Profile pictures, post images (CDN-optimized delivery) |

---

## 9. TECHNICAL SPECIFICATIONS

### 9.1 Application Configuration

| Parameter | Value |
|---|---|
| Application ID | `com.bisu.chickcare` |
| Version Name | 1.0 |
| Version Code | 1 |
| Compile SDK | 36 |
| Minimum SDK | 25 (Android 7.1 Nougat) |
| Target SDK | 36 |
| JVM Target | JDK 11 |
| Build System | Gradle (KTS) |
| UI Framework | Jetpack Compose with Material 3 |

### 9.2 Permissions Required

| Permission | Purpose |
|---|---|
| `CAMERA` | Capturing images for disease detection |
| `RECORD_AUDIO` | Recording chicken sounds for audio analysis |
| `INTERNET` | Firebase services, API calls, CDN access |
| `VIBRATE` | Notification haptic feedback |
| `POST_NOTIFICATIONS` | Displaying push notifications (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Restoring scheduled reminders after device restart |
| `SCHEDULE_EXACT_ALARM` | Precise scheduling of reminders |
| `ACCESS_FINE_LOCATION` | Location-based weather and vet search |
| `ACCESS_COARSE_LOCATION` | Approximate location services |
| `READ_MEDIA_IMAGES` | Accessing gallery for image upload (Android 13+) |
| `READ_MEDIA_AUDIO` | Accessing audio files (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Accessing storage (Android 12 and below) |

### 9.3 Deep Linking

ChickCare supports deep linking for seamless user navigation:
- **HTTPS:** `https://chickcare.app/*`
- **Custom Scheme:** `chickcare://app/*`

### 9.4 Background Processing

| Component | Technology | Purpose |
|---|---|---|
| Feeding Reminders | WorkManager | Scheduled feeding notifications |
| Auth Sync | Service | Synchronize authentication state |
| FCM Service | Firebase Messaging | Receive push notifications |
| Boot Receiver | Broadcast Receiver | Restore reminders on device restart |

---

## 10. SECURITY FEATURES

### 10.1 Authentication
- **Firebase Authentication** for secure user management
- **Google Sign-In** integration via Credential Manager
- **Email/Password** authentication with validation
- **Password Reset** via email

### 10.2 Biometric Security
- **Fingerprint Authentication** using AndroidX Biometric library
- Optional biometric lock for app access

### 10.3 Two-Factor Authentication (2FA)
- Additional verification layer for sensitive operations
- Configurable through Security & Privacy settings

### 10.4 Data Security
- All network communications over **HTTPS**
- Firebase security rules for data access control
- API keys and secrets stored in `local.properties` (excluded from version control via `.gitignore`)
- **Cloudinary** credentials secured via BuildConfig (not hardcoded)
- Room database for local data encryption capability

### 10.5 Privacy Controls
- Configurable privacy settings
- Block/unblock users
- Account deletion option
- Terms of Service and Privacy Policy compliance

---

## 11. TROUBLESHOOTING

### 11.1 Common Issues and Solutions

| Issue | Possible Cause | Solution |
|---|---|---|
| App crashes on startup | Outdated Android version | Ensure Android 7.1+ (API 25) |
| Camera not working | Permission denied | Go to Settings > Apps > ChickCare > Permissions, enable Camera |
| Detection results inaccurate | Poor image quality | Ensure good lighting, clear image of the chicken, avoid blurry photos |
| Audio detection fails | Background noise | Record in a quiet environment close to the chicken |
| Notifications not received | Permissions not granted | Enable notification permissions in device settings |
| Cannot upload images | No internet connection | Check Wi-Fi or mobile data connection |
| Login fails | Incorrect credentials | Use Forgot Password to reset, or check email/password |
| App runs slowly | Low device memory | Close other apps, clear cache in Settings > Apps > ChickCare |
| Weather not showing | Location permission denied | Enable location access in device settings |
| Reminders not firing | Battery optimization | Disable battery optimization for ChickCare |

### 11.2 Error Reporting
If issues persist, users can:
1. Use the **Help Center** within the app
2. Contact the development team via the **About** screen
3. Report bugs through the community platform

---

## 12. GLOSSARY

| Term | Definition |
|---|---|
| **Infectious Bronchitis (IB)** | A highly contagious viral respiratory disease of chickens caused by the Infectious Bronchitis Virus (IBV) |
| **Convolutional Neural Network (CNN)** | A type of deep learning algorithm primarily used for image recognition and classification |
| **Multi-Layer Perceptron (MLP)** | A type of artificial neural network with multiple layers for pattern recognition |
| **TensorFlow Lite (TFLite)** | A lightweight version of TensorFlow for deploying ML models on mobile and edge devices |
| **Spectrogram** | A visual representation of the frequency spectrum of audio signals over time |
| **Fusion Model** | A model that combines multiple data sources (image + audio) for improved prediction accuracy |
| **Firebase** | Google's mobile development platform providing backend services including authentication, database, and storage |
| **Firestore** | A NoSQL cloud database by Firebase for storing and syncing data |
| **Jetpack Compose** | Android's modern toolkit for building native UI declaratively |
| **MVVM** | Model-View-ViewModel architectural pattern for separating UI logic from business logic |
| **Room Database** | An Android persistence library providing an abstraction layer over SQLite |
| **WorkManager** | An Android library for scheduling deferrable, guaranteed background work |
| **FCM** | Firebase Cloud Messaging — Google's cross-platform messaging solution |
| **Cloudinary** | A cloud-based media management platform for image and video storage, optimization, and delivery |
| **CameraX** | An Android Jetpack library for camera functionality |
| **Deep Linking** | A technique that allows navigation to specific content within a mobile app via URLs |
| **Biometric Authentication** | Security authentication method using biological characteristics such as fingerprints or facial recognition |

---

**END OF SYSTEM MANUAL**

---

*ChickCare © 2026 — BISU. All rights reserved.*
