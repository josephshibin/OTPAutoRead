package com.example.otpautoread.sms

import android.content.Context
import android.content.ContextWrapper
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Helper class for SMS Retriever API functionality
 * 
 * This class provides utilities for:
 * 1. Starting SMS retriever client
 * 2. Generating app hash for SMS verification
 * 3. Managing SMS receiver registration
 * 
 * The SMS Retriever API allows your app to automatically verify a user's phone number
 * without requiring the user to manually type verification codes and without requiring
 * any extra app permissions.
 */
class SMSRetrieverHelper(private val context: Context) {

    companion object {
        private const val TAG = "SMSRetrieverHelper"
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }

    private var smsReceiver: SMSBroadcastReceiver? = null

    /**
     * Starts the SMS retriever client and registers the broadcast receiver
     * 
     * This method:
     * 1. Initializes the SMS Retriever client
     * 2. Registers a broadcast receiver to listen for incoming SMS
     * 3. Returns a callback indicating success or failure
     * 
     * @param onSmsReceived Callback function called when OTP is extracted from SMS
     * @param onError Callback function called when an error occurs
     */
    fun startSMSRetriever(
        onSmsReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            Log.d(TAG, "=== INITIALIZING SMS RETRIEVER ===")

            // Get SMS Retriever client
            val client = SmsRetriever.getClient(context)
            Log.d(TAG, "SMS Retriever client obtained")

            // Start SMS retriever
            val task = client.startSmsRetriever()
            Log.d(TAG, "SMS Retriever task started")

            task.addOnSuccessListener {
                Log.d(TAG, "=== SMS RETRIEVER STARTED SUCCESSFULLY ===")

                // Register broadcast receiver to listen for SMS
                registerSMSReceiver(onSmsReceived, onError)
            }

            task.addOnFailureListener { exception ->
                Log.e(TAG, "=== SMS RETRIEVER FAILED TO START ===", exception)
                onError("Failed to start SMS Retriever: ${exception.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR STARTING SMS RETRIEVER ===", e)
            onError("Error starting SMS Retriever: ${e.message}")
        }
    }

    /**
     * Registers the SMS broadcast receiver to listen for incoming SMS messages
     * 
     * @param onSmsReceived Callback function called when OTP is extracted
     * @param onError Callback function called when an error occurs
     */
    private fun registerSMSReceiver(
        onSmsReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Create and register SMS receiver
            smsReceiver = SMSBroadcastReceiver().apply {
                setOnSmsReceivedListener(onSmsReceived)
                setOnErrorListener(onError)
            }
            
            // Create intent filter for SMS_RETRIEVED action
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            
            // Register the receiver - use EXPORTED for SMS Retriever API
            Log.d(TAG, "Registering receiver with action: ${SmsRetriever.SMS_RETRIEVED_ACTION}")
            ContextCompat.registerReceiver(
                context,
                smsReceiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.d(TAG, "SMS Broadcast Receiver registered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering SMS receiver", e)
            onError("Error registering SMS receiver: ${e.message}")
        }
    }

    /**
     * Unregisters the SMS broadcast receiver
     * 
     * This should be called when the activity/fragment is destroyed
     * to prevent memory leaks and unnecessary processing
     */
    fun unregisterSMSReceiver() {
        try {
            smsReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
                smsReceiver = null
                Log.d(TAG, "SMS Broadcast Receiver unregistered successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering SMS receiver", e)
        }
    }

    /**
     * Generates the app hash required for SMS Retriever API
     * 
     * This hash must be included in the SMS message for the API to work.
     * The hash is generated based on your app's package name and signing certificate.
     * 
     * Format of SMS message should be:
     * "Your verification code is: 1234 [APP_HASH]"
     * 
     * @return The 11-character hash string for your app
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getAppHash(): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                0
            )
            val packageName = appInfo.packageName
            
            // Get all signatures for the app
            val signatures = context.packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            ).signatures
            
            // Generate hash for each signature
            signatures?.firstOrNull()?.let { signature ->
                val hash = generateHash(packageName, signature.toCharsString())
                Log.d(TAG, "Generated app hash: $hash")
                hash
            } ?: run {
                Log.e(TAG, "No signatures found for package")
                ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating app hash", e)
            ""
        }
    }

    /**
     * Generates hash from package name and signature
     * 
     * @param packageName The app's package name
     * @param signature The app's signature
     * @return The generated hash string
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateHash(packageName: String, signature: String): String {
        return try {
            val appInfo = "$packageName $signature"
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray())
            
            val hashSignature = messageDigest.digest()
            val truncatedHash = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
            val base64Hash = Base64.getEncoder().encodeToString(truncatedHash)
            
            // Return first 11 characters
            base64Hash.substring(0, NUM_BASE64_CHAR)
            
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Hash algorithm not available", e)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hash", e)
            ""
        }
    }

    /**
     * Extracts OTP from SMS message content
     * 
     * This method looks for 4-6 digit numbers in the SMS content
     * and returns the first match found.
     * 
     * @param smsBody The SMS message content
     * @return The extracted OTP or null if not found
     */
    fun extractOTPFromSMS(smsBody: String): String? {
        return try {
            // Regex pattern to match 4-6 digit OTP codes
            val otpPattern = Regex("\\b\\d{4,6}\\b")
            val matchResult = otpPattern.find(smsBody)
            
            matchResult?.value?.also { otp ->
                Log.d(TAG, "OTP extracted from SMS: $otp")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting OTP from SMS", e)
            null
        }
    }
}
