# Simple OTP Auto-Read Implementation Guide

## Overview

This guide explains how to implement automatic OTP reading from SMS using Android's SMS Retriever API with a simple 4-dash UI display. The implementation is purely automatic - no manual input required.

## 🎯 What We're Building

- **Simple UI**: 4 dash boxes that automatically fill with OTP digits
- **Automatic SMS Reading**: Uses SMS Retriever API to detect and extract OTP
- **No Manual Input**: Purely automatic - user doesn't need to type anything
- **Real-time Updates**: UI updates as each digit is received

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Google Play Services on target device
- Kotlin 1.8.0+

## 🛠️ Step-by-Step Implementation

### Step 1: Add Dependencies

First, add the required dependencies to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // Google Play Services for SMS Retriever API
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.1")
    
    // ViewModel and LiveData for state management
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
}
```

**Why these dependencies?**
- `play-services-auth`: Core Google Play Services authentication
- `play-services-auth-api-phone`: SMS Retriever API functionality
- `lifecycle-*`: For MVVM architecture and state management
- `runtime-livedata`: To observe LiveData in Compose

### Step 2: Configure AndroidManifest.xml

Add the required permission and broadcast receiver:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- SMS Retriever permission (automatically granted) -->
    <uses-permission android:name="com.google.android.gms.permission.SMS_RETRIEVER" />

    <application ...>
        
        <!-- Your MainActivity -->
        <activity android:name=".MainActivity" ... />

        <!-- SMS Broadcast Receiver -->
        <receiver
            android:name=".sms.SMSBroadcastReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.android.gms.auth.api.phone.SMS_RETRIEVED" />
            </intent-filter>
        </receiver>
        
    </application>
</manifest>
```

**Key Points:**
- `SMS_RETRIEVER` permission is automatically granted (no user prompt)
- Receiver must be registered to listen for SMS_RETRIEVED_ACTION
- `exported="false"` for security

### Step 3: Create SMS Retriever Helper

Create `SMSRetrieverHelper.kt` to handle SMS Retriever API:

```kotlin
package com.example.otpautoread.sms

import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import java.security.MessageDigest
import java.util.*

class SMSRetrieverHelper(private val context: Context) {

    companion object {
        private const val TAG = "SMSRetrieverHelper"
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }

    private var smsReceiver: SMSBroadcastReceiver? = null

    /**
     * Starts SMS Retriever and registers broadcast receiver
     */
    fun startSMSRetriever(
        onSmsReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Get SMS Retriever client
            val client = SmsRetriever.getClient(context)
            
            // Start SMS retriever (5-minute timeout)
            val task = client.startSmsRetriever()
            
            task.addOnSuccessListener { 
                Log.d(TAG, "SMS Retriever started successfully")
                registerSMSReceiver(onSmsReceived, onError)
            }
            
            task.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to start SMS Retriever", exception)
                onError("Failed to start SMS Retriever: ${exception.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SMS Retriever", e)
            onError("Error starting SMS Retriever: ${e.message}")
        }
    }

    /**
     * Registers broadcast receiver for SMS events
     */
    private fun registerSMSReceiver(
        onSmsReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            smsReceiver = SMSBroadcastReceiver().apply {
                setOnSmsReceivedListener(onSmsReceived)
                setOnErrorListener(onError)
            }
            
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            context.registerReceiver(smsReceiver, intentFilter)
            Log.d(TAG, "SMS Broadcast Receiver registered")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering SMS receiver", e)
            onError("Error registering SMS receiver: ${e.message}")
        }
    }

    /**
     * Generates app hash for SMS verification
     * This hash must be included in SMS messages
     */
    fun getAppHash(): String {
        return try {
            val packageName = context.packageName
            val signatures = context.packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            ).signatures
            
            signatures.firstOrNull()?.let { signature ->
                generateHash(packageName, signature.toCharsString())
            } ?: ""
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating app hash", e)
            ""
        }
    }

    /**
     * Generates hash from package name and signature
     */
    private fun generateHash(packageName: String, signature: String): String {
        return try {
            val appInfo = "$packageName $signature"
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray())
            
            val hashSignature = messageDigest.digest()
            val truncatedHash = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
            val base64Hash = Base64.getEncoder().encodeToString(truncatedHash)
            
            base64Hash.substring(0, NUM_BASE64_CHAR)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hash", e)
            ""
        }
    }

    /**
     * Unregisters SMS receiver
     */
    fun unregisterSMSReceiver() {
        try {
            smsReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
                smsReceiver = null
                Log.d(TAG, "SMS Receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering SMS receiver", e)
        }
    }
}
```

**Key Functions Explained:**

1. **`startSMSRetriever()`**: Initializes the SMS Retriever client and starts listening
2. **`getAppHash()`**: Generates unique app hash required for SMS verification
3. **`registerSMSReceiver()`**: Sets up broadcast receiver for SMS events
4. **`unregisterSMSReceiver()`**: Cleans up resources when done

### Step 4: Create SMS Broadcast Receiver

Create `SMSBroadcastReceiver.kt` to handle incoming SMS:

```kotlin
package com.example.otpautoread.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SMSBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSBroadcastReceiver"
    }

    private var onSmsReceivedListener: ((String) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null

    fun setOnSmsReceivedListener(listener: (String) -> Unit) {
        this.onSmsReceivedListener = listener
    }

    fun setOnErrorListener(listener: (String) -> Unit) {
        this.onErrorListener = listener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Broadcast received: ${intent?.action}")

        if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) {
            return
        }

        try {
            val extras: Bundle? = intent.extras
            val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (smsRetrieverStatus?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // SMS successfully retrieved
                    val smsMessage = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                    Log.d(TAG, "SMS received: $smsMessage")
                    
                    if (!smsMessage.isNullOrEmpty()) {
                        val otp = extractOTPFromMessage(smsMessage)
                        if (otp != null) {
                            onSmsReceivedListener?.invoke(otp)
                        } else {
                            onErrorListener?.invoke("Could not extract OTP from SMS")
                        }
                    }
                }
                
                CommonStatusCodes.TIMEOUT -> {
                    Log.w(TAG, "SMS Retriever timed out")
                    onErrorListener?.invoke("SMS verification timed out")
                }
                
                else -> {
                    Log.e(TAG, "SMS Retriever failed: ${smsRetrieverStatus?.statusCode}")
                    onErrorListener?.invoke("SMS Retriever failed")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            onErrorListener?.invoke("Error processing SMS: ${e.message}")
        }
    }

    /**
     * Extracts 4-digit OTP from SMS message using regex patterns
     */
    private fun extractOTPFromMessage(message: String): String? {
        return try {
            val otpPatterns = listOf(
                Regex("\\b\\d{4}\\b"),                                    // Simple 4 digits
                Regex("(?i)(?:otp|code|pin)\\s*:?\\s*(\\d{4})"),        // "OTP: 1234"
                Regex("(?i)verification\\s+code\\s*:?\\s*(\\d{4})"),     // "Verification code: 1234"
                Regex("\\[(\\d{4})\\]"),                                 // "[1234]"
                Regex("\\((\\d{4})\\)")                                  // "(1234)"
            )

            for (pattern in otpPatterns) {
                val matchResult = pattern.find(message)
                if (matchResult != null) {
                    val otp = if (matchResult.groupValues.size > 1) {
                        matchResult.groupValues[1]
                    } else {
                        matchResult.value
                    }
                    
                    if (otp.matches(Regex("\\d{4}"))) {
                        Log.d(TAG, "OTP extracted: $otp")
                        return otp
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting OTP", e)
            null
        }
    }
}
```

**OTP Extraction Logic:**
- Uses multiple regex patterns to find 4-digit codes
- Supports various SMS formats: "OTP: 1234", "[1234]", "Code 1234", etc.
- Returns first valid 4-digit match found

### Step 5: Create ViewModel for State Management

Create `OTPViewModel.kt` to manage app state:

```kotlin
package com.example.otpautoread.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.otpautoread.sms.SMSRetrieverHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OTPViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "OTPViewModel"
    }

    private val smsRetrieverHelper = SMSRetrieverHelper(application)

    // OTP value (4 digits)
    private val _otpValue = MutableLiveData<String>("")
    val otpValue: LiveData<String> = _otpValue

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error messages
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Verification success
    private val _isOtpVerified = MutableLiveData<Boolean>(false)
    val isOtpVerified: LiveData<Boolean> = _isOtpVerified

    // App hash for testing
    private val _appHash = MutableLiveData<String>("")
    val appHash: LiveData<String> = _appHash

    init {
        generateAppHash()
    }

    /**
     * Starts SMS Retriever and begins listening for OTP SMS
     */
    fun startSMSRetriever() {
        Log.d(TAG, "Starting SMS Retriever")

        _isLoading.value = true
        _errorMessage.value = null
        _isOtpVerified.value = false
        _otpValue.value = ""

        smsRetrieverHelper.startSMSRetriever(
            onSmsReceived = { otp ->
                handleOTPReceived(otp)
            },
            onError = { error ->
                handleError(error)
            }
        )
    }

    /**
     * Handles OTP received from SMS
     */
    private fun handleOTPReceived(otp: String) {
        Log.d(TAG, "OTP received: $otp")

        viewModelScope.launch {
            _otpValue.value = otp
            _isLoading.value = false
            _errorMessage.value = null

            // Simulate verification process
            delay(1000)
            _isOtpVerified.value = true
        }
    }

    /**
     * Handles errors from SMS Retriever
     */
    private fun handleError(error: String) {
        Log.e(TAG, "SMS Error: $error")
        _isLoading.value = false
        _errorMessage.value = error
    }

    /**
     * Generates app hash for SMS verification
     */
    private fun generateAppHash() {
        viewModelScope.launch {
            val hash = smsRetrieverHelper.getAppHash()
            _appHash.value = hash
            Log.d(TAG, "App hash: $hash")
        }
    }

    /**
     * Cleans up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        smsRetrieverHelper.unregisterSMSReceiver()
    }
}
```

**ViewModel Responsibilities:**
- Manages UI state (OTP value, loading, errors)
- Coordinates with SMS Retriever Helper
- Handles lifecycle events
- Provides data to UI through LiveData

### Step 6: Create Simple UI with 4 Dashes

The MainActivity contains the simple UI implementation:

```kotlin
/**
 * Simple 4-dash OTP display component
 */
@Composable
fun SimpleDashOTPDisplay(
    otp: String,
    isLoading: Boolean,
    isVerified: Boolean
) {
    val otpDigits = otp.padEnd(4, ' ').take(4).toList()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            OTPDashBox(
                digit = if (otpDigits[index] == ' ') null else otpDigits[index].toString(),
                isLoading = isLoading && index == otp.length,
                isVerified = isVerified
            )
        }
    }
}

/**
 * Individual dash box for each OTP digit
 */
@Composable
fun OTPDashBox(
    digit: String?,
    isLoading: Boolean,
    isVerified: Boolean
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = when {
                    isVerified -> MaterialTheme.colorScheme.primary
                    digit != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    isLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            digit != null -> {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}
```

**UI Features:**
- 4 boxes showing dashes initially
- Automatically fills with digits as OTP is received
- Loading indicator on the next expected digit
- Visual feedback for verification success

## 🧪 Testing Your Implementation

### 1. Get Your App Hash

Run the app and note the app hash displayed at the bottom. Example: `FA+9qCX9VSu`

### 2. Send Test SMS

Send an SMS to your device with this format:
```
Your verification code is: 1234 FA+9qCX9VSu
```

### 3. Observe Automatic Behavior

- App starts listening for SMS automatically
- When SMS arrives, OTP digits fill the dashes one by one
- Success message appears when verification completes

### 4. Supported SMS Formats

```
✅ Your OTP is 1234 [HASH]
✅ Verification code: 1234 [HASH]
✅ Code 1234 [HASH]
✅ [1234] [HASH]
✅ PIN: 1234 [HASH]
```

## 🔧 How It Works

### SMS Retriever API Flow

1. **App starts SMS Retriever** → Google Play Services begins monitoring
2. **SMS arrives** → If it contains your app hash, Google Play Services captures it
3. **Broadcast sent** → Your app receives SMS_RETRIEVED_ACTION broadcast
4. **OTP extracted** → Regex patterns extract the 4-digit code
5. **UI updated** → Dashes automatically fill with digits

### Security Features

- **No SMS permissions required** → Uses Google Play Services
- **App hash verification** → SMS must contain your unique hash
- **5-minute timeout** → Automatic security timeout
- **Cannot read other SMS** → Only works with hash-verified messages

## 🚀 Production Considerations

### Error Handling
- Network connectivity issues
- Google Play Services availability
- Invalid SMS formats
- Timeout scenarios

### Performance
- Automatic resource cleanup
- Efficient regex patterns
- Minimal battery usage
- Memory leak prevention

### Testing
- Test with different SMS formats
- Verify on multiple Android versions
- Test timeout behavior
- Validate app hash generation

This implementation provides a clean, automatic OTP reading experience with minimal user interaction required!
