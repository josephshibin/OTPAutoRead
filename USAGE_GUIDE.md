# OTP Auto Read - Usage Guide

## Quick Start

### 1. Running the Application

1. **Build and Install**
   ```bash
   ./gradlew installDebug
   ```

2. **Launch the App**
   - Open the app on your Android device
   - The app will automatically start listening for SMS messages
   - Note the app hash displayed at the bottom of the screen

### 2. Testing OTP Auto-Read

#### Method 1: Send Test SMS from Another Device

1. **Get the App Hash**
   - Launch the app and copy the app hash shown at the bottom
   - Example: `FA+9qCX9VSu`

2. **Send SMS with Correct Format**
   ```
   Your verification code is: 1234 FA+9qCX9VSu
   ```

3. **Observe Auto-Population**
   - The OTP fields should automatically fill with `1234`
   - The app will show verification success

#### Method 2: Using Android Emulator

1. **Extended Controls**
   - Open Android emulator
   - Click "..." (Extended controls)
   - Go to "Phone" section

2. **Send SMS**
   - Enter your app's phone number
   - Message: `Your OTP is 1234 [YOUR_APP_HASH]`
   - Click "Send message"

#### Method 3: ADB Command (For Testing)

```bash
# Send SMS via ADB
adb emu sms send +1234567890 "Your verification code is: 1234 FA+9qCX9VSu"
```

### 3. Supported SMS Formats

The app recognizes these OTP formats:

```
✅ Your OTP is 1234
✅ Verification code: 1234  
✅ OTP: 1234
✅ Code 1234
✅ Your verification code is: 1234
✅ Security code: 1234
✅ [1234]
✅ (1234)
✅ PIN: 1234
✅ Token: 1234
```

### 4. Manual OTP Entry

If automatic detection fails:

1. **Stop Auto-Detection**
   - Tap "Enter Manually" button
   - This stops SMS listening

2. **Manual Input**
   - Tap on the first OTP field
   - Enter digits using keyboard
   - Fields auto-advance as you type

3. **Verification**
   - Complete 4-digit entry triggers automatic verification
   - Or tap outside fields to verify

## Troubleshooting

### Common Issues and Solutions

#### 1. SMS Not Detected

**Problem**: SMS received but OTP not extracted

**Solutions**:
- ✅ Verify SMS contains the exact app hash
- ✅ Check SMS format matches supported patterns
- ✅ Ensure SMS arrives within 5 minutes of starting the app
- ✅ Try different SMS formats from the supported list

**Debug Steps**:
```bash
# Check logcat for SMS events
adb logcat | grep -E "(SMSRetriever|OTPViewModel)"
```

#### 2. App Hash Issues

**Problem**: App hash not generated or incorrect

**Solutions**:
- ✅ Ensure app is properly signed (debug or release)
- ✅ Verify Google Play Services is installed and updated
- ✅ Check package name matches AndroidManifest.xml
- ✅ Restart app if hash doesn't appear

**Verification**:
```kotlin
// Check in logs
Log.d("AppHash", "Generated hash: $hash")
```

#### 3. SMS Retriever Not Starting

**Problem**: No response to SMS messages

**Solutions**:
- ✅ Verify Google Play Services is available
- ✅ Check internet connectivity
- ✅ Ensure SMS_RETRIEVER permission is granted
- ✅ Restart the app

**Check Google Play Services**:
```kotlin
val availability = GoogleApiAvailability.getInstance()
val resultCode = availability.isGooglePlayServicesAvailable(context)
```

#### 4. Timeout Issues

**Problem**: SMS Retriever times out after 5 minutes

**Solutions**:
- ✅ Send SMS within 5 minutes of starting the app
- ✅ Tap "Resend OTP" to restart the timer
- ✅ Use manual entry if timeout occurs frequently

### Advanced Usage

#### 1. Custom OTP Patterns

To support custom SMS formats, modify `SMSBroadcastReceiver.kt`:

```kotlin
// Add custom patterns
val customPatterns = listOf(
    Regex("(?i)your\\s+token\\s*:?\\s*(\\d{4,6})"),
    Regex("(?i)access\\s+code\\s*:?\\s*(\\d{4,6})")
)
```

#### 2. Different OTP Lengths

For 6-digit OTP support:

```kotlin
// Update regex patterns
Regex("\\b\\d{6}\\b")

// Update UI component
repeat(6) { index -> // Change from 4 to 6
    OTPInputField(...)
}
```

#### 3. Integration with Backend

```kotlin
// In OTPViewModel.kt
fun verifyOTP(otp: String) {
    viewModelScope.launch {
        try {
            val result = apiService.verifyOTP(otp)
            if (result.isSuccess) {
                _isOtpVerified.value = true
            } else {
                _errorMessage.value = "Invalid OTP"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Verification failed"
        }
    }
}
```

## Performance Tips

### 1. Memory Management

- SMS Retriever automatically unregisters after 5 minutes
- ViewModel cleans up resources in `onCleared()`
- Use `viewModelScope` for coroutines to prevent leaks

### 2. Battery Optimization

- SMS Retriever uses minimal battery
- No continuous SMS monitoring
- Automatic timeout prevents indefinite listening

### 3. Network Usage

- SMS Retriever works offline
- Only initial setup requires network
- No data usage for SMS processing

## Security Best Practices

### 1. App Hash Security

- App hash is unique per app and signing certificate
- Cannot be spoofed by malicious apps
- Changes with different signing keys

### 2. SMS Content Validation

- Always validate extracted OTP format
- Implement server-side verification
- Use HTTPS for OTP verification API calls

### 3. Timeout Handling

- 5-minute timeout prevents indefinite listening
- Implement proper error handling for timeouts
- Provide manual fallback options

## Integration Examples

### 1. With Firebase Auth

```kotlin
// Integrate with Firebase Phone Auth
private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
    val credential = PhoneAuthProvider.getCredential(verificationId, code)
    signInWithPhoneAuthCredential(credential)
}
```

### 2. With Custom Backend

```kotlin
// API integration example
interface OTPService {
    @POST("verify-otp")
    suspend fun verifyOTP(@Body request: OTPRequest): OTPResponse
}

data class OTPRequest(val phoneNumber: String, val otp: String)
data class OTPResponse(val success: Boolean, val token: String?)
```

### 3. With Analytics

```kotlin
// Track OTP events
fun trackOTPEvent(event: String, success: Boolean) {
    analytics.logEvent("otp_verification") {
        param("event_type", event)
        param("success", success)
        param("method", "auto_sms")
    }
}
```

## Testing Checklist

### Before Release

- [ ] Test with different SMS formats
- [ ] Verify app hash generation
- [ ] Test timeout scenarios
- [ ] Test manual input fallback
- [ ] Verify error handling
- [ ] Test on different Android versions
- [ ] Test with/without Google Play Services
- [ ] Performance testing
- [ ] Security testing
- [ ] Accessibility testing

### Production Monitoring

- [ ] Monitor SMS detection success rate
- [ ] Track timeout occurrences
- [ ] Monitor error rates
- [ ] User feedback collection
- [ ] Performance metrics
- [ ] Crash reporting

This guide covers all aspects of using and integrating the OTP Auto Read functionality in your Android application.
