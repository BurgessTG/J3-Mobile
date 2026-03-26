package io.j3mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.j3mobile.domain.BridgeSettings

@Composable
fun SettingsScreen(
    currentSettings: BridgeSettings?,
    onSave: (String, String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit = {},
) {
    var baseUrl by remember(currentSettings?.baseUrl) {
        mutableStateOf(TextFieldValue(currentSettings?.baseUrl ?: "http://127.0.0.1:8181"))
    }
    var jwt by remember(currentSettings?.jwt) {
        mutableStateOf(TextFieldValue(currentSettings?.jwt.orEmpty()))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentSettings != null) {
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
            if (currentSettings != null) {
                OutlinedButton(onClick = onClear) { Text("Disconnect") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Settings")
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Bridge URL") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = jwt,
            onValueChange = { jwt = it },
            label = { Text("Dev JWT") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = baseUrl.text.isNotBlank() && jwt.text.isNotBlank(),
            onClick = {
                onSave(baseUrl.text, jwt.text)
            },
        ) {
            Text("Save and Connect")
        }
    }
}
