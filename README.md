# Hikigai Koi Chat

An innovative healthcare chat application powered by AI, focusing on mental health support and medical assistance.

## ⚠️ Disclaimer

This is a playground for Doctors to test out our AI engine capabilities. This should not be used for diagnosis. If you are not a medical professional, please talk to your doctor for any diagnosis. If you are a doctor, please do not use this free app for consulting with patients and use your knowledge and discretion instead.

## Features

- AI-powered chat assistance
- Medical consultation
- Secure user authentication
- Real-time chat functionality
- Runtime permissions handling
- Encrypted network communications


## Contact

Your Name - [Vaishnavi2306] Project Link: https://github.com/Vaishnavi2306/HikigaiKoiChat

### 2. User Authentication and Profile Management
- Secure user authentication using Firebase Authentication.
- User profile management to view and update personal information.
- Profile details are stored and managed securely.

### 3. Real-Time Messaging
- **Chat Functionality**: Real-time messaging system using Koi Chat API.
- **Message History**: Efficient handling and display of past chat messages.

## Getting Started

Follow these steps to set up the project on your local machine.

### Prerequisites
- Android Studio
- Firebase Project (for authentication and storage)
- Kotlin or Java (based on your project setup)

### Installation

1. **Clone the Repository:**
git clone https://github.com/Vaishnavi2306/HikigaiKoiChat


2. **Open the Project in Android Studio:**
- Open the `HikigaiKoiChat` folder in Android Studio.

3. **Set Up Firebase:**
- Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
- Enable Firebase Authentication (Email/Password & Google Sign-In).
- Download `google-services.json` and place it in the `app/` directory.

4. **Install Dependencies:**
- Ensure that all dependencies are added by syncing the project with Gradle files.
- Run the following command to fetch the necessary dependencies:
  ```
  ./gradlew build
  ```

### Firebase Authentication

1. **Firebase Authentication Setup**:  
- Firebase Authentication is used to handle user sign-up and login securely.  
- Supports email/password authentication and Google sign-in for convenience.

2. **Error Handling**:  
- Handles various authentication error scenarios (e.g., incorrect email/password, network issues).

3. **Session Management**:  
- Uses Firebase's persistent session management for automatic re-authentication.

### Profile Management

- Users can update their profiles (name, email, profile picture).
- The profile information is stored securely and can be updated in real-time.

### Koi Chat API Integration

1. **Real-Time Messaging**:  
- The app integrates with Koi Chat API to provide real-time chat between healthcare professionals.
- Supports both text messages and media sharing (images, videos).


### Optimizations

- **Firebase Caching**:  
The app uses Firebase's offline capabilities to cache user data for faster access, even when the network is unavailable.



## Usage

1. **Sign Up / Login**:  
- Users can sign up with their Mobile number. Upon successful authentication, they can access the main features of the app.

2. **Profile Management**:  
- Users can view and edit their profiles.

3. **Chat**:  
- Users can communicate in real-time with other AI  through the integrated chat functionality. 


