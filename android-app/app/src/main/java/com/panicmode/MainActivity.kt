package com.panicmode

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.work.WorkManager

private val Bg = Color(0xFF121212)
private val Accent = Color(0xFF88BDF2)
private val TextMain = Color(0xFFBDDDFC)
private val TextMuted = Color(0xFF9FB3C8)
private val Danger = Color(0xFFB04A4A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = "#121212".toColorInt()
        window.navigationBarColor = "#121212".toColorInt()

        setContent {
            AgentDashboard {
                requestRuntimePermissions()
                requestWriteSettingsPermission()
            }
        }
    }

    @Suppress("InlinedApi")
    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            100
        )
    }

    private fun requestWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        }
    }
}

@Composable
fun AgentDashboard(onSetupPermissions: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // LOAD PERSISTED STATE
    var contact by remember {
        mutableStateOf(PanicPreferences.getContact(context))
    }

    var trigger by remember {
        mutableStateOf(PanicPreferences.getTrigger(context))
    }

    var batteryMah by remember {
        mutableStateOf(
            PanicPreferences.getCapacity(context)
                .takeIf { it > 0 }
                ?.toString()
                ?: ""
        )
    }

    var selectedIntent by remember {
        mutableStateOf(PanicPreferences.getUserIntent(context))
    }

    val agentArmed = contact.isNotEmpty() && trigger.isNotEmpty()

    val intents = listOf("NORMAL", "TRAVELING", "CROWDED", "AGGRESSIVE", "SAVE_BATTERY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {

        // Header
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Survival Agent",
                color = TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "A self-surviving phone agent for lost, stolen, or high-risk situations.",
            color = TextMuted,
            fontSize = 12.sp
        )

        Text(
            "Operates autonomously, even when offline, using SMS and system services.",
            color = TextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            if (agentArmed) "Agent status: armed & active"
            else "Agent status: configuring",
            color = if (agentArmed) Accent else TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.testTag("agent_state")
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Trusted person (receives alerts)") },
            placeholder = { Text("Phone number", color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("input_contact"),
            colors = inputColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = trigger,
            onValueChange = { trigger = it },
            label = { Text("Activation code (SMS)") },
            placeholder = { Text("e.g. PANIC-9271", color = TextMuted) },
            modifier = Modifier.fillMaxWidth().testTag("input_trigger"),
            colors = inputColors()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = batteryMah,
            onValueChange = { batteryMah = it },
            label = { Text("Device battery capacity (mAh)") },
            placeholder = { Text("Battery capacity", color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("input_mah"),
            colors = inputColors()
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Protection mode",
                color = Accent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        intents.forEach { intent ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = intent == selectedIntent,
                    onClick = { selectedIntent = intent },
                    modifier = Modifier.testTag("intent_$intent"),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Accent,
                        unselectedColor = TextMuted
                    )
                )
                Text(
                    text = when (intent) {
                        "TRAVELING" -> "Traveling (survival · 60 min)"
                        "CROWDED" -> "Crowded place (visible · 15 min)"
                        "AGGRESSIVE" -> "Aggressive tracking (max · 10 min)"
                        "SAVE_BATTERY" -> "Save battery (survival · 60 min)"
                        else -> "Normal / adaptive (15–60 min)"
                    },
                    color = TextMain,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                PanicPreferences.saveSettings(
                    context,
                    contact,
                    trigger,
                    batteryMah.toIntOrNull() ?: 5000,
                    selectedIntent
                )
                onSetupPermissions()
            },
            modifier = Modifier.fillMaxWidth().testTag("btn_arm"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.Black
            )
        ) {
            Text("ARM AGENT")
        }

        Spacer(modifier = Modifier.height(15.dp))

        Button(
            onClick = {
                Log.w("PanicMode", "AGENT DISARMED BY USER")
                WorkManager.getInstance(context).cancelAllWork()
                context.stopService(Intent(context, PanicService::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Danger,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("DISARM AGENT & STOP PROTECTION")
        }

        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedBorderColor = Accent,
    unfocusedBorderColor = TextMuted,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextMuted
)
