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

/**
 * ViewModel for managing OTP verification state and SMS retrieval
 * 
 * This ViewModel handles:
 * 1. OTP input state management
 * 2. SMS Retriever API integration
 * 3. Loading states and error handling
 * 4. Auto-verification logic
 * 5. Timeout management
 * 
 * The ViewModel follows MVVM architecture pattern and provides
 * a clean separation between UI and business logic.
 */
class OTPViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "OTPViewModel"
        private const val SMS_TIMEOUT_DURATION = 300000L // 5 minutes in milliseconds
    }

    private val smsRetrieverHelper = SMSRetrieverHelper(application)

    // OTP state management
    private val _otpValue = MutableLiveData<String>("")
    val otpValue: LiveData<String> = _otpValue

    // Loading state for SMS retrieval
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state management
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Success state for OTP verification
    private val _isOtpVerified = MutableLiveData<Boolean>(false)
    val isOtpVerified: LiveData<Boolean> = _isOtpVerified



    // App hash for SMS verification
    private val _appHash = MutableLiveData<String>("")
    val appHash: LiveData<String> = _appHash

    init {
        // Generate and store app hash on initialization
        generateAppHash()
    }

    /**
     * Starts the SMS retriever and begins listening for OTP SMS
     */
    fun startSMSRetriever() {
        Log.d(TAG, "=== STARTING SMS RETRIEVER ===")

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
        Log.d(TAG, "=== OTP RECEIVED: $otp ===")

        viewModelScope.launch {
            _otpValue.value = otp
            _isLoading.value = false
            _errorMessage.value = null

            // Simulate verification process
            delay(1000)
            _isOtpVerified.value = true
            Log.d(TAG, "=== OTP VERIFIED SUCCESSFULLY ===")
        }
    }

    /**
     * Handles errors from SMS Retriever
     */
    private fun handleError(error: String) {
        Log.e(TAG, "=== SMS ERROR: $error ===")
        _isLoading.value = false
        _errorMessage.value = error
    }



    /**
     * Updates OTP value manually (when user types)
     * 
     * @param otp The new OTP value
     */
    fun updateOTP(otp: String) {
        if (otp.length <= 4 && otp.all { it.isDigit() }) {
            _otpValue.value = otp
            clearError()
            
            // Auto-verify when 4 digits are entered
            if (otp.length == 4) {
                verifyOTP(otp)
            }
        }
    }

    /**
     * Verifies the entered OTP
     * 
     * In a real app, this would make an API call to verify the OTP
     * For this demo, we'll simulate verification
     * 
     * @param otp The OTP to verify
     */
    fun verifyOTP(otp: String) {
        Log.d(TAG, "Verifying OTP: $otp")
        
        if (!isValidOTP(otp)) {
            _errorMessage.value = "Please enter a valid 4-digit OTP"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                clearError()

                // Simulate API call delay
                delay(1000)

                // In a real app, you would make an API call here
                // For demo purposes, we'll accept any 4-digit OTP
                _isOtpVerified.value = true
                _isLoading.value = false
                
                Log.d(TAG, "OTP verified successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error verifying OTP", e)
                _isLoading.value = false
                _errorMessage.value = "Verification failed. Please try again."
            }
        }
    }

    /**
     * Resends OTP by restarting SMS retriever
     */
    fun resendOTP() {
        Log.d(TAG, "Resending OTP")
        
        // Clear current state
        _otpValue.value = ""
        _isOtpVerified.value = false
        clearError()
        
        // Restart SMS retriever
        startSMSRetriever()
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Validates OTP format
     * 
     * @param otp The OTP to validate
     * @return true if valid, false otherwise
     */
    private fun isValidOTP(otp: String): Boolean {
        return otp.matches(Regex("\\d{4}")) && otp.isNotEmpty()
    }

    /**
     * Generates app hash for SMS verification
     */
    private fun generateAppHash() {
        viewModelScope.launch {
            try {
                val hash = smsRetrieverHelper.getAppHash()
                _appHash.value = hash
                Log.d(TAG, "App hash generated: $hash")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating app hash", e)
            }
        }
    }

    /**
     * Stops SMS retriever and cleans up resources
     */
    fun stopSMSRetriever() {
        Log.d(TAG, "Stopping SMS Retriever")
        
        try {
            smsRetrieverHelper.unregisterSMSReceiver()
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SMS Retriever", e)
        }
    }

    /**
     * Resets all state to initial values
     */
    fun resetState() {
        Log.d(TAG, "Resetting OTP state")
        
        _otpValue.value = ""
        _isLoading.value = false
        _errorMessage.value = null
        _isOtpVerified.value = false
        
        stopSMSRetriever()
    }

    /**
     * Called when ViewModel is cleared
     * Cleans up SMS retriever resources
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, cleaning up resources")
        stopSMSRetriever()
    }
}
