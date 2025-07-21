package com.example.otpautoread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Individual OTP input field component
 * 
 * This composable creates a single input field for one OTP digit with:
 * - Automatic focus management
 * - Visual feedback for focus state
 * - Keyboard navigation support
 * - Auto-advance to next field when digit is entered
 * 
 * @param value Current value of this field
 * @param onValueChange Callback when value changes
 * @param onNext Callback to move focus to next field
 * @param onPrevious Callback to move focus to previous field
 * @param focusRequester FocusRequester for managing focus
 * @param isLast Whether this is the last field in the sequence
 */
@Composable
fun OTPInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    focusRequester: FocusRequester,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = when {
                    isFocused -> MaterialTheme.colorScheme.primary
                    value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                // Only allow single digit
                if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                    onValueChange(newValue)
                    
                    // Auto-advance to next field when digit is entered
                    if (newValue.isNotEmpty() && !isLast) {
                        onNext()
                    }
                }
                // Handle backspace - move to previous field
                else if (newValue.isEmpty() && value.isNotEmpty()) {
                    onValueChange("")
                    onPrevious()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (isLast) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() },
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty() && !isFocused) {
                        Text(
                            text = "—",
                            style = TextStyle(
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Complete OTP input component with 4 fields
 * 
 * This composable creates a row of 4 OTP input fields with:
 * - Automatic focus management between fields
 * - Auto-population from external source (SMS)
 * - Visual feedback and animations
 * - Keyboard navigation support
 * 
 * @param otp Current OTP value (4 digits)
 * @param onOtpChange Callback when OTP changes
 * @param onOtpComplete Callback when all 4 digits are entered
 * @param isLoading Whether the component is in loading state
 * @param error Error message to display
 */
@Composable
fun OTPInputComponent(
    otp: String,
    onOtpChange: (String) -> Unit,
    onOtpComplete: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    // Create focus requesters for each field
    val focusRequesters = remember { List(4) { FocusRequester() } }
    
    // Split OTP into individual digits
    val otpDigits = remember(otp) {
        otp.padEnd(4, ' ').take(4).toList().map { if (it == ' ') "" else it.toString() }
    }

    // Auto-complete callback when all digits are filled
    LaunchedEffect(otp) {
        if (otp.length == 4) {
            onOtpComplete(otp)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subtitle
        Text(
            text = "We've sent a 4-digit code to your phone",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // OTP Input Fields
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            repeat(4) { index ->
                OTPInputField(
                    value = otpDigits[index],
                    onValueChange = { newValue ->
                        val newOtp = otpDigits.toMutableList()
                        newOtp[index] = newValue
                        val updatedOtp = newOtp.joinToString("").replace(" ", "")
                        onOtpChange(updatedOtp)
                    },
                    onNext = {
                        if (index < 3) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    },
                    onPrevious = {
                        if (index > 0) {
                            focusRequesters[index - 1].requestFocus()
                        }
                    },
                    focusRequester = focusRequesters[index],
                    isLast = index == 3
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Waiting for SMS...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Error message
        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Auto-focus first field when component is created
        LaunchedEffect(Unit) {
            focusRequesters[0].requestFocus()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OTPInputComponentPreview() {
    MaterialTheme {
        OTPInputComponent(
            otp = "12",
            onOtpChange = {},
            onOtpComplete = {},
            isLoading = false,
            error = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OTPInputComponentLoadingPreview() {
    MaterialTheme {
        OTPInputComponent(
            otp = "",
            onOtpChange = {},
            onOtpComplete = {},
            isLoading = true,
            error = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OTPInputComponentErrorPreview() {
    MaterialTheme {
        OTPInputComponent(
            otp = "123",
            onOtpChange = {},
            onOtpComplete = {},
            isLoading = false,
            error = "Invalid verification code. Please try again."
        )
    }
}
