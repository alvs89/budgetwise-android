package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.CurrencyDropdown

@Composable
fun OnboardingScreen(
  initialName: String,
  initialCurrency: String,
  onComplete: (displayName: String, currencyCode: String) -> Unit,
  onSkip: () -> Unit,
) {
  var displayName by remember { mutableStateOf(if (initialName == "Alex") "" else initialName) }
  var selectedCurrency by remember { mutableStateOf(initialCurrency) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 600.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(R.string.welcome_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("onboarding_tagline"),
          )
        }
        TextButton(
          onClick = onSkip,
          modifier = Modifier.testTag("onboarding_skip_button"),
        ) {
          Text("Skip")
        }
      }

      Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_onboarding_hero),
          contentDescription = "BudgetWise personal finance illustration",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Build better financial habits.",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Track expenses, plan monthly budgets, and grow savings offline on your device — no bank logins required.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OnboardingFeatureRow(
          icon = Icons.Rounded.AccountBalanceWallet,
          title = "Track your money",
          description = "Log daily income and expenses in seconds with smart category breakdowns.",
        )
        OnboardingFeatureRow(
          icon = Icons.Rounded.PieChart,
          title = "Manage your budgets",
          description = "Set monthly category limits and spot overspending before it happens.",
        )
        OnboardingFeatureRow(
          icon = Icons.Rounded.Savings,
          title = "Reach your savings goals",
          description = "Monitor dedicated milestones like your emergency fund or new laptop.",
        )
      }

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Personalize your workspace", style = MaterialTheme.typography.titleMedium)
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            placeholder = { Text("e.g., Alvin") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_name_input"),
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Preferred Currency", style = MaterialTheme.typography.labelLarge)
            CurrencyDropdown(
              selectedCurrencyCode = selectedCurrency,
              onSelectCurrency = { selectedCurrency = it },
            )
          }
        }
      }

      Button(
        onClick = { onComplete(displayName.ifBlank { "Alex" }, selectedCurrency) },
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("onboarding_get_started_button"),
      ) {
        Text("Get Started", style = MaterialTheme.typography.titleSmall)
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun OnboardingFeatureRow(
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
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(20.dp),
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
