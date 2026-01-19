import os
import sys
import json
import re
from google import genai

# ---------------- CONFIG ----------------

MODEL = "gemini-2.5-flash-lite"

VALID_INTENTS = {
    "NORMAL",
    "TRAVELING",
    "CROWDED",
    "AGGRESSIVE",
    "SAVE_BATTERY"
}

# ---------------- LOW-LEVEL RULES (NO LLM) ----------------

def extract_name(text: str) -> str:
    """
    Extract name after 'to' or 'for' (case-insensitive).
    Example: 'set up agent to aryan's number' → Aryan
    """
    match = re.search(r"(?:to|for)\s+([a-zA-Z]+)", text, re.IGNORECASE)
    if match:
        return match.group(1).capitalize()
    return ""

def infer_duration(text: str) -> str | None:
    """
    Extract duration like '2 hours', '5 hr'
    """
    match = re.search(r"(\d+)\s*(hour|hr)", text.lower())
    if match:
        return f"{match.group(1)}H"
    return None

def infer_activation_code(text: str) -> str:
    t = text.lower()
    duration = infer_duration(text)

    if any(k in t for k in ["hiking", "hicking", "trek"]):
        return f"HIKING-{duration}" if duration else "HIKING"

    if any(k in t for k in ["outside", "out", "away"]):
        return f"OUT-{duration}" if duration else "OUT"

    if "travel" in t:
        return f"TRAVEL-{duration}" if duration else "TRAVEL"

    return "SURVIVAL"

def infer_intent(text: str) -> str:
    t = text.lower()

    if any(k in t for k in [
        "hiking", "hicking", "trek",
        "outside", "out", "away",
        "travel", "journey", "overnight"
    ]):
        return "TRAVELING"

    if any(k in t for k in ["lost", "stolen", "robbed", "snatched"]):
        return "AGGRESSIVE"

    if "crowd" in t or "crowded" in t:
        return "CROWDED"

    if "battery" in t or "save power" in t:
        return "SAVE_BATTERY"

    return "NORMAL"

# ---------------- LLM (LIGHT CLEANUP ONLY) ----------------

def llm_cleanup(text: str) -> dict:
    """
    VERY light LLM usage:
    - Only to normalize language
    - Never trusted blindly
    """
    api_key = os.environ.get("GOOGLE_API_KEY")
    if not api_key:
        raise RuntimeError("GOOGLE_API_KEY not set")

    client = genai.Client(api_key=api_key)

    prompt = f"""
Extract only the trusted person's name from this command.
Return JSON: {{ "trusted_person": "<name or empty>" }}

Command:
"{text}"
"""

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=prompt
        )
        # return json.loads(response.text)
        return json.loads(response.text) if response.text is not None else {}
    except Exception:
        return {}

# ---------------- STAGE 1: INTERPRET ----------------

def interpret_user_command(text: str) -> dict:
    llm_data = llm_cleanup(text)

    trusted_person = extract_name(text)
    if not trusted_person:
        trusted_person = llm_data.get("trusted_person", "")

    result = {
        "trusted_person": trusted_person,
        "activation_code": infer_activation_code(text),
        "device_battery": None,  # always decided by Droidrun via Settings
        "situation_intent": infer_intent(text)
    }

    # Hard safety
    if result["situation_intent"] not in VALID_INTENTS:
        result["situation_intent"] = "NORMAL"

    return result

# ---------------- STAGE 2: BUILD DROIDRUN GOAL ----------------

def build_droidrun_goal(data: dict) -> str:
    name = data["trusted_person"]
    code = data["activation_code"]
    intent = data["situation_intent"].capitalize()

    if not name:
        raise ValueError("Trusted person could not be resolved")

    return f"""
Open the Survival Agent app.
Open Contacts and find the contact named {name}.
Open the Survival Agent app and then fill in the Trusted person field.
Click on the Activation code (SMS) field to focus it.
Enter {code} into the Activation code (SMS) field.
Open device Settings and check the battery capacity.
Return to the Survival Agent app and fill in the battery capacity field.
Select the {intent} situation context.
Tap the ARM AGENT button to activate protection.
""".strip()


# ---------------- PUBLIC ENTRY ----------------

def interpret_and_build_goal(text: str) -> dict:
    intent_data = interpret_user_command(text)
    goal = build_droidrun_goal(intent_data)

    return {
        "interpreted_intent": intent_data,
        "droidrun_goal": goal
    }

# ---------------- CLI ----------------

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python interpret_intent.py \"command\"")
        sys.exit(1)

    result = interpret_and_build_goal(sys.argv[1])
    print(json.dumps(result, indent=2))
