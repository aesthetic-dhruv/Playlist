#!/usr/bin/env python3
"""Apply Misri rename + Raj Chauhan credit to a JARVIS Part 7 checkout.

Run from the repo root (the fork of techgptx/Playlist), e.g.:
    python3 patch-misri.py
It edits files under "Part 7/" only. It deliberately does NOT touch:
  - package name com.jarvis.assistant (renaming it would break 78 files)
  - LICENSE / Tech GPTx attribution (kept, per TAMCL v1.0 section 2)
  - the original repo README credit
It ADDS Raj Chauhan as modifier alongside the original author.

Phase 2 (v2, user clarification 2026-11-03): every remaining standalone
user-visible "Jarvis"/"JARVIS" word in Kotlin, layout XML and HTML assets
becomes "Misri"/"MISRI" — app screens, dialogs, toasts, persona label
("Jarvis Pro" -> "Misri Pro"), voice-enrollment examples, settings HTML,
HUD/orb titles, and AI tool descriptions. Girlfriend Mode (Pari) is kept
unchanged. Word-boundary matching protects code identifiers: package
com.jarvis.assistant, class names like JarvisApp, view IDs like
tvJarvisResponse, JS bridges like JarvisOrb/JarvisBridge, and theme names
like Theme.JarvisAssistant are NOT renamed. The original-credit lines
("Original JARVIS by Tech GPTx") are restored after phase 2, per the
license, and DeveloperInfo/AppIdentityVerifier are excluded from phase 2.
"""
import pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parent
# When run from the fork root, Part 7 lives beside this script's repo root.
# Allow explicit root: python3 patch-misri.py /path/to/fork
base = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path.cwd()
P7 = base / "Part 7"
APP = P7 / "app/src/main/java/com/jarvis/assistant"
RES = P7 / "app/src/main/res/values/strings.xml"

def replace(path, pairs, must_find=True):
    p = pathlib.Path(path)
    txt = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in txt:
            if must_find:
                print(f"WARN not found in {p.relative_to(base)}: {old[:80]!r}")
            continue
        txt = txt.replace(old, new)
    p.write_text(txt, encoding="utf-8")
    print(f"patched {p.relative_to(base)}")

# 1. strings.xml — launcher / chat label, plus a visible credit string
replace(RES, [
    ('<string name="app_name">JARVIS</string>', '<string name="app_name">Misri</string>'),
    ('<string name="jarvis_title">JARVIS</string>', '<string name="jarvis_title">Misri</string>'),
    ('<string name="chat_label_jarvis">JARVIS</string>', '<string name="chat_label_jarvis">Misri</string>'),
    ('for JARVIS.', 'for Misri.'),
    ('<string name="username_hint">e.g. Tony Stark, Sir, Boss (optional)</string>',
     '<string name="username_hint">e.g. Tony Stark, Sir, Boss (optional)</string>\n    <string name="original_credit">Original JARVIS by Tech GPTx • Modified by Raj Chauhan</string>'),
])

# 2. PromptGenerator — AI introduces itself as Misri, credit kept in the prompt
replace(APP / "util/PromptGenerator.kt", [
    ('You are JARVIS, the legendary ultra-capable, witty, and hyper-intelligent AI assistant created by TechGPT X.',
     'You are Misri, the ultra-capable, witty, and hyper-intelligent AI assistant. You are based on the original JARVIS project by Tech GPTx, modified by Raj Chauhan.'),
    ('You are JARVIS, the legendary artificial intelligence voice assistant.',
     'You are Misri, the artificial intelligence voice assistant, based on the original JARVIS project by Tech GPTx and modified by Raj Chauhan.'),
    ('JARVIS is fully fluent, natively literate, and culturally attuned',
     'Misri is fully fluent, natively literate, and culturally attuned'),
    ('1. You are JARVIS. Always maintain',
     '1. You are Misri. Always maintain'),
    ('[PERSONALITY: Jarvis Pro Assistant Mode]', '[PERSONALITY: Misri Assistant Mode]'),
])

# 3. HomeFragment — on-screen status texts
replace(APP / "ui/home/HomeFragment.kt", [
    ('"JARVIS OFF • TAP TO START"', '"MISRI OFF • TAP TO START"'),
    ('"STANDBY • SAY \'HEY JARVIS\'"', '"STANDBY • SAY \'HEY MISRI\'"'),
    ('"JARVIS SPEAKING..."', '"MISRI SPEAKING..."'),
    ('"JARVIS AGENT PROGRESS"', '"MISRI AGENT PROGRESS"'),
    ('Apps → JARVIS → Display over other apps.', 'Apps → Misri → Display over other apps.'),
])

# 4. TaskProgressOverlay — floating panel title
replace(APP / "ui/agent/TaskProgressOverlay.kt", [
    ('"JARVIS  ⋮⋮"', '"MISRI  ⋮⋮"'),
    ('"Drag JARVIS task overlay anywhere on screen"', '"Drag Misri task overlay anywhere on screen"'),
    ('title.text = "JARVIS"', 'title.text = "Misri"'),
])

# 5. Conversation service — wake word Misri (grammar + valid phrases + notifications)
# Only the wake-word / notification strings, NOT package names or class names.
replace(APP / "background/JarvisConversationService.kt", [
    ('val grammar = "[\\"jarvis\\", \\"hey jarvis\\", \\"hi jarvis\\", \\"hello jarvis\\", \\"ok jarvis\\", \\"okay jarvis\\", \\"wake up jarvis\\", \\"sun jarvis\\", \\"suno jarvis\\", \\"jarvis jarvis\\", \\"[unk]\\"]"',
     'val grammar = "[\\"misri\\", \\"hey misri\\", \\"hi misri\\", \\"hello misri\\", \\"ok misri\\", \\"okay misri\\", \\"wake up misri\\", \\"sun misri\\", \\"suno misri\\", \\"misri misri\\", \\"[unk]\\"]"'),
    ('"jarvis",\n        "hey jarvis",\n        "hi jarvis",\n        "hello jarvis",\n        "ok jarvis",\n        "okay jarvis",\n        "wake up jarvis",\n        "sun jarvis",\n        "suno jarvis",\n        "jarvis jarvis"',
     '"misri",\n        "hey misri",\n        "hi misri",\n        "hello misri",\n        "ok misri",\n        "okay misri",\n        "wake up misri",\n        "sun misri",\n        "suno misri",\n        "misri misri"'),
    ('"Jarvis is in the background" to "Say \\"Hey Jarvis\\" to wake me up"',
     '"Misri is in the background" to "Say \\"Hey Misri\\" to wake me up"'),
    ('"Jarvis is reconnecting"', '"Misri is reconnecting"'),
    ('"Jarvis did not reply"', '"Misri did not reply"'),
    ('"Jarvis is replying"', '"Misri is replying"'),
    ('"Jarvis is thinking"', '"Misri is thinking"'),
    ('"Jarvis is listening"', '"Misri is listening"'),
    ('"Jarvis is ready" to "Connected. Say your request or \\"Hey Jarvis\\"."',
     '"Misri is ready" to "Connected. Say your request or \\"Hey Misri\\"."'),
    ('else -> "Jarvis" to "Starting up\\u2026"', 'else -> "Misri" to "Starting up\\u2026"'),
    ('liveWebSocket?.sendClientContent("Hey Jarvis")', 'liveWebSocket?.sendClientContent("Hey Misri")'),
])

# 6. DeveloperInfo + AppIdentityVerifier — Raj Chauhan ADDED, Tech GPTx KEPT
replace(APP / "security/DeveloperInfo.kt", [
    ('object DeveloperInfo {',
     'object DeveloperInfo {\n    const val MODIFIED_BY         = "Raj Chauhan"\n    const val ORIGINAL_CREDIT     = "Original JARVIS by Tech GPTx • Modified by Raj Chauhan"'),
])
replace(APP / "security/AppIdentityVerifier.kt", [
    ('appendLine("Developer: ${BuildConfig.DEVELOPER_NAME}")',
     'appendLine("Developer: ${BuildConfig.DEVELOPER_NAME}")\n            appendLine("Modified by: Raj Chauhan")\n            appendLine("Original: JARVIS by Tech GPTx")'),
])

# 7. Settings identity dialog title
replace(APP / "ui/settings/SettingsFragment.kt", [
    ('.setTitle("JARVIS App Identity")', '.setTitle("Misri App Identity")'),
    ('binding.tvProfileSubtitle.text = "JARVIS OPERATOR • PRIMARY USER"',
     'binding.tvProfileSubtitle.text = "MISRI OPERATOR • PRIMARY USER"'),
])

# 8. Phase 2 — remaining standalone user-visible Jarvis/JARVIS words
import re
PROTECTED = {
    (APP / "security/DeveloperInfo.kt").resolve(),
    (APP / "security/AppIdentityVerifier.kt").resolve(),
}
changed_files = 0
for root_dir in [P7 / "app/src/main/java", P7 / "app/src/main/res", P7 / "app/src/main/assets"]:
    if not root_dir.exists():
        continue
    for p in sorted(root_dir.rglob("*")):
        if not p.is_file() or p.suffix not in (".kt", ".xml", ".html"):
            continue
        if p.resolve() in PROTECTED:
            continue
        txt = p.read_text(encoding="utf-8", errors="ignore")
        new_txt, n1 = re.subn(r"\bJARVIS\b", "MISRI", txt)
        new_txt, n2 = re.subn(r"\bJarvis\b", "Misri", new_txt)
        if n1 + n2:
            p.write_text(new_txt, encoding="utf-8")
            changed_files += 1
            print(f"phase2 {p.relative_to(base)}: {n1 + n2} replacements")
print(f"phase2 changed {changed_files} files")
# Restore original-credit lines that phase 2 would otherwise rename
# (and de-duplicate original_credit if the patch ever runs twice)
_txt = RES.read_text(encoding="utf-8")
_lines = _txt.splitlines()
_seen_credit = False
_out = []
for _ln in _lines:
    if "original_credit" in _ln:
        if _seen_credit:
            continue
        _seen_credit = True
    _out.append(_ln)
RES.write_text("\n".join(_out) + "\n", encoding="utf-8")
replace(APP / "util/PromptGenerator.kt", [
    ('based on the original MISRI project by Tech GPTx', 'based on the original JARVIS project by Tech GPTx'),
], must_find=False)
replace(RES, [
    ('<string name="original_credit">Original MISRI by Tech GPTx', '<string name="original_credit">Original JARVIS by Tech GPTx'),
], must_find=False)

print("Done. Verify with: grep -rn 'Hey Misri\\|app_name.*Misri' 'Part 7/app/src/main' | head")
