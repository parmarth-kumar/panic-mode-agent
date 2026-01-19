# Droidrun Agent – Panic-Mode

This folder contains the **Droidrun-based agent logic** used to configure the Panic-Mode Android app from **natural language input**.

The Droidrun agent is responsible for **interpreting intent, navigating Android UI, and generating structured device configuration**.

---

## 🚀 Entry Point

### `run_with_fallback.py`

Primary execution script.

Responsibilities:

* Accepts natural language command
* Validates Droidrun environment (`droidrun ping`)
* Rotates API keys automatically to avoid quota failures
* Invokes intent interpretation
* Executes Droidrun `run` with a generated goal

Example:

```bash
python run_with_fallback.py \
"I will be outside for 5 hours, set up survival agent for Aryan"
```

---

## 🧠 Intent Interpretation

### `interpret_intent.py`

This file acts as an **intent-to-policy reasoning layer**.

It:

* Uses a lightweight LLM call
* Extracts structured intent:

  * Trusted contact name
  * Activation code
  * Situation context (Traveling, Aggressive, Save Battery, etc.)
* Generates a **Droidrun goal string** describing the full automation sequence

Example output:

```json
{
  "trusted_person": "Aryan",
  "activation_code": "OUT-5H",
  "situation_intent": "TRAVELING",
  "droidrun_goal": "Open the Survival Agent app..."
}
```

---

## 🤖 Why Droidrun Is Essential Here

Droidrun is used to:

* Convert human language into actionable UI steps
* Navigate real Android apps (Contacts, Settings, custom app UI)
* Execute reliable automation under real device constraints

Once configuration is complete, Droidrun hands off control to the Android system’s **headless survival engine**.

---

## 🔐 API Key Handling

* API keys are loaded from `.env`
* `GOOGLE_API_KEYS` supports multiple keys
* Keys are swapped dynamically on failure
* No secrets are committed to GitHub

---

## 📌 Design Philosophy

Droidrun is treated as:

> **An intent-to-policy bridge**, not just a UI automation tool.

This separation allows:

* Clean agent logic
* Reliable long-running execution
* Real-world fault tolerance

---

## 🔍 Evaluation Notes

* Droidrun usage is intentional and central
* Automation is demonstrated on real Android UI
* This is not a mock or simulated workflow
