package com.example.otpautoread.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * BroadcastReceiver for handling SMS messages through SMS Retriever API
 * 
 * This receiver:
 * 1. Listens for SMS_RETRIEVED_ACTION broadcasts from Google Play Services
 * 2. Extracts SMS content from the intent
 * 3. Parses OTP from the SMS message
 * 4. Notifies the app through callback functions
 * 
 * The SMS Retriever API only works with SMS messages that:
 * - Contain a 4-11 character alphanumeric string with at least one number
 * - Include your app's hash string
 * - Are sent to the device within 5 minutes of calling startSmsRetriever()
 */
class SMSBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSBroadcastReceiver"
    }

    // Callback functions for handling SMS reception and errors
    private var onSmsReceivedListener: ((String) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null

    /**
     * Sets the callback function to be called when OTP is successfully extracted
     * 
     * @param listener Function that receives the extracted OTP string
     */
    fun setOnSmsReceivedListener(listener: (String) -> Unit) {
        this.onSmsReceivedListener = listener
    }

    /**
     * Sets the callback function to be called when an error occurs
     * 
     * @param listener Function that receives error message string
     */
    fun setOnErrorListener(listener: (String) -> Unit) {
        this.onErrorListener = listener
    }

    /**
     * Called when a broadcast is received
     * 
     * This method handles the SMS_RETRIEVED_ACTION broadcast sent by Google Play Services
     * when an SMS message matching the criteria is received.
     * 
     * @param context The context in which the receiver is running
     * @param intent The intent containing the SMS data
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "=== BROADCAST RECEIVED ===")
        Log.d(TAG, "Context: $context")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Action: ${intent?.action}")
        Log.d(TAG, "Expected: ${SmsRetriever.SMS_RETRIEVED_ACTION}")
        Log.d(TAG, "Actions match: ${intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION}")

        // Log all intent extras for debugging
        intent?.extras?.let { extras ->
            Log.d(TAG, "Intent extras:")
            for (key in extras.keySet()) {
                Log.d(TAG, "  $key: ${extras.get(key)}")
            }
        }

        // Verify this is the SMS retriever action
        if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) {
            Log.w(TAG, "=== UNEXPECTED ACTION RECEIVED: ${intent?.action} ===")
            return
        }

        Log.d(TAG, "=== SMS RETRIEVER ACTION CONFIRMED ===")

        try {
            // Extract the SMS retriever result from the intent
            val extras: Bundle? = intent.extras
            val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            Log.d(TAG, "Status code: ${smsRetrieverStatus?.statusCode}")

            when (smsRetrieverStatus?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    Log.d(TAG, "=== SMS RETRIEVAL SUCCESS ===")
                    handleSuccessfulSMSRetrieval(extras)
                }

                CommonStatusCodes.TIMEOUT -> {
                    Log.w(TAG, "=== SMS RETRIEVER TIMED OUT ===")
                    onErrorListener?.invoke("SMS verification timed out. Please try again.")
                }

                else -> {
                    val errorMessage = "SMS Retriever failed with status: ${smsRetrieverStatus?.statusCode}"
                    Log.e(TAG, "=== SMS RETRIEVER ERROR: $errorMessage ===")
                    onErrorListener?.invoke(errorMessage)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS retriever broadcast", e)
            onErrorListener?.invoke("Error processing SMS: ${e.message}")
        }
    }

    /**
     * Handles successful SMS retrieval and extracts OTP
     * 
     * @param extras The bundle containing SMS data
     */
    private fun handleSuccessfulSMSRetrieval(extras: Bundle) {
        try {
            // Get the SMS message from the intent
            val smsMessage = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)

            Log.d(TAG, "=== SMS MESSAGE CONTENT ===")
            Log.d(TAG, "Raw SMS: '$smsMessage'")

            if (smsMessage.isNullOrEmpty()) {
                Log.e(TAG, "=== SMS MESSAGE IS NULL OR EMPTY ===")
                onErrorListener?.invoke("Received empty SMS message")
                return
            }

            // Extract OTP from the SMS message
            val extractedOTP = extractOTPFromMessage(smsMessage)

            if (extractedOTP != null) {
                Log.d(TAG, "=== OTP SUCCESSFULLY EXTRACTED: $extractedOTP ===")
                onSmsReceivedListener?.invoke(extractedOTP)
            } else {
                Log.w(TAG, "=== COULD NOT EXTRACT OTP FROM SMS ===")
                Log.w(TAG, "SMS content: '$smsMessage'")
                onErrorListener?.invoke("Could not find OTP in the received message")
            }

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR HANDLING SMS RETRIEVAL ===", e)
            onErrorListener?.invoke("Error extracting OTP: ${e.message}")
        }
    }

    /**
     * Extracts OTP code from SMS message content
     * Enhanced version with better pattern matching and debugging
     */
    private fun extractOTPFromMessage(message: String): String? {
        try {
            Log.d(TAG, "=== EXTRACTING OTP FROM MESSAGE ===")
            Log.d(TAG, "Message: '$message'")
            Log.d(TAG, "Message length: ${message.length}")

            // Clean the message - remove extra spaces and normalize
            val cleanMessage = message.trim().replace("\\s+".toRegex(), " ")
            Log.d(TAG, "Cleaned message: '$cleanMessage'")

            // List of regex patterns to match different OTP formats
            val otpPatterns = listOf(
                // Simple 4 digit numbers anywhere in the message
                Regex("\\b(\\d{4})\\b"),

                // 4 digits with common keywords
                Regex("(?i)(?:otp|code|pin|verification|verify)\\s*:?\\s*(\\d{4})"),

                // "Your verification code: 1234"
                Regex("(?i)verification\\s+code\\s*:?\\s*(\\d{4})"),

                // "Your OTP is 1234"
                Regex("(?i)otp\\s+is\\s*:?\\s*(\\d{4})"),

                // "Code: 1234" or "Code 1234"
                Regex("(?i)code\\s*:?\\s*(\\d{4})"),

                // Just find any 4 consecutive digits
                Regex("(\\d{4})"),

                // 4 digits in brackets or parentheses
                Regex("\\[(\\d{4})\\]"),
                Regex("\\((\\d{4})\\)")
            )

            // Try each pattern and log the attempts
            for ((index, pattern) in otpPatterns.withIndex()) {
                Log.d(TAG, "Trying pattern $index: ${pattern.pattern}")

                val matchResult = pattern.find(cleanMessage)
                if (matchResult != null) {
                    Log.d(TAG, "Pattern $index matched!")
                    Log.d(TAG, "Full match: '${matchResult.value}'")
                    Log.d(TAG, "Groups: ${matchResult.groupValues}")

                    // Get the OTP - prefer captured group if available
                    val otp = if (matchResult.groupValues.size > 1 && matchResult.groupValues[1].isNotEmpty()) {
                        matchResult.groupValues[1]
                    } else {
                        matchResult.value
                    }

                    Log.d(TAG, "Extracted OTP candidate: '$otp'")

                    // Validate that the extracted OTP is exactly 4 digits
                    if (otp.matches(Regex("\\d{4}"))) {
                        Log.d(TAG, "=== OTP SUCCESSFULLY EXTRACTED: '$otp' ===")
                        return otp
                    } else {
                        Log.w(TAG, "OTP candidate '$otp' is not 4 digits")
                    }
                } else {
                    Log.d(TAG, "Pattern $index did not match")
                }
            }

            Log.w(TAG, "=== NO OTP PATTERN MATCHED ===")
            Log.w(TAG, "Original message: '$message'")
            Log.w(TAG, "Cleaned message: '$cleanMessage'")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR EXTRACTING OTP ===", e)
            return null
        }
    }

    /**
     * Validates if the extracted string is a valid OTP
     * 
     * @param otp The string to validate
     * @return true if valid OTP, false otherwise
     */
    private fun isValidOTP(otp: String): Boolean {
        return otp.matches(Regex("\\d{4,6}")) && otp.isNotEmpty()
    }

    /**
     * Cleans up the receiver by clearing callback listeners
     * 
     * This should be called when the receiver is no longer needed
     * to prevent memory leaks
     */
    fun cleanup() {
        onSmsReceivedListener = null
        onErrorListener = null
        Log.d(TAG, "SMS Broadcast Receiver cleaned up")
    }
}
