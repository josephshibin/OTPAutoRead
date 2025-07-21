package com.example.otpautoread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.otpautoread.ui.theme.OTPAutoReadTheme
import com.example.otpautoread.viewmodel.OTPViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OTPAutoReadTheme {
                SimpleOTPScreen()
            }
        }
    }
}

/**
 * Simple OTP Screen with automatic SMS reading
 *
 * Features:
 * - 4 dash display for OTP digits
 * - Automatic SMS detection and OTP extraction
 * - No manual input - purely automatic
 * - Simple loading and success states
 */
@Composable
fun SimpleOTPScreen(
    viewModel: OTPViewModel = viewModel()
) {
    // Observe ViewModel state
    val otpValue by viewModel.otpValue.observeAsState("")
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isOtpVerified by viewModel.isOtpVerified.observeAsState(false)
    val appHash by viewModel.appHash.observeAsState("")

    // Start SMS retriever when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.startSMSRetriever()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Title
        Text(
            text = "OTP Auto Read",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Waiting for SMS...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Simple 4-dash OTP display
        SimpleDashOTPDisplay(
            otp = otpValue,
            isLoading = isLoading,
            isVerified = isOtpVerified
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Status messages
        when {
            isOtpVerified -> {
                Text(
                    text = "✅ OTP Verified Successfully!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            isLoading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Listening for SMS...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            errorMessage != null -> {
                Text(
                    text = "❌ $errorMessage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // App Hash for testing
        if (appHash.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "App Hash (for testing):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appHash,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Send SMS: 'Your OTP is 1234 $appHash'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Simple 4-dash OTP display component
 *
 * Shows 4 boxes with dashes that get filled automatically when OTP is received
 * No manual input - purely for display
 */
@Composable
fun SimpleDashOTPDisplay(
    otp: String,
    isLoading: Boolean,
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    // Pad OTP to 4 characters and convert to list
    val otpDigits = otp.padEnd(4, ' ').take(4).toList()

    Row(
        modifier = modifier,
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
 * Individual OTP dash box
 *
 * Shows either:
 * - A dash (—) when empty
 * - The digit when filled
 * - Loading indicator when waiting for next digit
 */
@Composable
fun OTPDashBox(
    digit: String?,
    isLoading: Boolean,
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            )
            .background(
                color = when {
                    isVerified -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    digit != null -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            digit != null -> {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isVerified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
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

@Preview(showBackground = true)
@Composable
fun SimpleOTPScreenPreview() {
    OTPAutoReadTheme {
        Surface {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "OTP Auto Read",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                SimpleDashOTPDisplay(
                    otp = "12",
                    isLoading = true,
                    isVerified = false
                )
            }
        }
    }
}