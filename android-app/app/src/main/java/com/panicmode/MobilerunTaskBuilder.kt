package com.panicmode

/**
 * Translates parsed natural-language intent into a deterministic,
 * step-by-step Mobilerun automation script.
 *
 * This output is consumed directly by the remote agent to
 * configure Panic Mode on the user's physical device.
 */
object MobilerunTaskBuilder {

    fun build(p: ParsedCommand): String {

        // Core setup flow required for any agent activation
        val base = """
1. Click Agent tab in navigation bar.

2. Open Contacts app.
3. Search for ${p.contactName}.
4. Remember the contact's phone number.

5. Open Panic Mode app.
6. Click Trusted Contact field.
7. Enter the phone number.

8. Click Activation SMS code field.
9. Enter ${p.code}.

10. Open device Settings.
11. Open About phone → Battery information.
12. Note the battery capacity.

13. Open Panic Mode app.
14. Click battery capacity field.
15. Enter the battery capacity.

16. Select the ${p.intent} situation context.
17. Tap ARM AGENT.
""".trimIndent()

        // Optional safety system configuration when DMS is requested
        return if (p.enableDms) {
            base + """

18. Click Safety tab in navigation bar.
19. Click Balanced option in QUICK PRESETS.
20. Click apply settings button.
21. Enable Safety Check System switch.
"""
        } else base
    }
}
