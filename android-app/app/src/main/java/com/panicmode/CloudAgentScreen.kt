package com.panicmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panicmode.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import compose.icons.TablerIcons
import compose.icons.tablericons.*

/*
 * UI entry point for configuring and executing cloud-based Mobilerun tasks.
 * Handles credential locking, prompt parsing, and remote execution while
 * keeping all side effects auditable via AgentLog.
 */
@Composable
fun CloudAgentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var credentialsLocked by remember {
        mutableStateOf(MobilerunPrefs.isConfigured(context))
    }

    var api by remember {
        mutableStateOf(TextFieldValue(text = MobilerunPrefs.getApi(context)))
    }

    var device by remember {
        mutableStateOf(TextFieldValue(text = MobilerunPrefs.getDevice(context)))
    }

    var prompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Idle") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CLOUD AGENT",
                    color = TacticalTextHigh,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Remote Mobilerun AI Execution",
                    color = TacticalTextMed,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TacticalCard(
            title = if (credentialsLocked) "CREDENTIALS SAVED" else "CREDENTIALS"
        ) {
            if (credentialsLocked) {
                Button(
                    onClick = {
                        // Explicit user action required to clear stored cloud credentials
                        MobilerunPrefs.clear(context)
                        api = TextFieldValue("")
                        device = TextFieldValue("")
                        credentialsLocked = false
                        AgentLog.log(
                            context,
                            AgentLog.Type.MOBILERUN,
                            "Credentials deleted"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalDanger),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(TablerIcons.Trash, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "DELETE CREDENTIALS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = api,
                        onValueChange = { api = it },
                        label = { Text("Mobilerun API Key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                // Prevent cursor persistence when focus is lost
                                if (!it.isFocused) {
                                    api = api.copy(selection = TextRange(0, 0))
                                }
                            },
                        singleLine = true,
                        shape = RoundedCornerShape(2.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = device,
                        onValueChange = { device = it },
                        label = { Text("Device ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (!it.isFocused) {
                                    device = device.copy(selection = TextRange(0, 0))
                                }
                            },
                        singleLine = true,
                        shape = RoundedCornerShape(2.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            // Credentials are persisted once and treated as immutable until reset
                            MobilerunPrefs.save(context, api.text, device.text)
                            credentialsLocked = true
                            AgentLog.log(
                                context,
                                AgentLog.Type.MOBILERUN,
                                "Credentials saved & locked"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = api.text.isNotBlank() && device.text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAccent),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(TablerIcons.Lock, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SAVE & LOCK CREDENTIALS",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Task Prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            enabled = credentialsLocked
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                status = "Parsing command..."

                val parsed = CommandParser.parse(prompt)
                val taskPrompt = MobilerunTaskBuilder.build(parsed)

                AgentLog.log(
                    context,
                    AgentLog.Type.MOBILERUN,
                    "Command parsed → ${parsed.intent}"
                )

                status = "Sending to Mobilerun..."

                // Network call isolated to IO dispatcher; UI updates marshalled back to Main
                scope.launch(Dispatchers.IO) {
                    MobilerunClient.createTask(
                        api.text,
                        device.text,
                        taskPrompt
                    ) { result ->

                        AgentLog.log(
                            context,
                            AgentLog.Type.MOBILERUN,
                            result
                        )

                        scope.launch(Dispatchers.Main) {
                            status = "Task running on device"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = credentialsLocked && prompt.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TacticalAccent,
                contentColor = Color.Black,
                disabledContainerColor = TacticalSurface,
                disabledContentColor = TacticalTextMed
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("SETUP ON MY DEVICE", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Text("Status: $status", color = TacticalTextMed)
    }
}
