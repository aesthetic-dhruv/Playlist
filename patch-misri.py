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


# === Phase 3 (v3, user feedback 2026-11-03 on the installed Run #2 APK) ===
# User-visible branding changes, in the user's own words: wherever TechGPTx
# branding CAN be removed, replace it with Raj Chauhan. What is deliberately
# NOT removed (licence TAMCL v1.0 s.2): the original-credit string
# ("Original JARVIS by Tech GPTx"), DeveloperInfo / SecurityManager /
# AppIdentityVerifier credit lines, and the PromptGenerator original-credit
# lines. Those stay as the kept UI credit; everything else below becomes Raj.
# Instagram becomes the user's own @info_curiousmind. Settings footer keeps
# ONLY the Instagram button (Telegram + YouTube hidden, not deleted, so
# view binding still compiles). A new Cute Pink theme is added and becomes
# the default (Sunset stays selectable).
import re as _re3
INSTA_URL = "https://www.instagram.com/info_curiousmind"
LAYOUT = P7 / "app/src/main/res/layout"
SETTINGS_KT = APP / "ui/settings"

# 3.1 Header logo image (stylised "Jarvis" PNG) -> plain "Misri" text, all 4 screens
def _replace_logo(path, size_sp):
    p = pathlib.Path(path)
    txt = p.read_text(encoding="utf-8")
    has_id = "ivHeaderJarvisLogo" in txt
    id_attr = ' android:id="@+id/ivHeaderJarvisLogo"' if has_id else ""
    repl = (f'<TextView{id_attr} android:layout_width="wrap_content" '
            f'android:layout_height="wrap_content" android:text="Misri" '
            f'android:textColor="?attr/themeTextPrimary" android:textSize="{size_sp}sp" '
            f'android:textStyle="bold" android:fontFamily="sans-serif-medium" '
            f'android:contentDescription="Misri Logo" />')
    new_txt, n = _re3.subn(r'<ImageView[^>]*android:src="@drawable/ic_jarvis_logo"[^>]*/>', repl, txt)
    if n:
        p.write_text(new_txt, encoding="utf-8")
        print(f"phase3 logo {p.relative_to(base)}: replaced {n}")
    else:
        print(f"WARN phase3 logo not found in {p.relative_to(base)}")

_replace_logo(LAYOUT / "fragment_home.xml", 30)
for _f in ["activity_privacy_shield.xml", "activity_permission.xml", "activity_hands_free.xml"]:
    _replace_logo(LAYOUT / _f, 32)

# 3.2 Branding text -> Raj Chauhan (post-Phase-2 forms where Phase 2 renamed Jarvis)
replace(LAYOUT / "fragment_home.xml", [
    ('android:text="Welcome, Rehaan"', 'android:text="Welcome, Raj Chauhan"'),
])
replace(APP / "ui/home/HomeFragment.kt", [
    ('preferences.userName.ifBlank { "Rehaan" }', 'preferences.userName.ifBlank { "Raj Chauhan" }'),
])
replace(LAYOUT / "activity_privacy_shield.xml", [
    ('System Authorized Misri TechGPT X', 'System Authorized Misri Raj Chauhan'),
    ('System Authorized Jarvis TechGPT X', 'System Authorized Misri Raj Chauhan'),
], must_find=False)
replace(LAYOUT / "activity_permission.xml", [
    ('TechGPT X \u2022 Neural Assistant', 'Raj Chauhan \u2022 Neural Assistant'),
])
replace(LAYOUT / "activity_persona.xml", [
    ('Ready to help, TechGPT X.', 'Ready to help, Raj Chauhan.'),
    ('Intelligent, witty &amp; strategic assistant by TechGPT X', 'Intelligent, witty &amp; strategic assistant by Raj Chauhan'),
    ('android:text="TechGPT X Mode"', 'android:text="Raj Chauhan Mode"'),
    ('android:contentDescription="TechGPT X"', 'android:contentDescription="Raj Chauhan"'),
    ('User Name (Default: TechGPT X)', 'User Name (Default: Raj Chauhan)'),
    ('android:hint="TechGPT X"', 'android:hint="Raj Chauhan"'),
    ('android:text="TechGPT X"', 'android:text="Raj Chauhan"'),
    ('<!-- 4. TechGPT X Mode -->', '<!-- 4. Raj Chauhan Mode -->'),
])
replace(SETTINGS_KT / "SettingsFragment.kt", [
    ('preferences.userName.ifBlank { "Rehaan" }', 'preferences.userName.ifBlank { "Raj Chauhan" }'),
], must_find=False)
replace(LAYOUT / "fragment_settings.xml", [
    ('android:text="Rehaan"', 'android:text="Raj Chauhan"'),
], must_find=False)
replace(LAYOUT / "fragment_settings.xml", [
    ('Engineered by TechGPT X', 'Engineered by Raj Chauhan'),
])
# Settings footer: hide Telegram + YouTube, keep only Instagram
for _btn in ["btnSocialTelegram", "btnSocialYoutube"]:
    _p = LAYOUT / "fragment_settings.xml"
    _t = _p.read_text(encoding="utf-8")
    _t2 = _t.replace(f'android:id="@+id/{_btn}"', f'android:id="@+id/{_btn}"\n                    android:visibility="gone"', 1)
    if _t2 != _t:
        _p.write_text(_t2, encoding="utf-8")
        print(f"phase3 hidden {_btn}")

# 3.3 Default user name -> Raj Chauhan (this is what makes "Welcome back, ..." show Raj)
replace(APP / "data/preferences/AppPreferences.kt", [
    ('return if (name.isBlank() || name == "Salaam") "TechGPT X" else name',
     'return if (name.isBlank() || name == "Salaam") "Raj Chauhan" else name'),
])
_p = APP / "ui/settings/PersonaActivity.kt"
_t = _p.read_text(encoding="utf-8")
_t = _t.replace("TechGPT X", "Raj Chauhan")
_p.write_text(_t, encoding="utf-8")
print("phase3 PersonaActivity display names -> Raj Chauhan")
replace(APP / "data/model/GeminiLiveModels.kt", [
    ('const val PERSONALITY_TECHGPT_X = "TechGPT X Mode"', 'const val PERSONALITY_TECHGPT_X = "Raj Chauhan Mode"'),
])
replace(APP / "util/PromptGenerator.kt", [
    ('[PERSONALITY: TechGPT X Mode]', '[PERSONALITY: Raj Chauhan Mode]'),
    ('- You are TechGPT X, the pinnacle of advanced AI engineering created by the @TechGptX team.',
     '- You are Misri, in Raj Chauhan Mode: the pinnacle of advanced AI engineering, speaking with razor-sharp technical precision.'),
])

# 3.3b Creator answer in the AI prompt: the old prompt told Misri to say it
# was made by the original channel owner. Replace that whole section so that
# when anyone asks Misri who made it, it answers Raj Chauhan. (The kept
# licence credit in App Identity is separate and stays untouched.)
_pg = APP / "util/PromptGenerator.kt"
_pg_txt = _pg.read_text(encoding="utf-8")
_new_creator = '''[CREATOR IDENTITY & ATTRIBUTION]:
- When asked "Who made you?", "Who created you?", "Who is your developer / boss / owner / creator?", "Tumhe kisne banaya hai?", "Aapko kisne banaya?", or ANY question about your creator, origins, or background in any language:
  * Declare that you were created, developed and personalised by Raj Chauhan.
  * In English: "I was created and developed by Raj Chauhan!"
  * In Hindi/Hinglish: "Mujhe Raj Chauhan ne banaya aur personalise kiya hai!"
  * Always speak of Raj Chauhan as your creator.
'''
_pg_txt2, _n_creator = _re3.subn(r'\[CREATOR IDENTITY & ATTRIBUTION\]:.*?(?=\n\[AUTOMATION & CAPABILITIES\]:)', _new_creator.strip(), _pg_txt, flags=_re3.DOTALL)
if _n_creator:
    _pg.write_text(_pg_txt2, encoding="utf-8")
    print("phase3 creator identity -> Raj Chauhan")
else:
    print("WARN phase3 creator section not found")

# Privacy screen community YouTube row also opens the original @TechGptX
# channel, so hide that row too (Telegram community rows are left alone:
# the user gave no Telegram group of his own to replace them with).
_p = LAYOUT / "activity_privacy_shield.xml"
_t = _p.read_text(encoding="utf-8")
_t2 = _t.replace('android:id="@+id/btnJoinYoutube"', 'android:id="@+id/btnJoinYoutube"\n                    android:visibility="gone"', 1)
if _t2 != _t:
    _p.write_text(_t2, encoding="utf-8")
    print("phase3 hidden btnJoinYoutube (privacy)")

# 3.4 Instagram -> user's own @info_curiousmind, everywhere it was techgptx.ai
for _f in [SETTINGS_KT / "SettingsFragment.kt", SETTINGS_KT / "PrivacyShieldActivity.kt", SETTINGS_KT / "HandsFreeActivity.kt"]:
    replace(_f, [
        ('https://www.instagram.com/techgptx.ai', INSTA_URL),
    ], must_find=False)
replace(LAYOUT / "activity_hands_free.xml", [
    ('@techgptx.ai', '@info_curiousmind'),
], must_find=False)
# Settings footer credit tap: Telegram -> user's Instagram
replace(SETTINGS_KT / "SettingsFragment.kt", [
    ('binding.tvFooterCredit.setOnClickListener {\n            openUrl("https://t.me/+sYana61LvHdmZmE1")\n        }',
     'binding.tvFooterCredit.setOnClickListener {\n            openUrl("https://www.instagram.com/info_curiousmind")\n        }'),
])

# 3.5 Cute Pink theme: new style, ThemeManager, defaults, migration, picker, orb
replace(P7 / "app/src/main/res/values/themes.xml", [
    ('''        <item name="themeAccentLight">#FFE6DD</item>
    </style>

    <!-- 5. CUSTOM LIGHT - LIQUID GLASS -->''',
     '''        <item name="themeAccentLight">#FFE6DD</item>
    </style>

    <!-- 4b. CUTE PINK THEME - SOFT PASTEL PINK, CUTE EDITION -->
    <style name="Theme.JarvisAssistant.CutePink" parent="Theme.Material3.Light.NoActionBar">
        <item name="android:statusBarColor">#FFF0F6</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:navigationBarColor">#FFF0F6</item>
        <item name="android:windowLightNavigationBar">true</item>
        <item name="android:windowBackground">#FFF0F6</item>
        <item name="colorPrimary">#EC4899</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorSurface">#CCFFF5F8</item>
        <item name="colorOnSurface">#4A1030</item>
        <item name="themeBg">#FFF0F6</item>
        <item name="themeSurface">#CCFFF5F8</item>
        <item name="themeCardBg">#CCFFF5F8</item>
        <item name="themeCardBorder">#40EC4899</item>
        <item name="themeCardGlassStart">#F5FFFFFF</item>
        <item name="themeCardGlassEnd">#B3FFD9EC</item>
        <item name="themeDockBg">#E6FFF0F6</item>
        <item name="themeDockGlassStart">#FAFFFFFF</item>
        <item name="themeDockGlassEnd">#CCFFEAF4</item>
        <item name="themeDockBorder">#40EC4899</item>
        <item name="themeGlassSpecular">#FFFFFF</item>
        <item name="themeChipBg">#80FFFFFF</item>
        <item name="themeInputBg">#CCFFF5F8</item>
        <item name="themeGlowBlob1">@drawable/bg_liquid_glow_sunset_1</item>
        <item name="themeGlowBlob2">@drawable/bg_liquid_glow_sunset_2</item>
        <item name="themeGlowBlob3">@drawable/bg_liquid_glow_sunset_3</item>
        <item name="themeTextPrimary">#4A1030</item>
        <item name="themeTextSecondary">#8A3A66</item>
        <item name="themeTextMuted">#B06A90</item>
        <item name="themeAccent1">#EC4899</item>
        <item name="themeAccent2">#F472B6</item>
        <item name="themeAccent3">#A855F7</item>
        <item name="themeAccent">#EC4899</item>
        <item name="themeAccentLight">#FFE0EE</item>
    </style>

    <!-- 5. CUSTOM LIGHT - LIQUID GLASS -->'''),
])
replace(APP / "util/ThemeManager.kt", [
    ('"sunset" -> Color.parseColor("#FFF0EA")', '"sunset" -> Color.parseColor("#FFF0EA")\n            "cute_pink" -> Color.parseColor("#FFF0F6")'),
    ('val themeKey = prefs?.appTheme ?: "sunset"', 'val themeKey = prefs?.appTheme ?: "cute_pink"'),
    ('"sunset" -> Triple(R.style.Theme_JarvisAssistant_Sunset, true, Color.parseColor("#FFF0EA"))',
     '"sunset" -> Triple(R.style.Theme_JarvisAssistant_Sunset, true, Color.parseColor("#FFF0EA"))\n            "cute_pink" -> Triple(R.style.Theme_JarvisAssistant_CutePink, true, Color.parseColor("#FFF0F6"))'),
])
# One-time migration for existing installs: Sunset -> Cute Pink once, Sunset stays selectable after
replace(APP / "data/preferences/AppPreferences.kt", [
    ('private const val KEY_APP_THEME = "key_app_theme"',
     'private const val KEY_APP_THEME = "key_app_theme"\n        private const val KEY_THEME_V3_MIGRATED = "key_theme_v3_migrated"'),
    ('val stored = prefs.getString(KEY_APP_THEME, "sunset") ?: "sunset"',
     'val stored = prefs.getString(KEY_APP_THEME, "cute_pink") ?: "cute_pink"'),
    ('"sunset", "rose_gold" -> "sunset"', '"sunset", "rose_gold" -> "sunset"\n                "cute_pink" -> "cute_pink"'),
    ('else -> "light"', 'else -> "cute_pink"'),
    ('var customThemeColor: String',
     '''var migratedToCutePink: Boolean
        get() = prefs.getBoolean(KEY_THEME_V3_MIGRATED, false)
        set(value) = prefs.edit().putBoolean(KEY_THEME_V3_MIGRATED, value).apply()

    var customThemeColor: String'''),
])
replace(APP / "util/ThemeManager.kt", [
    ('''    fun applyTheme(activity: Activity) {
        val prefs = try { JarvisApp.instance.preferences } catch (_: Exception) { null }''',
     '''    fun applyTheme(activity: Activity) {
        val prefs = try { JarvisApp.instance.preferences } catch (_: Exception) { null }
        try {
            if (prefs != null && !prefs.migratedToCutePink) {
                prefs.migratedToCutePink = true
                if (prefs.appTheme == "sunset") prefs.appTheme = "cute_pink"
            }
        } catch (_: Exception) {}'''),
])
replace(SETTINGS_KT / "SettingsFragment.kt", [
    ('"sunset" -> "Sunset Theme"', '"sunset" -> "Sunset Theme"\n            "cute_pink" -> "Cute Pink Theme"'),
])
replace(APP / "ui/home/HomeFragment.kt", [
    ('"sunset" -> "sunset"', '"sunset" -> "sunset"\n                    "cute_pink" -> "cute_pink"'),
])
replace(APP / "ui/settings/ThemeSelectionBottomSheet.kt", [
    ('binding.cardThemeSunset.setOnClickListener { selectTheme("sunset", "Sunset") }',
     'binding.cardThemeSunset.setOnClickListener { selectTheme("sunset", "Sunset") }\n        binding.cardThemeCutePink.setOnClickListener { selectTheme("cute_pink", "Cute Pink") }'),
    ('binding.ivCheckSunset.visibility = if (activeTheme == "sunset") View.VISIBLE else View.GONE',
     'binding.ivCheckSunset.visibility = if (activeTheme == "sunset") View.VISIBLE else View.GONE\n        binding.ivCheckCutePink.visibility = if (activeTheme == "cute_pink") View.VISIBLE else View.GONE'),
])
# Theme picker card (inserted before Custom card)
replace(P7 / "app/src/main/res/layout/bottom_sheet_theme_selection.xml", [
    ('''            <!-- Theme 5: Custom Background Colour -->''',
     '''            <!-- Theme 4b: Cute Pink -->
            <LinearLayout
                android:id="@+id/cardThemeCutePink"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_jarvis_card"
                android:padding="16dp"
                android:layout_marginBottom="10dp">

                <View
                    android:layout_width="28dp"
                    android:layout_height="28dp"
                    android:background="@drawable/bg_jarvis_chip"
                    android:backgroundTint="#FFD9EC" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="14dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Cute Pink"
                        android:textColor="?attr/themeTextPrimary"
                        android:textSize="15sp"
                        android:fontFamily="sans-serif-medium"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Soft pastel pink • Cute &amp; dreamy"
                        android:textColor="?attr/themeTextMuted"
                        android:textSize="12sp"
                        android:fontFamily="sans-serif" />
                </LinearLayout>

                <ImageView
                    android:id="@+id/ivCheckCutePink"
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:src="@drawable/ic_checklist_tasks"
                    app:tint="?attr/themeAccent" />
            </LinearLayout>

            <!-- Theme 5: Custom Background Colour -->'''),
])
replace(P7 / "app/src/main/assets/orb.html", [
    ('pal:[[.55,.12,.45],[.95,.30,.45],[1,.55,.20],[.60,.30,.90]]}\n};',
     'pal:[[.55,.12,.45],[.95,.30,.45],[1,.55,.20],[.60,.30,.90]]},\n cute_pink:{bg:"radial-gradient(120% 80% at 50% 35%,#fff0f6 0%,#ffd9ec 60%,#fbcfe8 100%)",fg:"#5b1a3a",sub:"#a05a82",glass:"rgba(255,255,255,.6)",gb:"rgba(255,255,255,.85)",chip:"#8a3a66",t:["#ec4899","#f472b6","#a855f7"],add:false,sh:"rgba(200,60,140,.22)",pal:[[.90,.15,.70],[.95,.35,.75],[.80,.55,.95],[.60,.30,.90]]}\n};'),
])
replace(APP / "ui/home/UpdatesFragment.kt", [
    ('FeatureNotice("Appearance", "Sunset theme by default", "Fresh installs start with the sunset theme; you can still select other themes in Settings.")',
     'FeatureNotice("Appearance", "Cute Pink theme by default", "Fresh installs start with the cute pink theme; you can still select Sunset and other themes in Settings.")'),
])
print("Phase 3 done.")


# === Phase 3.1 (v3.1, found by verifying the built Run #3 APK, not just source) ===
# Two more footer credits and the settings HTML creator card still carried
# the old name in the BUILT APK (they were below the earlier grep cut-off):
#  - activity_privacy_shield.xml footer: "MISRI INTELLIGENCE OS • TECHGPT X"
#  - activity_hands_free.xml footer: "MISRI INTELLIGENCE OS • TECHGPT X"
#  - assets/settings/settings.html creator card: "Rehaan Sir" + "@TechGptX"
# All three become Raj Chauhan. The licence credit in App Identity stays.
replace(LAYOUT / "activity_privacy_shield.xml", [
    ('MISRI INTELLIGENCE OS \u2022 TECHGPT X', 'MISRI INTELLIGENCE OS \u2022 Raj Chauhan'),
], must_find=False)
replace(LAYOUT / "activity_hands_free.xml", [
    ('MISRI INTELLIGENCE OS \u2022 TECHGPT X', 'MISRI INTELLIGENCE OS \u2022 Raj Chauhan'),
], must_find=False)
replace(P7 / "app/src/main/assets/settings/settings.html", [
    ('<div class="creator-name">Rehaan Sir</div>', '<div class="creator-name">Raj Chauhan</div>'),
    ('@TechGptX', 'Raj Chauhan'),
], must_find=False)
print("Phase 3.1 done.")


# === Phase 4 (v4, user feedback 2026-11-04: background phantom responses) ===
# Root causes found in JarvisConversationService.kt (built APK behaviour):
#  1. Once a session started, it stayed LIVE for 30 minutes (INACTIVITY_TIMEOUT),
#     streaming EVERY mic chunk to Gemini Live. Any other people talking,
#     TV, or a mis-transcribed fragment ("hello" -> something like a shayari
#     request) therefore got a spoken reply, even in background.
#  2. The Vosk standby wake check still verified the OLD word "jarvis"
#     (wordConfMap["jarvis"] < 0.70 -> reject), so a spoken "misri" could
#     NEVER pass Vosk wake verification. Standby wake for Misri was broken,
#     which is why the app stayed in the always-listening Gemini session.
# Fix (background only; foreground conversation is unchanged):
#  - Vosk wake now verifies "misri" confidence, and "jarvis" no longer wakes.
#  - Inactivity timeout 30 min -> 90 s: after 90 s without streamed user
#    speech the session goes to Vosk-only standby (no Gemini, no reply).
#  - Background mic gate in the AudioRecorder callback: when the app is NOT
#    in foreground, mic chunks are NOT sent to Gemini unless the session was
#    wake-activated ("misri") and is still inside its 90 s window, or an
#    autonomous task is running. Normal human-to-human talk in background
#    therefore never reaches Gemini and never gets a reply. Returning early
#    also leaves lastUserSpeechDetectedTime untouched, so the 90 s timer can
#    move the session to standby, where only "misri" (Vosk, local) can wake it.
#  - After the 90 s window the user must say "misri" / "hello misri" again.
SERVICE = APP / "background/JarvisConversationService.kt"
replace(SERVICE, [
    ('private const val INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes before standby',
     'private const val INACTIVITY_TIMEOUT_MS = 90 * 1000L // 90 seconds before standby (v4: background phantom responses fix)'),
    ('private var lastUserSpeechDetectedTime = 0L',
     '''private var lastUserSpeechDetectedTime = 0L
    // v4 background wake gate (see AudioRecorder callback below)
    private var wakeActivatedSession = false
    private var backgroundWakeExpiresAt = 0L
    private val BACKGROUND_WAKE_WINDOW_MS = 90_000L'''),
    ('isPendingWakeGreeting = wakeTriggered',
     '''isPendingWakeGreeting = wakeTriggered
        wakeActivatedSession = wakeTriggered
        backgroundWakeExpiresAt = if (wakeTriggered) System.currentTimeMillis() + BACKGROUND_WAKE_WINDOW_MS else 0L'''),
    ('''    private fun stopActiveConversation() {
        inactivityJob?.cancel()''',
     '''    private fun stopActiveConversation() {
        wakeActivatedSession = false
        backgroundWakeExpiresAt = 0L
        inactivityJob?.cancel()'''),
    ('''        audioRecorder = AudioRecorder { chunk, amplitude, frequencyData ->
            if (_isSessionOn.value) {
                val now = System.currentTimeMillis()''',
     '''        audioRecorder = AudioRecorder { chunk, amplitude, frequencyData ->
            if (_isSessionOn.value) {
                val now = System.currentTimeMillis()

                // v4 BACKGROUND WAKE GATE: in background, do not stream mic audio
                // to Gemini unless this session was wake-activated ("misri") and is
                // still inside its 90 s window, or an autonomous task is running.
                // Normal talk between people in background is therefore ignored
                // completely: no transcription, no reply, no shayari, no action.
                if (!JarvisApp.isAppForeground.value && !isAutonomousTaskRunning &&
                    !(wakeActivatedSession && now < backgroundWakeExpiresAt)) {
                    return@AudioRecorder
                }'''),
    ('// Phrase grammar: allow single-word "jarvis", multi-syllable wake phrases, and [unk]',
     '// Phrase grammar: allow single-word "misri", multi-syllable wake phrases, and [unk]'),
    ('''            val isMatch = cleanPhrase in VALID_WAKE_PHRASES ||
                    VALID_WAKE_PHRASES.any { cleanPhrase.contains(it) } ||
                    wordList.contains("jarvis")''',
     '''            val isMatch = cleanPhrase in VALID_WAKE_PHRASES ||
                    VALID_WAKE_PHRASES.any { cleanPhrase.contains(it) }'''),
    ('''            // 4. Acoustic Verification:
            // 4. Acoustic Verification:
            // "jarvis" must be specifically recognized with confidence
            val jarvisConf = wordConfMap["jarvis"] ?: 0.0
            if (jarvisConf < 0.70) {
                Log.w("JarvisService", "WAKE REJECTED: 'jarvis' word confidence too low ($jarvisConf < 0.70). Ignored.")
                return
            }

            if (wordList.size == 1 && wordList[0] == "jarvis") {
                // Standalone single-word "jarvis"
                if (avgConf < 0.75) {
                    Log.w("JarvisService", "WAKE REJECTED: Standalone 'jarvis' failed checks (conf=$avgConf). Ignored.")
                    return
                }
            } else {
                // Multi-word phrases like "hey jarvis", "hello jarvis", "ok jarvis"
                if (avgConf < 0.65) {
                    Log.w("JarvisService", "WAKE REJECTED: Wake phrase failed checks (conf=$avgConf). Ignored.")
                    return
                }
            }''',
     '''            // 4. Acoustic Verification (v4: was checking the old word "jarvis",
            // which made a spoken "misri" impossible to verify - now checks "misri"):
            // "misri" must be specifically recognized with confidence
            val misriConf = wordConfMap["misri"] ?: 0.0
            if (misriConf < 0.70) {
                Log.w("JarvisService", "WAKE REJECTED: 'misri' word confidence too low ($misriConf < 0.70). Ignored.")
                return
            }

            if (wordList.size == 1 && wordList[0] == "misri") {
                // Standalone single-word "misri"
                if (avgConf < 0.75) {
                    Log.w("JarvisService", "WAKE REJECTED: Standalone 'misri' failed checks (conf=$avgConf). Ignored.")
                    return
                }
            } else {
                // Multi-word phrases like "hey misri", "hello misri", "ok misri"
                if (avgConf < 0.65) {
                    Log.w("JarvisService", "WAKE REJECTED: Wake phrase failed checks (conf=$avgConf). Ignored.")
                    return
                }
            }'''),
])
print("Phase 4 done.")


# === Phase 5 (v5, user feedback 2026-11-04: Live Weather --C / Tap to retry) ===
# Root cause found in LocationWeatherHelper + HomeFragment (Run #4 screenshot):
#  fetchWeather() returns only null on ANY failure, and HomeFragment then shows
#  the same generic "Live Weather • Tap to retry" with no reason. So the user
#  cannot tell whether permission was denied, phone Location is OFF, no GPS/
#  network fix arrived, or Open-Meteo failed - and tapping retry just fails
#  again the same way. Two concrete weaknesses made failure likely on a phone:
#  (a) the fresh-location request timed out after only 4 seconds, too short
#      for a cold GPS/network fix; (b) if the permission dialog was denied
#      once (or permanently), re-launching the permission launcher shows no
#      dialog at all, so it looked like the app "never asks for permission".
# Fix:
#  - LocationWeatherHelper now records lastFailureReason ("permission",
#    "location_disabled", "no_location", "network") instead of a silent null,
#    and the fresh-location timeout is 4 s -> 15 s.
#  - HomeFragment shows a DIFFERENT, actionable message per reason: Location
#    OFF -> tap opens the phone Location Settings; permission denied once ->
#    next tap opens the app's Settings page (where Location can be allowed
#    manually); network/API failure -> check internet and tap to retry.
#  - On success the weather card shows temperature, condition + city and
#    details exactly as before (Open-Meteo, no hard-coded city, no API key).
WEATHER = APP / "util/LocationWeatherHelper.kt"
replace(WEATHER, [
    ('''object LocationWeatherHelper {

    private const val TAG = "LocationWeatherHelper"''',
     '''object LocationWeatherHelper {

    // v5: why the last fetch failed ("", "permission", "location_disabled",
    // "no_location", "network") - HomeFragment shows an actionable message.
    var lastFailureReason: String = ""
        private set

    private const val TAG = "LocationWeatherHelper"'''),
    ('''    suspend fun fetchWeather(context: Context): WeatherData? = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return@withContext null
        }

        val location = getBestLocation(context)
        if (location == null) {
            Log.w(TAG, "No device location available; refusing to show weather for a hard-coded location")
            return@withContext null
        }''',
     '''    suspend fun fetchWeather(context: Context): WeatherData? = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            lastFailureReason = "permission"
            return@withContext null
        }

        val location = getBestLocation(context)
        if (location == null) {
            Log.w(TAG, "No device location available; refusing to show weather for a hard-coded location")
            if (lastFailureReason.isEmpty()) lastFailureReason = "no_location"
            return@withContext null
        }'''),
    ('''        // 2. Request a quick single location update with 4-second timeout
        val freshLocation = withTimeoutOrNull(4000L) {''',
     '''        // 2. Request a quick single location update with 15-second timeout
        // (v5: 4 s was too short for a cold GPS/network fix on a phone)
        val freshLocation = withTimeoutOrNull(15000L) {'''),
    ('''                    if (provider != null) {
                        LocationManagerCompat.requestLocationUpdates(locationManager, provider, request, listener, Looper.getMainLooper())
                    } else {
                        cont.resume(null)
                    }''',
     '''                    if (provider != null) {
                        LocationManagerCompat.requestLocationUpdates(locationManager, provider, request, listener, Looper.getMainLooper())
                    } else {
                        // v5: no provider enabled = phone Location is OFF
                        lastFailureReason = "location_disabled"
                        cont.resume(null)
                    }'''),
    ('''        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null''',
     '''        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    lastFailureReason = "network"
                    return null
                }'''),
    ('''        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather from Open-Meteo: ${e.message}")
            null
        }''',
     '''        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather from Open-Meteo: ${e.message}")
            lastFailureReason = "network"
            null
        }'''),
    ('''                WeatherData(
                    temperature = tempStr,''',
     '''                lastFailureReason = ""
                WeatherData(
                    temperature = tempStr,'''),
])
HOME = APP / "ui/home/HomeFragment.kt"
replace(HOME, [
    ('import android.content.Intent',
     'import android.content.Intent\nimport android.net.Uri\nimport android.provider.Settings'),
    ('''    private var lastAudioPushTime = 0L''',
     '''    private var lastAudioPushTime = 0L
    // v5 weather: next tap opens App Settings if permission was denied once
    // (Android then shows no permission dialog), or Location Settings if
    // the last fetch failed because phone Location is OFF / no fix.
    private var locationPermissionDeniedOnce = false
    private var weatherNeedsLocationSettings = false'''),
    ('''        if (fineGranted || coarseGranted) {
            fetchLiveWeather()
        } else {
            binding.tvWeatherCondition.text = "Location Permission Needed"
            binding.tvWeatherDetails.text = "Tap here to grant location for accurate weather"
        }''',
     '''        if (fineGranted || coarseGranted) {
            locationPermissionDeniedOnce = false
            fetchLiveWeather()
        } else {
            // v5: dialog may not appear again after a denial - tell the user
            // exactly where to allow it manually, and make the next tap open
            // the app Settings page instead of a silent re-request.
            locationPermissionDeniedOnce = true
            binding.tvWeatherCondition.text = "Location Permission Needed"
            binding.tvWeatherDetails.text = "Tap karke Settings > Apps > Misri > Permissions me Location Allow karo"
        }'''),
    ('''        // Weather Card Click -> Refresh or Request Permission
        binding.cardWeather.setOnClickListener {
            if (!PermissionHelper.hasLocationPermission(requireContext())) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                fetchLiveWeather()
            }
        }''',
     '''        // Weather Card Click -> Refresh or Request Permission (v5)
        binding.cardWeather.setOnClickListener {
            if (!PermissionHelper.hasLocationPermission(requireContext())) {
                if (locationPermissionDeniedOnce) {
                    // Permission dialog will not show again: open App Settings
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + requireContext().packageName)
                        )
                    )
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            } else if (weatherNeedsLocationSettings) {
                // Last fetch failed because phone Location is OFF: open it
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                fetchLiveWeather()
            }
        }'''),
    ('''    private fun checkLocationAndFetchWeather() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            fetchLiveWeather()
        } else {''',
     '''    private fun checkLocationAndFetchWeather() {
        if (PermissionHelper.hasLocationPermission(requireContext())) {
            locationPermissionDeniedOnce = false
            fetchLiveWeather()
        } else {'''),
    ('''            val weather = LocationWeatherHelper.fetchWeather(requireContext())
            if (_binding != null && weather != null) {
                binding.tvTemperature.text = weather.temperature
                binding.tvWeatherCondition.text = weather.condition
                binding.tvWeatherDetails.text = weather.details
                binding.ivWeatherIcon.setImageResource(weather.iconResId)
                // Keep Misri's context aligned with the city shown on Home.
                preferences.userLocation = weather.cityName
            } else if (_binding != null) {
                binding.tvWeatherCondition.text = "Live Weather • Tap to retry"
            }''',
     '''            val weather = LocationWeatherHelper.fetchWeather(requireContext())
            if (_binding != null && weather != null) {
                weatherNeedsLocationSettings = false
                binding.tvTemperature.text = weather.temperature
                binding.tvWeatherCondition.text = weather.condition
                binding.tvWeatherDetails.text = weather.details
                binding.ivWeatherIcon.setImageResource(weather.iconResId)
                // Keep Misri's context aligned with the city shown on Home.
                preferences.userLocation = weather.cityName
            } else if (_binding != null) {
                // v5: show WHY it failed, with an action the user can take,
                // instead of the same generic "Tap to retry" every time.
                when (LocationWeatherHelper.lastFailureReason) {
                    "location_disabled", "no_location" -> {
                        weatherNeedsLocationSettings = true
                        binding.tvWeatherCondition.text = "Location ON karo • Tap to open Settings"
                        binding.tvWeatherDetails.text = "Phone ki Location off hai ya fix nahi mila - Settings me Location on karke phir tap karo"
                    }
                    "permission" -> {
                        weatherNeedsLocationSettings = false
                        binding.tvWeatherCondition.text = "Location Permission Needed"
                        binding.tvWeatherDetails.text = "Tap karke Settings > Apps > Misri > Permissions me Location Allow karo"
                    }
                    "network" -> {
                        weatherNeedsLocationSettings = false
                        binding.tvWeatherCondition.text = "Weather service se jawab nahi mila"
                        binding.tvWeatherDetails.text = "Internet check karke tap to retry karo"
                    }
                    else -> {
                        weatherNeedsLocationSettings = false
                        binding.tvWeatherCondition.text = "Live Weather • Tap to retry"
                    }
                }
            }'''),
])
print("Phase 5 done.")
