package com.panicmode

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.panicmode.ui.theme.PanicmodeTheme
import com.panicmode.ui.theme.TacticalAccent
import com.panicmode.ui.theme.TacticalBg
import com.panicmode.ui.theme.TacticalDanger
import com.panicmode.ui.theme.TacticalSurface
import com.panicmode.ui.theme.TacticalTextHigh
import com.panicmode.ui.theme.TacticalTextMed
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.*

// Tabler Icons imports
import compose.icons.TablerIcons
import compose.icons.tablericons.*

// Vector graphic imports
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.path

// -----------------------------------------------------------------------------
// SECTION: CUSTOM ICON DEFINITIONS
// -----------------------------------------------------------------------------

/**
 * Manually defined UserShield icon to ensure availability without
 * requiring specific library version updates.
 */
val TablerIcons.UserShield: ImageVector
    get() {
        if (_userShield != null) return _userShield!!
        _userShield = ImageVector.Builder(
            name = "UserShield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 7f)
                arcTo(4f, 4f, 0f, true, false, 16f, 7f)
                arcTo(4f, 4f, 0f, true, false, 8f, 7f)
                close()
            }
            // Body
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                verticalLineToRelative(-2f)
                arcToRelative(4f, 4f, 0f, false, true, 4f, -4f)
                horizontalLineToRelative(2f)
            }
            // Shield
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 16f)
                curveToRelative(0f, 4f, -2.5f, 6f, -3.5f, 6f)
                reflectiveCurveToRelative(-3.5f, -2f, -3.5f, -6f)
                curveToRelative(1f, 0f, 2.5f, -0.5f, 3.5f, -1.5f)
                curveToRelative(1f, 1f, 2.5f, 1.5f, 3.5f, 1.5f)
                close()
            }
        }.build()
        return _userShield!!
    }

private var _userShield: ImageVector? = null

/**
 * Manually defined Logs icon for the Activity tab.
 * Represents a systematic list of events.
 */
val TablerIcons.Logs: ImageVector
    get() {
        if (_logs != null) return _logs!!
        _logs = ImageVector.Builder(
            name = "Logs",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // The dots (h.01)
                moveTo(4f, 12f)
                horizontalLineToRelative(0.01f)
                moveTo(4f, 6f)
                horizontalLineToRelative(0.01f)
                moveTo(4f, 18f)
                horizontalLineToRelative(0.01f)

                // The short lines (h2)
                moveTo(8f, 18f)
                horizontalLineToRelative(2f)
                moveTo(8f, 12f)
                horizontalLineToRelative(2f)
                moveTo(8f, 6f)
                horizontalLineToRelative(2f)

                // The long lines (h6)
                moveTo(14f, 6f)
                horizontalLineToRelative(6f)
                moveTo(14f, 12f)
                horizontalLineToRelative(6f)
                moveTo(14f, 18f)
                horizontalLineToRelative(6f)
            }
        }.build()
        return _logs!!
    }

private var _logs: ImageVector? = null

// -----------------------------------------------------------------------------
// SECTION: NAVIGATION CONFIGURATION
// -----------------------------------------------------------------------------

/**
 * Enum defining the main navigation destinations of the application.
 */
enum class BottomTab(val label: String, val icon: ImageVector) {
    AGENT("Agent", TablerIcons.ShieldCheck),
    DEADMAN("Safety", TablerIcons.UserShield),
    CLOUD("Mobilerun", TablerIcons.Cloud),
    ACTIVITY("Activity", TablerIcons.Logs)
}

// -----------------------------------------------------------------------------
// SECTION: MAIN ACTIVITY ENTRY POINT
// -----------------------------------------------------------------------------

/**
 * Entry point for the PanicMode application.
 * Responsible for bootstrapping permissions (SMS, Location, Alarm)
 * and setting up the Compose UI content.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PanicmodeTheme { PanicApp() } }

        // Bootstrap required system permissions
        requestPermissions()
        requestWriteSettingsPermission()
        requestExactAlarmPermission()
    }

    /**
     * Request runtime permissions for SMS (Agent logic) and Location (Tracking).
     * Includes Android 13+ Notification permissions.
     */
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
    }

    /**
     * Required for modifying system settings if the Panic Agent needs to
     * adjust brightness/volume during an emergency.
     */
    private fun requestWriteSettingsPermission() {
        if (!Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData("package:$packageName".toUri()))
        }
    }

    /**
     * Android 12+ (API 31) requires explicit permission to schedule exact alarms,
     * which are critical for the Deadman safety check timing.
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SECTION: TOP-LEVEL UI COMPOSABLES
// -----------------------------------------------------------------------------

/**
 * Main application scaffold managing the bottom navigation bar
 * and screen content switching.
 */
@Composable
fun PanicApp() {
    var selectedTab by remember { mutableStateOf(BottomTab.AGENT) }

    Scaffold(
        containerColor = TacticalBg,
        bottomBar = {
            NavigationBar(
                containerColor = TacticalSurface,
                tonalElevation = 0.dp
            ) {
                BottomTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TacticalAccent,
                            selectedTextColor = TacticalAccent,
                            indicatorColor = TacticalAccent.copy(alpha = 0.1f),
                            unselectedIconColor = TacticalTextMed,
                            unselectedTextColor = TacticalTextMed
                        )
                    )
                }
            }
        }
    ) { padding ->
        // Container applying full scaffold padding to prevent content from hiding behind navbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                BottomTab.AGENT -> AgentScreen()
                BottomTab.DEADMAN -> DeadmanScreen()
                BottomTab.CLOUD -> CloudAgentScreen()
                BottomTab.ACTIVITY -> ActivityLogScreen()
            }
        }
    }
}

/**
 * A persistent dashboard element that displays the real-time status
 * of all subsystems (Panic, Deadman, etc.).
 * Uses a polling effect to update statuses every second.
 */
@Composable
fun TacticalMeter(
    label: String,
    value: Float, // 0.0 to 1.0
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(100.dp)) {
        // Background track (glow effect)
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.size(80.dp).graphicsLayer { alpha = 0.1f },
            color = color,
            strokeWidth = 4.dp
        )
        // Active progress
        CircularProgressIndicator(
            progress = value,
            modifier = Modifier.size(80.dp).graphicsLayer { rotationZ = -90f },
            color = color,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = TacticalTextMed, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", fontSize = 16.sp, color = TacticalTextHigh, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StatusPanel(context: Context) {
    // State holders for system statuses
    var isArmed by remember { mutableStateOf(PanicPreferences.isAgentArmed(context)) }
    var isPanic by remember { mutableStateOf(PanicPreferences.isPanicActive(context)) }
    var isDms by remember { mutableStateOf(DmsPreferences.isDmsEnabled(context)) }
    var missedChecks by remember { mutableStateOf(DmsPreferences.getMissed(context)) }
    var nextCheck by remember { mutableStateOf(DmsPreferences.getNextCheckAt(context)) }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Live polling loop
    LaunchedEffect(Unit) {
        while(true) {
            isArmed = PanicPreferences.isAgentArmed(context)
            isPanic = PanicPreferences.isPanicActive(context)
            isDms = DmsPreferences.isDmsEnabled(context)
            missedChecks = DmsPreferences.getMissed(context)
            nextCheck = DmsPreferences.getNextCheckAt(context)
            delay(1000)
        }
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        // Unified Status Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TacticalAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Grid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusLine("AGENT", if (isArmed) "ARMED" else "DISARMED", isArmed)
                        StatusLine("PANIC", if (isPanic) "ACTIVE" else "INACTIVE", isPanic, useWarningForValue = isPanic)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusLine("SAFETY", if (isDms) "ENABLED" else "DISABLED", isDms)
                        StatusLine("MISSED", missedChecks.toString(), missedChecks == 0, isMissedValue = true)
                    }
                }

                // Next scheduled check display integrated into the box
                if (isDms && nextCheck > 0) {
                    HorizontalDivider(color = TacticalTextMed.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(TablerIcons.Calendar, null, tint = TacticalTextMed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Next check: ${timeFormat.format(Date(nextCheck))}", fontSize = 11.sp, color = TacticalTextMed)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusLine(label: String, value: String, isGood: Boolean, useWarningForValue: Boolean = false, isMissedValue: Boolean = false) {
    // Logic to determine status color (Green for good, Red for bad/danger, Yellow for warning)
    val indicatorColor = when {
        isMissedValue && value == "0" -> TacticalTextHigh
        isMissedValue && value != "0" -> TacticalDanger
        isGood -> Color(0xFF4CAF50)
        useWarningForValue -> TacticalAccent
        else -> TacticalDanger
    }

    val textColor = when {
        isMissedValue && value == "0" -> TacticalTextHigh
        else -> if (useWarningForValue) TacticalAccent else if (isGood) TacticalAccent else TacticalDanger
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(indicatorColor, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, color = TacticalTextMed, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
    }
}

/**
 * Visualizes the Deadman Switch logic flow:
 * Check Interval -> Safety Confirm Window -> SMS Escalation.
 * Updates progress bars in real-time.
 */
@Composable
fun LogicFlowTimeline(context: Context) {
    var checkProgress by remember { mutableStateOf(0f) }
    var waitProgress by remember { mutableStateOf(0f) }
    var timeLeftText by remember { mutableStateOf("") }
    var activePhase by remember { mutableStateOf(0) } // 0: Off, 1: Check Interval, 2: Timeout Window
    var activeIntervalMin by remember { mutableStateOf(DmsPreferences.getCheckIntervalMinutes(context)) }
    var activeTimeoutSec by remember { mutableStateOf(DmsPreferences.getTimeoutSeconds(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val nextCheck = DmsPreferences.getNextCheckAt(context)
            val nextTimeout = DmsPreferences.getNextTimeoutAt(context)
            val isEnabled = DmsPreferences.isDmsEnabled(context)
            activeIntervalMin = DmsPreferences.getCheckIntervalMinutes(context)
            activeTimeoutSec = DmsPreferences.getTimeoutSeconds(context)

            if (!isEnabled) {
                checkProgress = 0f; waitProgress = 0f; activePhase = 0; timeLeftText = "OFF"
            } else if (nextTimeout > now) {
                // Phase 2: Waiting for user confirmation
                activePhase = 2; checkProgress = 0f
                val total = activeTimeoutSec * 1000
                val remaining = nextTimeout - now
                waitProgress = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                timeLeftText = formatMillis(remaining)
            } else if (nextCheck > now) {
                // Phase 1: Silent interval before next check
                activePhase = 1; waitProgress = 0f
                val total = activeIntervalMin * 60 * 1000
                val remaining = nextCheck - now
                checkProgress = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                timeLeftText = formatMillis(remaining)
            }
            delay(1000)
        }
    }

    // Timeline UI
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TimelineStepText("Check", "${activeIntervalMin}m", checkProgress, activePhase == 1, TacticalAccent, if(activePhase == 1) timeLeftText else null)
        Icon(TablerIcons.ArrowRight, null, tint = TacticalTextMed, modifier = Modifier.size(16.dp))
        TimelineStepText("Safety\nConfirm", "${activeTimeoutSec}s", waitProgress, activePhase == 2, TacticalDanger, if(activePhase == 2) timeLeftText else null)
        Icon(TablerIcons.ArrowRight, null, tint = TacticalTextMed, modifier = Modifier.size(16.dp))
        TimelineStepText("SMS", "Escalate", 0f, false, TacticalTextMed, null)
    }
}

@Composable
fun TimelineStepText(label: String, time: String, progress: Float, isActive: Boolean, color: Color, timeLeft: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = 1f, modifier = Modifier.size(64.dp), color = if (isActive) color.copy(alpha = 0.1f) else TacticalSurface, strokeWidth = 4.dp)
            if (isActive) {
                CircularProgressIndicator(progress = progress, modifier = Modifier.size(64.dp).graphicsLayer { rotationZ = -90f }, color = color, strokeWidth = 4.dp)
            }
            Text(text = if (isActive && timeLeft != null) timeLeft else "$label\n$time", textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = if (isActive) color else TacticalTextMed)
        }
    }
}

private fun formatMillis(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
}

// -----------------------------------------------------------------------------
// SECTION: SCREEN COMPOSABLES
// -----------------------------------------------------------------------------

/**
 * Screen for configuring the Hybrid Safety Agent (Panic Button).
 * Handles Contact info, SMS Triggers, and Protection Modes.
 */
@Composable
fun AgentScreen() {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    // Load existing settings for contact and trigger
    var contact by remember { mutableStateOf(PanicPreferences.getContact(context)) }
    var trigger by remember { mutableStateOf(PanicPreferences.getTrigger(context)) }

    // ⭐ FIX: Check if battery capacity has actually been saved to disk.
    // If not, default to empty string ("") so the user sees a blank field first.
    // If yes, load the saved value so it persists when switching tabs.
    val prefs = context.getSharedPreferences("panic_agent_memory", Context.MODE_PRIVATE)
    val hasSavedBattery = prefs.contains("battery_capacity_mah")
    val savedBattery = PanicPreferences.getCapacity(context)

    var batteryMah by remember {
        mutableStateOf(if (hasSavedBattery) savedBattery.toString() else "")
    }

    var intent by remember { mutableStateOf(PanicPreferences.getUserIntent(context)) }
    var agentArmed by remember { mutableStateOf(PanicPreferences.isAgentArmed(context)) }

    // Validation: Require Contact and Trigger to arm the system
    val isReady = contact.isNotBlank() && trigger.isNotBlank()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HYBRID SAFETY AGENT", color = TacticalTextHigh, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Autonomous protection powered by on-device + cloud intelligence", color = TacticalTextMed, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        StatusPanel(context)

        // --- Configuration Form ---
        TacticalCard(title = "AGENT CONFIGURATION") {
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Trusted Contact") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = trigger,
                onValueChange = { trigger = it },
                label = { Text("Activation SMS Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Battery capacity input
            OutlinedTextField(
                value = batteryMah,
                onValueChange = { batteryMah = it },
                label = { Text("Battery Capacity (mAh)") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp)
            )

            Spacer(Modifier.height(23.dp))
            HorizontalDivider(color = TacticalTextMed.copy(alpha = 0.2f))
            Spacer(Modifier.height(20.dp))

            // --- Protection Modes ---
            Text("PROTECTION MODE", color = TacticalAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeCard("Crowded /\nAggressive", "15 min", intent == "VISIBILITY", { intent = "VISIBILITY" }, Modifier.weight(1f))
                ModeCard("Adaptive", "15–60 min", intent == "NORMAL", { intent = "NORMAL" }, Modifier.weight(1f))
                ModeCard("Travel /\nSave Battery", "60 min", intent == "SURVIVAL", { intent = "SURVIVAL" }, Modifier.weight(1f))
            }
        }

        // --- Action Buttons ---
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                // If user left it empty, default to 5000 internally, but don't force it into the UI yet
                val finalMah = batteryMah.toIntOrNull() ?: 5000

                // Save triggers persistence
                PanicPreferences.saveSettings(context, contact, trigger, finalMah, intent)

                // ⭐ Update UI state to ensure consistency after save
                batteryMah = finalMah.toString()

                PanicPreferences.setAgentArmed(context, true)
                agentArmed = true
            },
            enabled = isReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = if (isReady) 1.dp else 0.dp,
                    color = if (isReady) TacticalAccent.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (agentArmed) TacticalAccent.copy(alpha = 0.2f) else TacticalAccent,
                contentColor = if (agentArmed) TacticalAccent else Color.Black,
                disabledContainerColor = TacticalSurface,
                disabledContentColor = TacticalTextMed
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isReady) 8.dp else 0.dp
            )
        ) {
            Icon(
                if (agentArmed) TablerIcons.ShieldCheck else TablerIcons.ShieldLock,
                null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (agentArmed) "AGENT ACTIVE" else "ACTIVATE AGENT",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                // Full system shutdown and cleanup
                PanicPreferences.setAgentArmed(context, false)
                agentArmed = false
                WorkManager.getInstance(context).cancelAllWork()
                context.stopService(Intent(context, PanicService::class.java))
                DmsManager.hardKillAll(context)
                PanicPreferences.setPanicActive(context, false)
                PanicPreferences.setSuspended(context, false)
                context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
                AgentLog.log(context, AgentLog.Type.AGENT, "Disarmed → all systems stopped")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TacticalDanger)
        ) {
            Text("FORCE STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Screen for configuring the Deadman Switch (Automated Safety Checks).
 * Allows setting Check Interval and Timeout durations.
 */
@Composable
fun DeadmanScreen() {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    // 1. Get Enabled State first
    var dmsEnabled by remember { mutableStateOf(DmsPreferences.isDmsEnabled(context)) }
    val missedCount = DmsPreferences.getMissed(context)

    // 2. Logic: If DMS is enabled, show the active values.
    // If disabled (First Start), show empty strings to force configuration.
    val initialInterval = if (dmsEnabled) DmsPreferences.getCheckIntervalMinutes(context).toString() else ""
    val initialTimeout = if (dmsEnabled) DmsPreferences.getTimeoutSeconds(context).toString() else ""

    // 3. Initialize state with the calculated values
    var checkInterval by remember { mutableStateOf(initialInterval) }
    var timeoutDuration by remember { mutableStateOf(initialTimeout) }

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Spacer(Modifier.height(8.dp))

        // Header with Toggle Switch
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SAFETY CHECK SYSTEM", color = TacticalTextHigh, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Periodic Safety Verification", color = TacticalTextMed, fontSize = 13.sp)
            }
            Switch(
                checked = dmsEnabled,
                onCheckedChange = { enabled ->
                    dmsEnabled = enabled
                    // If enabling, we trigger the manager. If fields are empty, Manager should use defaults.
                    if (enabled) DmsManager.enableDms(context) else DmsManager.disableDms(context)

                    // Optional: If turning ON, populate fields with the defaults if they are currently empty
                    if (enabled && checkInterval.isEmpty()) {
                        checkInterval = DmsPreferences.getCheckIntervalMinutes(context).toString()
                        timeoutDuration = DmsPreferences.getTimeoutSeconds(context).toString()
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = TacticalAccent, checkedTrackColor = TacticalAccent.copy(alpha = 0.3f))
            )
        }

        Spacer(Modifier.height(16.dp))
        LogicFlowTimeline(context)

        // Warning banner
        if (missedCount > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = TacticalDanger), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("⚠️ Missed checks: $missedCount", modifier = Modifier.padding(8.dp).fillMaxWidth(), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }

        // --- Timing Configuration ---
        Spacer(Modifier.height(16.dp))
        TacticalCard(title = "TIMING CONFIGURATION") {
            Text("Check Interval (minutes)", color = TacticalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Time between safety checks", color = TacticalTextMed, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

            // Updated TextField with Placeholder
            OutlinedTextField(
                value = checkInterval,
                onValueChange = { checkInterval = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                placeholder = { Text("e.g. 30", color = TacticalTextMed.copy(alpha = 0.5f)) }
            )

            Spacer(Modifier.height(16.dp))
            Text("Response Timeout (seconds)", color = TacticalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Time to respond before escalation", color = TacticalTextMed, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

            // Updated TextField with Placeholder
            OutlinedTextField(
                value = timeoutDuration,
                onValueChange = { timeoutDuration = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                placeholder = { Text("e.g. 300", color = TacticalTextMed.copy(alpha = 0.5f)) }
            )

            // --- Quick Presets ---
            Spacer(Modifier.height(16.dp))
            Text("QUICK PRESETS", color = TacticalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val buttonColors = ButtonDefaults.buttonColors(
                    containerColor = TacticalSurface,
                    contentColor = TacticalTextMed
                )

                Button(
                    onClick = { checkInterval = "15"; timeoutDuration = "300" },
                    modifier = Modifier.weight(1f),
                    colors = buttonColors
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Frequent", fontSize = 11.sp)
                        Text("15m/5m", fontSize = 9.sp)
                    }
                }

                Button(
                    onClick = { checkInterval = "30"; timeoutDuration = "600" },
                    modifier = Modifier.weight(1f),
                    colors = buttonColors
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Balanced", fontSize = 11.sp)
                        Text("30m/10m", fontSize = 9.sp)
                    }
                }

                Button(
                    onClick = { checkInterval = "60"; timeoutDuration = "900" },
                    modifier = Modifier.weight(1f),
                    colors = buttonColors
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Relaxed", fontSize = 11.sp)
                        Text("60m/15m", fontSize = 9.sp)
                    }
                }
            }
        }

        // Apply Button
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            val intervalMin = checkInterval.toLongOrNull()
            val timeoutSec = timeoutDuration.toLongOrNull()

            // Logic: Only apply if the user actually entered numbers
            if (intervalMin != null && timeoutSec != null) {
                DmsPreferences.setCheckIntervalMinutes(context, intervalMin)
                DmsPreferences.setTimeoutSeconds(context, timeoutSec)
                if (dmsEnabled) {
                    DmsManager.disableDms(context)
                    DmsManager.enableDms(context)
                }
            }
        }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = TacticalAccent)) { Text("APPLY SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Displays internal application logs with filtering capabilities.
 * Useful for debugging and user verification of actions.
 */
@Composable
fun ActivityLogScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var logs by remember { mutableStateOf(emptyList<String>()) }

    // Filter preference loading
    val (a, d, m) = LogFilterPrefs.load(context)
    var showAgent by remember { mutableStateOf(a) }
    var showDms by remember { mutableStateOf(d) }
    var showMobilerun by remember { mutableStateOf(m) }

    // Save filters on change
    LaunchedEffect(showAgent, showDms, showMobilerun) { LogFilterPrefs.save(context, showAgent, showDms, showMobilerun) }

    // Poll logs
    LaunchedEffect(Unit) { while (true) { logs = AgentLog.getLogs(context); delay(1000) } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(8.dp))

        Text(
            "ACTIVITY LOG",
            color = TacticalTextHigh,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 1.dp)
        )

        // --- Controls (Filters + Copy/Clear) ---
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = showAgent, onClick = { showAgent = !showAgent }, label = { Text("Agent", fontWeight = FontWeight.Normal) })
                FilterChip(selected = showDms, onClick = { showDms = !showDms }, label = { Text("Safety", fontWeight = FontWeight.Normal) })
                FilterChip(selected = showMobilerun, onClick = { showMobilerun = !showMobilerun }, label = { Text("Cloud", fontWeight = FontWeight.Normal) })
            }

            Row {
                IconButton(
                    onClick = { clipboard.setText(AnnotatedString(AgentLog.getRawJsonLogs(context))) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(TablerIcons.Copy, null, tint = TacticalTextMed)
                }
                IconButton(
                    onClick = { AgentLog.clear(context); logs = emptyList() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(TablerIcons.ClearAll, null, tint = TacticalTextMed)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- Log List ---
        Column(Modifier.verticalScroll(rememberScrollState())) {
            logs.filter { line ->
                when {
                    "[AGENT]" in line -> showAgent;
                    "[SAFETY]" in line -> showDms;
                    "[MOBILERUN]" in line -> showMobilerun;
                    else -> true
                }
            }.forEach {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        it,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SECTION: UI HELPERS
// -----------------------------------------------------------------------------

@Composable
fun ModeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.height(80.dp).then(if (selected) Modifier.border(2.dp, TacticalAccent, RoundedCornerShape(8.dp)) else Modifier), colors = CardDefaults.cardColors(containerColor = if (selected) TacticalAccent.copy(alpha = 0.15f) else TacticalSurface), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = title, color = if (selected) TacticalAccent else TacticalTextHigh, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center, lineHeight = 13.sp)
            Spacer(Modifier.height(4.dp)); Text(text = subtitle, color = if (selected) TacticalAccent else TacticalTextMed, fontSize = 10.sp, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
fun TacticalCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = TacticalSurface), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = TacticalAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}