# 🛡️ Panic Mode — Hybrid Autonomous Survival Agent

> **An autonomous, agent-driven personal safety system.**  
> **Mobilerun AI prepares your phone for emergencies you cannot predict.**  
> Built with **Kotlin**, **Jetpack Compose**, **WorkManager**, **AlarmManager**, **Foreground Services**, and **Mobilerun AI**

---

### 🌟 Core Capabilities

- **Find Offline Lost Devices:** Locate your phone without internet using SMS telemetry
    
- **Autonomous Protection:** The agent acts on its own when you are incapacitated or alone
    
- **Mobilerun Execution Layer:** Configure safety policies using simple natural-language commands
    

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

## 🧭 Agent Authority Model

Panic Mode operates under a strict authority contract:

- The **user defines intent once**
    
- The **agent executes autonomously**
    
- The **agent escalates only when explicit safety conditions are met**
    
- The **agent never waits for confirmation in incapacitation scenarios**
    

This ensures the system is proactive without being unpredictable.

---

## 🏗️ System Architecture Overview

```
        ┌───────────────────────────┐
        │        USER INTENT        │
        │   (One-time definition)   │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │  LAYER 1: EXECUTION LAYER │
        │      Mobilerun (Cloud)    │
        └─────────────┬─────────────┘
                      │  Policy Locked
──────────────────────┼──────────────────────
                      │  (Offline Authority)
        ┌─────────────▼─────────────┐
        │  LAYER 2: AUTONOMOUS CORE │
        │  Safety Checks + Escalate │
        └─────────────┬─────────────┘
                      │
        ┌─────────────▼─────────────┐
        │  LAYER 3: SURVIVAL        │
        │  INSTINCT (Recovery)      │
        └───────────────────────────┘
```

---

## 🧩 The 3-Layer Intelligence Model

---

## ☁️ Layer 1 — Cloud Intelligence (Mobilerun)

### Purpose: **Writes policy for survival behavior of the phone**

Configuring safety systems manually is slow, error-prone, and stressful — exactly when users don’t have time.

### What this layer does

- Accepts **natural-language instructions**
    
- Converts them into **deterministic UI automation**
    
- Programs the app _for the user_
    

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
Once configured, the system **does not depend on the cloud**.

**Mobilerun is used for policy generation and UI execution.**

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

## 🔴 Layer 3 — Survival Instinct (Phone Recovery Agent)

### Purpose: **Be found when the grid fails**

The **Survival Instinct** is the phone’s last-resort intelligence.  
It activates when data networks are unavailable, the device is lost, or the user cannot intervene.

This layer focuses on **physical recovery** and **maximum uptime**, not convenience.

### Offline Recovery Mechanisms

- **SMS Telemetry Tunneling**  
    The agent listens for a trusted SMS trigger and replies with GPS coordinates using the GSM layer, bypassing mobile data entirely.
    
- **Acoustic Beaconing**  
    When stationary or battery-critical, the device emits intermittent high-frequency chirps to enable last-meter recovery in terrain like forests, rubble, or tall grass.
    

**In practice, this allows a searcher to stand in a remote area, send a single SMS, and physically recover the device even when all network services are unavailable.**

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
📌 The goal is **staying alive long enough to be found**.

---

## 🧠 Confidence Scoring (Diagnostics, Not Control)

Each heartbeat computes a **confidence score** to explain how reliable the update is.

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

## 🧾 Why This Is an AI Agent Powered by Mobilerun

|Traditional App|Panic Mode Agent|
|---|---|
|User-driven|Agent-driven|
|UI dependent|UI optional|
|Internet-first|Offline-first|
|Passive|Proactive|
|One-shot|Continuous|

---

## 🧠 Design Philosophy

> **“A safety system must assume the user will fail — and still work.”**  
> **The safety policy running on the device is authored by Mobilerun AI from user intent.**

- No silent failures
    
- No blocking calls in critical paths
    
- No dependency on a single signal
    
- Graceful degradation over hard crashes
    

---



