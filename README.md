# Hikigai Koi Chat

A sophisticated AI-powered healthcare platform designed to assist medical professionals in providing better patient care. The application leverages advanced AI technology to enhance diagnosis, streamline workflows, and improve healthcare outcomes.

## Features

### 1. AI-Powered Assistance
- **Enhanced Diagnosis**: Assists doctors in making accurate, timely diagnoses by analyzing visit notes and medical history
- **Personalized Insights**: Delivers tailored recommendations based on individual patient profiles
- **Proactive Care**: Identifies missing lab tests or vaccinations, alerting doctors to ensure comprehensive care
- **OCR Integration**: Extracts key details from prescriptions and lab reports automatically

### 2. Core Functionalities
- **Secure Authentication**: Phone number-based OTP verification system
- **Profile Management**: Comprehensive doctor profile management
- **Interactive UI**: Modern, intuitive interface with smooth transitions and animations
- **Real-time Processing**: Quick response times for AI-powered features

### 3. Technical Features
- **Firebase Integration**: Authentication and data storage
- **ViewPager2**: Smooth sliding cards for features and information
- **Custom Dialogs**: Interactive feature cards and information displays
- **Responsive Design**: Adapts to different screen sizes while maintaining functionality

## Technical Requirements

- Android Studio Arctic Fox or later
- Minimum SDK: Android 21 (Lollipop)
- Target SDK: Android 33
- Kotlin version: 1.8.0
- Gradle version: 7.4.0

## Dependencies

```gradle
dependencies {
    // Firebase
    implementation 'com.google.firebase:firebase-auth-ktx:21.1.0'
    implementation 'com.google.firebase:firebase-firestore-ktx:24.4.1'

    // UI Components
    implementation 'com.tbuonomo:dotsindicator:4.3'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Kotlin Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4'
}
```

## Installation Guide

### Method 1: Install from APK
1. **Download the APK**
   - Visit the [Releases](https://github.com/yourusername/HikigaiKoiChat/releases) page
   - Download the latest `HikigaiKoiChat.apk` file
   
2. **Enable Unknown Sources**
   - Go to `Settings` > `Security` or `Privacy`
   - Enable `Install from Unknown Sources` or `Install Unknown Apps`
   - If prompted, allow your browser/file manager to install apps

3. **Install the APK**
   - Open the downloaded APK file
   - Tap "Install" when prompted
   - Wait for the installation to complete
   - Tap "Open" to launch the app

### Method 2: Build from Source
1. **Prerequisites**
   - Install [Android Studio](https://developer.android.com/studio)
   - Install [Git](https://git-scm.com/downloads)
   - Minimum 8GB RAM recommended
   - At least 10GB free disk space

2. **Clone and Setup**
   ```bash
   # Clone the repository
   git clone https://github.com/yourusername/HikigaiKoiChat.git
   cd HikigaiKoiChat

   # Checkout the latest stable version
   git checkout main
   ```

3. **Open in Android Studio**
   - Launch Android Studio
   - Select `File` > `Open`
   - Navigate to the cloned repository
   - Click `OK` to open the project
   - Wait for Gradle sync to complete

3. Configure Firebase:
   - Create a new Firebase project
   - Add your Android app to Firebase project
   - Download `google-services.json` and place it in the app directory
   - Enable Phone Authentication in Firebase Console

5. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon)
   - Select your device/emulator
   - Wait for the app to build and install

### Minimum Requirements
- Android 5.0 (Lollipop) or higher
- 2GB RAM
- 50MB free storage
- Active internet connection
- Phone number for authentication

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/hikigai/koichat/
│   │   │   ├── adapters/           # RecyclerView adapters
│   │   │   ├── data/              # Data models
│   │   │   ├── network/           # Network related classes
│   │   │   └── [Activities].kt    # Activity classes
│   │   └── res/
│   │       ├── layout/            # XML layouts
│   │       ├── drawable/          # Images and shapes
│   │       └── values/            # Resources
│   └── test/                      # Unit tests
└── build.gradle                   # App level build file
```

## Security Features

- Phone number verification using Firebase Authentication
- Secure data storage with Firebase Firestore
- Runtime permissions handling
- Encrypted network communications

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Contact

Your Name - [@yourusername]
Project Link: [https://github.com/yourusername/HikigaiKoiChat](https://github.com/yourusername/HikigaiKoiChat)

## Acknowledgments

- Firebase Authentication and Firestore for backend services
- [WormDotsIndicator](https://github.com/tommybuonomo/dotsindicator) for ViewPager2 indicators
- Material Design components for UI elements 