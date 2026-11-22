package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Branded BudgetWise welcome & sign-in screen supporting 1-tap Google Sign-In as well as a
 * zero-friction offline on-device mode.
 */
@Composable
fun SignInScreen(
  isSigningIn: Boolean,
  errorMessage: String?,
  onSignInWithGoogle: () -> Unit,
  onContinueOffline: (() -> Unit)? = null,
  canNavigateBack: Boolean = false,
  onBack: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (canNavigateBack) {
    BackHandler(enabled = !isSigningIn, onBack = onBack)
  }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (canNavigateBack) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = onBack,
              enabled = !isSigningIn,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
              modifier = Modifier.testTag("sign_in_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.welcome_back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        } else {
          Spacer(modifier = Modifier.height(14.dp))
        }

        // Branded dark-emerald mark
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                )
              )
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.AccountBalanceWallet,
            contentDescription = stringResource(R.string.app_name),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
          ),
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(
            if (canNavigateBack) R.string.sign_in_upgrade_headline else R.string.sign_in_headline
          ),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(
            if (canNavigateBack) R.string.sign_in_upgrade_subtitle else R.string.sign_in_subtitle
          ),
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Value highlights card
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            SignInFeatureRow(
              icon = Icons.Rounded.CloudDone,
              title = stringResource(R.string.sign_in_feature_sync_title),
              description = stringResource(R.string.sign_in_feature_sync_desc),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            SignInFeatureRow(
              icon = Icons.Rounded.WifiOff,
              title = stringResource(R.string.sign_in_feature_offline_title),
              description = stringResource(R.string.sign_in_feature_offline_desc),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            SignInFeatureRow(
              icon = Icons.Rounded.Lock,
              title = stringResource(R.string.sign_in_feature_private_title),
              description = stringResource(R.string.sign_in_feature_private_desc),
            )
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!errorMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sign_in_error_banner"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        Button(
          onClick = onSignInWithGoogle,
          enabled = !isSigningIn,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("sign_in_google_button"),
        ) {
          if (isSigningIn) {
            CircularProgressIndicator(
              strokeWidth = 2.5.dp,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.sign_in_loading),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Login,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.sign_in_cta_google),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
          }
        }

        if (onContinueOffline != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            HorizontalDivider(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
            Text(
              text = stringResource(R.string.sign_in_or_divider),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
          }

          OutlinedButton(
            onClick = onContinueOffline,
            enabled = !isSigningIn,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
              .testTag("sign_in_offline_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Smartphone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.sign_in_continue_offline),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Text(
            text = stringResource(R.string.sign_in_offline_caption),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )
        } else {
          Text(
            text = stringResource(R.string.sign_in_footer_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun SignInFeatureRow(
  icon: ImageVector,
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(19.dp),
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
