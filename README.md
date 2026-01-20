# Panic-Mode: Autonomous Android Survival Agent

Panic-Mode is an **agent-driven Android safety system** built for real-world failure conditions such as phone theft, loss, no internet, locked screens, or low battery.

Unlike traditional safety apps, Panic-Mode uses **Droidrun agents** to bridge  **human intent → device-level automation → long-running autonomous behavior** .

---

## 🚨 What Problem Does This Solve?

Most phone safety solutions fail precisely when they are needed most:

* Device is locked or unattended
* Internet connectivity is unavailable
* Battery is critically low
* User cannot manually interact with the phone

Existing apps rely on cloud services, continuous background processes, or user interaction, making them unreliable in real emergency or theft scenarios.

Panic-Mode addresses this gap by introducing an **agent-driven system** that can:

* Interpret user intent
* Configure itself automatically
* Continue operating autonomously at the device level

---

## 🧠 How It Works (High-Level)

Panic-Mode operates in  **two distinct phases** :

### 1️⃣ Unlocked Phase (Agent Configuration)

A  **Droidrun agent** :

* Interprets natural language commands
* Navigates Android UI automatically
* Configures:
  * Trusted contact
  * Activation SMS code
  * Battery capacity
  * Risk / situation context

This phase uses  **Droidrun UI automation + reasoning** .

### 2️⃣ Locked / Headless Phase (Autonomous Survival)

Once armed:

* Foreground services + WorkManager take over
* No UI interaction required
* Operates even when:
  * Screen is locked
  * Internet is unavailable
  * App is restarted or backgrounded

Location updates and agent state are sent via  **SMS** , not cloud APIs.

---

## 🤖 Droidrun Agent Code

📂 **`droidrun-agent/`**

* `run_with_fallback.py` – Primary Droidrun execution entrypoint with API-key fallback
* `interpret_intent.py` – Intent-to-policy reasoning layer (natural language → device configuration)

This is where Droidrun is actively used to:

* Interpret human language
* Generate structured automation goals
* Execute Android actions reliably

---

## 📱 Android App (Headless Survival Engine)

📂 **`android-app/`**

Core components:

* **Foreground Service** – Persistent execution
* **WorkManager** – Periodic & adaptive location updates
* **Policy Engine** – Battery-aware, intent-aware decision logic
* **SMS Receiver** – Authorized trigger detection
* **SMS Sender** – Offline communication channel

The Android app continues operating **without Droidrun** once configured, demonstrating a real transition from agent-driven setup → autonomous execution.

---

## 🎥 Demo Video

👉 https://youtu.be/vhTvIm0TIGI

Demo shows:

1. Natural language command
2. Droidrun agent configuring the app
3. SMS trigger activation
4. Foreground notification + location SMS

---

## 🧪 Why This Matters for Droidrun

This project demonstrates that  **Droidrun agents are not limited to UI scripting** .

They can be used as:

* Intent interpreters
* Device policy generators
* Entry points into long-running autonomous systems

Panic-Mode showcases how Droidrun can power  **safety-critical, real-world Android automation** .

---

## 🧾 Repository Structure

```text
panic-mode-agent/
├── droidrun-agent/
│   ├── run_with_fallback.py
│   ├── interpret_intent.py
│   └── README.md
│
├── android-app/
│   └── app/src/main/java/com/panicmode/
│       ├── LocationWorker.kt
│       ├── PanicService.kt
│       ├── PolicyManager.kt
│       ├── SmsReceiver.kt
│       └── SmsSender.kt
│
└── README.md
```

