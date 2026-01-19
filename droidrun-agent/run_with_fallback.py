import os
import subprocess
import sys
import time
import json
from dotenv import load_dotenv

load_dotenv()

PROVIDER = "GoogleGenAI"
MODEL = "gemini-2.5-flash-lite"
ENV_VAR = "GOOGLE_API_KEY"

INTERPRETER = os.path.join(
    os.path.dirname(__file__),
    "interpret_intent.py"
)


def check_droidrun_ping():
    print("🔍 Checking Droidrun environment (ping)…")

    result = subprocess.run(
        ["droidrun", "ping"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="ignore",
    )

    output = (result.stdout + result.stderr).strip()
    print(output)

    if "You're good to go!" in output:
        print("✅ Droidrun ping successful.")
        return True

    print("❌ Droidrun ping failed. Android device not ready.")
    return False


def interpret_command(command: str) -> str:
    """
    Runs interpret_intent.py once and returns a Droidrun goal string
    """
    result = subprocess.run(
        [sys.executable, INTERPRETER, command],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="ignore",
    )

    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip())

    data = json.loads(result.stdout)
    return data["droidrun_goal"]


def run_droidrun(goal: str) -> bool:
    cmd = [
        "droidrun",
        "run",
        goal,
        "--provider", PROVIDER,
        "--model", MODEL,
        "--vision",
        "--steps", "30",
    ]

    print(f"\n🚀 Running Droidrun goal:\n{goal}\n")

    env = os.environ.copy()
    env["PYTHONUTF8"] = "1"

    result = subprocess.run(cmd, env=env)
    return result.returncode == 0


def main():
    if len(sys.argv) < 2:
        print("Usage: python run_with_fallback.py \"natural language command\"")
        sys.exit(1)

    user_command = sys.argv[1]

    # 🛑 HARD GATE: device must be ready
    if not check_droidrun_ping():
        sys.exit(1)

    raw_keys = os.getenv("GOOGLE_API_KEYS")
    if not raw_keys:
        print("❌ GOOGLE_API_KEYS not set in .env")
        sys.exit(1)

    api_keys = [k.strip() for k in raw_keys.split(",") if k.strip()]
    if not api_keys:
        print("❌ No valid API keys found")
        sys.exit(1)

    # 🔑 Use FIRST key for interpretation only
    os.environ[ENV_VAR] = api_keys[0]

    # 🔍 Interpret ONCE
    try:
        goal = interpret_command(user_command)
    except Exception as e:
        print(f"❌ Interpretation failed: {e}")
        sys.exit(1)

    # 🔁 Retry execution with fallback keys
    for idx, key in enumerate(api_keys, start=1):
        print(f"\n🔑 Trying API key #{idx}")
        os.environ[ENV_VAR] = key

        if run_droidrun(goal):
            print("✅ Success")
            return

        print("⚠️ Run failed, trying next key…")
        time.sleep(1)

    print("\n🚨 All API keys exhausted")
    sys.exit(1)


if __name__ == "__main__":
    main()
