
# 🛡️ Panic Mode — Hybrid Survival Agent for Android

> **An autonomous, agent-driven personal safety system**
> 
> Built with **Kotlin**, **Jetpack Compose**, **WorkManager**, **AlarmManager**, **Foreground Services**, and **Mobilerun AI**

---

## 🚨 The Core Problem

Most safety apps **fail when you need them most**.

They assume:

- You are **conscious**
    
- You can **unlock your phone**
    
- You have **network connectivity**
    
- You can **interact with the UI**
    

That assumption is **fatal**.

### Real failure scenarios

- You collapse or lose consciousness while hiking
    
- You are under threat and cannot openly use your phone
    
- Your battery is critically low and drains before help arrives
    
- You lose data connectivity in a remote area
    

> **If the system waits for the user, the system is already broken.**

---

## 🧠 The Solution: An Agent That Acts _Instead_ of You

**Panic Mode inverts control.**

Instead of reacting to user input, it runs as an **autonomous survival agent** that:

- Operates **without UI**
    
- Works **offline**
    
- Survives **Doze, idle, and background limits**
    
- Escalates **without confirmation** when required
    

---

## 🏗️ System Architecture Overview

```
                    ┌─────────────────────────┐
                    │   CLOUD INTELLIGENCE    │
                    │  (Mobilerun AI Agent)   │
                    │  Natural Language Setup │
                    └───────────▲─────────────┘
                                │
                                │ Agent Configuration
                                │
┌───────────────────────────────┴───────────────────────────────┐
│                    ON-DEVICE AUTONOMY                         │
│                                                               │
│  ┌──────────────────┐     ┌──────────────────┐                │
│  │  SAFETY CHECK    │◀──▶│  PANIC AGENT     │                │
│  │  SYSTEM (DMS)    │     │  (Core Brain)    │                │
│  └──────────────────┘     └──────────────────┘                │
│            │                          │                       │
│            ▼                          ▼                       │
│     Escalation SMS             Location Heartbeats            │
│     Timeout Alarms             Battery-Aware Policies         │
└───────────────────────────────────────────────────────────────┘
```

---

## 🧩 The 3-Layer Intelligence Model

---

## ☁️ Layer 1 — Cloud Intelligence (Mobilerun)

### Purpose: **Zero-Friction Setup**

Configuring safety systems manually is slow, error-prone, and stressful — exactly when users don’t have time.

### What this layer does

- Accepts **natural-language instructions**
    
- Converts them into **deterministic UI automation**
    
- Configures the app _for the user_
    

### Example

> “I’m hiking for 4 hours, enable panic mode for my mom with safety checks.”

### What happens internally

```
User Text
   ↓
CommandParser
   ↓
ParsedCommand(intent, contact, duration, DMS)
   ↓
MobilerunTaskBuilder
   ↓
Step-by-Step UI Automation
```

📌 **Important:**  
This layer is **setup-only**.  
Once configured, the system **does not depend on the cloud**.

---

## 🟡 Layer 2 — Safety Check System (Dead Man’s Switch)

### Purpose: **Act when the user cannot**

This layer assumes the **worst case**:  
the user is unconscious, immobilized, or unable to respond.

### How it works

```
[Scheduled Timer]
     ↓
Safety Check Notification
     ↓
User Confirms?
     ├── YES → Reset cycle
     └── NO  → Escalation
```

### Escalation Behavior

If the user does **not** respond:

- Increment missed count
    
- Fetch best-effort location
    
- Send escalation SMS with:
    
    - Location (if available)
        
    - Battery status
        
    - Instructions for remote control
        

```
⚠️ User missed safety checks
Try contacting them.
📍 Location: Google Maps link
🔋 Battery: 23%

Send:
TRIGGER → activate live tracking
TRIGGER-STOP → pause tracking
```

📌 This system:

- Survives app restarts
    
- Recovers from device idle
    
- Uses **exact alarms + foreground keepalive**
    
- Never double-fires or ghosts
    

---

## 🔴 Layer 3 — Autonomous Panic Agent (Core Brain)

### Purpose: **Survive when everything else degrades**

This is the **always-on intelligence** that manages:

- Power
    
- Frequency
    
- Location quality
    
- Communication reliability
    

---

### Agent Decision Flow

```
Panic Activated
     ↓
Read User Intent + Battery State
     ↓
Policy Engine
     ├── High Battery → VISIBILITY (15 min)
     ├── Medium Battery → ADAPTIVE
     └── Low Battery (<15%) → SURVIVAL (60 min)
     ↓
Schedule Heartbeats
     ↓
Send SMS Updates
```

---

### Battery-Aware Survival Logic

```
Battery Level
     │
     ├─ >30% → High-frequency updates
     ├─ 15–30% → Reduced frequency
     └─ <15% → Survival Mode
                    ↓
              Minimum updates
              Maximum uptime
```

📌 The goal is **not accuracy**.  
The goal is **staying alive long enough to be found**.

---

## 🧠 Confidence Scoring (Diagnostics, Not Control)

Each heartbeat computes a **confidence score** to explain _how reliable_ the current update is.

Inputs:

- Location availability
    
- Live vs cached fix
    
- Battery health
    
- Cold start detection
    

```
Confidence = 100
  -30 if no location
  -15 if cached only
  -20 if battery critical
  -10 if cold start
```

Used only for:

- Logs
    
- Debugging
    
- Demo transparency
    

🚫 **Never used for decisions**

---

## 🧾 Why This Is an Agent (Not Just an App)

|Traditional App|Panic Mode Agent|
|---|---|
|User-driven|Agent-driven|
|UI dependent|UI optional|
|Internet-first|Offline-first|
|Passive|Proactive|
|One-shot|Continuous|

---

## 🧠 Design Philosophy

> “A safety system must assume the user will fail — and still work.”

- No silent failures
    
- No blocking calls in critical paths
    
- No dependency on a single signal
    
- Graceful degradation over hard crashes
    