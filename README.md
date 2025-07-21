# OTP Auto Read - Android SMS Verification

A complete Android application demonstrating automatic OTP (One-Time Password) reading using Google's SMS Retriever API with Jetpack Compose UI.

## 🚀 Features

- **Automatic SMS Detection**: Uses SMS Retriever API to automatically detect and extract OTP from incoming SMS messages
- **Modern UI**: Built with Jetpack Compose featuring 4 elegant input fields for OTP display
- **Auto-Population**: OTP fields are automatically filled when SMS is received
- **Manual Input Support**: Users can manually enter OTP if automatic detection fails
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Loading States**: Visual feedback during SMS waiting and verification processes
- **Production Ready**: Follows Android development best practices and MVVM architecture

## 📱 Screenshots

The app features a clean, modern interface with:
- 4 individual OTP input fields with focus management
- Automatic SMS detection indicator
- Error messages and success confirmation
- App hash display for testing purposes

## 🏗️ Architecture

The application follows MVVM (Model-View-ViewModel) architecture:

```
├── ui/
│   ├── components/
│   │   └── OTPInputField.kt          # Reusable OTP input components
│   └── theme/                        # Material Design theme
├── viewmodel/
│   └── OTPViewModel.kt               # State management and business logic
├── sms/
│   ├── SMSRetrieverHelper.kt         # SMS Retriever API utilities
│   └── SMSBroadcastReceiver.kt       # SMS broadcast receiver
└── MainActivity.kt                   # Main UI screen
```

## 🔧 Implementation Details

### 1. SMS Retriever API Integration

The SMS Retriever API allows automatic SMS verification without requiring SMS permissions:

- **No Permissions Required**: Uses Google Play Services, no SMS read permissions needed
- **Secure**: Only works with SMS containing your app's hash
- **Time Limited**: 5-minute timeout for security
- **Hash-Based**: SMS must contain app-specific hash for verification

### 2. Key Components

#### SMSRetrieverHelper.kt
- Initializes SMS Retriever client
- Generates app hash for SMS verification
- Manages receiver registration/unregistration
- Provides OTP extraction utilities

#### SMSBroadcastReceiver.kt
- Listens for SMS_RETRIEVED_ACTION broadcasts
- Extracts OTP from SMS content using multiple regex patterns
- Handles success/error scenarios
- Provides callbacks for UI updates

#### OTPViewModel.kt
- Manages OTP state and loading states
- Handles SMS retriever lifecycle
- Provides auto-verification logic
- Manages timeout and error handling

#### OTPInputField.kt
- Custom Compose components for OTP input
- Auto-focus management between fields
- Visual feedback for focus states
- Keyboard navigation support

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Google Play Services installed on target device
- Kotlin 1.8.0 or later

## 🛠️ Setup Instructions

### 1. Clone and Build

```bash
git clone <repository-url>
cd OTPAutoRead
./gradlew build
```

### 2. Dependencies

The following dependencies are automatically included:

```kotlin
// Google Play Services for SMS Retriever API
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")

// ViewModel and LiveData
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
```

### 3. Permissions

The app only requires the SMS_RETRIEVER permission (automatically granted):

```xml
<uses-permission android:name="com.google.android.gms.permission.SMS_RETRIEVER" />
```

### 4. Manifest Configuration

The SMS broadcast receiver is configured in AndroidManifest.xml:

```xml
<receiver
    android:name=".sms.SMSBroadcastReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.android.gms.auth.api.phone.SMS_RETRIEVED" />
    </intent-filter>
</receiver>
```

## 🧪 Testing

### 1. Get App Hash

Run the app and note the app hash displayed at the bottom of the screen. This hash is unique to your app's package name and signing certificate.

### 2. Send Test SMS

Send an SMS to your device with the following format:

```
Your verification code is: 1234 [YOUR_APP_HASH]
```

Example:
```
Your verification code is: 1234 FA+9qCX9VSu
```

### 3. Supported OTP Formats

The app can extract OTP from various SMS formats:

- `Your OTP is 1234`
- `Verification code: 1234`
- `OTP: 1234`
- `Code 1234`
- `[1234]`
- `(1234)`
- Simple 4-6 digit numbers

## 🔒 Security Considerations

### 1. App Hash Security
- App hash is generated from package name and signing certificate
- Each app has a unique hash
- SMS must contain exact hash to be processed

### 2. Timeout Protection
- SMS Retriever automatically times out after 5 minutes
- Prevents indefinite listening for SMS messages
- User can manually enter OTP if automatic detection fails

### 3. No Sensitive Permissions
- No SMS read permissions required
- Uses Google Play Services for secure SMS handling
- Cannot read arbitrary SMS messages

## 🚀 Production Deployment

### 1. Release Build Configuration

For production builds, ensure:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 2. ProGuard Rules

Add to `proguard-rules.pro`:

```proguard
# Keep SMS Retriever classes
-keep class com.google.android.gms.auth.api.phone.** { *; }
-keep class com.example.otpautoread.sms.** { *; }
```

### 3. App Signing

- Use consistent signing certificate across builds
- App hash changes with different signing certificates
- Test with release keystore before production

## 🔧 Customization

### 1. OTP Length

To support different OTP lengths, modify the regex patterns in `SMSBroadcastReceiver.kt`:

```kotlin
// For 6-digit OTP
Regex("\\b\\d{6}\\b")

// For variable length (4-8 digits)
Regex("\\b\\d{4,8}\\b")
```

### 2. UI Customization

Modify `OTPInputField.kt` to customize:
- Field appearance and colors
- Animation effects
- Input validation
- Focus behavior

### 3. SMS Patterns

Add custom SMS patterns in `SMSBroadcastReceiver.kt`:

```kotlin
val customPatterns = listOf(
    Regex("(?i)your\\s+token\\s*:?\\s*(\\d{4,6})"),
    Regex("(?i)security\\s+code\\s*:?\\s*(\\d{4,6})")
)
```

## 🐛 Troubleshooting

### Common Issues

1. **SMS Not Detected**
   - Verify app hash is included in SMS
   - Check SMS format matches supported patterns
   - Ensure Google Play Services is updated

2. **App Hash Not Generated**
   - Verify app is signed (debug or release)
   - Check package name matches manifest
   - Ensure Google Play Services dependency is included

3. **Receiver Not Triggered**
   - Verify receiver is registered in manifest
   - Check intent filter action name
   - Ensure SMS Retriever is started before SMS arrives

### Debug Tips

1. Enable verbose logging:
```kotlin
Log.setProperty("log.tag.SMSRetrieverHelper", "VERBOSE")
```

2. Test with Android emulator:
   - Use extended controls to send SMS
   - Include app hash in test messages

3. Monitor logcat for SMS Retriever events:
```bash
adb logcat | grep -E "(SMSRetriever|OTPViewModel|SMSBroadcast)"
```

## 📚 Additional Resources

- [SMS Retriever API Documentation](https://developers.google.com/identity/sms-retriever/overview)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android MVVM Architecture](https://developer.android.com/jetpack/guide)

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Check existing documentation
- Review troubleshooting section

---

## 📖 Detailed Implementation Guide

### Step-by-Step Implementation Process

This section provides a comprehensive walkthrough of how the OTP auto-read functionality was implemented.

#### Step 1: Project Setup and Dependencies

1. **Add Required Dependencies**
   ```kotlin
   // In app/build.gradle.kts
   dependencies {
       // Google Play Services for SMS Retriever API
       implementation("com.google.android.gms:play-services-auth:21.0.0")
       implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")

       // ViewModel and LiveData for state management
       implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
       implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
   }
   ```

2. **Configure Manifest Permissions**
   ```xml
   <!-- SMS Retriever permission (automatically granted) -->
   <uses-permission android:name="com.google.android.gms.permission.SMS_RETRIEVER" />

   <!-- Register SMS Broadcast Receiver -->
   <receiver
       android:name=".sms.SMSBroadcastReceiver"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.android.gms.auth.api.phone.SMS_RETRIEVED" />
       </intent-filter>
   </receiver>
   ```

#### Step 2: SMS Retriever Helper Implementation

The `SMSRetrieverHelper` class handles the core SMS Retriever API functionality:

**Key Methods:**
- `startSMSRetriever()`: Initializes the SMS Retriever client
- `getAppHash()`: Generates the unique app hash for SMS verification
- `extractOTPFromSMS()`: Parses OTP from SMS content
- `registerSMSReceiver()`: Sets up broadcast receiver for SMS events

**Important Implementation Details:**
```kotlin
// Starting SMS Retriever
val client = SmsRetriever.getClient(context)
val task = client.startSmsRetriever()

task.addOnSuccessListener {
    // SMS Retriever started successfully
    registerSMSReceiver(onSmsReceived, onError)
}
```

#### Step 3: Broadcast Receiver Implementation

The `SMSBroadcastReceiver` listens for SMS_RETRIEVED_ACTION broadcasts:

**Key Features:**
- Multiple regex patterns for OTP extraction
- Comprehensive error handling
- Callback-based communication with UI
- Automatic cleanup and resource management

**OTP Extraction Logic:**
```kotlin
val otpPatterns = listOf(
    Regex("\\b\\d{4,6}\\b"),                    // Standard digits
    Regex("(?i)otp\\s*:?\\s*(\\d{4,6})"),      // "OTP: 1234"
    Regex("(?i)code\\s*:?\\s*(\\d{4,6})"),     // "Code: 1234"
    // Additional patterns...
)
```

#### Step 4: ViewModel Architecture

The `OTPViewModel` manages application state using MVVM pattern:

**State Management:**
- `otpValue`: Current OTP input
- `isLoading`: Loading state for UI feedback
- `errorMessage`: Error messages for user display
- `isOtpVerified`: Verification success state
- `smsRetrievalActive`: SMS listening status

**Lifecycle Management:**
```kotlin
override fun onCleared() {
    super.onCleared()
    stopSMSRetriever() // Clean up resources
}
```

#### Step 5: Jetpack Compose UI Components

**OTPInputField Component Features:**
- Individual digit input with auto-focus
- Visual feedback for focus states
- Keyboard navigation support
- Auto-advance to next field

**Component Architecture:**
```kotlin
@Composable
fun OTPInputComponent(
    otp: String,
    onOtpChange: (String) -> Unit,
    onOtpComplete: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
)
```

#### Step 6: Integration and Testing

**MainActivity Integration:**
- ViewModel initialization and state observation
- Lifecycle-aware SMS Retriever management
- Error handling and user feedback
- Success state management

**Testing Considerations:**
- App hash generation and display
- Multiple SMS format support
- Timeout handling (5-minute limit)
- Manual fallback options

### Best Practices Implemented

1. **Security**
   - No sensitive SMS permissions required
   - App hash verification for SMS authenticity
   - Automatic timeout for security

2. **User Experience**
   - Automatic OTP population
   - Manual input fallback
   - Clear loading and error states
   - Intuitive UI with focus management

3. **Performance**
   - Efficient regex patterns for OTP extraction
   - Proper resource cleanup
   - Minimal memory footprint

4. **Maintainability**
   - Clean architecture with separation of concerns
   - Comprehensive error handling
   - Extensive logging for debugging
   - Well-documented code

### Production Considerations



1. **Testing Strategy**
   - Unit tests for OTP extraction logic
   - Integration tests for SMS Retriever flow
   - UI tests for user interactions
   - End-to-end testing with real SMS

2. **Error Handling**
   - Network connectivity issues
   - Google Play Services availability
   - SMS format variations
   - Timeout scenarios

3. **Performance Optimization**
   - Lazy initialization of components
   - Efficient state management
   - Memory leak prevention
   - Battery usage optimization

This implementation provides a robust, production-ready solution for automatic OTP reading in Android applications while maintaining security and user experience standards.


adb -s emulator-5554 adb -s emulator-5554 emu sms send 1234567890 "<#> Your OTP is 123456\nFA+9qCX9VNm" emu sms send <phone_number> "<sms_body>"

adb -s emulator-5554 emu sms send 1234567890 "<#> Your OTP is 123456\nFA+9qCX9VNm"
adb -s emulator-5554 emu sms send 1234567890 "<#> Your OTP is 123456\nFA+9qCX9VNm"
adb -s emulator-5554 emu sms send 1234567890 "Your verification code: 1234 FA+9qCX9VNm"
✅ "Your verification code: 1234 FA+9qCX9VSu"
✅ "OTP: 1234 (Auto-code: FA+9qCX9VSu)"
✅ "Login code: 1234 FA+9qCX9VSu - Don't share this code"
✅ "1234 is your verification code FA+9qCX9VSu"


to check devices:- abd devices
 to snd sms :- adb -s emulator-5554 emu sms send 1234567890 "<#> Your OTP is 123456\nMDgjQjcQqmo"