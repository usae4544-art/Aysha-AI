package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.LicenseInfo
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.components.AyshaInfinityLogo

@Composable
fun LicenseGateScreen(
    license: LicenseInfo,
    isChecking: Boolean,
    onBrowserGoogleSignIn: () -> Unit,
    onOpenPlansOrCouponWebsite: () -> Unit,
    onRefreshLicense: () -> Unit,
    onClaimLicenseKey: (String) -> Unit,
    onVerifyManualToken: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val c = LocalAyshaColors.current
    var showTokenFallback by remember { mutableStateOf(false) }
    var manualToken by remember { mutableStateOf("") }
    var licenseKeyInput by remember { mutableStateOf("") }

    LiquidAuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AyshaInfinityLogo(size = 86.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "AYSHA AI",
                color = c.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Google Account & License Required",
                color = c.accentPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(18.dp))

            val statusDot = when (license.status) {
                LicenseStatus.ACTIVE, LicenseStatus.ADMIN_ACTIVE -> c.success
                LicenseStatus.EXPIRED, LicenseStatus.REVOKED -> c.error
                LicenseStatus.UNCONFIGURED -> c.warning
                else -> c.accentPrimary
            }
            val statusLabel = when (license.status) {
                LicenseStatus.SIGNED_OUT -> "Signed Out"
                LicenseStatus.CHECKING -> "Verifying License..."
                LicenseStatus.ACTIVE -> "License Active"
                LicenseStatus.ADMIN_ACTIVE -> "Admin Active"
                LicenseStatus.EXPIRED -> "License Required / Expired"
                LicenseStatus.REVOKED -> "License Revoked"
                LicenseStatus.UNCONFIGURED -> "Firebase Setup Pending"
                LicenseStatus.NETWORK_ERROR -> "Verification Offline"
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevated = true,
                contentPadding = PaddingValues(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassPill(text = statusLabel, dotColor = statusDot)
                        if (isChecking) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = c.accentPrimary,
                                modifier = Modifier.height(18.dp).width(18.dp),
                            )
                        }
                    }

                    if (license.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = c.accentPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = license.displayName.ifBlank { "Google Account" },
                                    color = c.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = license.email,
                                    color = c.textSecondary,
                                    fontSize = 12.5.sp,
                                )
                            }
                        }
                    }

                    val bodyMessage = license.message.ifBlank {
                        if (license.email.isBlank()) {
                            "Sign in with your Google account in your browser. AYSHA automatically unlocks when your account holds an active 1-Month, 2-Month, 1-Year, 2-Year, Lifetime, or Coupon license."
                        } else {
                            "Your Google account is signed in, but does not have an active AYSHA license yet. Activate a plan or redeem a coupon code on the website, then tap Refresh."
                        }
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        val icon = if (license.isUnlocked()) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline
                        Icon(icon, contentDescription = null, tint = statusDot)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bodyMessage,
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }

                    AuroraGradientButton(
                        text = if (license.email.isBlank()) "Sign in with Google (Browser)" else "Switch Google Account (Browser)",
                        onClick = onBrowserGoogleSignIn,
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    )

                    if (license.email.isNotBlank()) {
                        OutlinedTextField(
                            value = licenseKeyInput,
                            onValueChange = { licenseKeyInput = it.uppercase() },
                            label = { Text("Enter Unique License Key (optional)") },
                            placeholder = { Text("AYSHA-XXXX-XXXX-XXXX-...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onClaimLicenseKey(licenseKeyInput.trim()) },
                            enabled = licenseKeyInput.trim().length >= 12 && !isChecking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with License Key")
                        }

                        OutlinedButton(
                            onClick = onOpenPlansOrCouponWebsite,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buy Plan or Redeem Coupon on Site")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onRefreshLicense,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh")
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenPlansOrCouponWebsite,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open AYSHA License & Coupon Site")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = { showTokenFallback = !showTokenFallback }) {
                Text(
                    text = if (showTokenFallback) "Hide Manual Token Box" else "Browser didn't auto-return? Paste token",
                    color = c.textSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                )
            }

            AnimatedVisibility(visible = showTokenFallback) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = manualToken,
                            onValueChange = { manualToken = it },
                            label = { Text("Verification Token from Browser") },
                            placeholder = { Text("Paste refresh token shown in browser...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                if (manualToken.isNotBlank()) {
                                    onVerifyManualToken(manualToken.trim())
                                }
                            },
                            enabled = manualToken.isNotBlank() && !isChecking,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Verify Token & Unlock")
                        }
                    }
                }
            }
        }
    }
}
